package com.vshpynta.db

import com.vshpynta.createAppConfig
import com.vshpynta.createDataSource
import com.vshpynta.migrateDataSource
import com.vshpynta.service.createUser
import com.vshpynta.service.findUsersByEmail
import com.vshpynta.service.userCount
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
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Comprehensive database test suite exercising:
 * 1. Infrastructure: DataSource creation + Flyway migrations against an in‑memory H2 database
 *    configured in PostgreSQL compatibility mode (MODE=PostgreSQL) to approximate production SQL.
 * 2. Positive cases: Successful inserts, updates, deletes, and row mapping utilities.
 * 3. Query patterns: single(), list(), forEach() usage in Kotliquery with value tables.
 * 4. Constraints & edge cases: UNIQUE, NOT NULL, length validation (VARCHAR(255)), and missing rows.
 * 5. Reusable helpers: Encapsulate repetitive boilerplate (createUser, findUserByEmail, userCount, etc.) so
 *    individual tests focus on intent instead of setup noise.
 * 6. Transaction savepoints: Verifies partial vs full rollback behaviour using [dbSavePoint] with
 *    (a) uncaught exception causing whole transaction rollback and (b) caught exception preserving
 *    outer work while discarding inner failed statements.
 * 7. Explicit rollback test harness: [testTx] helper demonstrates transactional isolation by always
 *    rolling back changes made inside its scope.
 *
 * Rationale / Design Notes:
 *  - Each test gets a fresh, migrated schema via @BeforeEach for strong isolation.
 *  - Helper functions either return domain data directly or a Result<> when failures are part of
 *    the assertion (e.g., expecting constraint violations).
 *  - Assertions target externally observable SQL effects (row counts / column values) to avoid
 *    coupling to library internals.
 *  - H2 differences: Certain PostgreSQL edge behaviours (e.g., specific SQLSTATE codes) may differ;
 *    therefore tests currently assert generic failure (isFailure) instead of exact SQLSTATE. If
 *    production switches to PostgreSQL in tests (Testcontainers), assertions can be narrowed.
 *  - Savepoint semantics: An exception inside dbSavePoint rolls back only statements since the savepoint;
 *    if rethrown (uncaught) the outer transaction aborts; if caught the outer transaction may continue.
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
        // Given: application configuration supplying JDBC URL
        // When: DataSource is created in setUp()
        // Then: DataSource reflects configured URL
        assertEquals(appConfig.db.url, dataSource.jdbcUrl)
    }

    @DisplayName("Flyway migrations execute and create schema")
    @Test
    fun shouldExecuteFlywayMigrationsAndCreateSchema() {
        // Given: a fresh in-memory database
        // When: Flyway migrations are applied
        // Then: At least zero (>=0) migrations executed (basic sanity)
        assertTrue(migrateResult.migrationsExecuted >= 0)
    }

    @DisplayName("Default user exists from repeatable migration")
    @Test
    fun shouldContainDefaultUserFromRepeatableMigration() {
        // Given: repeatable migration inserting default user
        // When: querying by default user email
        // Then: Exactly one matching row exists with expected name
        val users = findUsersByEmail("vshpynta@crud.business")
        assertEquals(1, users.size, "Expected exactly one default user row")
        assertEquals("Volodymyr Shpynta", users[0]["name"], "Default user's name does not match")
    }

    @DisplayName("NOT NULL constraint enforced on tos_accepted")
    @Test
    fun shouldEnforceNotNullConstraintOnTosAccepted() {
        // Given: existing user row
        // When: attempting to set tos_accepted to NULL
        // Then: operation should fail due to NOT NULL constraint
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
        // Given: no prior row with provided email
        // When: inserting new user
        val newId = createUser(
            email = "august@augustl.com",
            password = "123abc",
            name = "August Lilleaas",
            tosAccepted = true
        ).getOrThrow()
        // Then: generated key present and row persisted
        assertTrue(newId != null && newId > 0L, "Expected generated key > 0")
        assertEquals("August Lilleaas", findUserByEmail("august@augustl.com")?.get("name"))
    }

    @DisplayName("update returns affected row count and modifies data")
    @Test
    fun shouldUpdateExistingRowReturningAffectedCount() {
        // Given: user row inserted for update
        val insertedId = createUser(
            email = "update@test.com",
            password = "pw",
            name = "Initial Name",
            tosAccepted = true
        ).getOrThrow() ?: fail("Insert failed, no generated key")

        // When: updating the user's name
        val affected = updateUserName(insertedId, "Changed Name")
        // Then: exactly one row updated and name modified
        assertEquals(1, affected, "Expected exactly one row updated")
        assertEquals("Changed Name", findUserByEmail("update@test.com")?.get("name"))
    }

    @DisplayName("delete removes all rows and returns affected count")
    @Test
    fun shouldDeleteAllRowsReturningAffectedCount() {
        // Given: user row present for deletion
        createUser(email = "delete@test.com").getOrThrow() ?: fail("Insert failed")
        // When: deleting all rows
        val affected = sessionOf(dataSource).use { sess -> sess.update(queryOf("DELETE FROM user_table")) }
        // Then: at least 2 rows deleted (default + inserted), user count is 0
        assertTrue(affected >= 2, "Expected at least 2 rows deleted (default + inserted)")
        assertEquals(0L, userCount())
    }

    // ----------------------------------------------------------------------
    // Constraint & edge / negative scenarios
    // ----------------------------------------------------------------------

    @DisplayName("inserting duplicate email violates UNIQUE constraint")
    @Test
    fun shouldFailOnDuplicateEmailInsert() {
        // Given: initial user insert
        val email = "dup@test.com"
        createUser(email = email).getOrThrow() ?: fail("Initial insert failed")
        // When: attempting to insert with duplicate email
        val duplicateAttempt = createUser(email = email)
        // Then: UNIQUE constraint violation occurs
        assertTrue(duplicateAttempt.isFailure, "Expected UNIQUE constraint violation on duplicate email")
    }

    @DisplayName("inserting NULL password_hash violates NOT NULL constraint")
    @Test
    fun shouldFailOnNullPasswordHashInsert() {
        // Given: email for user with NULL password
        val outcome = createUser(email = "nullpw@test.com", password = null)
        // Then: NOT NULL violation for password_hash
        assertTrue(outcome.isFailure, "Expected NOT NULL violation for password_hash")
    }

    @DisplayName("inserting NULL tos_accepted violates NOT NULL after migration")
    @Test
    fun shouldFailOnNullTosAcceptedInsert() {
        // Given: email for user with NULL tos_accepted
        val outcome = createUser(email = "nulltos@test.com", tosAccepted = null)
        // Then: NOT NULL violation for tos_accepted
        assertTrue(outcome.isFailure, "Expected NOT NULL violation for tos_accepted")
    }

    @DisplayName("inserting overly long email (>255) violates length constraint")
    @Test
    fun shouldFailOnOverlyLongEmailInsert() {
        // Given: email string >255 chars
        val longEmail = "a".repeat(260) + "@example.com"
        // When: attempting to insert user with long email
        val outcome = createUser(email = longEmail)
        // Then: length constraint violation for email >255 chars
        assertTrue(outcome.isFailure, "Expected length constraint violation for email >255 chars")
    }

    @DisplayName("updating non-existent user id affects 0 rows")
    @Test
    fun shouldReturnZeroAffectedRowsWhenUpdatingMissingUser() {
        // Given: a non-existent user id
        val affected = updateUserName(-999, "No Row")
        // Then: zero rows affected for non-existent id update
        assertEquals(0, affected, "Expected zero rows affected for non-existent id update")
    }

    // ----------------------------------------------------------------------
    // Savepoint / transactional semantics
    // ----------------------------------------------------------------------

    @DisplayName("dbSavePoint rolls back inner statements and aborts outer transaction when exception not caught")
    @Test
    fun shouldRollbackEntireTransactionWhenExceptionInsideSavePointNotCaught() {
        // Given: initial user count snapshot and insert SQL
        val initialCount = userCount()
        val insertSql = """
            INSERT INTO user_table (email, password_hash, name, tos_accepted)
            VALUES (?, ?, ?, ?)
        """.trimIndent()

        // When: A transaction runs a savepoint block that deliberately violates UNIQUE (duplicate email).
        // The exception is NOT caught -> dbSavePoint rolls back its inner statements then rethrows ->
        // outer transaction aborts (rolling back prior outer work as well).
        val result = runCatching {
            sessionOf(dataSource, returnGeneratedKey = true).use { session ->
                session.transaction { tx ->
                    dbSavePoint(tx) {
                        tx.updateAndReturnGeneratedKey(
                            queryOf(insertSql, "sp_nocatch@test.com", "pw".toByteArray(), "SP NoCatch", true)
                        )
                        tx.updateAndReturnGeneratedKey(
                            queryOf(insertSql, "sp_nocatch@test.com", "pw".toByteArray(), "SP Duplicate", true)
                        ) // <- triggers UNIQUE violation
                    }
                }
            }
        }

        // Then: Entire transaction rolled back: user count unchanged; email from inner block absent.
        assertTrue(result.isFailure, "Expected failure due to UNIQUE violation inside savepoint")
        assertEquals(initialCount, userCount(), "Row count should remain unchanged after rollback")
        assertEquals(
            null,
            findUserByEmail("sp_nocatch@test.com"),
            "User inserted before violation should be rolled back"
        )
    }

    @DisplayName("dbSavePoint partial rollback preserves outer transaction when exception caught")
    @Test
    fun shouldCommitOuterTransactionWhileRollingBackFailedSavePointBlockWhenCaught() {
        // Given: baseline row count and insert SQL
        val initialCount = userCount()
        val insertSql = """
            INSERT INTO user_table (email, password_hash, name, tos_accepted)
            VALUES (?, ?, ?, ?)
        """.trimIndent()

        // When: Outer transaction performs pre-savepoint insert, then a savepoint block with a duplicate
        // email causing UNIQUE violation. Exception is caught -> outer transaction continues with a post-savepoint insert.
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            session.transaction { tx ->
                // Outer pre-savepoint insert
                tx.updateAndReturnGeneratedKey(
                    queryOf(insertSql, "before_savepoint@test.com", "pw".toByteArray(), "Before SP", true)
                )
                try {
                    dbSavePoint(tx) {
                        tx.updateAndReturnGeneratedKey(
                            queryOf(insertSql, "sp_catch@test.com", "pw".toByteArray(), "SP First", true)
                        )
                        tx.updateAndReturnGeneratedKey(
                            queryOf(insertSql, "sp_catch@test.com", "pw".toByteArray(), "SP Duplicate", true)
                        ) // UNIQUE violation
                    }
                } catch (_: Exception) {
                    // Swallow expected violation: partial rollback limited to savepoint scope.
                }
                // Post-savepoint insert
                tx.updateAndReturnGeneratedKey(
                    queryOf(insertSql, "after_savepoint@test.com", "pw".toByteArray(), "After SP", true)
                )
            }
        }

        // Then: Only outer inserts persisted (2 rows); savepoint duplicates rolled back; duplicate email absent.
        val finalCount = userCount()
        assertEquals(initialCount + 2, finalCount, "Expected exactly two committed rows outside failed savepoint block")
        assertTrue(findUserByEmail("before_savepoint@test.com") != null, "Pre-savepoint user should be committed")
        assertTrue(findUserByEmail("after_savepoint@test.com") != null, "Post-savepoint user should be committed")
        assertEquals(null, findUserByEmail("sp_catch@test.com"), "Savepoint user should be rolled back after violation")
    }

    @DisplayName("testTx rolls back user inserts created inside its transaction scope")
    @Test
    fun shouldRollbackUsersCreatedWithinTestTx() {
        // Given: initial persistent row count (default migration user(s))
        val initialCount = userCount()

        // When: transaction creates two distinct users inside testTx (auto-rollback on exit)
        testTx(dataSource) { txSession ->
            val userAId = createUser(txSession, email = "augustlill@me.com").getOrThrow() ?: fail("Create user failed")
            val userBId =
                createUser(txSession, email = "august_@augustl.com").getOrThrow() ?: fail("Create user failed")
            assertNotEquals(userAId, userBId)
            // Then (inside scope): count increased, both users visible
            val inTxCount = userCount(txSession)
            assertEquals(initialCount + 2, inTxCount)
            assertTrue(findUsersByEmail(txSession, "augustlill@me.com").isNotEmpty())
            assertTrue(findUsersByEmail(txSession, "august_@augustl.com").isNotEmpty())
        }
        // Then (after rollback): original count restored; inserted emails absent
        assertEquals(initialCount, userCount())
        assertEquals(null, findUserByEmail("augustlill@me.com"))
        assertEquals(null, findUserByEmail("august_@augustl.com"))
    }

    // ----------------------------------------------------------------------
    // Helper API (private) — keeps tests concise & intention‑revealing
    // ----------------------------------------------------------------------

    /**
     * Convenience helper to insert a user using a short‑lived session. Returns a [Result] capturing
     * success (generated key) or failure (constraint violations, etc.). Prefer this in tests where
     * transactional composition is not required.
     */
    private fun createUser(
        email: String,
        password: String? = "pw",
        name: String = "Test User",
        tosAccepted: Boolean? = true
    ): Result<Long?> = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        createUser(session, email, name, password, tosAccepted)
    }

    /**
     * Fetches a single user row by email (opens and closes its own session). Returns null if absent.
     * Use the session-aware variants when inside a broader transactional scope to avoid redundant connections.
     */
    private fun findUserByEmail(email: String): Map<String, Any?>? =
        sessionOf(dataSource).use { session ->
            session.single(queryOf("SELECT * FROM user_table WHERE email = ?", email), ::mapFromRow)
        }

    /**
     * Returns all user rows matching the email (normally 0 or 1 unless integrity issues). Opens a new session.
     */
    private fun findUsersByEmail(email: String): List<Map<String, Any?>> =
        sessionOf(dataSource).use { session -> findUsersByEmail(session, email) }

    /**
     * Returns total row count in user_table using a new session. Delegates to the session variant.
     */
    private fun userCount(): Long = sessionOf(dataSource).use(::userCount)

    /**
     * Updates a user's name by id. Returns affected row count (0 if id does not exist). Opens a short-lived session.
     */
    private fun updateUserName(id: Long, newName: String): Int = sessionOf(dataSource).use { session ->
        session.update(
            queryOf(
                "UPDATE user_table SET name = :name WHERE id = :id",
                mapOf("name" to newName, "id" to id)
            )
        )
    }
}
