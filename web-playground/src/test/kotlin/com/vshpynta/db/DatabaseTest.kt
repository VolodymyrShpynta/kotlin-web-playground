package com.vshpynta.db

import com.vshpynta.config.WebappConfig
import com.vshpynta.createDataSource
import com.vshpynta.migrateDataSource
import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import org.flywaydb.core.api.output.MigrateResult
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

private const val JDBC_URL = "jdbc:h2:mem:playground;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"

/**
 * Unit tests for database connectivity, schema migration, and basic queries.
 *
 * Test coverage:
 *  - DataSource creation and configuration
 *  - Flyway migration execution and schema presence
 *  - Default user existence from repeatable migration
 *  - NOT NULL constraint enforcement
 *  - mapFromRow utility mapping
 *
 * Uses an in-memory H2 database for isolation and speed.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatabaseTest {

    private lateinit var dataSource: HikariDataSource
    private lateinit var migrateResult: MigrateResult

    @BeforeEach
    fun setUp() {
        val cfg = WebappConfig(
            httpPort = 0,
            dbUrl = JDBC_URL,
            dbUser = "",
            dbPassword = ""
        )
        dataSource = createDataSource(cfg)
        migrateResult = migrateDataSource(dataSource)
    }

    @AfterEach
    fun tearDown() {
        runCatching { dataSource.close() }
    }

    @Test
    fun shouldConfigureDataSourceWithProvidedJdbcUrl() {
        // Verifies that the DataSource uses the expected JDBC URL.
        assertEquals(JDBC_URL, dataSource.jdbcUrl)
    }

    @Test
    fun shouldExecuteFlywayMigrationsAndCreateSchema() {
        // Ensures that Flyway migrations run and schema is present.
        assertTrue(migrateResult.migrationsExecuted > 0)
    }

    @Test
    fun shouldContainDefaultUserFromRepeatableMigration() {
        // Checks that the default user inserted by migration exists.
        val users = sessionOf(dataSource).use { session ->
            session.list(
                queryOf(
                    "SELECT * FROM user_table WHERE email = :email",
                    mapOf("email" to "vshpynta@crud.business")
                )
            ) {
                mapFromRow(it)
            }
        }
        assertEquals(1, users.size, "Expected exactly one default user row")
        assertEquals("Volodymyr Shpynta", users[0]["name"], "Default user's name does not match")
    }

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
}
