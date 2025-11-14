package com.vshpynta.db.mapping

import com.vshpynta.model.User
import java.nio.ByteBuffer
import java.time.OffsetDateTime

/**
 * Maps a generic row Map (column -> value) into a [User] domain object.
 *
 * Assumptions:
 * - Timestamp columns (`created_at`, `updated_at`) are `OffsetDateTime` from JDBC driver and converted to ZonedDateTime.
 * - `password_hash` is a raw ByteArray representing the hashed password.
 * - All non-nullable fields must be present; missing or type-mismatched values will throw at cast time.
 *
 * Keep database-specific naming isolated here to avoid leaking persistence details into domain code.
 */
fun User.Companion.fromRow(row: Map<String, Any?>): User = User(
    id = row["id"] as Long,
    createdAt = (row["created_at"] as OffsetDateTime).toZonedDateTime(),
    updatedAt = (row["updated_at"] as OffsetDateTime).toZonedDateTime(),
    email = row["email"] as String,
    name = row["name"] as? String,
    tosAccepted = row["tos_accepted"] as Boolean,
    passwordHash = ByteBuffer.wrap(row["password_hash"] as ByteArray)
)
