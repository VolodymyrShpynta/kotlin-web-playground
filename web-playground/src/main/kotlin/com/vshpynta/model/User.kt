package com.vshpynta.model

import java.nio.ByteBuffer
import java.time.ZonedDateTime

data class User(
    val id: Long,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
    val email: String,
    val tosAccepted: Boolean,
    val name: String?,
    val passwordHash: ByteBuffer
) {
    // Empty companion to allow extension factories elsewhere.
    companion object
}
