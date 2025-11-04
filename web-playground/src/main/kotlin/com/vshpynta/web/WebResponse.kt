package com.vshpynta.web

sealed class WebResponse {
    abstract val statusCode: Int
    abstract val headers: Map<String, List<String>>

    abstract fun copyResponse(
        statusCode: Int,
        headers: Map<String, List<String>>
    ): WebResponse

    fun header(headerName: String, headerValue: String): WebResponse =
        header(headerName, listOf(headerValue))

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

