/**
 * Main application entry point and Ktor server configuration.
 *
 * This file contains:
 * - Application startup logic with environment-based configuration
 * - Ktor server setup with cookie-based session authentication
 * - Route definitions for API endpoints (hello world, user management, auth)
 * - Database connection pooling with HikariCP and Flyway migrations
 * - Demo third-party service for showcasing coroutine-based HTTP calls
 *
 * The application uses functional error handling with Arrow's Either and Raise DSL,
 * separating validation errors from database errors while maintaining a unified error flow.
 *
 * **URL Structure**
 * All API endpoints use the `/api` prefix (e.g., `/api/users`, `/api/login`) to prevent
 * conflicts with the Single Page Application (SPA) routing. The SPA serves `index.html`
 * for all unmatched routes, enabling client-side routing for paths without the `/api` prefix.
 * This clear separation allows the SPA to handle routes like `/`, `/about`, `/profile`,
 * while backend API endpoints remain under `/api/<sub_url>`.
 */
package com.vshpynta

import arrow.core.raise.either
import com.typesafe.config.ConfigFactory
import com.vshpynta.config.ConfigStringifier.stringify
import com.vshpynta.config.WebappConfig
import com.vshpynta.db.mapFromRow
import com.vshpynta.db.mapping.fromRow
import com.vshpynta.model.User
import com.vshpynta.security.UserSession
import com.vshpynta.service.authenticateUser
import com.vshpynta.service.createUser
import com.vshpynta.service.findUserById
import com.vshpynta.web.HtmlWebResponse
import com.vshpynta.web.JsonWebResponse
import com.vshpynta.web.TextWebResponse
import com.vshpynta.web.dto.CreateUserRequest
import com.vshpynta.web.dto.PublicUser
import com.vshpynta.web.dto.ValidationError
import com.vshpynta.web.html.AppLayout
import com.vshpynta.web.ktor.webResponse
import com.vshpynta.web.ktor.webResponseDb
import com.vshpynta.web.serialization.GsonProvider
import com.vshpynta.web.validation.validateCreateUserRequest
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
import io.ktor.server.http.content.singlePageApplication
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
import javax.sql.DataSource
import kotlin.time.Duration

/**
 * Cookie name used to store encrypted user session data.
 * This name is used in the Sessions plugin configuration and matches the UserSession data class type.
 */
private const val USER_SESSION_COOKIE_NAME = "user-session"

