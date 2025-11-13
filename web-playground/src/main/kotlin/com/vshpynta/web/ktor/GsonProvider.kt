package com.vshpynta.web.ktor

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.nio.ByteBuffer
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64

/**
 * Centralized Gson instance with custom adapters for Java time and binary types to avoid
 * reflection issues on newer JVMs (JPMS encapsulation) and to produce a stable ISO-8601 JSON format.
 *
 * NOTE: ByteBuffer requires a hierarchy adapter because runtime concrete classes (HeapByteBuffer, DirectByteBuffer)
 * are subclasses of ByteBuffer; a simple registerTypeAdapter won't match those and Gson would fall back to reflection.
 */
object GsonProvider {

    private val zonedFormatter: DateTimeFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME
    private val offsetFormatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    private val zonedAdapter = object : JsonSerializer<ZonedDateTime>, JsonDeserializer<ZonedDateTime> {
        override fun serialize(src: ZonedDateTime?, typeOfSrc: Type?, context: JsonSerializationContext): JsonElement =
            JsonPrimitive(src?.format(zonedFormatter))

        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext
        ): ZonedDateTime? =
            json?.asString?.let { ZonedDateTime.parse(it, zonedFormatter) }
    }

    private val offsetAdapter = object : JsonSerializer<OffsetDateTime>, JsonDeserializer<OffsetDateTime> {
        override fun serialize(src: OffsetDateTime?, typeOfSrc: Type?, context: JsonSerializationContext): JsonElement =
            JsonPrimitive(src?.format(offsetFormatter))

        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext
        ): OffsetDateTime? =
            json?.asString?.let { OffsetDateTime.parse(it, offsetFormatter) }
    }

    private val byteBufferAdapter = object : JsonSerializer<ByteBuffer>, JsonDeserializer<ByteBuffer> {
        override fun serialize(src: ByteBuffer?, typeOfSrc: Type?, context: JsonSerializationContext): JsonElement {
            if (src == null) return JsonPrimitive(null as String?)
            val dup = src.asReadOnlyBuffer()
            dup.rewind()
            val bytes = ByteArray(dup.remaining())
            dup.get(bytes)
            return JsonPrimitive(Base64.getEncoder().encodeToString(bytes))
        }

        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext): ByteBuffer? =
            json?.asString?.let { ByteBuffer.wrap(Base64.getDecoder().decode(it)) }
    }

    private val byteArrayAdapter = object : JsonSerializer<ByteArray>, JsonDeserializer<ByteArray> {
        override fun serialize(src: ByteArray?, typeOfSrc: Type?, context: JsonSerializationContext): JsonElement =
            JsonPrimitive(src?.let { Base64.getEncoder().encodeToString(it) })

        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext): ByteArray? =
            json?.asString?.let { Base64.getDecoder().decode(it) }
    }

    val gson: Gson = GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .registerTypeAdapter(ZonedDateTime::class.java, zonedAdapter)
        .registerTypeAdapter(OffsetDateTime::class.java, offsetAdapter)
        .registerTypeHierarchyAdapter(ByteBuffer::class.java, byteBufferAdapter)
        .registerTypeAdapter(ByteArray::class.java, byteArrayAdapter)
        .disableHtmlEscaping()
        .create()
}
