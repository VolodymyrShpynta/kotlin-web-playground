package com.vshpynta

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationModuleTest {

    @Test
    fun `GET - root returns Hello, World!`() = testApplication {
        // Given: application module installed and root endpoint
        application { module() } // use production wiring

        // When: client performs GET request
        val res = client.get("/")

        // Then: status and body match expected greeting
        assertEquals(HttpStatusCode.OK, res.status)
        assertEquals("Hello, World!", res.bodyAsText())
    }
}
