package com.vshpynta

import com.typesafe.config.ConfigFactory
import com.vshpynta.config.ConfigStringifier.stringify
import com.vshpynta.config.WebappConfig
import com.vshpynta.db.mapFromRow
import com.vshpynta.db.mapping.fromRow
import com.vshpynta.model.User
import com.vshpynta.security.UserSession
import com.vshpynta.service.authenticateUser
import com.vshpynta.service.findUserById
import com.vshpynta.web.HtmlWebResponse
import com.vshpynta.web.JsonWebResponse
import com.vshpynta.web.TextWebResponse
import com.vshpynta.web.dto.PublicUser
import com.vshpynta.web.html.AppLayout
import com.vshpynta.web.ktor.webResponse
import com.vshpynta.web.ktor.webResponseDb
import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.auth.session
import io.ktor.server.engine.embeddedServer
import io.ktor.server.html.Template
import io.ktor.server.html.respondHtmlTemplate
import io.ktor.server.http.content.staticFiles
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receiveParameters
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sessions.SessionTransportTransformerEncrypt
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.maxAge
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.util.hex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.html.ButtonType
import kotlinx.html.FormMethod
import kotlinx.html.HTML
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.p
import kotlinx.html.title
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.slf4j.LoggerFactory
import java.io.File
import javax.sql.DataSource
import kotlin.time.Duration

private const val USER_SESSION_COOKIE_NAME = "user-session"
private const val SESSION_AUTH_PROVIDER = "auth-session"

private val log = LoggerFactory.getLogger("com.vshpynta.Main")

/**
 * Application entry point for starting the Ktor Netty server.
 * - Loads configuration based on environment variable `WEB_PLAYGROUND_ENV` (defaults to 'local').
 * - Migrates the database using Flyway.
 * - Starts a demo third-party service for coroutine examples.
 * - Launches the main Ktor server on the configured port.
 *
 * Delegates configuration to [Application.setUpKtorApplication] for testability and separation of concerns.
 */
fun main() {
    log.debug("Starting web-playground application...")

    val env = System.getenv("WEB_PLAYGROUND_ENV") ?: "local"
    log.debug("Application runs in the environment '$env'")

    val appConfig = createAppConfig(env)
    log.debug("Configuration loaded successfully: \n${stringify(appConfig)}")

    val dataSource = createAndMigrateDataSource(appConfig)
    log.debug("Database connection pool initialized: {}", dataSource)

    // Start a simple third-party service to demonstrate coroutine usage
    startThirdPartyService()

    // embeddedServer creates and starts the engine. `wait = true` blocks the main thread.
    embeddedServer(Netty, port = appConfig.httpPort) {
        setUpKtorCookieSecurity(appConfig, dataSource)
        setUpKtorApplication(appConfig, dataSource)
    }.start(wait = true)
}

/**
 * Starts a demo third-party service on port 9876 for coroutine and HTTP client examples.
 * Provides endpoints:
 * - GET /random_number: returns a random number after a random delay
 * - GET /ping: returns 'pong'
 * - POST /reverse: reverses the posted body text
 *
 * This service is used in the /coroutine_demo endpoint to demonstrate async HTTP calls.
 */
fun startThirdPartyService() {
    embeddedServer(Netty, port = 9876) {
        routing {
            get("/random_number", webResponse {
                val num = (200L..2000L).random()
                delay(num)
                TextWebResponse(num.toString())
            })

            get("/ping", webResponse {
                TextWebResponse("pong")
            })

            post("/reverse", webResponse {
                TextWebResponse(call.receiveText().reversed())
            })
        }
    }.start(wait = false)
}

/**
 * Ktor application module installed both in production (via `main`) and tests (via testApplication {}).
 * Put feature installs, routing, dependency wiring, etc., here.
 *
 * @param webappConfig The application configuration.
 * @param dataSource The configured JDBC datasource for database access.
 */
fun Application.setUpKtorApplication(
    webappConfig: WebappConfig,
    dataSource: DataSource
) {
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
        helloWorldRoutes(webappConfig, dataSource)
    }
}

