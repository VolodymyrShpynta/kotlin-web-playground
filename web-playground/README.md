# Kotlin Web Playground

A comprehensive Kotlin (JVM) web application showcasing modern backend development with Ktor framework. This project
demonstrates authentication, database operations, functional error handling, and Single Page Application (SPA) hosting.

> ** Multi-Module Architecture:** This project uses a modular structure with separate services that can be built,
> deployed, and scaled independently.

## Overview

This playground application provides a production-ready foundation for building web applications with:

- **Cookie-based and JWT authentication** with CSRF protection
- **Database management** with migrations and connection pooling
- **RESTful API endpoints** with functional error handling (Arrow)
- **Single Page Application** hosting with client-side routing support
- **Coroutine-based async operations** with HTTP client examples
- **Comprehensive testing** with integration and unit tests
- **Multi-module architecture** with independent Docker containers
- **Automated build and deployment** with Docker Compose

## Table of Contents

- [Tech Stack](#tech-stack)
- [Multi-Module Architecture](#multi-module-architecture)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Requirements](#requirements)
- [Running the Application](#running-the-application)
- [Building & Packaging](#building--packaging)
- [Docker Deployment](#docker-deployment)
- [Azure Deployment](#azure-deployment)
- [Common Gradle Tasks](#common-gradle-tasks)
- [Testing](#testing)
- [Database](#database)
- [Authentication](#authentication)
- [Error Handling](#error-handling)
- [Logging](#logging)
- [Single Page Application (SPA) Support](#single-page-application-spa-support)
- [Monitoring & Metrics](#monitoring--metrics)
- [Rate Limiting](#rate-limiting)
- [Dependency & Version Management](#dependency--version-management)
- [JDK Toolchain](#jdk-toolchain)
- [Future Enhancements](#future-enhancements)
- [Troubleshooting](#troubleshooting)
- [Contribution Guidelines](#contribution-guidelines)
- [License](#license)

## Tech Stack

### Core Framework

- **Kotlin**: 2.2.20 (configured via `pluginManagement` in `settings.gradle.kts`)
- **Ktor**: 3.3.2 (managed via BOM) - Web framework with Netty engine
- **Gradle**: Kotlin DSL with JVM Toolchain 24

### Key Dependencies

- **Arrow FX**: Functional error handling with `Either` and `Raise` DSL
- **HikariCP**: JDBC connection pooling (v7.0.2)
- **Flyway**: Database migration management (v11.16.0)
- **H2 Database**: In-memory and file-based database (v2.4.240)
- **Kotliquery**: Type-safe SQL queries (v1.9.1)
- **BCrypt**: Password hashing (v0.10.2)
- **Hoplite**: Modern Kotlin configuration library (v2.9.5)
- **Gson**: JSON serialization/deserialization (v2.13.2)
- **Logback**: Logging implementation (v1.5.20)
- **JWT**: JWT authentication (via Ktor)

### Ktor Features Used

- Server Core & Netty Engine
- Authentication (Session & JWT)
- Sessions with encrypted cookies
- Status Pages (error handling)
- CORS support
- HTML DSL
- Single Page Application support
- HTTP Client (CIO engine)

## Multi-Module Architecture

The project is organized into two independent Gradle modules:

### 📦 Module 1: `main-app`

The primary web application containing:

- All API endpoints and business logic
- Cookie and JWT authentication
- Database operations and migrations
- User management
- CORS configuration
- SPA hosting

**Entry Point:** `com.vshpynta.MainKt`  
**Port:** 4207 (Docker: 9000)

### 📦 Module 2: `third-party-service`

A lightweight demo service for testing async HTTP calls:

- `GET /random_number` - Returns random number after random delay (200-2000ms)
- `GET /ping` - Returns "pong"
- `POST /reverse` - Reverses the posted body text

**Entry Point:** `com.vshpynta.thirdparty.ThirdPartyServiceKt`  
**Port:** 9876

### Project Structure

```
web-playground/
├── main-app/                          # Module 1: Main Application
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/com/vshpynta/
│   │   │   │   ├── Main.kt                 # Application entry point
│   │   │   │   ├── config/                 # Configuration classes
│   │   │   │   ├── db/                     # Database utilities and mappings
│   │   │   │   ├── model/                  # Domain models (User, etc.)
│   │   │   │   ├── security/               # Authentication (UserSession)
│   │   │   │   ├── service/                # Business logic
│   │   │   │   └── web/                    # Web layer (DTOs, responses, validation)
│   │   │   └── resources/
│   │   │       ├── app.conf                # Base configuration
│   │   │       ├── app-local.conf          # Local development config
│   │   │       ├── app-prod.conf           # Production config
│   │   │       ├── logback.xml             # Logging configuration
│   │   │       ├── db/migration/           # Flyway SQL migrations
│   │   │       └── public/                 # Static files for SPA
│   │   └── test/
│   │       ├── kotlin/com/vshpynta/
│   │       │   ├── ApplicationModuleTest.kt    # Unit tests
│   │       │   ├── SmokeIntegrationTest.kt     # Integration tests
│   │       │   └── UserTests.kt                # User service tests
│   │       └── resources/
│   │           └── app-test.conf               # Test environment config
│   ├── build.gradle.kts                # Module build configuration
│   └── Dockerfile                      # Container image definition
│
├── third-party-service/               # Module 2: Demo Service
│   ├── src/main/
│   │   ├── kotlin/com/vshpynta/thirdparty/
│   │   │   └── ThirdPartyService.kt    # Simple Ktor service
│   │   └── resources/
│   │       └── logback.xml             # Logging configuration
│   ├── build.gradle.kts                # Module build configuration
│   └── Dockerfile                      # Container image definition
│
├── build.gradle.kts                   # Root parent build configuration
├── settings.gradle.kts                # Module includes and plugin management
├── docker-compose.yml                 # Multi-container orchestration
├── build-docker.ps1                   # Automated build script
├── AUTHENTICATION-GUIDE.md            # Authentication documentation
└── AZURE-DEPLOYMENT.md                # Azure deployment guide
```

### Benefits of Multi-Module Architecture

✅ **Independent Deployment** - Scale services separately  
✅ **Faster Builds** - Build only what changed  
✅ **Clear Boundaries** - Better separation of concerns  
✅ **Resource Optimization** - Minimal third-party service footprint  
✅ **Docker Ready** - Separate containers for each service

## Quick Start

### Option 1: Docker (Recommended)

Build and deploy everything with one command:

```powershell
# Build both modules and create Docker images
.\build-docker.ps1

# Start both services
docker-compose up -d

# View logs
docker-compose logs -f

# Access services
# Main app: http://localhost:9000
# Third-party service: http://localhost:9876
```

### Option 2: Local Development

Run both services in separate terminals:

**Terminal 1 - Third-Party Service:**

```powershell
.\gradlew.bat :third-party-service:run
```

**Terminal 2 - Main Application:**

```powershell
.\gradlew.bat :main-app:run
```

Main app will be available at **http://localhost:4207/**

### Option 3: Run Main App Only

If you don't need the demo service:

```powershell
.\gradlew.bat :main-app:run
```

## Architecture

### API Endpoints (main-app)

All API endpoints use the `/api` prefix to avoid conflicts with SPA client-side routing.

#### Demo & Hello World Endpoints

| Endpoint                              | Method | Description                      | Auth Required |
|---------------------------------------|--------|----------------------------------|---------------|
| `/api`                                | GET    | Returns "Hello, World!"          | No            |
| `/api/param_test`                     | GET    | Echoes query parameter `foo`     | No            |
| `/api/json_test`                      | GET    | Returns simple JSON object       | No            |
| `/api/json_test`                      | POST   | Echoes back posted JSON          | No            |
| `/api/json_test_with_header`          | GET    | Returns JSON with custom header  | No            |
| `/api/db_test`                        | GET    | Returns database query result    | No            |
| `/api/db_get_user`                    | GET    | Returns first user as JSON       | No            |
| `/api/coroutine_demo`                 | GET    | Demonstrates async HTTP/DB calls | No            |
| `/api/html_demo`                      | GET    | HTML templating example          | No            |
| `/api/html_webresponse_demo`          | GET    | Custom HTML response with layout | No            |
| `/api/html_webresponse_nolayout_demo` | GET    | HTML response without layout     | No            |

#### User Management Endpoints

| Endpoint          | Method | Description     | Auth Required |
|-------------------|--------|-----------------|---------------|
| `/api/users/{id}` | GET    | Get user by ID  | No            |
| `/api/users`      | POST   | Create new user | No            |

#### Cookie-Based Authentication Endpoints

| Endpoint      | Method | Description               | Auth Required |
|---------------|--------|---------------------------|---------------|
| `/api/login`  | GET    | Display login form        | No            |
| `/api/login`  | POST   | Process login (form data) | No            |
| `/api/logout` | GET    | Logout and clear session  | No            |
| `/api/secret` | GET    | Protected endpoint (HTML) | Yes + CSRF    |

#### JWT Authentication Endpoints

| Endpoint          | Method | Description                        | Auth Required      |
|-------------------|--------|------------------------------------|--------------------|
| `/api/jwt/login`  | POST   | Login with JSON, returns JWT token | No                 |
| `/api/jwt/secret` | GET    | Protected endpoint (returns JSON)  | Yes (Bearer token) |

#### SPA Routes

| Path                 | Description                                        |
|----------------------|----------------------------------------------------|
| `/`                  | Serves `index.html` (SPA entry point)              |
| Any non-`/api` route | Falls back to `index.html` for client-side routing |

## Configuration

### Modern Kotlin Configuration with Hoplite

This application uses **Hoplite** - the most idiomatic Kotlin configuration library. Hoplite provides zero-boilerplate,
type-safe configuration with automatic environment variable mapping.

#### Why Hoplite?

**Zero Boilerplate** - Automatic data class mapping (4 lines vs 30+ with TypeSafe Config)  
**Type-Safe** - Compiler validates everything at compile time  
**Auto Environment Variables** - Automatic SCREAMING_SNAKE_CASE → camelCase conversion  
**Clean Structure** - Nested data classes for logical grouping  
**Built-in Validation** - Clear error messages for configuration issues  
**Most Idiomatic** - The Kotlin way to handle configuration

### Configuration Architecture

The application uses a **hybrid configuration strategy** combining multiple sources:

**Configuration Priority (highest to lowest):**

1. **Environment Variables** - For secrets and deployment-specific settings (automatically mapped by Hoplite)
2. **System Properties** - JVM properties (`-Dhttp.port=8080`)
3. **Environment-Specific Config** (`app-{env}.conf`) - Environment overrides
4. **Base Config** (`app.conf`) - Sensible defaults for all environments

**Key Benefits:**

- **Security** - Secrets in environment variables, not in source control
- **Flexibility** - Easy override for different environments (local, staging, prod)
- **Developer Experience** - Sensible defaults for local development
- **Cloud-Native** - Perfect for Docker, Kubernetes, Azure Container Apps
- **Automatic Mapping** - No manual parsing code required

### How Hoplite Works

Hoplite automatically maps configuration from multiple sources to Kotlin data classes:

```kotlin
// Automatic data class mapping - no boilerplate!
fun createAppConfig(env: String): WebappConfig =
    ConfigLoaderBuilder.default()
        .addSource(PropertySource.resource("/app-${env}.conf"))
        .addSource(PropertySource.resource("/app.conf"))
        .build()
        .loadConfigOrThrow<WebappConfig>()
```

**Configuration Structure:**

```kotlin
data class WebappConfig(
    val httpPort: Int = 4207,
    val db: DatabaseConfig,                    // Nested config
    val cookie: CookieConfig,                  // Nested config
    val cors: CorsConfig = CorsConfig(),       // Nested config with defaults
    val hikari: HikariConfig = HikariConfig(), // Nested config with defaults
    val useFileSystemAssets: Boolean = false,
    val thirdPartyServiceUrl: String = "http://localhost:9876"
)

data class DatabaseConfig(
    val url: String,
    val user: String = "",
    val password: String = ""
)

data class CookieConfig(
    val useSecure: Boolean = true,
    val sameSite: String = "Lax",
    val encryptionKey: String,
    val signingKey: String
)

data class CorsConfig(
    val allowedHosts: List<String> = emptyList(),
    val allowedHttpsHosts: List<String> = emptyList()
)

data class HikariConfig(
    val maxPoolSize: Int = 10,
    val minIdle: Int = 2,
    val connectionTimeoutMs: Long = 5000,
    val validationTimeoutMs: Long = 3000,
    val idleTimeoutMs: Long = 600000,
    val maxLifetimeMs: Long = 1800000,
    val leakDetectionThresholdMs: Long = 60000
)
```

### Automatic Environment Variable Mapping

Hoplite uses smart naming conventions to automatically map environment variables:

| Environment Variable                 | Config Property                   | Data Class Path                          | Example Value                   |
|--------------------------------------|-----------------------------------|------------------------------------------|---------------------------------|
| `HTTP_PORT`                          | `httpPort`                        | `config.httpPort`                        | `8080`                          |
| `DB_URL`                             | `db.url`                          | `config.db.url`                          | `jdbc:postgresql://...`         |
| `DB_USER`                            | `db.user`                         | `config.db.user`                         | `appuser`                       |
| `DB_PASSWORD`                        | `db.password`                     | `config.db.password`                     | `secret`                        |
| `COOKIE_ENCRYPTION_KEY`              | `cookie.encryptionKey`            | `config.cookie.encryptionKey`            | `hex_key`                       |
| `COOKIE_SIGNING_KEY`                 | `cookie.signingKey`               | `config.cookie.signingKey`               | `hex_key`                       |
| `COOKIE_USE_SECURE`                  | `cookie.useSecure`                | `config.cookie.useSecure`                | `true`                          |
| `COOKIE_SAME_SITE`                   | `cookie.sameSite`                 | `config.cookie.sameSite`                 | `Lax`                           |
| `CORS_ALLOWED_HOSTS`                 | `cors.allowedHosts`               | `config.cors.allowedHosts`               | `host1,host2` (comma-separated) |
| `CORS_ALLOWED_HTTPS_HOSTS`           | `cors.allowedHttpsHosts`          | `config.cors.allowedHttpsHosts`          | `domain.com`                    |
| `HIKARI_MAX_POOL_SIZE`               | `hikari.maxPoolSize`              | `config.hikari.maxPoolSize`              | `50`                            |
| `HIKARI_MIN_IDLE`                    | `hikari.minIdle`                  | `config.hikari.minIdle`                  | `10`                            |
| `HIKARI_CONNECTION_TIMEOUT_MS`       | `hikari.connectionTimeoutMs`      | `config.hikari.connectionTimeoutMs`      | `5000`                          |
| `HIKARI_VALIDATION_TIMEOUT_MS`       | `hikari.validationTimeoutMs`      | `config.hikari.validationTimeoutMs`      | `3000`                          |
| `HIKARI_IDLE_TIMEOUT_MS`             | `hikari.idleTimeoutMs`            | `config.hikari.idleTimeoutMs`            | `600000`                        |
| `HIKARI_MAX_LIFETIME_MS`             | `hikari.maxLifetimeMs`            | `config.hikari.maxLifetimeMs`            | `1800000`                       |
| `HIKARI_LEAK_DETECTION_THRESHOLD_MS` | `hikari.leakDetectionThresholdMs` | `config.hikari.leakDetectionThresholdMs` | `60000`                         |

**Conversion Rules:**

- `SCREAMING_SNAKE_CASE` → `camelCase`
- Underscores indicate nesting: `DB_URL` → `db.url`
- Lists from comma-separated values: `host1,host2` → `["host1", "host2"]`

### Environment Selection

Set the `WEB_PLAYGROUND_ENV` environment variable to select the configuration environment:

```bash
# Local development (default)
export WEB_PLAYGROUND_ENV=local

# Production
export WEB_PLAYGROUND_ENV=prod

# Testing
export WEB_PLAYGROUND_ENV=test
```

**Available environments:**

- `local` - Development (uses `app-local.conf`)
- `prod` - Production (uses `app-prod.conf`)
- `test` - Testing (uses `app-test.conf`)

### Configuration Properties

#### Application Settings

| Property                | Environment Variable      | Config File Path       | Default          | Description                        |
|-------------------------|---------------------------|------------------------|------------------|------------------------------------|
| HTTP Port               | `HTTP_PORT`               | `httpPort`             | `4207`           | HTTP server listening port         |
| Use Filesystem Assets   | `USE_FILESYSTEM_ASSETS`   | `useFileSystemAssets`  | `false`          | Serve static files from filesystem |
| Third-Party Service URL | `THIRD_PARTY_SERVICE_URL` | `thirdPartyServiceUrl` | `localhost:9876` | Base URL for demo service          |

#### Database Settings (`db.*`)

| Property          | Environment Variable | Config File Path | Default      | Description         |
|-------------------|----------------------|------------------|--------------|---------------------|
| Database URL      | `DB_URL`             | `db.url`         | H2 in-memory | JDBC connection URL |
| Database User     | `DB_USER`            | `db.user`        | `""`         | Database username   |
| Database Password | `DB_PASSWORD`        | `db.password`    | `""`         | Database password   |

#### Cookie Security Settings (`cookie.*`)

| Property              | Environment Variable    | Config File Path       | Default  | Description                           |
|-----------------------|-------------------------|------------------------|----------|---------------------------------------|
| Use Secure Cookie     | `COOKIE_USE_SECURE`     | `cookie.useSecure`     | `true`   | Enable Secure flag on cookies (HTTPS) |
| Cookie SameSite       | `COOKIE_SAME_SITE`      | `cookie.sameSite`      | `"Lax"`  | SameSite cookie policy                |
| Cookie Encryption Key | `COOKIE_ENCRYPTION_KEY` | `cookie.encryptionKey` | Required | Cookie encryption key (hex)           |
| Cookie Signing Key    | `COOKIE_SIGNING_KEY`    | `cookie.signingKey`    | Required | Cookie signing key (hex)              |

#### CORS Settings (`cors.*`)

| Property           | Environment Variable       | Config File Path         | Default | Description                      |
|--------------------|----------------------------|--------------------------|---------|----------------------------------|
| CORS Allowed Hosts | `CORS_ALLOWED_HOSTS`       | `cors.allowedHosts`      | `[]`    | Comma-separated allowed hosts    |
| CORS HTTPS Hosts   | `CORS_ALLOWED_HTTPS_HOSTS` | `cors.allowedHttpsHosts` | `[]`    | Comma-separated HTTPS-only hosts |

#### HikariCP Connection Pool Settings (`hikari.*`)

| Property                 | Environment Variable                 | Config File Path                  | Default   | Description                                    |
|--------------------------|--------------------------------------|-----------------------------------|-----------|------------------------------------------------|
| Max Pool Size            | `HIKARI_MAX_POOL_SIZE`               | `hikari.maxPoolSize`              | `10`      | Maximum connections in pool                    |
| Min Idle                 | `HIKARI_MIN_IDLE`                    | `hikari.minIdle`                  | `2`       | Minimum idle connections to maintain           |
| Connection Timeout       | `HIKARI_CONNECTION_TIMEOUT_MS`       | `hikari.connectionTimeoutMs`      | `5000`    | Max wait for connection from pool (ms)         |
| Validation Timeout       | `HIKARI_VALIDATION_TIMEOUT_MS`       | `hikari.validationTimeoutMs`      | `3000`    | Max wait for connection validation (ms)        |
| Idle Timeout             | `HIKARI_IDLE_TIMEOUT_MS`             | `hikari.idleTimeoutMs`            | `600000`  | Remove idle connections after this time (ms)   |
| Max Lifetime             | `HIKARI_MAX_LIFETIME_MS`             | `hikari.maxLifetimeMs`            | `1800000` | Recycle connections after this time (ms)       |
| Leak Detection Threshold | `HIKARI_LEAK_DETECTION_THRESHOLD_MS` | `hikari.leakDetectionThresholdMs` | `60000`   | Warn if connection held longer (ms, 0=disable) |

### Example: Local Development (app-local.conf)

```hocon
httpPort = 4207

db {
  url = "jdbc:h2:./build/local;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
  user = ""
  password = ""
}

useFileSystemAssets = true  # Hot-reload static files during development

cookie {
  useSecure = false  # Allow cookies over HTTP for local development
  sameSite = "Lax"
  encryptionKey = "1d13f63b868ad26c46151245e1b5175c"
  signingKey = "d232897cbcc6cc89579bfbfc060632945e0dc519927c891733421f0f4a9ae48f"
}

cors {
  allowedHosts = [
    "localhost:4207",
    "localhost:9000",
    "127.0.0.1:9876"
  ]
  allowedHttpsHosts = []
}

thirdPartyServiceUrl = "http://localhost:9876"
```

### Example: Production Configuration (Environment Variables)

**Docker/Docker Compose:**

```yaml
environment:
  - WEB_PLAYGROUND_ENV=prod
  - DB_URL=jdbc:postgresql://db:5432/myapp
  - DB_USER=appuser
  - DB_PASSWORD=secure_password_from_secret
  - COOKIE_ENCRYPTION_KEY=production_encryption_key_hex
  - COOKIE_SIGNING_KEY=production_signing_key_hex
  - USE_SECURE_COOKIE=true
  - CORS_ALLOWED_HTTPS_HOSTS=myapp.com,www.myapp.com
  - HIKARI_MAX_POOL_SIZE=20
  - HIKARI_CONNECTION_TIMEOUT_MS=3000
```

**Kubernetes/Azure Container Apps:**

```yaml
env:
  - name: WEB_PLAYGROUND_ENV
    value: "prod"
  - name: DB_URL
    value: "jdbc:postgresql://postgres-service:5432/myapp"
  - name: DB_USER
    value: "appuser"
  - name: DB_PASSWORD
    valueFrom:
      secretKeyRef:
        name: db-secret
        key: password
  - name: COOKIE_ENCRYPTION_KEY
    valueFrom:
      secretKeyRef:
        name: app-secret
        key: cookie-encryption-key
  - name: COOKIE_SIGNING_KEY
    valueFrom:
      secretKeyRef:
        name: app-secret
        key: cookie-signing-key
  - name: HIKARI_MAX_POOL_SIZE
    value: "50"
  - name: HIKARI_CONNECTION_TIMEOUT_MS
    value: "3000"
```

**Shell Export (Linux/macOS):**

```bash
export WEB_PLAYGROUND_ENV=prod
export DB_URL="jdbc:postgresql://localhost:5432/myapp"
export DB_USER="appuser"
export DB_PASSWORD="secure_password"
export COOKIE_ENCRYPTION_KEY="prod_encryption_key_hex"
export COOKIE_SIGNING_KEY="prod_signing_key_hex"
export HIKARI_MAX_POOL_SIZE=20
export HIKARI_CONNECTION_TIMEOUT_MS=3000
```

**PowerShell (Windows):**

```powershell
$env:WEB_PLAYGROUND_ENV="prod"
$env:DB_URL="jdbc:postgresql://localhost:5432/myapp"
$env:DB_USER="appuser"
$env:DB_PASSWORD="secure_password"
$env:COOKIE_ENCRYPTION_KEY="prod_encryption_key_hex"
$env:COOKIE_SIGNING_KEY="prod_signing_key_hex"
$env:HIKARI_MAX_POOL_SIZE=20
$env:HIKARI_CONNECTION_TIMEOUT_MS=3000
```

### HikariCP Tuning Guide

#### Development (Low Load)

Use defaults - fast startup, adequate for local testing:

```bash
# No need to set anything, defaults are sensible
```

#### Production (High Traffic)

Increase pool size and reduce timeouts:

```bash
export HIKARI_MAX_POOL_SIZE=50
export HIKARI_MIN_IDLE=10
export HIKARI_CONNECTION_TIMEOUT_MS=3000
export HIKARI_IDLE_TIMEOUT_MS=300000
```

#### Production (Low Latency Requirements)

Aggressive timeouts for fast failure:

```bash
export HIKARI_CONNECTION_TIMEOUT_MS=2000
export HIKARI_VALIDATION_TIMEOUT_MS=1000
```

#### Debugging Connection Leaks

Enable leak detection in development:

```bash
export HIKARI_LEAK_DETECTION_THRESHOLD_MS=10000  # Warn after 10 seconds
```

Disable in production (performance):

```bash
export HIKARI_LEAK_DETECTION_THRESHOLD_MS=0  # Disabled
```

### Configuration Loading Example

**Resolution Order Example:**

```
Environment: prod
Looking for: db.url

Step 1: Check DB_URL environment variable
        → Found: "jdbc:postgresql://prod-db:5432/myapp"
        → Use this value ✓

Step 2: (Skipped - env var found)
        app-prod.conf: db.url = ${WEB_PLAYGROUND_DB_URL}

Step 3: (Skipped - env var found)
        app.conf: db.url = null

Result: config.db.url = "jdbc:postgresql://prod-db:5432/myapp"
```

### Configuration Troubleshooting

#### Problem: Configuration Not Loading

**Symptom:** Application fails to start with Hoplite error

**Solution:** Hoplite provides clear error messages:

```
Error loading config:
- Missing required field: cookie.encryptionKey
- Type mismatch at hikari.maxPoolSize: expected Int, got String
```

Fix by:

1. Checking the error message - it tells you exactly what's wrong
2. Verifying required fields are set (in config file or env vars)
3. Ensuring correct types (numbers as numbers, not strings)

#### Problem: Environment Variables Not Working

**Symptom:** Application uses config file values instead of env vars

**Common Causes:**

1. Wrong naming: `DB.URL` ❌ should be `DB_URL` ✓
2. Not exported: `DB_URL=value` ❌ should be `export DB_URL=value` ✓
3. Wrong shell: PowerShell uses `$env:DB_URL="value"`

**Solution:**

```bash
# Linux/macOS
export DB_URL=jdbc:postgresql://localhost:5432/myapp
export HIKARI_MAX_POOL_SIZE=50
export CORS_ALLOWED_HOSTS=localhost:4207,example.com

# Windows PowerShell
$env:DB_URL="jdbc:postgresql://localhost:5432/myapp"
$env:HIKARI_MAX_POOL_SIZE=50
$env:CORS_ALLOWED_HOSTS="localhost:4207,example.com"

# Verify
echo $DB_URL           # Linux/macOS
echo $env:DB_URL       # PowerShell
```

#### Problem: Nested Properties Not Mapped

**Symptom:** Config property is null despite being set

**Wrong HOCON syntax:**

```hocon
# ❌ This doesn't work with nested data classes
db.url = "jdbc:..."
db.user = "admin"
```

**Correct HOCON syntax:**

```hocon
# ✓ This works with Hoplite
db {
  url = "jdbc:..."
  user = "admin"
}
```

#### Problem: List Properties Not Parsing

**Symptom:** Lists are empty despite being set

**Solutions:**

**In config file:**

```hocon
cors {
  allowedHosts = ["localhost:4207", "example.com"]  # ✓ HOCON array
}
```

**In environment variable:**

```bash
export CORS_ALLOWED_HOSTS=localhost:4207,example.com  # ✓ Comma-separated
```

### Configuration Best Practices

#### ✅ DO

1. **Use environment variables for secrets**
   ```bash
   export DB_PASSWORD=secure_password
   export COOKIE_ENCRYPTION_KEY=production_key
   ```

2. **Use config files for defaults**
   ```hocon
   hikari {
     maxPoolSize = 10
     connectionTimeoutMs = 5000
   }
   ```

3. **Leverage defaults in data classes**
   ```kotlin
   data class HikariConfig(
       val maxPoolSize: Int = 10,  // ✓ Default value
       val minIdle: Int = 2
   )
   ```

4. **Group related settings**
   ```kotlin
   data class WebappConfig(
       val db: DatabaseConfig,     // ✓ Grouped
       val cookie: CookieConfig,   // ✓ Grouped
       val hikari: HikariConfig    // ✓ Grouped
   )
   ```

#### ❌ DON'T

1. **Don't commit secrets to config files**
   ```hocon
   # ❌ Bad - password in source control
   db.password = "production_password"
   
   # ✓ Good - reference env var
   db.password = ${?DB_PASSWORD}
   ```

2. **Don't use flat structure for large configs**
   ```kotlin
   // ❌ Hard to maintain
   data class Config(
       val dbUrl: String,
       val dbUser: String,
       val dbPassword: String,
       val hikariMaxPoolSize: Int,
       val hikariMinIdle: Int,
       // ... 20 more properties
   )
   
   // ✓ Clean and organized
   data class Config(
       val db: DatabaseConfig,
       val hikari: HikariConfig
   )
   ```

3. **Don't make everything required**
   ```kotlin
   // ❌ Forces users to set everything
   data class HikariConfig(
       val maxPoolSize: Int,
       val minIdle: Int
   )
   
   // ✓ Sensible defaults
   data class HikariConfig(
       val maxPoolSize: Int = 10,
       val minIdle: Int = 2
   )
   ```

## Requirements

- **JDK 24** (or JDK 21 LTS - adjust `jvmToolchain` in `build.gradle.kts`)
- Internet access for dependency resolution
- **JAVA_HOME** environment variable set correctly
- **%JAVA_HOME%\bin** added to system PATH

### Setting up Java Environment (Windows)

1. Set `JAVA_HOME` as System environment variable pointing to your JDK installation
2. Add `%JAVA_HOME%\bin` to your PATH
3. Verify:
   ```cmd
   echo %JAVA_HOME%
   java --version
   ```

## Running the Application

### Local Development

#### Run Main Application Only

Windows (PowerShell or CMD):

```bat
gradlew.bat :main-app:run
```

Unix-like:

```bash
./gradlew :main-app:run
```

The main server will start on **http://localhost:4207/**

#### Run Both Services (Main App + Third-Party Service)

**Terminal 1 - Start Third-Party Service:**

Windows:

```bat
gradlew.bat :third-party-service:run
```

Unix-like:

```bash
./gradlew :third-party-service:run
```

**Terminal 2 - Start Main Application:**

Windows:

```bat
gradlew.bat :main-app:run
```

Unix-like:

```bash
./gradlew :main-app:run
```

Services will be available at:

- Main app: **http://localhost:4207/**
- Third-party service: **http://localhost:9876/**

### With Custom Environment

Windows (PowerShell):

```powershell
$env:WEB_PLAYGROUND_ENV="prod"; gradlew.bat run
```

Windows (CMD):

```cmd
set WEB_PLAYGROUND_ENV=prod && gradlew.bat :main-app:run
```

Unix-like:

```bash
WEB_PLAYGROUND_ENV=prod ./gradlew :main-app:run
```

### Testing Endpoints

**Using cURL:**

```bash
# Hello World
curl http://localhost:4207/api

# Health Check (no auth required)
curl http://localhost:4207/api/health

# Prometheus Metrics (no auth required)
curl http://localhost:4207/api/metrics

# Query parameter test
curl "http://localhost:4207/api/param_test?foo=test123"

# JSON endpoint
curl http://localhost:4207/api/json_test

# Get user by ID
curl http://localhost:4207/api/users/1

# Database test
curl http://localhost:4207/api/db_test

# Coroutine demo (async operations)
curl http://localhost:4207/api/coroutine_demo
```

**API Documentation:**

Open your browser to view the interactive Swagger UI:

```
http://localhost:4207/swagger
```

Or download the OpenAPI specification:

```bash
curl http://localhost:4207/openapi
```

### OpenAPI/Swagger Implementation

This project uses a **manually maintained OpenAPI 3.0 specification** with Swagger UI for interactive API documentation.
This is the industry-standard approach for Ktor applications, as Ktor does not provide automatic OpenAPI generation like
Spring Boot.

#### Files Structure

```
main-app/src/main/resources/
├── openapi/
│   └── api-docs.yaml          # OpenAPI 3.0 specification (manually maintained)
└── swagger/
    └── index.html             # Swagger UI page (loads from CDN)
```

#### Available Endpoints

| Endpoint           | Purpose                       | Format | Authentication |
|--------------------|-------------------------------|--------|----------------|
| `GET /api/health`  | Health check for K8s probes   | JSON   | None           |
| `GET /api/metrics` | Prometheus metrics            | Text   | None           |
| `GET /openapi`     | OpenAPI 3.0 specification     | YAML   | None           |
| `GET /swagger`     | Interactive API documentation | HTML   | None           |

#### Updating API Documentation

When you add or modify endpoints:

1. **Implement the endpoint** in Ktor code
2. **Update the OpenAPI spec** in `main-app/src/main/resources/openapi/api-docs.yaml`:
    - Add the path and HTTP method
    - Define request parameters/body schemas
    - Define response schemas and status codes
    - Specify security requirements (if authenticated)
3. **Test in Swagger UI** - refresh http://localhost:4207/swagger to see changes

#### OpenAPI Specification Format

The `api-docs.yaml` follows OpenAPI 3.0 specification:

```yaml
openapi: 3.0.0
info:
  title: Web Playground API
  version: 1.0.0
  description: API documentation for the Web Playground application

paths:
  /api/users/{id}:
    get:
      summary: Get user by ID
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
      responses:
        '200':
          description: User found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/User'
        '404':
          description: User not found

components:
  schemas:
    User:
      type: object
      properties:
        id:
          type: integer
        email:
          type: string
          format: email
        name:
          type: string
```

#### Why Manual OpenAPI?

**Ktor Philosophy:** Ktor is a micro-framework that intentionally stays lightweight and unopinionated. Unlike Spring
Boot (Springdoc) or FastAPI, Ktor does not provide automatic OpenAPI generation. This is by design.

**Benefits:**

- ✅ **Full Control** - You decide exactly what's documented
- ✅ **Reliability** - No dependency on unstable auto-generation libraries
- ✅ **Simplicity** - Clean, maintainable code
- ✅ **Professional** - Industry-standard approach for Ktor applications
- ✅ **Flexibility** - Document complex authentication flows, custom headers, etc.

**How Production Ktor Projects Handle This:**

- JetBrains (Ktor creators) use manual OpenAPI specs
- Enterprise Ktor projects follow this approach
- Ktor documentation recommends this pattern

#### Swagger UI Features

The Swagger UI at `/swagger` provides:

- **Interactive Testing** - Try out API endpoints directly from the browser
- **Request Builder** - Automatically builds requests with proper format
- **Response Viewer** - View actual API responses
- **Schema Validation** - Validates requests against the OpenAPI spec
- **Authentication Support** - Works with both Cookie and JWT authentication
- **Deep Linking** - Share links to specific endpoints

**Using IntelliJ IDEA HTTP Client:**

The project includes `test-requests.http` with pre-configured API requests and multiple environments.

1. **Open the file**: `test-requests.http`

2. **Select environment** from the dropdown in the HTTP client:
    - `local` - Local development server (`http://127.0.0.1:4207`)
    - `docker` - Docker container (`http://localhost:9000`)
    - `azure` - Azure deployment (`https://your-app.azurecontainerapps.io`)

3. **Run requests**: Click the ▶️ (Run) icon next to any request

**Environment Configuration:**

Environments are defined in `http-client.env.json`:

```json
{
  "local": {
    "BaseUrl": "http://127.0.0.1:4207"
  },
  "docker": {
    "BaseUrl": "http://localhost:9000"
  },
  "azure": {
    "BaseUrl": "https://web-playground-app.northeurope.azurecontainerapps.io"
  }
}
```

To add or modify environments, edit `http-client.env.json` and update the `BaseUrl` variable for each environment.

**Available Test Requests:**

- Basic endpoints (hello world, param test, JSON)
- Database operations (get user, create user)
- Authentication (login, logout, protected routes)
- JWT authentication (login, protected endpoint)
- HTML responses and coroutine demos

## Building & Packaging

### Build All Modules

Build both modules with fat JARs:

Windows:

```bat
gradlew.bat clean :main-app:shadowJar :third-party-service:shadowJar
```

Unix-like:

```bash
./gradlew clean :main-app:shadowJar :third-party-service:shadowJar
```

Output:

- `main-app/build/libs/main-app-1.0-SNAPSHOT-all.jar` (~110 MB)
- `third-party-service/build/libs/third-party-service-1.0-SNAPSHOT-all.jar` (~45 MB)

### Build Specific Module

Build only the main application:

```bat
gradlew.bat :main-app:shadowJar
```

Build only the third-party service:

```bat
gradlew.bat :third-party-service:shadowJar
```

### Run from JAR

**Important:** When running from a shadow JAR, ensure `useFileSystemAssets = false` in your configuration to serve
static files from JAR resources instead of the filesystem.

Run main application:

Windows:

```bat
java -jar main-app\build\libs\main-app-1.0-SNAPSHOT-all.jar
```

Unix-like:

```bash
java -jar main-app/build/libs/main-app-1.0-SNAPSHOT-all.jar
```

Run third-party service:

```bash
java -jar third-party-service/build/libs/third-party-service-1.0-SNAPSHOT-all.jar
```

With production configuration:

```bash
WEB_PLAYGROUND_ENV=prod java -jar main-app/build/libs/main-app-1.0-SNAPSHOT-all.jar
```

## Docker Deployment

The project includes Docker support with separate containers for each module.

### Quick Start with Docker

**Option 1: Automated Build Script (Recommended):**

```powershell
# Build both modules and create Docker images
.\build-docker.ps1

# Start both services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

**Option 2: Manual Build Then Docker Compose:**

```bash
# Step 1: Build JARs first
./gradlew clean :main-app:shadowJar :third-party-service:shadowJar

# Step 2: Build Docker images and start services
docker-compose up -d --build
```

**Option 3: Docker Compose Without Pre-building JARs:**

```bash
# Build images using existing JARs (must be already built)
docker-compose up -d
```

### Docker Images

Two Docker images are created:

**main-app:latest:**

- Base Image: `amazoncorretto:24.0.2-alpine3.22`
- JAR: `main-app-1.0-SNAPSHOT-all.jar`
- Exposed Port: 4207 (mapped to host port 9000)
- Size: ~110 MB

**third-party-service:latest:**

- Base Image: `amazoncorretto:24.0.2-alpine3.22`
- JAR: `third-party-service-1.0-SNAPSHOT-all.jar`
- Exposed Port: 9876 (mapped to host port 9876)
- Size: ~45 MB

### Access Services

When running via Docker Compose:

- **Main Application:** http://localhost:9000
    - API endpoints: http://localhost:9000/api/*
    - SPA: http://localhost:9000/
- **Third-Party Service:** http://localhost:9876
    - Ping: http://localhost:9876/ping
    - Random number: http://localhost:9876/random_number
    - Reverse text: http://localhost:9876/reverse (POST)

### Environment Configuration

**Environment Variables:**

**main-app service:**

| Variable                                  | Default                           | Description                                                        |
|-------------------------------------------|-----------------------------------|--------------------------------------------------------------------|
| `WEB_PLAYGROUND_ENV`                      | `prod`                            | Environment name (local, prod)                                     |
| `WEB_PLAYGROUND_HTTP_PORT`                | `4207`                            | HTTP port for the server                                           |
| `WEB_PLAYGROUND_DB_USER`                  | -                                 | Database username                                                  |
| `WEB_PLAYGROUND_DB_PASSWORD`              | -                                 | Database password                                                  |
| `WEB_PLAYGROUND_DB_URL`                   | -                                 | JDBC connection URL                                                |
| `WEB_PLAYGROUND_USE_SECURE_COOKIE`        | `true`                            | Enable secure cookies                                              |
| `WEB_PLAYGROUND_COOKIE_ENCRYPTION_KEY`    | -                                 | Cookie encryption key (hex)                                        |
| `WEB_PLAYGROUND_COOKIE_SIGNING_KEY`       | -                                 | Cookie signing key (hex)                                           |
| `WEB_PLAYGROUND_CORS_ALLOWED_HOSTS`       | -                                 | Comma-separated CORS hosts                                         |
| `WEB_PLAYGROUND_CORS_ALLOWED_HTTPS_HOSTS` | -                                 | Comma-separated HTTPS-only CORS hosts                              |
| `THIRD_PARTY_SERVICE_URL`                 | `http://third-party-service:9876` | Base URL for third-party service (for microservices communication) |

**third-party-service:**

| Variable                   | Default | Description               |
|----------------------------|---------|---------------------------|
| `THIRD_PARTY_SERVICE_PORT` | `9876`  | HTTP port for the service |

**Using .env file:**

1. Copy the example environment file:
   ```bash
   cp .env.example .env
   ```

2. Edit `.env` with your settings:
   ```bash
   WEB_PLAYGROUND_ENV=prod
   WEB_PLAYGROUND_HTTP_PORT=4207
   WEB_PLAYGROUND_DB_URL=jdbc:h2:./build/prod;MODE=PostgreSQL
   WEB_PLAYGROUND_USE_SECURE_COOKIE=false
   # ... more settings
   ```

3. Start with your configuration:
   ```bash
   docker-compose up -d
   ```

### Common Docker Commands

**View logs:**

```powershell
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f main-app
docker-compose logs -f third-party-service

# Last 100 lines
docker-compose logs --tail=100 main-app
```

**Restart services:**

```powershell
docker-compose restart main-app
docker-compose restart third-party-service

# Restart all
docker-compose restart
```

**Stop services:**

```powershell
# Stop but keep containers
docker-compose stop

# Stop and remove containers
docker-compose down

# Stop, remove containers, and remove images
docker-compose down --rmi local

# Stop and remove volumes
docker-compose down -v
```

**Rebuild services:**

```powershell
# Rebuild main-app and restart
docker-compose up -d --build main-app

# Rebuild all services
docker-compose up -d --build
```

**Inspect services:**

```powershell
# List running containers
docker-compose ps

# View resource usage
docker stats

# Enter a container shell
docker exec -it main-app sh
docker exec -it third-party-service sh

# View container details
docker inspect main-app
```

**Scale services:**

```powershell
# Run multiple instances (requires load balancer configuration)
docker-compose up -d --scale main-app=3
```

### Docker Networking

Docker Compose creates a default network where services can communicate:

- **Network Name:** `web-playground_default` (auto-created)
- **Service Discovery:** Services can reach each other by service name
- **Internal Communication:** `http://third-party-service:9876` from main-app
- **External Access:** Both services exposed on host via port mappings

**Network diagram:**

```
┌─────────────────────────────────────┐
│   Docker Network (bridge)           │
│                                     │
│  ┌──────────────┐  ┌──────────────┐ │
│  │  main-app    │  │  third-party │ │
│  │  :4207       │──│    -service  │ │
│  │              │  │    :9876     │ │
│  └──────────────┘  └──────────────┘ │
│         │                  │        │
└─────────┼──────────────────┼────────┘
          │                  │
      localhost:9000    localhost:9876
```

### Troubleshooting Docker Issues

**Build Fails:**

*Problem: Gradle build fails*

```powershell
# Solution: Check Java version (JDK 24 required)
.\gradlew.bat --version
```

*Problem: Docker build fails with "no such file or directory"*

```powershell
# Solution: Ensure JAR files exist
ls main-app/build/libs/main-app-1.0-SNAPSHOT-all.jar
ls third-party-service/build/libs/third-party-service-1.0-SNAPSHOT-all.jar
```

**Container Won't Start:**

View detailed logs:

```powershell
docker logs main-app
docker logs third-party-service
# Or with docker-compose
docker-compose logs main-app
```

Common issues:

- Missing environment variables (especially for main-app)
- Port conflicts (9000 or 9876 already in use)
- Insufficient resources allocated to Docker
- JAR file not found in container

Check container status:

```powershell
docker ps -a
docker-compose ps
```

**Port Already in Use:**

```powershell
# Find what's using the port
netstat -ano | findstr :9000
netstat -ano | findstr :9876

# Kill the process (replace PID with actual process ID)
taskkill /PID <PID> /F

# Or change the port mapping in docker-compose.yml
```

**Service Can't Connect to Another Service:**

*Problem: main-app can't reach third-party-service*

Solutions:

1. Verify both services are running: `docker-compose ps`
2. Check network: `docker network ls` and `docker network inspect web-playground_default`
3. Use service name, not localhost: `http://third-party-service:9876`
4. Ensure `depends_on` is configured in docker-compose.yml

**Database Issues:**

*Problem: Database migration fails or data not persisted*

Solutions:

1. Check database URL in environment variables
2. For persistent data, mount a volume in docker-compose.yml:
   ```yaml
   volumes:
     - ./data:/app/build
   ```
3. View database logs in container logs
4. Reset database by removing volume: `docker-compose down -v`

**Out of Memory:**

*Problem: Container exits with OOM error*

Solutions:

1. Increase Docker Desktop memory limit (Settings → Resources)
2. Add memory limits in docker-compose.yml:
   ```yaml
   services:
     main-app:
       deploy:
         resources:
           limits:
             memory: 512M
   ```
3. Optimize JVM settings: `-Xmx256m` in CMD or ENTRYPOINT

### Development Workflow

**Making code changes:**

1. Edit code in your IDE
2. Rebuild the module:
   ```powershell
   .\gradlew.bat :main-app:shadowJar
   # or
   .\gradlew.bat :third-party-service:shadowJar
   ```
3. Rebuild and restart the container:
   ```powershell
   # Option 1: Rebuild image and restart
   docker build -f main-app/Dockerfile -t main-app:latest ./main-app
   docker-compose up -d main-app

   # Option 2: Use docker-compose build
   docker-compose up -d --build main-app
   ```
4. View logs to verify:
   ```powershell
   docker-compose logs -f main-app
   ```

**Hot reload during development:**

For faster development, run services locally instead of in Docker:

```powershell
# Terminal 1
.\gradlew.bat :third-party-service:run

# Terminal 2
.\gradlew.bat :main-app:run
```

Use Docker only for testing the production-like environment.

### Production Considerations

**1. Use Specific Image Tags:**

Instead of `latest`, use version tags:

```yaml
services:
  main-app:
    image: main-app:1.0.0
```

**2. Security:**

- **Don't commit `.env` files** - Use secrets management
- **Enable secure cookies** - Set `WEB_PLAYGROUND_USE_SECURE_COOKIE=true`
- **Use HTTPS** - Deploy behind a reverse proxy (nginx, Traefik)
- **Scan images** for vulnerabilities: `docker scan main-app:latest`
- **Run as non-root user** in Dockerfile:
  ```dockerfile
  RUN addgroup -S appgroup && adduser -S appuser -G appgroup
  USER appuser
  ```

**3. Health Checks:**

Add health checks to docker-compose.yml:

```yaml
services:
  main-app:
    healthcheck:
      test: [ "CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:4207/api" ]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
```

**4. Resource Limits:**

Set CPU and memory limits:

```yaml
services:
  main-app:
    deploy:
      resources:
        limits:
          cpus: '0.5'
          memory: 512M
        reservations:
          cpus: '0.25'
          memory: 256M
```

**5. Logging:**

Configure logging drivers for centralized log management:

```yaml
services:
  main-app:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

**6. Persistent Data:**

Mount volumes for database persistence:

```yaml
services:
  main-app:
    volumes:
      - app-data:/app/build

volumes:
  app-data:
    driver: local
```

**7. Multi-Stage Builds:**

Multi-stage Docker builds use **multiple `FROM` statements** to create smaller, more secure images. Each `FROM` starts a
new stage, and you can copy artifacts from one stage to another, leaving behind everything you don't need.

**Example - Two-stage build:**

```dockerfile
# Stage 1: Build Stage (Builder)
FROM gradle:8.14-jdk24 AS builder
WORKDIR /build
COPY . .
RUN gradle :main-app:shadowJar --no-daemon

# Stage 2: Runtime Stage (Final Image)
FROM amazoncorretto:24.0.2-alpine3.22
RUN mkdir /app
COPY --from=builder /build/main-app/build/libs/main-app-1.0-SNAPSHOT-all.jar /app/
CMD exec java -jar /app/main-app-1.0-SNAPSHOT-all.jar
```

**How it works:**

- **Stage 1** uses `gradle:8.14-jdk24` (~800 MB) with Gradle pre-installed, builds the JAR
- **Stage 2** starts fresh with `amazoncorretto:24.0.2-alpine3.22` (~150 MB) - Java runtime only
- `COPY --from=builder` copies **only the JAR** from Stage 1, leaving behind source code, Gradle, and build tools

**Benefits of multi-stage builds:**

✅ **Smaller Images** - Final image excludes build tools and source code  
✅ **Better Security** - No source code, Gradle, or build dependencies in production image  
✅ **Reproducible Builds** - Consistent Gradle/JDK environment regardless of local machine  
✅ **Simplified CI/CD** - One command builds JAR and creates Docker image

**Comparison:**

| Approach                          | Image Size | Build Speed | Requires Pre-built JAR? | Best For                       |
|-----------------------------------|------------|-------------|-------------------------|--------------------------------|
| **Current** (pre-built JAR)       | ~200 MB    | ⚡ Very fast | ✅ Yes                   | Local dev, CI with caching     |
| **Multi-stage**                   | ~200 MB    | 🐢 Slower   | ❌ No                    | Public distribution, simple CI |
| **Single-stage with build tools** | ~900+ MB   | 🐢 Slower   | ❌ No                    | Not recommended                |

**When to use each approach:**

**Use current approach (pre-built JAR) when:**

- Local development - faster iterations, test before Docker build
- CI/CD with caching - build JAR once, reuse across multiple deployments
- Multiple environments - build once, deploy to dev/staging/prod

**Use multi-stage when:**

- CI/CD without Gradle - build entirely in Docker (no Gradle on CI server)
- Public distribution - users don't need Gradle installed
- Simpler workflow - one command builds everything

**Advanced: Optimized multi-stage with dependency caching:**

```dockerfile
# Stage 1: Download dependencies (cached layer)
FROM gradle:8.14-jdk24 AS deps
WORKDIR /build
COPY gradle/ gradle/
COPY gradlew gradlew.bat settings.gradle.kts ./
COPY main-app/build.gradle.kts main-app/
RUN gradle :main-app:dependencies --no-daemon

# Stage 2: Build application (uses cached dependencies)
FROM deps AS builder
COPY main-app/src/ main-app/src/
RUN gradle :main-app:shadowJar --no-daemon

# Stage 3: Runtime
FROM amazoncorretto:24.0.2-alpine3.22
RUN mkdir /app
COPY --from=builder /build/main-app/build/libs/main-app-1.0-SNAPSHOT-all.jar /app/
CMD exec java -jar /app/main-app-1.0-SNAPSHOT-all.jar
```

This three-stage approach caches dependencies in a separate layer, so they're only re-downloaded when build files
change, making rebuilds much faster.

**8. Monitoring:**

Add monitoring tools:

- **Prometheus** - Metrics collection
- **Grafana** - Visualization
- **cAdvisor** - Container metrics
- **Jaeger** - Distributed tracing

### Docker Cleanup

**Remove everything:**

```powershell
# Stop and remove containers, networks
docker-compose down

# Also remove volumes
docker-compose down -v

# Remove images
docker rmi main-app:latest third-party-service:latest

# Remove all unused Docker resources
docker system prune -a --volumes
```

**Free up space:**

```powershell
# Remove dangling images
docker image prune

# Remove unused networks
docker network prune

# Remove stopped containers
docker container prune

# Remove everything unused
docker system prune -a
```

## Azure Deployment

The application can be deployed to **Azure Container Apps** for production hosting with automatic scaling, HTTPS, and
zero-downtime deployments.

### Quick Start

```powershell
# 1. Build and tag main-app image
docker build -f main-app/Dockerfile -t main-app:latest ./main-app

# 2. Push to Azure Container Registry
az acr create --resource-group web-playground-rg --name YOUR_UNIQUE_ACR_NAME --sku Basic
az acr login --name YOUR_UNIQUE_ACR_NAME
docker tag main-app:latest YOUR_UNIQUE_ACR_NAME.azurecr.io/main-app:latest
docker push YOUR_UNIQUE_ACR_NAME.azurecr.io/main-app:latest

# 3. Deploy to Azure Container Apps
az containerapp create --name web-playground-app --resource-group web-playground-rg --environment web-playground-env --image YOUR_UNIQUE_ACR_NAME.azurecr.io/main-app:latest --target-port 4207 --ingress external
```

**Note:** For complete microservices deployment (including third-party-service), see the full guide below.

### Complete Guide

For detailed step-by-step instructions, troubleshooting, cost optimization, and advanced configuration, see:

📘 **[AZURE-DEPLOYMENT.md](AZURE-DEPLOYMENT.md)** - Complete Azure Deployment Guide

The guide includes:

- Azure CLI installation and authentication
- Creating Azure Container Registry (ACR)
- Deploying to Azure Container Apps (single service or microservices)
- Deploying both main-app and third-party-service
- Inter-service communication configuration
- Internal vs external ingress patterns
- Environment variable configuration with secrets
- Monitoring, logging, and troubleshooting
- Cost optimization strategies

## Common Gradle Tasks

### All Modules

| Command                                                      | Description                     |
|--------------------------------------------------------------|---------------------------------|
| `gradlew build`                                              | Build all modules + run tests   |
| `gradlew test`                                               | Run all tests in all modules    |
| `gradlew clean`                                              | Clean all build artifacts       |
| `gradlew :main-app:shadowJar :third-party-service:shadowJar` | Build fat JARs for both modules |
| `gradlew tasks --all`                                        | List all available tasks        |

### Main App Module

| Command                       | Description                        |
|-------------------------------|------------------------------------|
| `gradlew :main-app:build`     | Build main-app + run tests         |
| `gradlew :main-app:test`      | Run main-app tests                 |
| `gradlew :main-app:run`       | Start main-app in development mode |
| `gradlew :main-app:shadowJar` | Build main-app fat JAR             |

### Third-Party Service Module

| Command                                  | Description                       |
|------------------------------------------|-----------------------------------|
| `gradlew :third-party-service:build`     | Build third-party-service         |
| `gradlew :third-party-service:run`       | Start third-party-service         |
| `gradlew :third-party-service:shadowJar` | Build third-party-service fat JAR |

## Testing

### Test Structure

Tests are located in `main-app/src/test/` and include:

1. **ApplicationModuleTest.kt** - Unit tests using Ktor's `testApplication` harness (no real network)
2. **SmokeIntegrationTest.kt** - Integration tests with real embedded Netty server
3. **DatabaseTest.kt** - Database layer tests
4. **UserTests.kt** - User service tests

### Running Tests

Run all tests (all modules):

```bat
gradlew.bat test
```

Run tests for specific module:

```bat
gradlew.bat :main-app:test
```

Run specific test class:

```bat
gradlew.bat :main-app:test --tests com.vshpynta.SmokeIntegrationTest
```

Run specific test method:

```bat
gradlew.bat :main-app:test --tests com.vshpynta.ApplicationModuleTest.shouldReturnHelloWorldOnRootGet
```

### Test Environment

Tests use the `test` environment configuration (`app-test.conf`) with an in-memory H2 database. Database schema is
created and populated automatically via Flyway migrations.

## Database

### Database Schema

The application uses Flyway for database migrations located in `main-app/src/main/resources/db/migration/`:

- **V1__initial.sql** - Creates `user_table` with id, email, password_hash, timestamps
- **V2__add_name_to_user.sql** - Adds name column
- **V3__add_tos_accepted_backward_compatible.sql** - Adds Terms of Service acceptance flag
- **V4__make_tos_accepted_not_nullable.sql** - Makes TOS field mandatory
- **R__populate_default_user.sql** - Repeatable migration that populates default test user

### Default User

For testing purposes, a default user is created:

- Email: `user@example.com`
- Password: `password`

### Database Configuration

**Local Development** (H2 file-based):

```hocon
db.url = "jdbc:h2:./build/local;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
```

**Test** (H2 in-memory):

```hocon
db.url = "jdbc:h2:mem:test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
```

## Authentication

The application demonstrates two authentication methods:

### 1. Cookie-Based Session Authentication

- Uses encrypted session cookies
- CSRF protection via custom token validation
- Session stored in memory (UserSession data class)
- Suitable for traditional web applications
- See `AUTHENTICATION-GUIDE.md` for detailed documentation

### 2. JWT Token Authentication

- Stateless authentication with JWT tokens
- Bearer token in Authorization header
- 1-day token expiration
- Suitable for APIs and mobile clients
- See `AUTHENTICATION-GUIDE.md` for detailed documentation

## Error Handling

The application uses **Arrow's functional error handling** with `Either` and `Raise` DSL:

- **Validation errors** - Returns structured error responses (400/404)
- **Database errors** - Mapped to appropriate HTTP status codes
- **Unhandled exceptions** - Caught by StatusPages plugin (500)

Example:

```kotlin
either {
    val userId = call.parameters["id"]?.toLongOrNull()
        ?: raise(ValidationError("Invalid user ID", 400))
    findUserById(dbSession, userId)
        ?: raise(ValidationError("User not found", 404))
}
```

### JSON Serialization Security

**Important:** The application uses Gson with `excludeFieldsWithoutExposeAnnotation()` for security. This means:

- **Only fields with `@Expose` annotation are serialized to JSON**
- This prevents accidental exposure of sensitive data (e.g., password hashes)
- All DTO/response classes must have `@Expose` on fields you want in JSON

Example:

```kotlin
data class PublicUser(
    @Expose val id: Long,
    @Expose val email: String,
    @Expose val name: String?
    // passwordHash field without @Expose will NOT be serialized
)
```

**Why this matters:**

- Prevents accidentally leaking sensitive internal fields
- Forces explicit declaration of what's public in your API
- Common security best practice for production applications

If you see `{}` in JSON responses, check that your data classes have `@Expose` annotations!

## Logging

The application uses **Logback** for logging (configured in `logback.xml`).

Available loggers:

- `com.vshpynta.Main` - Application lifecycle and configuration
- `com.vshpynta.db` - Database operations
- `io.ktor` - Ktor framework logs

Log levels can be adjusted in `logback.xml`.

## Single Page Application (SPA) Support

The application serves static files and supports client-side routing:

- **Development mode** (`useFileSystemAssets = true`): Serves files from `main-app/src/main/resources/public` for
  hot-reload
- **Production mode** (`useFileSystemAssets = false`): Serves files from JAR resources

**URL routing convention:**

- Routes starting with `/api` → Backend API endpoints
- All other routes → Served by SPA (falls back to `index.html`)

This allows the SPA to handle routes like `/home`, `/about`, `/profile` while keeping backend APIs under `/api/*`.

## Dependency & Version Management

### Version Catalogs (Modern Gradle Approach)

All dependency versions are centrally managed using **Gradle Version Catalogs** in `gradle/libs.versions.toml`. This
provides type-safe accessors with IDE autocomplete support.

**gradle/libs.versions.toml:**

```toml
[versions]
ktor = "3.3.2"
logback = "1.5.20"

[libraries]
ktor-bom = { module = "io.ktor:ktor-bom", version.ref = "ktor" }
logback-classic = { module = "ch.qos.logback:logback-classic", version.ref = "logback" }

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version = "2.2.20" }
```

**In build files - Type-safe accessors with autocomplete:**

```kotlin
// Plugins
plugins {
    alias(libs.plugins.kotlin.jvm)  // ✅ Type-safe, IDE autocomplete!
}

// Dependencies
dependencies {
    implementation(libs.logback.classic)  // ✅ Ctrl+Space shows all available libs
    implementation(libs.gson)             // ✅ Ctrl+Click jumps to version catalog
}
```

**To upgrade a dependency:** Edit `gradle/libs.versions.toml` and change the version - all modules automatically use the
new version!

### Ktor BOM (Bill of Materials)

All Ktor modules use the Ktor BOM for version consistency:

```kotlin
dependencies {
    implementation(platform(libs.ktor.bom))  // Version from catalog
    implementation(libs.ktor.server.core)    // Version from BOM
}
```

### Common Configuration

The root build file provides common configuration for all modules:

- **Repositories**: `mavenCentral()` configured for all modules
- **Group & Version**: `com.vshpynta` and `1.0-SNAPSHOT` for all modules
- **JVM Toolchain**: JDK 24 configured for all modules
- **Kotlin stdlib**: Automatically included in all modules
- **Dependency Versions**: Centrally managed in `gradle/libs.versions.toml`

### Plugin Versions

All plugin versions are in the version catalog (`gradle/libs.versions.toml`):

- Kotlin: 2.2.20
- Shadow: 8.1.1
- Foojay Toolchain Resolver: 0.8.0 (in settings.gradle.kts)

## JDK Toolchain

The project uses Gradle's JVM toolchain feature set to JDK 24. The toolchain is configured centrally in the root
`build.gradle.kts` for all modules.

To switch to an LTS version, update the root build file:

```kotlin
// In root build.gradle.kts -> subprojects block
configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
    jvmToolchain(21)  // Switch to Java 21 LTS
}
```

Ensure your IDE's Gradle JVM setting matches the toolchain version.

## Monitoring & Metrics

The application includes comprehensive monitoring and observability features using **Micrometer** with **Prometheus**
metrics export.

### Instrumentation Endpoints

All monitoring endpoints are publicly accessible (no authentication required) as they're typically used by
infrastructure:

| Endpoint           | Purpose                                               | Format                   |
|--------------------|-------------------------------------------------------|--------------------------|
| `GET /api/health`  | Health check for Kubernetes liveness/readiness probes | JSON                     |
| `GET /api/metrics` | Prometheus metrics for monitoring dashboards          | Text (Prometheus format) |

### Health Check

```bash
curl http://localhost:4207/api/health
```

**Response:**

```json
{
  "status": "UP",
  "timestamp": "2026-01-04T...",
  "checks": {
    "application": {
      "status": "UP"
    },
    "database": {
      "status": "UP",
      "message": "Database is reachable"
    }
  }
}
```

### Prometheus Metrics

```bash
curl http://localhost:4207/api/metrics
```

**Available Metrics:**

- **JVM Metrics**: Memory usage, GC stats, thread counts, class loader stats
- **System Metrics**: CPU usage, processor count
- **HTTP Metrics**: Request counts, duration, status codes
- **Custom Metrics**: Add your own using Micrometer API

**Example Output:**

```
# HELP jvm_memory_used_bytes Used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="G1 Eden Space"} 52428800.0
...
# HELP http_server_requests_seconds HTTP request duration
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{method="GET",status="200",uri="/api/health"} 42.0
http_server_requests_seconds_sum{method="GET",status="200",uri="/api/health"} 0.524
...
```

### Micrometer Configuration

The application uses **Micrometer with Prometheus registry**:

```kotlin
// Create Prometheus registry
val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

// Install MicrometerMetrics plugin
install(MicrometerMetrics) {
    registry = appMicrometerRegistry

    // Automatic JVM and system metrics
    meterBinders = listOf(
        ClassLoaderMetrics(),
        JvmMemoryMetrics(),
        JvmGcMetrics(),
        JvmThreadMetrics(),
        ProcessorMetrics()
    )
}
```

**Dependencies:**

- `io.ktor:ktor-server-metrics-micrometer` (from Ktor BOM)
- `io.micrometer:micrometer-registry-prometheus:1.15.5` (matches Ktor BOM version)

### Integrating with Monitoring Tools

#### Prometheus

Add to your `prometheus.yml`:

```yaml
scrape_configs:
  - job_name: 'kotlin-web-playground'
    scrape_interval: 15s
    static_configs:
      - targets: [ 'localhost:4207' ]
    metrics_path: '/api/metrics'
```

#### Grafana

1. Add Prometheus as a data source
2. Import a JVM dashboard (e.g., Grafana dashboard #4701)
3. Create custom dashboards for your application metrics

#### Kubernetes

Use the health endpoint for liveness/readiness probes:

```yaml
livenessProbe:
  httpGet:
    path: /api/health
    port: 4207
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /api/health
    port: 4207
  initialDelaySeconds: 10
  periodSeconds: 5
```

### Adding Custom Metrics

Use Micrometer's API to add custom metrics:

```kotlin
// Counter
val requestCounter = registry.counter("api.requests", "endpoint", "/api/users")
requestCounter.increment()

// Gauge
registry.gauge("active.connections", activeConnections)

// Timer
registry.timer("api.request.duration", "endpoint", "/api/users").record {
    // Your operation here
}

// Distribution Summary
registry.summary("response.size", "endpoint", "/api/users").record(responseSize.toDouble())
```

## Rate Limiting

### Why Rate Limiting is NOT in Application Code

This application **intentionally does not include** application-level rate limiting. Here's why:

### ❌ Problems with Application-Level Rate Limiting

**In-Memory Limitations:**

- ❌ **Lost on restart** - Rate limit state disappears when pod restarts
- ❌ **Not distributed** - Each pod has independent limits (user can bypass by hitting different pods)
- ❌ **Memory leaks** - Unbounded growth with unique keys (IPs, user IDs)
- ❌ **No persistence** - Can't track long-term abuse patterns

**Architectural Issues:**

```
User makes 100 req/min → Pod 1 ✅ Allowed
User makes 100 req/min → Pod 2 ✅ Allowed  
User makes 100 req/min → Pod 3 ✅ Allowed
Total: 300 req/min ❌ Limit bypassed!
```

### ✅ Recommended Approach: Infrastructure-Level Rate Limiting

Rate limiting should be implemented **before requests reach your application** using infrastructure components.

#### Option 1: API Gateway (Best for Production)

**Kong Gateway:**

```yaml
plugins:
  - name: rate-limiting
    config:
      minute: 100
      hour: 5000
      policy: redis  # Distributed across all gateway instances
      redis:
        host: redis.example.com
        port: 6379
```

**NGINX Plus / NGINX Ingress:**

```nginx
# Define rate limit zone (10MB can track ~160k IPs)
limit_req_zone $binary_remote_addr zone=api_limit:10m rate=100r/m;

location /api {
    # Apply rate limiting with burst allowance
    limit_req zone=api_limit burst=20 nodelay;
    
    # Return 429 with custom message
    limit_req_status 429;
    
    proxy_pass http://backend;
}
```

**AWS API Gateway:**

```yaml
# serverless.yml or CloudFormation
Resources:
  ApiGateway:
    Type: AWS::ApiGateway::RestApi
    Properties:
      Name: web-playground-api

  UsagePlan:
    Type: AWS::ApiGateway::UsagePlan
    Properties:
      Throttle:
        BurstLimit: 200
        RateLimit: 100
```

**Azure API Management:**

```xml

<policies>
    <inbound>
        <rate-limit calls="100" renewal-period="60"/>
        <quota calls="10000" renewal-period="86400"/>
    </inbound>
</policies>
```

**Google Cloud Armor:**

```yaml
securityPolicy:
  rateLimitOptions:
    rateLimitThreshold:
      count: 100
      intervalSec: 60
```

#### Option 2: Load Balancer

**HAProxy:**

```haproxy
frontend api
    bind *:443
    
    # Track by source IP
    stick-table type ip size 100k expire 1m store http_req_rate(60s)
    
    # Rate limit: 100 req/min
    http-request track-sc0 src
    http-request deny if { sc_http_req_rate(0) gt 100 }
    
    default_backend app_servers
```

**Traefik:**

```yaml
http:
  middlewares:
    rate-limit:
      rateLimit:
        average: 100
        period: 1m
        burst: 20
```

#### Option 3: CDN / DDoS Protection

**Cloudflare Rate Limiting:**

```
Rules → Rate Limiting:
  - If requests per minute > 100
  - From same IP
  - Then block for 1 hour
```

**Cloudflare Workers (Advanced):**

```javascript
export default {
  async fetch(request, env) {
    const ip = request.headers.get('CF-Connecting-IP');
    const key = `rate_limit:${ip}`;
    
    const count = await env.KV.get(key);
    if (count > 100) {
      return new Response('Too Many Requests', { status: 429 });
    }
    
    await env.KV.put(key, (count || 0) + 1, { expirationTtl: 60 });
    return fetch(request);
  }
};
```

### Production Architecture

```
┌──────────────────────────────────────────────────┐
│ Internet                                         │
└───────────────────────┬──────────────────────────┘
                        │
                   ┌────▼──────┐
                   │ Cloudflare│  1. DDoS protection
                   │ / CDN     │  2. Bot detection
                   └────┬──────┘  3. Global rate limiting
                        │
                   ┌────▼────────┐
                   │ API Gateway │  1. Authentication
                   │ (Kong/NGINX)│  2. Rate limiting (100/min/IP)
                   └────┬────────┘  3. Request routing
                        │
              ┌─────────┼─────────┐
              │         │         │
         ┌────▼───┐ ┌───▼────┐ ┌─▼──────┐
         │Pod 1   │ │Pod 2   │ │Pod 3   │  Application logic ONLY
         │        │ │        │ │        │  No rate limiting here!
         └────────┘ └────────┘ └────────┘
```

### When Application-Level Rate Limiting IS Appropriate

Use application-level rate limiting (with Redis) ONLY for:

✅ **Business logic limits:**

```kotlin
// Example: Premium users get higher API quotas
class UserQuotaService(private val redis: RedisClient) {
    fun checkQuota(userId: String, userTier: UserTier): Boolean {
        val limit = when (userTier) {
            UserTier.FREE -> 100
            UserTier.PREMIUM -> 1000
            UserTier.ENTERPRISE -> 10000
        }
        return redis.checkAndIncrement("quota:$userId", limit, 24.hours)
    }
}
```

✅ **Per-tenant limits in multi-tenant systems**
✅ **API endpoint-specific limits** (e.g., expensive operations)
✅ **Already behind API gateway** that handles basic DDoS

### Redis-Based Distributed Rate Limiting (If Needed)

If you **must** implement application-level rate limiting, use Redis:

**Dependencies:**

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.bucket4j:bucket4j-redis:8.5.0")
    implementation("redis.clients:jedis:5.0.0")
}
```

**Implementation:**

```kotlin
class RedisRateLimiter(private val redis: JedisPool) {
    fun isAllowed(key: String, maxRequests: Int, window: Duration): Boolean {
        val redisKey = "rate_limit:$key"
        val now = Instant.now().toEpochMilli()
        val windowStart = now - window.toMillis()

        return redis.resource.use { conn ->
            // Atomic operations in Redis
            val pipe = conn.pipelined()

            // Remove old entries
            pipe.zremrangeByScore(redisKey, 0.0, windowStart.toDouble())

            // Count current requests
            val countResponse = pipe.zcard(redisKey)

            // Execute pipeline
            pipe.sync()

            val count = countResponse.get()

            if (count < maxRequests) {
                conn.zadd(redisKey, now.toDouble(), UUID.randomUUID().toString())
                conn.expire(redisKey, window.seconds.toInt())
                true
            } else {
                false
            }
        }
    }
}
```

**Using Bucket4j:**

```kotlin
val proxyManager = RedisProxyManager.builderFor(jedisPool).build()

val bucket = proxyManager.builder().build(userId) {
    Bandwidth.simple(100, Duration.ofMinutes(1))
}

if (bucket.tryConsume(1)) {
    // Allow request
} else {
    // Reject with 429
}
```

### Monitoring Rate Limiting

**Prometheus Metrics (from API Gateway):**

```
rate_limit_exceeded_total{endpoint="/api/users"} 42
rate_limit_allowed_total{endpoint="/api/users"} 9958
```

**Grafana Dashboard:**

- Requests per second by endpoint
- Rate limit rejections over time
- Top rate-limited IPs/users

### Key Takeaways

✅ **DO:** Implement rate limiting at API Gateway/Load Balancer  
✅ **DO:** Use CDN for DDoS protection  
✅ **DO:** Use Redis for distributed application limits (if needed)  
❌ **DON'T:** Use in-memory rate limiting in microservices  
❌ **DON'T:** Implement in application code for basic protection  
❌ **DON'T:** Trust client-side rate limiting

**Remember:** Rate limiting is **infrastructure concern**, not application logic concern.

## Future Enhancements

- [x] ~~Health check endpoint (`GET /api/health`)~~ - **Implemented!** ✅
- [x] ~~Code quality tools (Detekt / ktlint)~~ - **Implemented!** ✅
- [x] ~~Gradle version catalog (`libs.versions.toml`)~~ - **Implemented!** ✅
- [x] ~~Config file (HOCON) and typed settings~~ - **Implemented!** ✅
- [x] ~~OpenAPI/Swagger documentation~~ - **Implemented!** ✅
- [x] ~~Metrics and monitoring (Micrometer)~~ - **Implemented!** ✅
- [ ] Rate limiting (API Gateway level - see Rate Limiting section)
- [ ] Docker image with multi-stage build
- [ ] Database connection from connection string secret
- [ ] Production-grade session storage (Redis, database)
- [ ] Distributed tracing (OpenTelemetry)
- [ ] Circuit breakers and resilience patterns

## Troubleshooting

| Issue                             | Solution                                                             |
|-----------------------------------|----------------------------------------------------------------------|
| `NoSuchMethodError` with Ktor     | Mixed Ktor versions - verify BOM and remove explicit versions        |
| `java` command not found          | Add `%JAVA_HOME%\bin` to PATH (not just `%JAVA_HOME%`)               |
| Port 4207 already in use          | Change `httpPort` in config or free the port                         |
| Static files not found in JAR     | Ensure `useFileSystemAssets = false` in production config            |
| Database migration errors         | Delete `build/local.mv.db` and restart for fresh database            |
| Cookie authentication not working | Check `cookieEncryptionKey` and `cookieSigningKey` are set correctly |
| CORS errors                       | Verify CORS configuration in `Main.kt`                               |
| 404 for API endpoints             | Ensure routes are prefixed with `/api`                               |

## Contribution Guidelines

1. **Keep BOM aligned** - Avoid mixing Ktor versions; use BOM for all Ktor modules
2. **Add KDoc** - Document public APIs and complex functions
3. **Run tests before commits** - `./gradlew test`
4. **Update README** - Document new endpoints, configuration, or major changes
5. **Follow project structure** - Keep separation between layers (web, service, db)
6. **Use functional error handling** - Leverage Arrow's `Either` and `Raise` DSL
7. **Add migrations for schema changes** - Create Flyway migration files in `db/migration`
8. **Test both authentication methods** - Ensure changes work with cookies and JWT
9. **Module-specific changes** - Build and test only affected modules during development

## Additional Documentation

- **[AUTHENTICATION-GUIDE.md](AUTHENTICATION-GUIDE.md)** - Detailed guide for cookie and JWT authentication
- **[AZURE-DEPLOYMENT.md](AZURE-DEPLOYMENT.md)** - Step-by-step Azure deployment instructions with microservices support

## License

This is a playground/learning project. Feel free to use, modify, and experiment!

---

**Happy coding!** Experiment freely, extend routes, add middleware, and build something awesome.

