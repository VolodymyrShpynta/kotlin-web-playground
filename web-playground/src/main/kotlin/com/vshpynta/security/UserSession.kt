package com.vshpynta.security

import kotlinx.serialization.Serializable
import java.security.Principal

/**
 * Represents an authenticated user session identified by a unique user ID.
 *
 * @property userId The unique identifier of the authenticated user.
 * @property csrfToken CSRF token for protecting sensitive operations.
 *                     Generated on login and validated on protected requests.
 * @constructor Creates a new [UserSession] with the specified [userId] and [csrfToken].
 * Serializable for session storage.
 */
@Serializable
data class UserSession(
    val userId: Long,
    val csrfToken: String
) : Principal {
    override fun getName(): String {
        return userId.toString()
    }
}
