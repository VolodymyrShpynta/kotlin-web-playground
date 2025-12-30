/**
 * Simple third-party service for demonstrating async HTTP calls from the main application.
 *
 * This standalone service provides three endpoints:
 * - GET /random_number: returns a random number after a random delay (200-2000ms)
 * - GET /ping: returns 'pong'
 * - POST /reverse: reverses the posted body text
 *
 * Runs on port 9876 by default, configurable via THIRD_PARTY_SERVICE_PORT environment variable.
 */
package com.vshpynta.thirdparty

import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("com.vshpynta.thirdparty.ThirdPartyService")

/**
 * Main entry point for the third-party service.
 */
fun main() {
    val port = System.getenv("THIRD_PARTY_SERVICE_PORT")?.toIntOrNull() ?: 9876
    log.info("Starting third-party service on port $port...")

    embeddedServer(Netty, port = port) {
        routing {
            get("/random_number") {
                val num = (200L..2000L).random()
                delay(num)
                call.respondText(num.toString())
            }

            get("/ping") {
                call.respondText("pong")
            }

            post("/reverse") {
                val body = call.receiveText()
                call.respondText(body.reversed())
            }
        }
    }.start(wait = true)

    log.info("Third-party service started successfully on port $port")
}

