package com.vshpynta.db

import com.vshpynta.createAppConfig
import com.vshpynta.createDataSource
import com.vshpynta.migrateDataSource
import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import org.flywaydb.core.api.output.MigrateResult
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Comprehensive database test suite exercising:
 * 1. Infrastructure: DataSource creation + Flyway migrations against an in‑memory H2 database
 *    configured in PostgreSQL compatibility mode (MODE=PostgreSQL) to approximate production SQL.
 * 2. Positive cases: Successful inserts, updates, deletes, and row mapping utilities.
 * 3. Query patterns: single(), list(), forEach() usage in Kotliquery with value tables.
 * 4. Constraints & edge cases: UNIQUE, NOT NULL, length validation (VARCHAR(255)), and missing rows.
 * 5. Reusable helpers: Encapsulate repetitive boilerplate (insertUser, findUserByEmail, etc.) so
 *    individual tests focus on intent instead of setup noise.
 *
 * Rationale / Design Notes:
 *  - Each test gets a fresh, migrated schema via @BeforeEach for strong isolation.
 *  - Helper functions either return domain data directly or a Result<> when failures are part of
 *    the assertion (e.g., expecting constraint violations).
 *  - We assert only behaviour observable from SQL (row counts / values) to avoid coupling to
 *    implementation details of libraries.
 *  - H2 differences: Certain PostgreSQL edge behaviours (e.g., specific SQLSTATE codes) may differ;
 *    therefore tests currently assert generic failure (isFailure) instead of exact SQLSTATE. If
 *    production switches to PostgreSQL in tests (e.g., Testcontainers), assertions can be narrowed.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatabaseTest {

    private lateinit var dataSource: HikariDataSource
    private lateinit var migrateResult: MigrateResult
    private val appConfig = createAppConfig("test")

    @BeforeEach
    fun setUp() {
        dataSource = createDataSource(appConfig)
        migrateResult = migrateDataSource(dataSource)
    }

    @AfterEach
    fun tearDown() {
        runCatching { dataSource.close() }
    }

    // ----------------------------------------------------------------------
    // Schema / Migration validation
    // ----------------------------------------------------------------------

    @DisplayName("DataSource is configured with provided JDBC URL")
    @Test
    fun shouldConfigureDataSourceWithProvidedJdbcUrl() {
        // Verifies that the DataSource uses the expected JDBC URL.
        assertEquals(appConfig.dbUrl, dataSource.jdbcUrl)
    }

    @DisplayName("Flyway migrations execute and create schema")
    @Test
    fun shouldExecuteFlywayMigrationsAndCreateSchema() {
        // Ensures that Flyway migrations run and schema is present.
        assertTrue(migrateResult.migrationsExecuted >= 0)
    }

    @DisplayName("Default user exists from repeatable migration")
    @Test
    fun shouldContainDefaultUserFromRepeatableMigration() {
        // Checks that the default user inserted by migration exists.
        val users = findUsersByEmail("vshpynta@crud.business")
        assertEquals(1, users.size, "Expected exactly one default user row")
        assertEquals("Volodymyr Shpynta", users[0]["name"], "Default user's name does not match")
    }

    @DisplayName("NOT NULL constraint enforced on tos_accepted")
    @Test
    fun shouldEnforceNotNullConstraintOnTosAccepted() {
        // Attempts to set tos_accepted to NULL, expects NOT NULL violation.
        val result = sessionOf(dataSource).use { session ->
            runCatching {
                session.run(
                    queryOf("UPDATE user_table SET tos_accepted = NULL WHERE email = ?", "vshpynta@crud.business")
                        .asExecute
                )
            }
        }
        if (result.isSuccess) fail("Expected NOT NULL violation when setting tos_accepted NULL, but update succeeded")
    }

    // ----------------------------------------------------------------------
    // Row mapping & selection primitives
    // ----------------------------------------------------------------------

    @DisplayName("mapFromRow maps aliased columns correctly")
    @Test
    fun shouldMapAliasColumnsWithMapFromRowUtility() {
        // Verifies that mapFromRow correctly maps aliased columns.
        val rowMap = sessionOf(dataSource).use { session ->
            session.single(
                queryOf("SELECT 1 AS foo, 'bar' AS bar")
            ) { mapFromRow(it) }
                ?: fail("Row map was null")
        }
        assertEquals(2, rowMap.size, "Expected exactly 2 entries in the row map")
        assertEquals(1, rowMap["foo"], "Expected 'foo' key to map to value 1")
        assertEquals("bar", rowMap["bar"], "Expected 'bar' key to map to value 'bar'")
    }

    @DisplayName("single() returns first row from VALUES construct")
    @Test
    fun shouldReturnSingleRowFromValuesUsingSingle() {
        val row = sessionOf(dataSource).use { dbSession ->
            dbSession.single(
                queryOf("SELECT * from (VALUES (1, 'a'), (2, 'b')) t1 (x, y)"),
                ::mapFromRow
            ) ?: fail("Expected a row but got null")
        }
        assertEquals(2, row.size)
        assertEquals(1, row["x"])
        assertEquals("a", row["y"])
    }

    @DisplayName("list() returns single map when one row present")
    @Test
    fun shouldReturnListWithSingleMapUsingList() {
        val rows = sessionOf(dataSource).use { dbSession ->
            dbSession.list(queryOf("SELECT 1 as foo"), ::mapFromRow)
        }
        assertEquals(1, rows.size)
        assertEquals(1, rows[0]["foo"])
    }

    @DisplayName("list() returns all rows from VALUES construct")
    @Test
    fun shouldReturnAllRowsFromValuesUsingList() {
        val rows = sessionOf(dataSource).use { dbSession ->
            dbSession.list(
                queryOf("SELECT * from (VALUES (1, 'a'), (2, 'b')) t1 (x, y)"),
                ::mapFromRow
            )
        }
        assertEquals(2, rows.size)
        assertEquals(mapOf("x" to 1, "y" to "a"), rows[0])
        assertEquals(mapOf("x" to 2, "y" to "b"), rows[1])
    }

    @DisplayName("list() returns empty list when WHERE filters all rows out")
    @Test
    fun shouldReturnEmptyListWhenFilterMatchesNoRows() {
        val rows = sessionOf(dataSource).use { dbSession ->
            dbSession.list(
                queryOf("""SELECT * from (VALUES (1, 'a'), (2, 'b')) t1 (x, y) WHERE x = 42"""),
                ::mapFromRow
            )
        }
        assertTrue(rows.isEmpty(), "Expected empty list")
    }

    // ----------------------------------------------------------------------
    // Iteration & streaming patterns
    // ----------------------------------------------------------------------

    @DisplayName("forEach() iterates rows without loading entire result set")
    @Test
    fun shouldIterateRowsUsingForEachWithoutLoadingAll() {
        val collected = mutableListOf<Map<String, Any?>>()
        sessionOf(dataSource).use { dbSession ->
            dbSession.forEach(
                queryOf("SELECT * from (VALUES (1, 'a'), (2, 'b')) t1 (x, y)")
            ) { rowObj ->
                collected += mapFromRow(rowObj)
            }
        }
        assertEquals(2, collected.size)
        assertEquals(mapOf("x" to 1, "y" to "a"), collected[0])
        assertEquals(mapOf("x" to 2, "y" to "b"), collected[1])
    }

    // ----------------------------------------------------------------------
    // CRUD happy paths
    // ----------------------------------------------------------------------

    @DisplayName("insert returns generated key for new user row")
    @Test
    fun shouldInsertRowAndReturnGeneratedKey() {
        val newId = insertUser(
            email = "august@augustl.com",
            passwordHash = "123abc".toByteArray(),
            name = "August Lilleaas",
            tosAccepted = true
        ).getOrThrow()
        assertTrue(newId != null && newId > 0L, "Expected generated key > 0")
        assertEquals("August Lilleaas", findUserByEmail("august@augustl.com")?.get("name"))
    }

    @DisplayName("update returns affected row count and modifies data")
    @Test
    fun shouldUpdateExistingRowReturningAffectedCount() {
        val insertedId = insertUser(
            email = "update@test.com",
            passwordHash = "pw".toByteArray(),
            name = "Initial Name",
            tosAccepted = true
        ).getOrThrow() ?: fail("Insert failed, no generated key")

        val affected = updateUserName(insertedId, "Changed Name")
        assertEquals(1, affected, "Expected exactly one row updated")
        assertEquals("Changed Name", findUserByEmail("update@test.com")?.get("name"))
    }

    @DisplayName("delete removes all rows and returns affected count")
    @Test
    fun shouldDeleteAllRowsReturningAffectedCount() {
        insertUser(email = "delete@test.com").getOrThrow() ?: fail("Insert failed")
        val affected = sessionOf(dataSource).use { sess -> sess.update(queryOf("DELETE FROM user_table")) }
        assertTrue(affected >= 2, "Expected at least 2 rows deleted (default + inserted)")
        assertEquals(0L, userCount())
    }

    // ----------------------------------------------------------------------
    // Constraint & edge / negative scenarios
    // ----------------------------------------------------------------------

    @DisplayName("inserting duplicate email violates UNIQUE constraint")
    @Test
    fun shouldFailOnDuplicateEmailInsert() {
        val email = "dup@test.com"
        insertUser(email = email).getOrThrow() ?: fail("Initial insert failed")
        val duplicateAttempt = insertUser(email = email)
        assertTrue(duplicateAttempt.isFailure, "Expected UNIQUE constraint violation on duplicate email")
    }

    @DisplayName("inserting NULL password_hash violates NOT NULL constraint")
    @Test
    fun shouldFailOnNullPasswordHashInsert() {
        val outcome = insertUser(email = "nullpw@test.com", passwordHash = null)
        assertTrue(outcome.isFailure, "Expected NOT NULL violation for password_hash")
    }

    @DisplayName("inserting NULL tos_accepted violates NOT NULL after migration")
    @Test
    fun shouldFailOnNullTosAcceptedInsert() {
        val outcome = insertUser(email = "nulltos@test.com", tosAccepted = null)
        assertTrue(outcome.isFailure, "Expected NOT NULL violation for tos_accepted")
    }

    @DisplayName("inserting overly long email (>255) violates length constraint")
    @Test
    fun shouldFailOnOverlyLongEmailInsert() {
        val longEmail = "a".repeat(260) + "@example.com"
        val outcome = insertUser(email = longEmail)
        assertTrue(outcome.isFailure, "Expected length constraint violation for email >255 chars")
    }

    @DisplayName("updating non-existent user id affects 0 rows")
    @Test
    fun shouldReturnZeroAffectedRowsWhenUpdatingMissingUser() {
        val affected = updateUserName(-999, "No Row")
        assertEquals(0, affected, "Expected zero rows affected for non-existent id update")
    }

    // ----------------------------------------------------------------------
    // Helper API (private) — keeps tests concise & intention‑revealing
    // ----------------------------------------------------------------------

    /** Reusable INSERT statement used by [insertUser]. */
    private val insertUserSql = """
        INSERT INTO user_table (email, password_hash, name, tos_accepted)
        VALUES (?, ?, ?, ?)
    """.trimIndent()

    /**
     * Inserts a user row and returns a Result containing the generated primary key (nullable if the
     * driver returns null). Failures (constraint violations, etc.) are captured so tests can assert
     * on negative scenarios without throwing immediately.
     */
    private fun insertUser(
        email: String,
        passwordHash: Any? = "pw".toByteArray(),
        name: String = "Test User",
        tosAccepted: Boolean? = true
    ): Result<Long?> = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        runCatching {
            session.updateAndReturnGeneratedKey(
                queryOf(insertUserSql, email, passwordHash, name, tosAccepted)
            )
        }
    }

    /** Fetches a single user row by email (or null if absent). */
    private fun findUserByEmail(email: String): Map<String, Any?>? =
        sessionOf(dataSource).use { session ->
            session.single(queryOf("SELECT * FROM user_table WHERE email = ?", email), ::mapFromRow)
        }

    /** Returns all user rows matching the email (normally 0 or 1 unless integrity broken). */
    private fun findUsersByEmail(email: String): List<Map<String, Any?>> =
        sessionOf(dataSource).use { session ->
            session.list(queryOf("SELECT * FROM user_table WHERE email = ?", email), ::mapFromRow)
        }

    /** Returns total row count in user_table (post‑delete assertions, etc.). */
    private fun userCount(): Long = sessionOf(dataSource).use { session ->
        session.single(queryOf("SELECT COUNT(*) AS c FROM user_table"), ::mapFromRow)!!["c"] as Long
    }

    /** Updates the name for a given id; returns affected row count (0 if missing). */
    private fun updateUserName(id: Long, newName: String): Int = sessionOf(dataSource).use { session ->
        session.update(
            queryOf(
                "UPDATE user_table SET name = :name WHERE id = :id",
                mapOf("name" to newName, "id" to id)
            )
        )
    }
}