/**
 * Authentication provider name used to protect routes requiring authentication.
 * Referenced in both the Authentication plugin configuration and the authenticate() route wrapper.
 */
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
        singlePageApplicationRoutes(webappConfig)
        helloWorldApiRoutes(dataSource)
        userApiRoutes(dataSource)
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
        // Configure session-based authentication with the SESSION_AUTH_PROVIDER name
        // Routes wrapped with authenticate(SESSION_AUTH_PROVIDER) will require a valid session
        session<UserSession>(SESSION_AUTH_PROVIDER) {
            // Validate session - here we simply check it exists, but you could add:
            // - Database lookup to verify user still exists
            // - Check if user is active/not banned
            // - Verify session hasn't been revoked
            validate { session ->
                session
            }
            // Challenge function called when authentication fails (no session or validation fails)
            // Redirects unauthenticated users to the login page
            challenge {
                call.respondRedirect("/api/login")
            }
        }
    }

    routing {
        // GET /api/login - Display login form
        get("/api/login", webResponse {
            HtmlWebResponse(AppLayout("Log in").apply {
                pageBody {
                    form(method = FormMethod.post, action = "/api/login") {
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

        // POST /api/login - Process login form submission
        post("/api/login") {
            sessionOf(dataSource).use { dbSession ->
                // Extract username (email) and password from form parameters
                val params = call.receiveParameters()

                // Authenticate user by checking email and password against database
                // Returns user ID if credentials are valid, null otherwise
                val userId = authenticateUser(
                    dbSession,
                    params["username"]!!,
                    params["password"]!!
                )

                if (userId == null) {
                    // Authentication failed - invalid credentials
                    // TODO: Consider adding error message to session and displaying on login page
                    call.respondRedirect("/api/login")
                } else {
                    // Authentication successful - create encrypted session cookie
                    // The cookie<UserSession>() configuration already maps the UserSession type to USER_SESSION_COOKIE_NAME
                    // Ktor automatically encrypts, signs, and sets the cookie based on the Sessions configuration
                    call.sessions.set(UserSession(userId = userId))

                    // Redirect to protected page
                    call.respondRedirect("/api/secret")
                }
            }
        }

        // Protected routes - require valid session (authenticate wrapper enforces this)
        // If session is missing or invalid, the challenge block redirects to /api/login
        authenticate(SESSION_AUTH_PROVIDER) {
            // GET /api/secret - Protected page that displays user information
            get("/api/secret", webResponseDb(dataSource) { dbSession ->
                // Extract session from request - guaranteed non-null because authenticate() wrapper validates it
                val userSession = call.principal<UserSession>()!!

                // Fetch user details from database using session's user ID
                val user = findUserById(dbSession, userSession.userId)!!

                // Render HTML page with user information
                HtmlWebResponse(
                    AppLayout("Welcome, ${user.email}").apply {
                        pageBody {
                            h1 {
                                +"Hello there, ${user.email}"
                            }
                            p { +"You're logged in." }
                            p {
                                a(href = "/api/logout") { +"Log out" }
                            }
                        }
                    }
                )
            })

            // GET /api/logout - Clear session and redirect to login page
            get("/api/logout") {
                // Remove session cookie, effectively logging out the user
                call.sessions.clear<UserSession>()
                call.respondRedirect("/api/login")
            }
        }
    }
}

/**
 * Configures Single Page Application (SPA) routing.
 *
 * Serves static files and provides SPA fallback behavior - when a route is not matched
 * by any defined endpoint, serves the default page (index.html). This allows client-side
 * routing in SPAs to work correctly.
 *
 * **Important**: All API endpoints use the `/api` prefix to avoid conflicts with SPA routes.
 * - Routes with `/api` prefix → handled by backend API endpoints
 * - Routes without `/api` prefix → served by SPA (falls back to index.html)
 *
 * This pattern ensures that SPA client-side routes (e.g., `/home`, `/profile`, `/settings`)
 * don't interfere with backend API routes (e.g., `/api/users`, `/api/login`).
 *
 * @param webappConfig The application configuration.
 */
private fun Routing.singlePageApplicationRoutes(
    webappConfig: WebappConfig
) {
    singlePageApplication {
        // In development mode, serve files from file system for hot-reload
        // In production, serve from JAR resources
        if (webappConfig.useFileSystemAssets) {
            filesPath = "src/main/resources/public"
        } else {
            useResources = true
            filesPath = "public"
        }
        // Default page served for unmatched routes (enables client-side routing)
        defaultPage = "index.html"
    }
}

/**
 * Defines demo and hello world HTTP endpoints for the playground application.
 *
 * Endpoints:
 * - GET /api: returns a static "Hello, World!" greeting
 * - GET /api/param_test: echoes a query parameter
 * - GET /api/json_test: returns a simple JSON object
 * - POST /api/json_test: echoes back the posted JSON input
 * - GET /api/json_test_with_header: returns JSON with a custom header
 * - GET /api/db_test: returns result of a simple DB query
 * - GET /api/db_get_user: returns the first user from the DB as a public DTO
 * - GET /api/coroutine_demo: demonstrates async HTTP and DB calls with coroutines
 * - GET /api/html_demo: demonstrates HTML templating with Ktor HTML DSL
 * - GET /api/html_webresponse_demo: demonstrates custom HTML response with layout
 * - GET /api/html_webresponse_nolayout_demo: demonstrates HTML response without layout
 *
 * @param dataSource The JDBC datasource for DB-backed endpoints.
 */
private fun Routing.helloWorldApiRoutes(
    dataSource: DataSource
) {
    get("/api", webResponse {
        TextWebResponse("Hello, World!")
    })

    get("/api/param_test", webResponse {
        TextWebResponse("The param is: ${call.request.queryParameters["foo"]}")
    })

    get("/api/json_test", webResponse {
        JsonWebResponse(mapOf("foo" to "bar"))
    })

    get("/api/json_test_with_header", webResponse {
        JsonWebResponse(mapOf("foo" to "bar"))
            .header("X-Test-Header", "Just a test!")
    })

    post("/api/json_test", webResponse {
        val input = GsonProvider.gson.fromJson(
            call.receiveText(), Map::class.java
        )
        JsonWebResponse(mapOf("input" to input))
    })

    get("/api/db_test", webResponseDb(dataSource) { dbSession ->
        JsonWebResponse(
            dbSession.single(queryOf("SELECT 1 as one"), ::mapFromRow)
        )
    })

    get("/api/db_get_user", webResponseDb(dataSource) { dbSession ->
        JsonWebResponse(
            dbSession.single(
                queryOf("SELECT * FROM user_table"),
                ::mapFromRow
            )
                ?.let { User.fromRow(it) }
                ?.let { PublicUser.fromDomain(it) }
        )
    })

    get("/api/coroutine_demo", webResponseDb(dataSource) { dbSession ->
        handleCoroutineDemo(dbSession)
    })

    get("/api/html_demo") {
        htmlDemoResponseBuilder()
    }

    get("/api/html_webresponse_demo", webResponse {
        HtmlWebResponse(AppLayout("Hello, world!").apply {
            pageBody {
                h1 {
                    +"Hello, readers!"
                }
            }
        })
    })

    get("/api/html_webresponse_nolayout_demo", webResponse {
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
 * Defines the API HTTP endpoints for user management.
 *
 * Endpoints:
 * - GET /api/users/{id}: retrieves a user by ID
 * - POST /api/users: creates a new user
 *
 * @param dataSource The JDBC datasource for DB access.
 */
private fun Routing.userApiRoutes(
    dataSource: DataSource
) {
    // GET /api/users/{id} - get user by ID
    get("/api/users/{id}", webResponseDb(dataSource) { dbSession ->
        either {
            val userId = call.parameters["id"]?.toLongOrNull()
                ?: raise(ValidationError("Invalid user ID", 400))

            findUserById(dbSession, userId)
                ?: raise(ValidationError("User not found", 404))
        }.fold(
            { err -> JsonWebResponse(mapOf("error" to err.error), statusCode = err.statusCode) },
            { user -> JsonWebResponse(PublicUser.fromDomain(user)) }
        )
    })

    // POST /api/users - create a new user
    post("/api/users", webResponse {
        either {
            // Deserialize the incoming JSON request body to CreateUserRequest DTO
            val request = GsonProvider.gson.fromJson(
                call.receiveText(),
                CreateUserRequest::class.java
            )

            // Validate the deserialized request (email, password, tosAccepted)
            // .bind() extracts the valid request or short-circuits with validation error
            val validRequest = validateCreateUserRequest(request).bind()

            // Create user in database and handle potential errors
            // Result<Long> is converted to Either via getOrElse + raise pattern:
            // - Success: returns the generated user ID
            // - Failure: converts exception to ValidationError and raises it (short-circuits)
            sessionOf(dataSource, returnGeneratedKey = true)
                .use { dbSession ->
                    createUser(
                        dbSession,
                        validRequest.email,
                        validRequest.name,
                        validRequest.password,
                        validRequest.tosAccepted
                    )
                }
                .getOrElse { exception -> raise(handleDatabaseException(exception)) }
        }.fold(
            // Error case: validation or database error - return error response with appropriate status code
            { err -> JsonWebResponse(mapOf("error" to err.error), statusCode = err.statusCode) },
            // Success case: user created - return user ID with 201 Created status
            { userId -> JsonWebResponse(mapOf("userId" to userId), statusCode = 201) }
        )
    })
}

/**
 * Converts a database exception to a ValidationError with an appropriate error message and status code.
 *
 * This function provides user-friendly error messages and correct HTTP status codes for common
 * database failures, making API responses more informative and following REST conventions.
 *
 * Error mappings:
 * - Unique/duplicate constraint violations → 409 Conflict (e.g., email already exists)
 * - Other constraint violations → 400 Bad Request (e.g., NULL constraint, length violation)
 * - General errors → 500 Internal Server Error
 *
 * @param exception The database exception to handle.
 * @return ValidationError with a user-friendly message and HTTP status code.
 */
private fun handleDatabaseException(exception: Throwable): ValidationError {
    log.error("Database operation failed", exception)

    return when {
        // Unique constraint violation (e.g., duplicate email)
        exception.message?.contains("unique", ignoreCase = true) == true ||
                exception.message?.contains("duplicate", ignoreCase = true) == true ->
            ValidationError("User with this email already exists", 409)

        // Other constraint violations (NOT NULL, CHECK, length, etc.)
        exception.message?.contains("constraint", ignoreCase = true) == true ->
            ValidationError("Invalid user data: ${exception.message}", 400)

        // General database errors
        else -> ValidationError("Failed to create user: ${exception.message}", 500)
    }
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
