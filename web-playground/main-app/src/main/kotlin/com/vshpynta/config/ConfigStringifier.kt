package com.vshpynta.config

import kotlin.reflect.full.declaredMemberProperties

/**
 * Utility for producing a sanitized multi-line string representation of a config object.
 *
 * Masks property values whose names match [secretsRegex] (case-insensitive) by preserving the
 * first two characters and replacing the remainder with asterisks (basic obfuscation meant for logs).
 *
 * Example output:
 * dbPassword = pa*****
 * httpPort = 8080
 *
 * Intended for diagnostic logging only; avoid using for security auditing.
 */
object ConfigStringifier {
    val secretsRegex = "password|secret|key"
        .toRegex(RegexOption.IGNORE_CASE)

    inline fun <reified T : Any> stringify(appConfig: T): String =
        T::class.declaredMemberProperties
            .sortedBy { it.name }
            .joinToString(separator = "\n") {
                if (secretsRegex.containsMatchIn(it.name)) {
                    "${it.name} = ${it.get(appConfig).toString().take(2)}*****"
                } else {
                    "${it.name} = ${it.get(appConfig)}"
                }
            }
}
