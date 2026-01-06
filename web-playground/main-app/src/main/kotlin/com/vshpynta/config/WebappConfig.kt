package com.vshpynta.config

/**
 * Main application configuration using Hoplite for automatic mapping.
 *
 * **Configuration Sources (priority order):**
 * 1. Environment variables (SCREAMING_SNAKE_CASE)
 * 2. System properties
 * 3. app-{env}.conf file
 * 4. app.conf file
 *
 * **Hoplite Benefits:**
 * - ✅ Zero boilerplate - automatic data class mapping
 * - ✅ Type-safe - compiler validates configuration structure
 * - ✅ Environment variables - automatic with proper naming conventions
 * - ✅ Validation - built-in with clear error messages
 * - ✅ Nested configuration - clean, organized structure
 *
 * **Example Environment Variables:**
 * ```
 * HTTP_PORT=8080
 * DB_URL=jdbc:postgresql://localhost:5432/myapp
 * DB_PASSWORD=secret
 * HIKARI_MAX_POOL_SIZE=50
 * CORS_ALLOWED_HOSTS=localhost:4207,example.com
 * ```
 *
 * Hoplite automatically converts between camelCase (config files) and SCREAMING_SNAKE_CASE (env vars).
 */
data class WebappConfig(
    val httpPort: Int = 4207,
    val db: DatabaseConfig,
    val useFileSystemAssets: Boolean = false,
    val cookie: CookieConfig,
    val cors: CorsConfig = CorsConfig(),
    val thirdPartyServiceUrl: String,
    val hikari: HikariConfig = HikariConfig()
)

/**
 * Database configuration.
 *
 * **Environment Variables:**
 * - DB_URL
 * - DB_USER
 * - DB_PASSWORD
 */
data class DatabaseConfig(
    val url: String,
    val user: String = "",
    val password: String = ""
)

/**
 * Cookie security configuration.
 *
 * **Environment Variables:**
 * - COOKIE_USE_SECURE
 * - COOKIE_SAME_SITE
 * - COOKIE_ENCRYPTION_KEY
 * - COOKIE_SIGNING_KEY
 */
data class CookieConfig(
    val useSecure: Boolean = true,
    val sameSite: String = "Lax",
    val encryptionKey: String,
    val signingKey: String
)

/**
 * CORS (Cross-Origin Resource Sharing) configuration.
 *
 * **Environment Variables:**
 * - CORS_ALLOWED_HOSTS (comma-separated)
 * - CORS_ALLOWED_HTTPS_HOSTS (comma-separated)
 */
data class CorsConfig(
    val allowedHosts: List<String> = emptyList(),
    val allowedHttpsHosts: List<String> = emptyList()
)

/**
 * HikariCP connection pool configuration.
 *
 * **Environment Variables:**
 * - HIKARI_MAX_POOL_SIZE
 * - HIKARI_MIN_IDLE
 * - HIKARI_CONNECTION_TIMEOUT_MS
 * - HIKARI_VALIDATION_TIMEOUT_MS
 * - HIKARI_IDLE_TIMEOUT_MS
 * - HIKARI_MAX_LIFETIME_MS
 * - HIKARI_LEAK_DETECTION_THRESHOLD_MS
 *
 * **Tuning Guide:**
 * - Development: Use defaults
 * - Production (high traffic): maxPoolSize=50, minIdle=10
 * - Production (low latency): connectionTimeoutMs=2000
 * - Debugging: leakDetectionThresholdMs=10000
 * - Production (performance): leakDetectionThresholdMs=0
 */
data class HikariConfig(
    val maxPoolSize: Int = 10,
    val minIdle: Int = 2,
    val connectionTimeoutMs: Long = 5000,
    val validationTimeoutMs: Long = 3000,
    val idleTimeoutMs: Long = 600000,
    val maxLifetimeMs: Long = 1800000,
    val leakDetectionThresholdMs: Long = 60000
)
