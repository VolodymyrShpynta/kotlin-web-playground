package com.vshpynta.web.dto

import com.google.gson.annotations.Expose
import com.vshpynta.model.User
import com.vshpynta.web.dto.PublicUser.Companion.fromDomain
import java.time.ZonedDateTime

/**
 * Public-facing projection of [User] excluding sensitive fields like passwordHash.
 *
 * Use [fromDomain] to convert safely before outbound serialization. Keep this DTO minimal;
 * add view-specific fields (e.g. avatarUrl) by composing at the edge rather than bloating domain.
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

        /** Creates a [PublicUser] from a domain [User]. */
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
