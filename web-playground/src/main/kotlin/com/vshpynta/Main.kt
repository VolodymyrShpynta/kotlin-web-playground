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
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.typesafe.config.ConfigFactory
import com.vshpynta.config.ConfigStringifier.stringify
import com.vshpynta.config.WebappConfig
import com.vshpynta.config.getStringListOrCommaSeparated
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
import com.vshpynta.web.ktor.KtorJsonWebResponse
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
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.auth.session
import io.ktor.server.engine.embeddedServer
import io.ktor.server.html.Template
import io.ktor.server.html.respondHtmlTemplate
import io.ktor.server.http.content.singlePageApplication
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receiveParameters
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
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
import io.ktor.server.sessions.get
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
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date
import javax.sql.DataSource
import kotlin.time.Duration

/**
 * Cookie name used to store encrypted user session data.
 * This name is used in the Sessions plugin configuration and matches the UserSession data class type.
 */
private const val USER_SESSION_COOKIE_NAME = "user-session"

/**
 * Authentication provider name used to protect routes requiring cookie-based session authentication.
 * Referenced in both the Authentication plugin configuration and the authenticate() route wrapper.
 */
private const val SESSION_AUTH_PROVIDER = "auth-session"

/**
 * Authentication provider name used to protect routes requiring JWT token authentication.
 * Routes wrapped with authenticate(JWT_AUTH_PROVIDER) will require a valid JWT token
 * in the Authorization header (Bearer scheme).
 */
private const val JWT_AUTH_PROVIDER = "jwt-auth"

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
        setUpKtorJwtSecurity(appConfig, dataSource)
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

    // CORS: Configured for cross-domain architecture with explicit allowlist
    // Only domains explicitly listed here can authenticate with the backend
    // No wildcards - maximum security through allowlist approach
    //
    // Architecture:
    // - Backend API can be hosted on api-service.com
    // - Frontend(s) can be hosted on myapp.com, another-domain.org, etc.
    // - Each allowed domain must be explicitly configured
    // - Uses SameSite=None for cross-domain cookies (less secure than Lax, but necessary)
    //
    // Configuration is loaded from app-<env>.conf files:
    // - cors.allowedHosts: hosts allowed for any protocol (HTTP/HTTPS)
    // - cors.allowedHttpsHosts: hosts allowed only via HTTPS (production)
    install(CORS) {
        // HTTP methods GET, POST are allowed by default
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)

        // Allow hosts from configuration (any protocol)
        webappConfig.corsAllowedHosts.forEach { host ->
            allowHost(host)
        }

        // Allow HTTPS-only hosts from configuration (production)
        webappConfig.corsAllowedHttpsHosts.forEach { host ->
            allowHost(host, schemes = listOf("https"))
        }

        // Required for cookie-based authentication across origins
        allowCredentials = true
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
 * This implementation is stateless - session data (userId, csrfToken) is encrypted and stored
 * in a cookie on the client side, NOT on the server. On each request, the server decrypts
 * the cookie, validates it, and extracts the session data. No server-side session storage.
 *
 * **Key Features:**
 * - Session data encrypted with AES (cookieEncryptionKey) and signed with HMAC (cookieSigningKey)
 * - Cookies automatically sent by browser (requires CSRF protection)
 * - Suitable for traditional web applications with browser clients
 *
 * **Security:**
 * - CSRF token validation required for all authenticated requests (including GET)
 * - HttpOnly cookies prevent XSS attacks
 * - Secure flag ensures HTTPS-only transmission in production
 * - SameSite policy configurable for cross-domain scenarios
 *
 * @param appConfig The application configuration.
 * @param dataSource The JDBC datasource for user authentication and lookup.
 */
