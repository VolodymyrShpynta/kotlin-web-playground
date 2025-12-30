package com.vshpynta.web.ktor

import com.vshpynta.web.HtmlWebResponse
import com.vshpynta.web.JsonWebResponse
import com.vshpynta.web.TextWebResponse
import com.vshpynta.web.WebResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.html.respondHtml
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.RoutingHandler
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.sessionOf
import javax.sql.DataSource

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
        is HtmlWebResponse -> call.respondHtml(statusCode) { with(resp.body) { apply() } }
    }
}

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
 * Wraps a Ktor route handler so that all database operations run inside a single JDBC transaction.
 *
 * Flow:
 * 1. Opens a regular Kotliquery [Session] (same as [webResponseDb]).
 * 2. Starts a transaction via `Session.transaction` producing a [TransactionalSession].
 * 3. Invokes the user-provided [handler].
 * 4. Commits if the handler completes normally; automatically rolls back if an exception is thrown.
 * 5. Closes the underlying session when the request finishes.
 *
 * Use this variant when multiple statements must succeed atomically (e.g. creating a user and related audit row).
 *
 * Example (two related writes must either both succeed or both fail):
 * ```kotlin
 * post("/users", webResponseTx(dataSource) { tx ->
 *     // Insert the user (first write)
 *     val userId = tx.updateAndReturnGeneratedKey(queryOf(
 *         "INSERT INTO user_table (email, password_hash, name, tos_accepted) VALUES (?, ?, ?, ?)",
 *         email, passwordHash, name, tosAccepted
 *     ))
 *
 *     // Insert an audit event referencing the newly created user (second write)
 *     tx.update(queryOf(
 *         "INSERT INTO audit_log (user_id, event_type, details) VALUES (?, ?, ?)",
 *         userId, "USER_CREATED", "Created via /users endpoint"
 *     ))
 *
 *     // Return the new user id; if either insert fails the entire transaction is rolled back
 *     JsonWebResponse(mapOf("userId" to userId))
 * })
 * ```
 * If the audit insert fails, the user insert is rolled back automatically, keeping the database consistent.
 *
 * Notes:
 * - Keep transactional handlers short-lived to avoid locking contention.
 * - Throwing any exception inside the handler triggers rollback; map it to an error response upstream if desired.
 * - If you need nested transactions / savepoints, Kotliquery exposes lower-level APIs—this helper keeps things simple.
 */
fun webResponseTx(
    dataSource: DataSource,
    handler: suspend RoutingContext.(TransactionalSession) -> WebResponse
): RoutingHandler =
    webResponseDb(dataSource) { dbSession ->
        dbSession.transaction { txSession ->
            handler(txSession)
        }
    }

