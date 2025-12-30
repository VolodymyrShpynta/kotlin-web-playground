package com.vshpynta.web.validation

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import com.vshpynta.web.dto.CreateUserRequest
import com.vshpynta.web.dto.ValidationError

/**
 * Validates a [CreateUserRequest], ensuring all fields meet the required criteria.
 *
 * @param request The [CreateUserRequest] to validate.
 * @return An [Either] containing a [ValidationError] on failure or the validated [CreateUserRequest] on success.
 */
fun validateCreateUserRequest(
    request: CreateUserRequest
): Either<ValidationError, CreateUserRequest> = either {
    CreateUserRequest(
        email = validateEmail(request.email).bind(),
        password = validatePassword(request.password).bind(),
        tosAccepted = validateTosAccepted(request.tosAccepted).bind(),
        name = request.name
    )
}

/**
 * Validates an email address.
 *
 * @param email The email address to validate.
 * @return An [Either] containing a [ValidationError] on failure or the validated email [String] on success.
 */
fun validateEmail(email: Any?): Either<ValidationError, String> {
    if (email !is String) {
        return ValidationError("E-mail must be set").left()
    }
    if (!email.contains("@")) {
        return ValidationError("Invalid e-mail").left()
    }
    return email.right()
}

/**
 * Validates a password.
 *
 * @param password The password to validate.
 * @return An [Either] containing a [ValidationError] on failure or the validated password [String] on success.
 */
fun validatePassword(password: Any?): Either<ValidationError, String> {
    if (password !is String) {
        return ValidationError("Password must be set").left()
    }
    if (password == "1234") {
        return ValidationError("Insecure password").left()
    }
    return password.right()
}

/**
 * Validates the Terms of Service acceptance.
 *
 * @param tosAccepted The TOS acceptance value to validate.
 * @return An [Either] containing a [ValidationError] on failure or the validated TOS acceptance [Boolean] on success.
 */
fun validateTosAccepted(tosAccepted: Any?): Either<ValidationError, Boolean> {
    if (tosAccepted !is Boolean) {
        return ValidationError("TOS acceptance must be set").left()
    }
    if (!tosAccepted) {
        return ValidationError("TOS must be accepted").left()
    }
    return tosAccepted.right()
}
