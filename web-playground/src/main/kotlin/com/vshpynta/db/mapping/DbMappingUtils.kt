package com.vshpynta.db.mapping

import com.vshpynta.model.User
import java.nio.ByteBuffer
import java.time.OffsetDateTime

fun User.Companion.fromRow(row: Map<String, Any?>): User = User(
    id = row["id"] as Long,
    createdAt = (row["created_at"] as OffsetDateTime).toZonedDateTime(),
    updatedAt = (row["updated_at"] as OffsetDateTime).toZonedDateTime(),
    email = row["email"] as String,
    name = row["name"] as? String,
    tosAccepted = row["tos_accepted"] as Boolean,
    passwordHash = ByteBuffer.wrap(row["password_hash"] as ByteArray)
)
