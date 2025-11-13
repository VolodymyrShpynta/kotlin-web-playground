package com.vshpynta

import com.typesafe.config.ConfigFactory
import com.vshpynta.config.ConfigStringifier.stringify
import com.vshpynta.config.WebappConfig
import com.vshpynta.db.mapFromRow
import com.vshpynta.db.mapping.fromRow
import com.vshpynta.model.User
import com.vshpynta.web.JsonWebResponse
import com.vshpynta.web.TextWebResponse
import com.vshpynta.web.dto.PublicUser
import com.vshpynta.web.ktor.webResponse
import com.vshpynta.web.ktor.webResponseDb
import com.zaxxer.hikari.HikariDataSource
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
import kotliquery.queryOf
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val log = LoggerFactory.getLogger("com.vshpynta.Main")

/**
 * Application entry point for starting the Ktor Netty server.
 * Delegates configuration to [Application.module] for testability.
 */
fun main() {
    log.debug("Starting web-playground application...")

    val env = System.getenv("WEB_PLAYGROUND_ENV") ?: "local"
    log.debug("Application runs in the environment '$env'")

    val appConfig = createAppConfig(env)
    log.debug("Configuration loaded successfully: \n${stringify(appConfig)}")

    val dataSource = createAndMigrateDataSource(appConfig)
    log.debug("Database connection pool initialized: {}", dataSource)

    // embeddedServer creates and starts the engine. `wait = true` blocks the main thread.
    embeddedServer(Netty, port = appConfig.httpPort, module = { module(dataSource) })
        .start(wait = true)
}

/**
 * Ktor application module installed both in production (via `main`) and tests (via testApplication {}).
 * Put feature installs, routing, dependency wiring, etc., here.
 */
fun Application.module(dataSource: DataSource) {
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
        helloWorldRoutes(dataSource)
    }
}

/**
 * Defines the `GET /` endpoint returning a static greeting.
 * Marked private because it's an implementation detail of routing setup.
 */
private fun Routing.helloWorldRoutes(dataSource: DataSource) {
    get("/", webResponse {
        TextWebResponse("Hello, World!")
    })

    get("/param_test", webResponse {
        TextWebResponse("The param is: ${call.request.queryParameters["foo"]}")
    })

    get("/json_test", webResponse {
        JsonWebResponse(mapOf("foo" to "bar"))
    })

    get("/json_test_with_header", webResponse {
        JsonWebResponse(mapOf("foo" to "bar"))
            .header("X-Test-Header", "Just a test!")
    })

    get("/db_test", webResponseDb(dataSource) { dbSession ->
        JsonWebResponse(
            dbSession.single(queryOf("SELECT 1 as one"), ::mapFromRow)
        )
    })

    get("/db_get_user", webResponseDb(dataSource) { dbSession ->
        JsonWebResponse(
            dbSession.single(
                queryOf("SELECT * FROM user_table"),
                ::mapFromRow
            )
                ?.let { User.fromRow(it) }
                ?.let { PublicUser.fromDomain(it) }
        )
    })
}

fun createAndMigrateDataSource(config: WebappConfig) =
    createDataSource(config).also(::migrateDataSource)

fun createDataSource(config: WebappConfig) =
    HikariDataSource().apply {
        jdbcUrl = config.dbUrl
        username = config.dbUser
        password = config.dbPassword
    }

fun migrateDataSource(dataSource: DataSource): MigrateResult =
    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .table("flyway_schema_history")
        .load()
        .migrate()

fun createAppConfig(env: String) =
    ConfigFactory
        .parseResources("app-${env}.conf")
        .withFallback(ConfigFactory.parseResources("app.conf"))
        .resolve()
        .let {
            WebappConfig(
                httpPort = it.getInt("httpPort"),
                dbUrl = it.getString("db.url"),
                dbUser = it.getString("db.user"),
                dbPassword = it.getString("db.password")
            )
        }
