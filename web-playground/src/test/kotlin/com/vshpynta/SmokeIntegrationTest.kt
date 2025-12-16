package com.vshpynta

import com.vshpynta.web.dto.PublicUser
import com.vshpynta.web.serialization.GsonProvider
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.net.ServerSocket
import java.time.ZonedDateTime
import javax.sql.DataSource
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
    private lateinit var dataSource: DataSource
    private var port: Int = -1
    private val appConfig = createAppConfig("test")

    @BeforeAll
    fun startServer() {
        // Ensure schema & default user exist before hitting DB-backed routes
        dataSource = createAndMigrateDataSource(appConfig)
        port = ServerSocket(0).use { it.localPort } // Ephemeral free port
        server = embeddedServer(Netty, port = port) {
            setUpKtorApplication(appConfig, dataSource)
        }.start(wait = false)
        client = HttpClient(CIO)
    }

    @AfterAll
    fun stopServer() {
        runCatching { client.close() }
        runCatching { server.stop(gracePeriodMillis = 100, timeoutMillis = 1000) }
    }

    private suspend fun get(pathAndQuery: String): HttpResponse =
        client.get("http://localhost:$port$pathAndQuery")

    @DisplayName("GET / returns Hello, World!")
    @Test
    fun shouldReturnHelloWorldOnRootGet() = runTest(timeout = 10.seconds) {
        // Given: root endpoint path
        val path = "/"

        // When: executing GET request
        val res = get(path)

        // Then: status, body and content type are correct
        assertEquals(200, res.status.value)
        assertEquals("Hello, World!", res.bodyAsText())
        assertEquals(ContentType.Text.Plain.withCharset(Charsets.UTF_8), res.contentType())
    }

    @DisplayName("GET /param_test returns provided param value")
    @Test
    fun shouldReturnProvidedParamValueFromParamTestEndpoint() = runTest(timeout = 10.seconds) {
        // Given: endpoint with query param foo=abc123
        val path = "/param_test?foo=abc123"

        // When: performing request
        val res = get(path)

        // Then: response contains the provided param value
        assertEquals(200, res.status.value)
        assertEquals("The param is: abc123", res.bodyAsText())
    }

    @DisplayName("GET /param_test returns null when param missing")
    @Test
    fun shouldReturnNullWhenParamMissingOnParamTestEndpoint() = runTest(timeout = 10.seconds) {
        // Given: endpoint without foo query parameter
        val path = "/param_test"

        // When: performing request
        val res = get(path)

        // Then: body reflects missing param as null
        assertEquals(200, res.status.value)
        assertEquals("The param is: null", res.bodyAsText())
    }

    @DisplayName("GET /json_test returns JSON payload")
    @Test
    fun shouldReturnJsonPayloadOnJsonTestEndpoint() = runTest(timeout = 10.seconds) {
        // Given: json_test endpoint
        val path = "/json_test"

        // When: performing request
        val res = get(path)

        // Then: status and content type are JSON; payload matches expected
        assertEquals(200, res.status.value)
        assertEquals(ContentType.Application.Json.withCharset(Charsets.UTF_8), res.contentType())
        // Gson renders mapOf("foo" to "bar") as {"foo":"bar"}
        assertEquals("""{"foo":"bar"}""", res.bodyAsText())
    }

    @DisplayName("GET /json_test_with_header returns JSON payload and custom header")
    @Test
    fun shouldReturnJsonPayloadAndCustomHeaderOnJsonTestWithHeaderEndpoint() = runTest(timeout = 10.seconds) {
        // Given: json_test_with_header endpoint
        val path = "/json_test_with_header"

        // When: performing request
        val res = get(path)

        // Then: verify body and custom header
        assertEquals(200, res.status.value)
        assertEquals(ContentType.Application.Json.withCharset(Charsets.UTF_8), res.contentType())
        assertEquals("""{"foo":"bar"}""", res.bodyAsText())
        // Header names are lowercased when set by webResponse helper
        assertEquals("Just a test!", res.headers["x-test-header"])
    }

    @DisplayName("GET /db_test returns JSON with single SELECT result")
    @Test
    fun shouldReturnSingleSelectResultFromDbTestEndpoint() = runTest(timeout = 10.seconds) {
        // Given: db_test endpoint executing 'SELECT 1 as one'
        val path = "/db_test"

        // When: performing request
        val res = get(path)

        // Then: verify status, content type, and JSON body
        assertEquals(200, res.status.value)
        assertEquals(ContentType.Application.Json.withCharset(Charsets.UTF_8), res.contentType())
        // mapFromRow on 'SELECT 1 as one' yields {"one":1}
        assertEquals("""{"one":1}""", res.bodyAsText())
    }

    @DisplayName("GET /db_get_user returns public user without passwordHash")
    @Test
    fun shouldReturnPublicUserFromDbGetUserEndpoint() = runTest(timeout = 10.seconds) {
        // Given: endpoint '/db_get_user' with seeded default user
        val path = "/db_get_user"

        // When: calling the endpoint
        val res = get(path)

        assertEquals(200, res.status.value)
        assertEquals(ContentType.Application.Json.withCharset(Charsets.UTF_8), res.contentType())

        val body = res.bodyAsText()

        // Then: raw JSON does not contain passwordHash
        assertTrue(!body.contains("passwordHash")) { "passwordHash should not be serialized" }

        // And: deserialize to PublicUser projection
        val actual = GsonProvider.gson.fromJson(body, PublicUser::class.java)

        // Then: recursive comparison ignoring dynamic infra fields (id/timestamps)
        assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields("id", "createdAt", "updatedAt")
            .isEqualTo(
                PublicUser(
                    id = -1, // ignored
                    createdAt = ZonedDateTime.now(), // ignored
                    updatedAt = ZonedDateTime.now(), // ignored
                    email = "vshpynta@crud.business",
                    tosAccepted = true,
                    name = "Volodymyr Shpynta"
                )
            )

        // And: ordering invariant for timestamps
        assert(actual.createdAt <= actual.updatedAt) { "createdAt must be <= updatedAt" }
    }
}
