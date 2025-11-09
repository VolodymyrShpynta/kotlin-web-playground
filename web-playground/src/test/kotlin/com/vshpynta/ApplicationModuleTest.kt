package com.vshpynta

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ApplicationModuleTest {

    @DisplayName("GET / returns Hello, World!")
    @Test
    fun shouldReturnHelloWorldOnRootGet() = testApplication {
        // Given: application module installed and root endpoint
        application { module() } // use production wiring

        // When: client performs GET request
        val res = client.get("/")

        // Then: status and body match expected greeting
        assertEquals(HttpStatusCode.OK, res.status)
        assertEquals("Hello, World!", res.bodyAsText())
    }
}