/**
 * Configures Ktor cookie-based session security with encryption and signing.
 *
 * @param appConfig The application configuration.
 * @param dataSource The JDBC datasource (not used here but could be for session storage).
 */
fun Application.setUpKtorCookieSecurity(
    appConfig: WebappConfig,
    dataSource: DataSource
) {
    // Configure cookie-based sessions with encryption and signing
    install(Sessions) {
        cookie<UserSession>(USER_SESSION_COOKIE_NAME) {
            transform(
                SessionTransportTransformerEncrypt(
                    hex(appConfig.cookieEncryptionKey),
                    hex(appConfig.cookieSigningKey)
                )
            )
            cookie.maxAge = Duration.parse("1d")
            cookie.httpOnly = true
            cookie.path = "/" // Cookie valid for entire site
            cookie.secure = appConfig.useSecureCookie // Use secure cookies in production
            cookie.extensions["SameSite"] = "lax" // Mitigate CSRF (Cross-Site Request Forgery) for modern browsers
        }
    }

    // Set up authentication feature with session validation
    install(Authentication) {
        session<UserSession>(SESSION_AUTH_PROVIDER) {
            // Validate that session exists; more complex validation can be added here
            validate { session ->
                session
            }
            // Redirect to /login if not authenticated
            challenge {
                call.respondRedirect("/login")
            }
        }
    }

    routing {
        get("/login", webResponse {
            HtmlWebResponse(AppLayout("Log in").apply {
                pageBody {
                    form(method = FormMethod.post, action = "/login") {
                        p {
                            label { +"E-mail" }
                            input(type = InputType.text, name = "username")
                        }
                        p {
                            label { +"Password" }
                            input(type = InputType.password, name = "password")
                        }
                        button(type = ButtonType.submit) { +"Log in" }
                    }
                }
            })
        })

        post("/login") {
            sessionOf(dataSource).use { dbSession ->
                val params = call.receiveParameters()
                val userId = authenticateUser(
                    dbSession,
                    params["username"]!!,
                    params["password"]!!
                )
                if (userId == null) {
                    // Authentication failed - redirect back to /login
                    call.respondRedirect("/login")
                } else {
                    // Store authenticated user in session
                    // No name parameter needed - Ktor uses the UserSession type mapped to USER_SESSION_COOKIE_NAME cookie name above
                    call.sessions.set(UserSession(userId = userId))
                    call.respondRedirect("/secret")
                }
            }
        }

        authenticate(SESSION_AUTH_PROVIDER) { // Protect routes with session authentication
            get("/secret", webResponseDb(dataSource) { dbSession ->
                val userSession = call.principal<UserSession>()!! // Guaranteed to be non-null due to authentication
                val user = findUserById(dbSession, userSession.userId)!! // Should exist if session is valid
                HtmlWebResponse(
                    AppLayout("Welcome, ${user.email}").apply {
                        pageBody {
                            h1 {
                                +"Hello there, ${user.email}"
                            }
                            p { +"You're logged in." }
                            p {
                                a(href = "/logout") { +"Log out" }
                            }
                        }
                    }
                )
            })

            get("/logout") {
                call.sessions.clear<UserSession>()
                call.respondRedirect("/login")
            }
        }
    }
}

/**
 * Defines the main HTTP endpoints for the playground application.
 *
 * Endpoints:
 * - GET /: returns a static greeting
 * - GET /param_test: echoes a query parameter
 * - GET /json_test: returns a simple JSON object
 * - GET /json_test_with_header: returns JSON with a custom header
 * - GET /db_test: returns result of a simple DB query
 * - GET /db_get_user: returns the first user from the DB as a public DTO
 * - GET /coroutine_demo: demonstrates async HTTP and DB calls
 *
 * @param webappConfig The application configuration.
 * @param dataSource The JDBC datasource for DB-backed endpoints.
 */
