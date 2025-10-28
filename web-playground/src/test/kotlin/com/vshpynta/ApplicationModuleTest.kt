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
        application {
            module()   // use production wiring
        }

        val res = client.get("/")
        assertEquals(HttpStatusCode.OK, res.status)
        assertEquals("Hello, World!", res.bodyAsText())
    }
}