fun Application.setUpKtorCookieSecurity(
    appConfig: WebappConfig,
    dataSource: DataSource
) {
    // Configure cookie-based sessions with encryption and signing
    // Session data is stored in an encrypted cookie on the client, not on the server
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
            cookie.extensions["SameSite"] = appConfig.cookieSameSite // Configure CORS policy
        }
    }

    // Set up authentication feature with session validation
    install(Authentication) {
        // Configure session-based authentication with the SESSION_AUTH_PROVIDER name
        // Routes wrapped with authenticate(SESSION_AUTH_PROVIDER) will require a valid session
        session<UserSession>(SESSION_AUTH_PROVIDER) {
            // Validate session and CSRF token for ALL authenticated requests
            // CSRF token is required for all HTTP methods (GET, POST, PUT, DELETE, etc.)
            // Returns the session if valid, or null if CSRF validation fails
            // Additional checks can be added here:
            // - Database lookup to verify user still exists
            // - Check if user is active/not banned
            // - Verify session hasn't been revoked
            validate { session ->
                session.takeIf { validCsrfToken(it) }
            }

            // Challenge block called when authentication/validation fails
            // Distinguishes between missing session (401) and invalid CSRF token (403)
            // Note: call.principal<UserSession>() is always null here because validate() returned null,
            // so we must access the session cookie directly via call.sessions.get<UserSession>()
            challenge {
                // Access session cookie directly (bypasses principal which is null)
                val session = call.sessions.get<UserSession>()

                if (session != null) {
                    // Session cookie exists but CSRF token invalid/missing
                    call.respond(
                        KtorJsonWebResponse(
                            body = mapOf("error" to "Invalid or missing CSRF token"),
                            status = HttpStatusCode.Forbidden // 403
                        )
                    )
                } else {
                    // No session cookie at all - authentication required
                    call.respond(
                        KtorJsonWebResponse(
                            body = mapOf("error" to "Authentication required", "requiresAuth" to true),
                            status = HttpStatusCode.Unauthorized // 401
                        )
                    )
                }
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

        // POST /api/login - Process login form submission for cross-origin AJAX
        post("/api/login", webResponse {
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
                    // Authentication failed - return 401 Unauthorized with error message
                    // JavaScript will parse and display the error to user
                    JsonWebResponse(
                        body = mapOf("error" to "Invalid credentials"),
                        statusCode = 401
                    )
                } else {
                    // Authentication successful - generate CSRF token and set encrypted session cookie
                    // CSRF token protects against Cross-Site Request Forgery attacks
                    // Since we use SameSite=None for cross-domain, CSRF tokens provide additional protection
                    val csrfToken = java.util.UUID.randomUUID().toString()

                    // Store CSRF token in session (encrypted in cookie)
                    call.sessions.set(UserSession(userId = userId, csrfToken = csrfToken))

                    // Return CSRF token to client (stored in JavaScript memory, not localStorage)
                    // Client must include this token in X-CSRF-Token header for state-changing requests
                    JsonWebResponse(
                        body = mapOf(
                            "success" to true,
                            "message" to "Login successful",
                            "csrfToken" to csrfToken
                        )
                    )
                }
            }
        })

        // Protected routes - require valid session (authenticate wrapper enforces this)
        // If session is missing or invalid, the challenge block redirects to /api/login
        authenticate(SESSION_AUTH_PROVIDER) {
            // GET /api/secret - Protected page that displays user information (CSRF protected)
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

            // GET /api/logout - Clear session and return JSON confirmation
            get("/api/logout", webResponse {
                // Remove session cookie, effectively logging out the user
                call.sessions.clear<UserSession>()

                // Frontend can handle navigation (e.g., redirect to login page)
                JsonWebResponse(
                    body = mapOf("success" to true, "message" to "Logged out successfully")
                )
            })
        }
    }
}

/**
 * Configures Ktor JWT (JSON Web Token) authentication.
 *
 * JWT authentication is stateless - the server doesn't store session data. All user
 * information and authentication state is encoded in the token itself, which the
 * client includes in the Authorization header of each request.
 *
 * **Comparison with Cookie-based Auth (both stateless in this implementation):**
 * - JWT: Token in Authorization header, signed with HMAC256, no CSRF protection needed
 * - Cookies: Encrypted cookie sent automatically by browser, requires CSRF protection
 * - Both: Server doesn't store session data, validates token/cookie on each request
 *
 * **Key Differences:**
 * - JWT tokens must be manually included in each request header
 * - Cookies are automatically sent by the browser (requires CSRF protection)
 * - JWT is ideal for APIs and mobile apps; Cookies work well for traditional web apps
 *
 * **Security Notes:**
 * - Tokens are signed with HMAC256 to prevent tampering
 * - Tokens expire after 1 day (configurable in login endpoint)
 * - No CSRF protection needed (tokens not automatically sent by browser)
 * - Audience and issuer claims provide additional validation layers
 *
 * @param appConfig The application configuration.
 * @param dataSource The JDBC datasource for user authentication and lookup.
 */
