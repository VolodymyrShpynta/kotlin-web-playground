package com.vshpynta.web.ktor

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.withCharset

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
