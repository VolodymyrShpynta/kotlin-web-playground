package com.vshpynta.web.dto

import com.google.gson.annotations.Expose
import com.vshpynta.model.User
import java.time.ZonedDateTime

/**
 * Public-facing user DTO excluding sensitive fields (e.g. passwordHash).
 */
data class PublicUser(
    @Expose val id: Long,
    @Expose val createdAt: ZonedDateTime,
    @Expose val updatedAt: ZonedDateTime,
    @Expose val email: String,
    @Expose val tosAccepted: Boolean,
    @Expose val name: String?
) {
    companion object {
        fun fromDomain(user: User) = PublicUser(
            id = user.id,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
            email = user.email,
            tosAccepted = user.tosAccepted,
            name = user.name
        )
    }
}
