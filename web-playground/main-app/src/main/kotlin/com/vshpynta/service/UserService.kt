package com.vshpynta.service

import com.vshpynta.db.mapFromRow
import com.vshpynta.db.mapping.fromRow
import com.vshpynta.model.User
import com.vshpynta.security.hashPassword
import com.vshpynta.security.passwordMatches
import kotliquery.Session
import kotliquery.queryOf

/**
 * Reusable parameterized INSERT statement used by [createUser] helper methods.
 * Keeps SQL definition centralized for consistency and easier refactoring.
 */
private val insertUserSql = """
        INSERT INTO user_table (email, password_hash, name, tos_accepted)
        VALUES (?, ?, ?, ?)
    """.trimIndent()

/**
 * Low-level insert using an existing [Session] (can be transactional). Accepts nullable password
 * to exercise NOT NULL constraint test paths. Returns generated key or failure wrapped in [Result].
 *
 * @param dbSession Active database session (does not close the session)
 * @param email User's email address (must be unique)
 * @param name User's display name, defaults to "Volodymyr Shpynta"
 * @param passwordText Plain text password to be hashed, nullable for constraint testing
 * @param tosAccepted Whether user accepted terms of service, defaults to true
 * @return [Result] containing the generated user ID or an exception
 */
fun createUser(
    dbSession: Session,
    email: String,
    name: String? = "Volodymyr Shpynta",
    passwordText: String? = "1234",
    tosAccepted: Boolean? = true
): Result<Long?> {

    val passwordHash = passwordText?.let(::hashPassword)

    return runCatching {
        dbSession.updateAndReturnGeneratedKey(
            queryOf(insertUserSql, email, passwordHash, name, tosAccepted)
        )
    }
}

/**
 * Session-aware variant of user lookup that does not close the provided session (useful inside transactions).
 *
 * @param dbSession Active database session (does not close the session)
 * @param email Email address to search for
 * @return List of user records matching the email, mapped to key-value pairs
 */
fun findUsersByEmail(dbSession: Session, email: String): List<Map<String, Any?>> =
    dbSession.list(queryOf("SELECT * FROM user_table WHERE email = ?", email), ::mapFromRow)

/**
 * Session-aware variant of user lookup by ID that does not close the provided session (useful inside transactions).
 *
 * @param dbSession Active database session (does not close the session)
 * @param userId Unique user ID to search for
 * @return User domain object if found, null otherwise
 */
fun findUserById(dbSession: Session, userId: Long): User? =
    dbSession.single(
        queryOf("SELECT * FROM user_table WHERE id = ?", userId),
        ::mapFromRow
    )?.let { User.fromRow(it) }

/**
 * Session-aware row count for reuse within transactional test scopes (avoids opening extra sessions).
 *
 * @param dbSession Active database session (does not close the session)
 * @return Total number of users in the user_table
 */
fun userCount(dbSession: Session): Long =
    dbSession.single(queryOf("SELECT COUNT(*) AS c FROM user_table"), ::mapFromRow)!!["c"] as Long

/**
 * Authenticates a user by verifying their email and password.
 *
 * @param dbSession Active database session (does not close the session)
 * @param email User's email address
 * @param passwordText Plain text password to verify
 * @return User ID if authentication succeeds, null otherwise
 */
fun authenticateUser(
    dbSession: Session,
    email: String,
    passwordText: String
): Long? {
    return dbSession.single(
        queryOf("SELECT * FROM user_table WHERE email = ?", email),
        ::mapFromRow
    )?.let {
        val pwHash = it["password_hash"] as ByteArray
        return if (passwordMatches(passwordText, pwHash)
        ) {
            it["id"] as Long
        } else {
            null
        }
    }
}
