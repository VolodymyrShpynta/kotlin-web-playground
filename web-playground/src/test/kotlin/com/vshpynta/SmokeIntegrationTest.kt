package com.vshpynta

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.withCharset
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.net.ServerSocket
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

/**
 * Smoke tests against a real embedded Netty engine.
 *
 * Optimized to start the server only once for the whole class (can noticeably speed up test suite
 * when the number of tests grows). We use JUnit 5's PER_CLASS test instance lifecycle so
 * @BeforeAll / @AfterAll can be non-static regular methods in Kotlin.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SmokeIntegrationTest {

    private lateinit var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>
    private lateinit var client: HttpClient
    private var port: Int = -1

    @BeforeAll
    fun startServer() {
        port = ServerSocket(0).use { it.localPort } // Ephemeral free port
        server = embeddedServer(Netty, port = port) { module() }.start(wait = false)
        client = HttpClient(CIO)
    }

    @AfterAll
    fun stopServer() {
        runCatching { client.close() }
        runCatching { server.stop(gracePeriodMillis = 100, timeoutMillis = 1000) }
    }

    private suspend fun get(pathAndQuery: String): HttpResponse =
        client.get("http://localhost:$port$pathAndQuery")

    @Test
    fun `GET - root returns Hello, World!`() = runTest(timeout = 10.seconds) {
        // Given: root endpoint path
        val path = "/"

        // When: executing GET request
        val res = get(path)

        // Then: status, body and content type are correct
        assertEquals(200, res.status.value)
        assertEquals("Hello, World!", res.bodyAsText())
        assertEquals(ContentType.Text.Plain.withCharset(Charsets.UTF_8), res.contentType())
    }

    @Test
    fun `GET - param_test returns provided param value`() = runTest(timeout = 10.seconds) {
        // Given: endpoint with query param foo=abc123
        val path = "/param_test?foo=abc123"

        // When
        val res = get(path)

        // Then
        assertEquals(200, res.status.value)
        assertEquals("The param is: abc123", res.bodyAsText())
    }

    @Test
    fun `GET - param_test returns null when param missing`() = runTest(timeout = 10.seconds) {
        // Given: endpoint without foo query parameter
        val path = "/param_test"

        // When
        val res = get(path)

        // Then: body reflects missing param as null
        assertEquals(200, res.status.value)
        assertEquals("The param is: null", res.bodyAsText())
    }

    @Test
    fun `GET - json_test returns JSON payload`() = runTest(timeout = 10.seconds) {
        // Given: json_test endpoint
        val path = "/json_test"

        // When
        val res = get(path)

        // Then
        assertEquals(200, res.status.value)
        assertEquals(ContentType.Application.Json.withCharset(Charsets.UTF_8), res.contentType())
        // Gson renders mapOf("foo" to "bar") as {"foo":"bar"}
        assertEquals("""{"foo":"bar"}""", res.bodyAsText())
    }

    @Test
    fun `GET - json_test_with_header returns JSON payload and custom header`() = runTest(timeout = 10.seconds) {
        // Given: json_test_with_header endpoint
        val path = "/json_test_with_header"

        // When
        val res = get(path)

        // Then: verify body and custom header
        assertEquals(200, res.status.value)
        assertEquals(ContentType.Application.Json.withCharset(Charsets.UTF_8), res.contentType())
        assertEquals("""{"foo":"bar"}""", res.bodyAsText())
        // Header names are lowercased when set by webResponse helper
        assertEquals("Just a test!", res.headers["x-test-header"])
    }
}
