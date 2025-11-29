package com.vshpynta.security

import kotlinx.serialization.Serializable
import java.security.Principal

/**
 * Represents an authenticated user session identified by a unique user ID.
 *
 * @property userId The unique identifier of the authenticated user.
 * @constructor Creates a new [UserSession] with the specified [userId].
 * Serializable for session storage.
 */
@Serializable
data class UserSession(val userId: Long) : Principal {
    override fun getName(): String {
        return userId.toString()
    }
}