private fun Routing.helloWorldRoutes(
    webappConfig: WebappConfig,
    dataSource: DataSource
) {
    if (webappConfig.useFileSystemAssets) {
        // Serve static files from filesystem 'src/main/resources/public' at root path
        staticFiles("/", File("src/main/resources/public"))
    } else {
        // Serve static resources from 'public' at root path
        staticResources("/", "public")
    }

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

    get("/coroutine_demo", webResponseDb(dataSource) { dbSession ->
        handleCoroutineDemo(dbSession)
    })

    get("/html_demo") {
        htmlDemoResponseBuilder()
    }

    get("/html_webresponse_demo", webResponse {
        HtmlWebResponse(AppLayout("Hello, world!").apply {
            pageBody {
                h1 {
                    +"Hello, readers!"
                }
            }
        })
    })

    get("/html_webresponse_nolayout_demo", webResponse {
        HtmlWebResponse(object : Template<HTML> { // Anonymous Template without layout
            override fun HTML.apply() {
                head {
                    title { +"Plain HTML here! " }
                }
                body {
                    h1 { +"Very plan header" }
                }
            }
        })
    })
}

/**
 * Demonstrates coroutine usage by making async HTTP requests to the third-party service
 * and performing a blocking DB query in the IO dispatcher.
 *
 * @param dbSession The database session for DB queries.
 * @return [TextWebResponse] with results of all async operations.
 */
suspend fun handleCoroutineDemo(dbSession: Session) =
    coroutineScope {
        val client = HttpClient(CIO)
        val randomNumberRequest = async {
            client.get("http://localhost:9876/random_number").bodyAsText()
        }

        val reverseRequest = async {
            client.post("http://localhost:9876/reverse") {
                setBody(randomNumberRequest.await())
            }.bodyAsText()
        }

        val queryOperation = async {
            val pingPong = client.get("http://localhost:9876/ping").bodyAsText()

            // Perform blocking DB operation in IO dispatcher (Kotliquery uses blocking JDBC calls)
            withContext(Dispatchers.IO) {
                dbSession.single(
                    queryOf(
                        "SELECT count(*) c from user_table WHERE email != ?",
                        pingPong
                    ),
                    ::mapFromRow
                )
            }
        }

        TextWebResponse(
            """
            Random number: ${randomNumberRequest.await()}
            Reversed: ${reverseRequest.await()}
            Query: ${queryOperation.await()}
        """.trimIndent()
        )
    }

private suspend fun RoutingContext.htmlDemoResponseBuilder() {
    call.respondHtmlTemplate(AppLayout("Hello, world!")) {
        pageBody {
            h1 {
                +"Hello, World!"
            }
        }
    }
}

/**
 * Creates and migrates a HikariCP datasource using Flyway.
 *
 * @param config The loaded application configuration.
 * @return The initialized and migrated [DataSource].
 */
fun createAndMigrateDataSource(config: WebappConfig) =
    createDataSource(config).also(::migrateDataSource)

/**
 * Creates a HikariCP JDBC datasource from the provided config.
 *
 * @param config The loaded application configuration.
 * @return The initialized [HikariDataSource].
 */
fun createDataSource(config: WebappConfig) =
    HikariDataSource().apply {
        jdbcUrl = config.dbUrl
        username = config.dbUser
        password = config.dbPassword
    }

/**
 * Runs Flyway migrations on the provided datasource.
 *
 * @param dataSource The JDBC datasource to migrate.
 * @return The Flyway [MigrateResult].
 */
fun migrateDataSource(dataSource: DataSource): MigrateResult =
    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .table("flyway_schema_history")
        .load()
        .migrate()

/**
 * Loads and parses the application configuration for the given environment.
 *
 * @param env The environment name (e.g., 'local', 'prod', 'test').
 * @return The parsed [WebappConfig].
 */
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
                dbPassword = it.getString("db.password"),
                useFileSystemAssets = it.getBoolean("useFileSystemAssets"),
                useSecureCookie = it.getBoolean("useSecureCookie"),
                cookieEncryptionKey = it.getString("cookieEncryptionKey"),
                cookieSigningKey = it.getString("cookieSigningKey")
            )
        }
