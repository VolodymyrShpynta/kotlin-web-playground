package com.vshpynta.web.ktor

import com.vshpynta.web.JsonWebResponse
import com.vshpynta.web.TextWebResponse
import com.vshpynta.web.WebResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.RoutingHandler
import kotliquery.Session
import kotliquery.sessionOf
import javax.sql.DataSource

/**
 * Wraps a Ktor route handler that needs a database session.
 *
 * Creates a Kotliquery [Session] per request (with `returnGeneratedKey=true` so inserts can retrieve keys),
 * passes it to [handler], then closes it automatically via `use`.
 *
 * Usage:
 * ```kotlin
 * get("/users", webResponseDb(ds) { session ->
 *     JsonWebResponse(userRepository.list(session))
 * })
 * ```
 */
fun webResponseDb(
    dataSource: DataSource,
    handler: suspend RoutingContext.(Session) -> WebResponse
): RoutingHandler =
    webResponse {
        sessionOf(dataSource, returnGeneratedKey = true)
            .use { dbSession -> handler(dbSession) }
    }

/**
 * Core adapter converting a simple [WebResponse]-returning handler into a Ktor [RoutingHandler].
 *
 * Responsibilities:
 * - Execute user handler and obtain [WebResponse].
 * - Apply headers (case-insensitive merging already done inside WebResponse).
 * - Convert status code integer into Ktor's [HttpStatusCode].
 * - Dispatch body based on concrete type (text vs JSON), delegating JSON serialization to [KtorJsonWebResponse].
 */
fun webResponse(
    handler: suspend RoutingContext.() -> WebResponse
): RoutingHandler = {
    val resp = this.handler()

    // Propagate headers to the outgoing response
    for ((name, values) in resp.headers())
        for (value in values)
            call.response.header(name, value)

    val statusCode = HttpStatusCode.fromValue(resp.statusCode)

    when (resp) {
        is TextWebResponse -> call.respondText(resp.body, status = statusCode)
        is JsonWebResponse -> call.respond(KtorJsonWebResponse(resp.body, statusCode))
    }
}
