package com.vshpynta

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.test.runTest
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class SmokeIntegrationTest {

    @Test
    fun `embedded Netty server starts and responds`() = runTest(timeout = 10.seconds) {
        val port = ServerSocket(0).use { it.localPort } // find a free port

        val server = embeddedServer(Netty, port = port) {
            module()
        }.start(wait = false)

        HttpClient(CIO).use { client ->
            val body = client.get("http://localhost:$port/").bodyAsText()
            assertEquals("Hello, World!", body)
        }

        server.stop(gracePeriodMillis = 100, timeoutMillis = 1000)
    }
}
