package com.vshpynta.web.ktor

import com.vshpynta.web.serialization.GsonProvider
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.withCharset

/**
 * Outgoing Ktor content for JSON responses using the shared [com.vshpynta.web.serialization.GsonProvider].
 *
 * Converts the provided [body] object to UTF-8 encoded JSON byte array. Serialization adapters
 * registered in [com.vshpynta.web.serialization.GsonProvider] handle time and binary types safely.
 */
class KtorJsonWebResponse(
    val body: Any?,
    override val status: HttpStatusCode = HttpStatusCode.OK
) : OutgoingContent.ByteArrayContent() {

    override val contentType: ContentType = ContentType.Application.Json.withCharset(Charsets.UTF_8)

    override fun bytes(): ByteArray {
        // Use custom Gson with adapters for ZonedDateTime, OffsetDateTime, ByteBuffer, etc., to avoid JPMS reflection issues.
        return GsonProvider.gson.toJson(body).toByteArray(Charsets.UTF_8)
    }
}
