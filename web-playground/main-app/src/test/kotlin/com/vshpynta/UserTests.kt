package com.vshpynta

import com.vshpynta.db.mapFromRow
import com.vshpynta.db.testTx
import com.vshpynta.service.authenticateUser
import com.vshpynta.service.createUser
import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import org.flywaydb.core.api.output.MigrateResult
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Test suite for user management functionality including user creation, authentication,
 * and password security features.
 *
 * Uses a test database instance that is created fresh for each test via Flyway migrations.
 * All tests run within transactional contexts that automatically roll back after completion.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserTests {

    private lateinit var dataSource: HikariDataSource
    private lateinit var migrateResult: MigrateResult
    private val appConfig = createAppConfig("test")

    /** SQL query to retrieve a user record by their unique ID. */
    private val findUserByIdSql = "SELECT * FROM user_table WHERE id = ?"

    /**
     * Initializes a fresh test database before each test.
     * Creates a new HikariCP connection pool and runs Flyway migrations.
     */
    @BeforeEach
    fun setUp() {
        dataSource = createDataSource(appConfig)
        migrateResult = migrateDataSource(dataSource)
    }

    /**
     * Cleans up database resources after each test.
     * Closes the HikariCP connection pool safely.
     */
    @AfterEach
    fun tearDown() {
        runCatching { dataSource.close() }
    }

    /**
     * Tests user password authentication with correct and incorrect passwords.
     *
     * Verifies that:
     * - Correct email and password combination returns the user ID
     * - Incorrect password returns null
     * - Non-existent email returns null
     */
    @Test
    fun testUserPasswordAuthentication() = testTx(dataSource) { dbSess ->
        val userId = createUser(
            dbSess,
            email = "a@b.com",
            name = "August Lilleaas",
            passwordText = "1234",
            tosAccepted = true
        ).getOrNull()

        assertEquals(
            userId,
            authenticateUser(dbSess, "a@b.com", "1234")
        )
        assertEquals(
            null,
            authenticateUser(dbSess, "a@b.com", "incorrect")
        )
        assertEquals(
            null,
            authenticateUser(dbSess, "does@not.exist", "1234")
        )
    }

    /**
     * Tests authentication of the default user created by the repeatable migration.
     *
     * Verifies that:
     * - The default user from R__populate_default_user.sql can be authenticated
     * - The password hash stored in the migration file works correctly
     * - Authentication fails with an incorrect password
     */
    @Test
    fun testDefaultUserAuthentication() = testTx(dataSource) { dbSess ->
        // The default user is created by R__populate_default_user.sql migration
        val defaultUserEmail = "vshpynta@crud.business"
        val defaultPassword = "1234"

        // Verify the default user can authenticate with correct password
        val authenticatedUserId = authenticateUser(dbSess, defaultUserEmail, defaultPassword)
        assertEquals(
            expected = true,
            actual = authenticatedUserId != null,
            message = "Default user should authenticate successfully with correct password"
        )

        // Verify authentication fails with incorrect password
        assertEquals(
            null,
            authenticateUser(dbSess, defaultUserEmail, "wrongpassword"),
            "Default user authentication should fail with incorrect password"
        )
    }

    /**
     * Tests that BCrypt password hashing uses unique salts for each user.
     *
     * Creates two users with identical passwords and verifies that their
     * stored password hashes are different. This ensures that BCrypt's
     * salting mechanism is working correctly, preventing rainbow table attacks
     * and making it impossible to detect users with the same password.
     */
    @Test
    fun testUserPasswordSalting() = testTx(dataSource) { dbSess ->
        val userAId = createUser(
            dbSess,
            email = "a@b.com",
            name = "A",
            passwordText = "1234",
            tosAccepted = true
        ).getOrNull()

        val userBId = createUser(
            dbSess,
            email = "x@b.com",
            name = "X",
            passwordText = "1234",
            tosAccepted = true
        ).getOrNull()

        val userAHash = dbSess.single(
            queryOf(findUserByIdSql, userAId),
            ::mapFromRow
        )!!["password_hash"] as ByteArray

        val userBHash = dbSess.single(
            queryOf(findUserByIdSql, userBId),
            ::mapFromRow
        )!!["password_hash"] as ByteArray

        assertFalse(userAHash.contentEquals(userBHash))
    }
}
