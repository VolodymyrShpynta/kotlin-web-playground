package com.vshpynta.config

/**
 * Immutable application configuration resolved from HOCON files (app-<env>.conf + fallback app.conf).
 *
 * Populated via `createAppConfig(env)` in `Main.kt`.
 * Use a dedicated loader to merge environment-specific overrides and secrets injection.
 *
 * @property httpPort Server listening port (0 for ephemeral in tests).
 * @property dbUrl JDBC URL for the primary data source.
 * @property dbUser Database username (empty for H2 test usage).
 * @property dbPassword Database password (empty for H2 test usage).
 * @property useFileSystemAssets Flag to serve static assets from the file system (true in local, false in prod).
 * @property useSecureCookie Enable Secure flag on cookies (requires HTTPS).
 * @property cookieSameSite SameSite cookie attribute (lax, strict, or none).
 * @property cookieEncryptionKey Key for encrypting session cookies.
 * @property cookieSigningKey Key for signing session cookies.
 * @property corsAllowedHosts List of allowed hosts for CORS (format: "host:port" or just "host").
 * @property corsAllowedHttpsHosts List of allowed HTTPS-only hosts for CORS.
 * @property thirdPartyServiceUrl Base URL for the third-party demo service (for async HTTP call demonstrations).
 */
data class WebappConfig(
    val httpPort: Int,
    val dbUrl: String,
    val dbUser: String,
    val dbPassword: String,
    val useFileSystemAssets: Boolean,
    val useSecureCookie: Boolean,
    val cookieSameSite: String,
    val cookieEncryptionKey: String,
    val cookieSigningKey: String,
    val corsAllowedHosts: List<String>,
    val corsAllowedHttpsHosts: List<String>,
    val thirdPartyServiceUrl: String
)
