package com.vshpynta.security

import at.favre.lib.crypto.bcrypt.BCrypt
import io.ktor.util.hex
import java.security.SecureRandom

/**
 * BCrypt hashing cost factor. Higher values increase security but also computation time.
 * Value of 10 provides a good balance between security and performance.
 */
private const val HASHING_COST = 10

/**
 * BCrypt hasher instance used for creating password hashes.
 */
private val bcryptHasher = BCrypt.withDefaults()

/**
 * BCrypt verifier instance used for validating passwords against stored hashes.
 */
private val bcryptVerifier = BCrypt.verifyer()

/**
 * Hashes a plain text password using BCrypt.
 *
 * @param password The plain text password to hash.
 * @return The resulting BCrypt hash as a byte array.
 */
fun hashPassword(password: String): ByteArray =
    bcryptHasher.hash(HASHING_COST, password.toByteArray(Charsets.UTF_8))

/**
 * Hashes a plain text password using BCrypt and encodes the result as a hexadecimal string.
 *
 * @param password The plain text password to hash.
 * @return The resulting BCrypt hash as a hexadecimal string.
 */
fun hashPasswordAsHex(password: String): String =
    hex(
        hashPassword(password)
    )

/**
 * Checks if a plain text password matches a BCrypt hash.
 *
 * @param password The plain text password to check.
 * @param bcryptHash The BCrypt hash as a byte array.
 * @return True if the password matches the hash, false otherwise.
 */
fun passwordMatches(password: String, bcryptHash: ByteArray): Boolean =
    bcryptVerifier.verify(
        password.toByteArray(Charsets.UTF_8),
        bcryptHash
    ).verified

/**
 * Generates a secure random byte array of the specified length and encodes it as a hexadecimal string.
 * @param length The length of the byte array.
 * @return A hexadecimal string representation of the random bytes.
 * NOTE: This function is useful for generating random tokens or keys.
 */
fun generateRandomBytesHex(length: Int) =
    ByteArray(length)
        .also { SecureRandom().nextBytes(it) }
        .let(::hex)
