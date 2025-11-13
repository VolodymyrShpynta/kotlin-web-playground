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

fun webResponseDb(
    dataSource: DataSource,
    handler: suspend RoutingContext.(Session) -> WebResponse
): RoutingHandler =
    webResponse {
        sessionOf(dataSource, returnGeneratedKey = true)
            .use { dbSession -> handler(dbSession) }
    }

fun webResponse(
    handler: suspend RoutingContext.() -> WebResponse
): RoutingHandler {
    return {
        val resp = this.handler()

        for ((name, values) in resp.headers())
            for (value in values)
                call.response.header(name, value)

        val statusCode = HttpStatusCode.fromValue(resp.statusCode)

        when (resp) {
            is TextWebResponse -> {
                // `call` holds request/response context. respondText sends a plain text body
                call.respondText(
                    text = resp.body,
                    status = statusCode
                )
            }

            is JsonWebResponse -> {
                call.respond(
                    KtorJsonWebResponse(
                        body = resp.body,
                        status = statusCode
                    )
                )
            }
        }
    }
}
