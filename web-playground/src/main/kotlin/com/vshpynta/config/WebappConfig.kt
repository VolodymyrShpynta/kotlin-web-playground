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
 */
data class WebappConfig(
    val httpPort: Int,
    val dbUrl: String,
    val dbUser: String,
    val dbPassword: String,
    val useFileSystemAssets: Boolean,
    val useSecureCookie: Boolean,
    val cookieEncryptionKey: String,
    val cookieSigningKey: String
)
