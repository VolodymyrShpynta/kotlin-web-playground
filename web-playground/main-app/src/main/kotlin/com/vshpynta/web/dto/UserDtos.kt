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

/**
 * Request payload for creating a new user via API.
 *
 * This DTO captures the necessary fields for user creation. It is separate from the domain
 * [User] type to avoid exposing sensitive fields (e.g. password hash) and to enforce
 * validation rules specific to the creation process.
 *
 * @property email User's email address (must be unique and valid).
 * @property password Plain text password to be hashed and stored securely.
 * @property tosAccepted Whether the user has accepted the Terms of Service (must be true).
 * @property name Optional display name for the user.
 */
data class CreateUserRequest(
    @Expose val email: String,
    @Expose val password: String,
    @Expose val tosAccepted: Boolean,
    @Expose val name: String?
)
