package com.vshpynta.web

/**
 * Polymorphic abstraction over HTTP response payloads used by routing helpers.
 *
 * Instances are transformed into Ktor responses by `webResponse` (see KtorWebUtils).
 * Two concrete variants exist: [TextWebResponse] (plain text) and [JsonWebResponse] (serialized JSON).
 *
 * Design goals:
 * - Immutable: operations like [header] return new instances via [copyResponse].
 * - Header merging: calling [header] repeatedly appends values rather than replacing them.
 * - Case-insensitive header lookups: [headers] normalizes keys to lowercase, collapsing duplicates.
 */
sealed class WebResponse {
    /** HTTP status code (e.g. 200, 404). */
    abstract val statusCode: Int
    /** Raw header map preserving insertion order of values for each key. */
    abstract val headers: Map<String, List<String>>

    /** Internal clone operation used by mutation helpers to preserve body polymorphism. */
    abstract fun copyResponse(
        statusCode: Int,
        headers: Map<String, List<String>>
    ): WebResponse

    /** Convenience overload for adding a single header value. */
    fun header(headerName: String, headerValue: String): WebResponse =
        header(headerName, listOf(headerValue))

    /**
     * Returns a new response with additional header values merged.
     * Existing values are preserved (multi-value headers allowed).
     */
    fun header(headerName: String, headerValue: List<String>): WebResponse =
        copyResponse(
            statusCode,
            headers.plus(
                Pair(
                    headerName,
                    headers.getOrDefault(headerName, listOf()) // preserve existing values
                        .plus(headerValue)
                )
            )
        )

    /**
     * Normalizes header names to lowercase and collapses duplicates across the original map.
     */
    fun headers(): Map<String, List<String>> =
        headers
            .map { (name, values) -> name.lowercase() to values }
            .fold(mapOf()) { result, (name, values) ->
                result.plus(
                    Pair(
                        name,
                        result.getOrDefault(name, listOf()) + values
                    )
                )
            }
}

/** Plain text response wrapper. */
data class TextWebResponse(
    val body: String,
    override val statusCode: Int = 200,
    override val headers: Map<String, List<String>> = mapOf()
) : WebResponse() {
    override fun copyResponse(
        statusCode: Int,
        headers: Map<String, List<String>>
    ): WebResponse =
        copy(body = body, statusCode = statusCode, headers = headers)
}

/** JSON response wrapper; body can be any Gson-serializable type. */
data class JsonWebResponse(
    val body: Any?,
    override val statusCode: Int = 200,
    override val headers: Map<String, List<String>> = mapOf()
) : WebResponse() {
    override fun copyResponse(
        statusCode: Int,
        headers: Map<String, List<String>>
    ): WebResponse =
        copy(body = body, statusCode = statusCode, headers = headers)
}

