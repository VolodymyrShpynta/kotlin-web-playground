package com.vshpynta

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.withCharset
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import javax.sql.DataSource
import kotlin.test.assertEquals

/**
 * Unit-style tests using Ktor's `testApplication` harness (no real network socket).
 * Focus: fast feedback on routing + minimal DB interaction through in-memory H2.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApplicationModuleTest {

    private val appConfig = createAppConfig("test")
    private lateinit var dataSource: DataSource

    @BeforeAll
    fun init() {
        dataSource = createDataSource(appConfig)
    }

    @DisplayName("GET / returns Hello, World!")
    @Test
    fun shouldReturnHelloWorldOnRootGet() = testApplication {
        // Given: application module installed and root endpoint
        application { module(appConfig, dataSource) } // use production wiring

        // When: client performs GET request
        val res = client.get("/")

        // Then: status and body match expected greeting
        assertEquals(HttpStatusCode.OK, res.status)
        assertEquals("Hello, World!", res.bodyAsText())
    }

    @DisplayName("GET /db_test returns JSON with single SELECT result")
    @Test
    fun shouldReturnSingleSelectResultFromDbTestEndpointUsingTestApplication() = testApplication {
        // Given: application module installed exposing /db_test
        application { module(appConfig, dataSource) }

        // When
        val res = client.get("/db_test")

        // Then: verify status and JSON body. ContentType defaults to application/json in custom response helper
        assertEquals(HttpStatusCode.OK, res.status)
        assertEquals(ContentType.Application.Json.withCharset(Charsets.UTF_8), res.contentType())
        assertEquals("""{"one":1}""", res.bodyAsText())
    }
}