fun Application.setUpKtorJwtSecurity(
    appConfig: WebappConfig,
    dataSource: DataSource
) {
    // JWT audience and issuer are used for token verification only, not during authentication.
    // They help validate JWTs from multiple sources and enable different authentication scopes.
    // The authentication process itself doesn't use these properties - they only serve as
    // verification criteria when validating incoming JWT tokens.
    val jwtAudience = "myApp"
    val jwtIssuer = "http://0.0.0.0:4207"

    authentication {
        // JWT authentication uses the built-in JWTPrincipal class instead of a custom principal.
        // JWTPrincipal automatically parses Authorization headers (Bearer tokens) and decodes
        // the JWT payload, making it simpler than cookie-based auth which requires custom session classes.
        jwt(JWT_AUTH_PROVIDER) {
            realm = "myApp"

            // Configure JWT verifier to validate token signature and claims
            // Uses same signing key as cookies for simplicity (could be different in production)
            verifier(
                JWT
                    .require(Algorithm.HMAC256(appConfig.cookieSigningKey))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .build()
            )

            // Validate JWT claims and return JWTPrincipal if valid
            // Returns null if audience doesn't match, which triggers authentication challenge
            validate { credential ->
                credential.payload.takeIf { it.audience.contains(jwtAudience) }
                    ?.let { JWTPrincipal(it) }
            }
        }
    }

    routing {
        // POST /api/jwt/login - Authenticate user and issue JWT token
        // Request body: JSON with "username" (email) and "password" fields
        // Response: JSON with "token" field containing the JWT, or error message
        //
        // Example request:
        // POST /api/jwt/login
        // Content-Type: application/json
        // {"username": "user@example.com", "password": "secret"}
        //
        // Example response (success):
        // {"token": "eyJ0eXAiOiJKV1QiLCJhbGc..."}
        //
        // Example response (failure):
        // {"error": "Invalid username and/or password"}
        post("/api/jwt/login", webResponseDb(dataSource) { dbSession ->
            // Parse JSON request body to extract username and password
            val input = GsonProvider.gson.fromJson(call.receiveText(), Map::class.java)

            // Authenticate user credentials against database
            val userId = authenticateUser(
                dbSession,
                input["username"] as String,
                input["password"] as String
            )

            // Generate JWT token if authentication successful, otherwise return error
            userId?.let { id ->
                // Create signed JWT token with user claims
                // Token contains: audience, issuer, userId claim, and expiration (1 day)
                val token = JWT.create()
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .withClaim("userId", id)
                    .withExpiresAt(
                        Date.from(
                            LocalDateTime.now()
                                .plusDays(1)
                                .toInstant(ZoneOffset.UTC)
                        )
                    )
                    .sign(Algorithm.HMAC256(appConfig.cookieSigningKey))

                // Return token to client - client must store it and include in Authorization header
                JsonWebResponse(mapOf("token" to token))
            } ?: JsonWebResponse(
                mapOf("error" to "Invalid username and/or password"),
                statusCode = 403
            )
        })

        // Protected JWT routes - require valid JWT token in Authorization header
        // Client must include: Authorization: Bearer <token>
        authenticate(JWT_AUTH_PROVIDER) {
            // GET /api/jwt/secret - Protected endpoint that returns user information
            // Demonstrates accessing JWT claims (userId) and fetching user from database
            //
            // Example request:
            // GET /api/jwt/secret
            // Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGc...
            //
            // Example response:
            // {"hello": "user@example.com"}
            get("/api/jwt/secret", webResponseDb(dataSource) { dbSession ->
                // Extract JWT principal - guaranteed non-null because authenticate() wrapper validates it
                val jwtPrincipal = call.principal<JWTPrincipal>()!!

                // Extract userId from JWT claims (set during login)
                val userId = jwtPrincipal.getClaim("userId", Long::class)!!

                // Fetch user details from database using JWT's userId claim
                val user = findUserById(dbSession, userId)!!

                // Return user information as JSON
                JsonWebResponse(mapOf("hello" to user.email))
            })
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
 * Validates CSRF token for all authenticated requests.
 *
 * Called from the session authentication validate block to protect against CSRF attacks
 * when using SameSite=None cookies for cross-domain authentication.
 *
 * Note: This validates CSRF token for ALL HTTP methods (GET, POST, PUT, DELETE, etc.),
 * not just state-changing requests. This provides additional security for all authenticated
 * endpoints when using cross-domain cookies with SameSite=None.
 *
 * How it works:
 * - Extracts X-CSRF-Token header from the request
 * - Compares it with the CSRF token stored in the user's session
 * - Returns true only if both exist and match
 *
 * Used in validate block with takeIf:
 * ```kotlin
 * validate { session ->
 *     session.takeIf { validCsrfToken(it) }
 * }
 * ```
 *
 * @param session The user session containing the expected CSRF token
 * @return true if CSRF token is valid (header matches session), false otherwise
 */
private fun ApplicationCall.validCsrfToken(session: UserSession): Boolean {
    val providedToken = this.request.headers["X-CSRF-Token"]

    // Both token and session must exist, and tokens must match
    return providedToken != null && session.csrfToken == providedToken
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
                cookieSameSite = it.getString("cookieSameSite"),
                cookieEncryptionKey = it.getString("cookieEncryptionKey"),
                cookieSigningKey = it.getString("cookieSigningKey"),
                corsAllowedHosts = it.getStringListOrCommaSeparated("cors.allowedHosts"),
                corsAllowedHttpsHosts = it.getStringListOrCommaSeparated("cors.allowedHttpsHosts")
            )
        }
