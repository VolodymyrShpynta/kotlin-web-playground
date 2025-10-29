package com.vshpynta.config

import kotlin.reflect.full.declaredMemberProperties

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
