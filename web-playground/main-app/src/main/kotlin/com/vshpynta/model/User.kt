package com.vshpynta.model

import java.nio.ByteBuffer
import java.time.ZonedDateTime

/**
 * Domain representation of an application user.
 *
 * This type is intentionally kept free of transport / JSON concerns. Public exposure of user
 * information should occur through a dedicated DTO (e.g. `PublicUser`) that omits sensitive fields
 * such as the raw password hash.
 *
 * Invariants / notes:
 * - All fields except [name] are non-null. A user must always have timestamps, an email, and a password hash.
 * - [passwordHash] stores the already-hashed password (never plain text). It is a binary value wrapped in
 *   a [ByteBuffer] for efficiency; treat its contents as opaque and never log them.
 * - Timestamps ([createdAt], [updatedAt]) use UTC or a consistent zone; business logic should enforce
 *   that `updatedAt >= createdAt`.
 * - Use a mapping layer or extension (e.g. `User.fromRow(row)` defined externally) to construct instances
 *   from persistence rows. This keeps database-specific details out of the domain model.
 * - Do NOT serialize this class directly to JSON; some serializers (e.g. Gson) can bypass Kotlin's
 *   null-safety via reflective / unsafe allocation. Always convert to a safe projection before exposure.
 *
 * Typical usage:
 * ```kotlin
 * val user: User = User.fromRow(dbRowMap)          // create from database row
 * val public = PublicUser.fromDomain(user)         // create safe external view
 * service.updateUser(user.copy(name = "New Name")) // immutable modifications
 * ```
 *
 * Future extensions:
 * - Add account status flags (locked, verified) via additional boolean fields or a sealed hierarchy.
 * - Wrap `email` in a value class for stronger validation guarantees.
 * - Replace [ByteBuffer] with a dedicated value class `PasswordHash` to clarify semantics and
 *   encapsulate hash algorithm metadata.
 *
 * @property id Stable primary key (database-generated).
 * @property createdAt Timestamp when the user record was first persisted.
 * @property updatedAt Timestamp of the last mutation to the user record.
 * @property email Unique, canonicalized user email address.
 * @property tosAccepted Indicates whether the user has accepted the Terms of Service.
 * @property name Optional display name; may be null if not provided.
 * @property passwordHash Opaque binary representation of the hashed password (never plain text).
 */
data class User(
    val id: Long,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
    val email: String,
    val tosAccepted: Boolean,
    val name: String?,
    val passwordHash: ByteBuffer
) {
    /**
     * Empty companion retained to allow external extension functions (e.g. `fun User.Companion.fromRow(...)`).
     * This avoids coupling construction logic (SQL column names, migration quirks) to the pure domain type.
     */
    companion object
}
