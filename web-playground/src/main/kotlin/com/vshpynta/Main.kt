package com.vshpynta

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("com.vshpynta.Main")

/**
 * Application entry point for starting the Ktor Netty server.
 * Delegates configuration to [Application.module] for testability.
 */
fun main() {
    log.debug("Starting Ktor Netty server...")

    // embeddedServer creates and starts the engine. `wait = true` blocks the main thread.
    embeddedServer(Netty, port = 4207, module = Application::module)
        .start(wait = true)
}

/**
 * Ktor application module installed both in production (via `main`) and tests (via testApplication {}).
 * Put feature installs, routing, dependency wiring, etc., here.
 */
fun Application.module() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            log.error("An unknown error occurred", cause)
            call.respondText(
                text = "500: $cause",
                status = HttpStatusCode.InternalServerError
            )
        }
    }

    routing {
        // Register simple hello world endpoint. More routes can be added similarly.
        helloWorldRoute()
    }
}

/**
 * Defines the `GET /` endpoint returning a static greeting.
 * Marked private because it's an implementation detail of routing setup.
 */
private fun Routing.helloWorldRoute() {
    get("/") {
        // `call` holds request/response context. respondText sends a plain text body.
        call.respondText("Hello, World!")
    }
}
