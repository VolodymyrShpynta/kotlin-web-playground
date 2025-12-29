# Kotlin Web Playground

A comprehensive Kotlin (JVM) web application showcasing modern backend development with Ktor framework. This project
demonstrates authentication, database operations, functional error handling, and Single Page Application (SPA) hosting.

## Overview

This playground application provides a production-ready foundation for building web applications with:

- **Cookie-based and JWT authentication** with CSRF protection
- **Database management** with migrations and connection pooling
- **RESTful API endpoints** with functional error handling (Arrow)
- **Single Page Application** hosting with client-side routing support
- **Coroutine-based async operations** with HTTP client examples
- **Comprehensive testing** with integration and unit tests

## Table of Contents

- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Configuration](#configuration)
- [Requirements](#requirements)
- [Running the Application](#running-the-application)
- [Building & Packaging](#building--packaging)
- [Docker](#docker)
- [Azure Deployment](#azure-deployment)
- [Common Gradle Tasks](#common-gradle-tasks)
- [Testing](#testing)
- [Database](#database)
- [Authentication](#authentication)
- [Error Handling](#error-handling)
- [Logging](#logging)
- [Single Page Application (SPA) Support](#single-page-application-spa-support)
- [Dependency & Version Management](#dependency--version-management)
- [JDK Toolchain](#jdk-toolchain)
- [Future Enhancements](#future-enhancements)
- [Troubleshooting](#troubleshooting)
- [Contribution Guidelines](#contribution-guidelines)
- [Documentation](#documentation)
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
- **Typesafe Config**: HOCON configuration management (v1.4.3)
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

## Architecture

### Project Structure

```
web-playground/
├── build.gradle.kts                    # Build configuration with Shadow plugin
├── settings.gradle.kts                 # Plugin management and toolchain
├── src/
│   ├── main/
│   │   ├── kotlin/com/vshpynta/
│   │   │   ├── Main.kt                 # Application entry point
│   │   │   ├── config/                 # Configuration classes
│   │   │   ├── db/                     # Database utilities and mappings
│   │   │   ├── model/                  # Domain models (User, etc.)
│   │   │   ├── security/               # Authentication (UserSession)
│   │   │   ├── service/                # Business logic
│   │   │   └── web/                    # Web layer (DTOs, responses, validation)
│   │   └── resources/
│   │       ├── app.conf                # Base configuration
│   │       ├── app-local.conf          # Local development config
│   │       ├── app-prod.conf           # Production config
│   │       ├── app-test.conf           # Test config
│   │       ├── logback.xml             # Logging configuration
│   │       ├── db/migration/           # Flyway SQL migrations
│   │       └── public/                 # Static files for SPA
│   │           ├── index.html
│   │           ├── css/app.css
│   │           └── js/app.js
│   └── test/
│       ├── kotlin/com/vshpynta/
│       │   ├── ApplicationModuleTest.kt    # Unit tests
│       │   ├── SmokeIntegrationTest.kt     # Integration tests
│       │   └── db/DatabaseTest.kt          # Database tests
│       └── resources/
│           └── app-test.conf               # Test environment config
└── AUTHENTICATION-GUIDE.md             # Comprehensive auth documentation
```

### API Endpoints

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

### Third-Party Demo Service

A demo service runs on port **9876** to showcase coroutine-based HTTP calls:

- `GET /random_number` - Returns random number after random delay
- `GET /ping` - Returns "pong"
- `POST /reverse` - Reverses the posted body text

## Configuration

### Environment-Based Configuration

The application uses HOCON (Typesafe Config) for configuration management. Environment is selected via the
`WEB_PLAYGROUND_ENV` environment variable (defaults to `local`).

**Available environments:**

- `local` - Development (uses `app-local.conf`)
- `prod` - Production (uses `app-prod.conf`)
- `test` - Testing (uses `app-test.conf`)

**Configuration hierarchy:**

1. `app.conf` - Base configuration
2. `app-{env}.conf` - Environment-specific overrides

### Configuration Properties

| Property              | Description                              | Default      |
|-----------------------|------------------------------------------|--------------|
| `httpPort`            | HTTP server port                         | `4207`       |
| `db.url`              | JDBC connection URL                      | H2 in-memory |
| `db.user`             | Database username                        | `null`       |
| `db.password`         | Database password                        | `null`       |
| `useFileSystemAssets` | Serve static files from filesystem (dev) | `false`      |
| `useSecureCookie`     | Enable secure flag on session cookies    | `true`       |
| `cookieSameSite`      | SameSite cookie policy                   | `"Lax"`      |
| `cookieEncryptionKey` | Cookie encryption key (hex)              | Required     |
| `cookieSigningKey`    | Cookie signing key (hex)                 | Required     |

### Example: app-local.conf

```hocon
db.user = ""
db.password = ""
db.url = "jdbc:h2:./build/local;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"

useFileSystemAssets = true  # Hot-reload static files during development

useSecureCookie = false  # Allow cookies over HTTP for local development
cookieEncryptionKey = "1d13f63b868ad26c46151245e1b5175c"
cookieSigningKey = "d232897cbcc6cc89579bfbfc060632945e0dc519927c891733421f0f4a9ae48f"
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

### Development Mode (from IDE or Gradle)

Windows (PowerShell or CMD):

```bat
gradlew.bat run
```

Unix-like:

```bash
./gradlew run
```

The server will start on **http://localhost:4207/**

### With Custom Environment

Windows (PowerShell):

```powershell
$env:WEB_PLAYGROUND_ENV="prod"; gradlew.bat run
```

Windows (CMD):

```cmd
set WEB_PLAYGROUND_ENV=prod && gradlew.bat run
```

Unix-like:

```bash
WEB_PLAYGROUND_ENV=prod ./gradlew run
```

### Testing Endpoints

**Using cURL:**

```bash
# Hello World
curl http://localhost:4207/api

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

### Fat/Uber JAR with Shadow Plugin

The project uses the **Shadow Plugin** to create self-contained executable JAR files with all dependencies included.

#### Build the Shadow JAR

Windows:

```bat
gradlew.bat shadowJar
```

Unix-like:

```bash
./gradlew shadowJar
```

Output: `build/libs/web-playground-1.0-SNAPSHOT-all.jar`

#### Run the Shadow JAR

**Important:** When running from a shadow JAR, ensure `useFileSystemAssets = false` in your configuration to serve
static files from JAR resources instead of the filesystem.

Windows:

```bat
java -jar build\libs\web-playground-1.0-SNAPSHOT-all.jar
```

Unix-like:

```bash
java -jar build/libs/web-playground-1.0-SNAPSHOT-all.jar
```

With production configuration:

```bash
WEB_PLAYGROUND_ENV=prod java -jar build/libs/web-playground-1.0-SNAPSHOT-all.jar
```

#### Verify JAR Contents

```bash
jar tf build/libs/web-playground-1.0-SNAPSHOT-all.jar | grep public
```

### Standard Distribution

Create a distribution with shell scripts (dependencies not bundled into single JAR):

```bash
./gradlew installDist
```

Run:

```bash
# Windows
build\install\web-playground\bin\web-playground.bat

# Unix-like
./build/install/web-playground/bin/web-playground
```

Create ZIP distribution:

```bash
./gradlew distZip
```

## Docker

The project includes a Dockerfile for containerization using Amazon Corretto Java 24 on Alpine Linux.

### Build Docker Image

First, build the fat JAR:

Windows:

```bat
gradlew.bat shadowJar
```

Unix-like:

```bash
./gradlew shadowJar
```

Then build the Docker image:

```bash
docker build -f Dockerfile -t web-playground:latest .
```

### Run Application in Docker

#### Option 1: Docker Compose (Recommended)

The easiest way to run the application with all necessary environment variables:

1. **Copy the example environment file:**
   ```bash
   cp .env.example .env
   ```

2. **Edit `.env` file with your configuration** (optional - defaults work for local testing)

3. **Run with Docker Compose:**
   ```bash
   docker-compose up -d
   ```

4. **View logs:**
   ```bash
   docker-compose logs -f
   ```

5. **Stop the application:**
   ```bash
   docker-compose down
   ```

The application will be accessible at **http://localhost:9000/**

#### Option 2: Docker Run (Basic)

Run the container directly with default local configuration:

```bash
docker run -p 9000:4207 --name web-playground web-playground:latest
```

#### Option 3: Docker Run with Environment Variables

For production with custom configuration:

```bash
docker run -p 9000:4207 --name web-playground \
  -e WEB_PLAYGROUND_ENV=prod \
  -e WEB_PLAYGROUND_HTTP_PORT=4207 \
  -e WEB_PLAYGROUND_DB_USER="" \
  -e WEB_PLAYGROUND_DB_PASSWORD="" \
  -e WEB_PLAYGROUND_DB_URL="jdbc:h2:./build/prod;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;" \
  -e WEB_PLAYGROUND_USE_SECURE_COOKIE=false \
  web-playground:latest
```

#### Option 4: Docker Run with Environment File

```bash
docker run -p 9000:4207 --name web-playground --env-file .env web-playground:latest
```

The application will be accessible at **http://localhost:9000/**

### Docker Commands

| Command                                                               | Description               |
|-----------------------------------------------------------------------|---------------------------|
| `docker build -f Dockerfile -t web-playground:latest .`               | Build Docker image        |
| `docker-compose up -d`                                                | Start with Docker Compose |
| `docker-compose down`                                                 | Stop Docker Compose       |
| `docker-compose logs -f`                                              | View Docker Compose logs  |
| `docker run -p 9000:4207 --name web-playground web-playground:latest` | Run container (direct)    |
| `docker ps`                                                           | List running containers   |
| `docker logs web-playground`                                          | View container logs       |
| `docker stop web-playground`                                          | Stop the container        |
| `docker rm web-playground`                                            | Remove the container      |
| `docker rmi web-playground:latest`                                    | Remove the image          |

### Environment Configuration

The application uses environment variables for configuration when running in production mode (
`WEB_PLAYGROUND_ENV=prod`):

| Variable                           | Description                   | Example Value                                                  |
|------------------------------------|-------------------------------|----------------------------------------------------------------|
| `WEB_PLAYGROUND_ENV`               | Environment name              | `prod` (or `local` for default)                                |
| `WEB_PLAYGROUND_HTTP_PORT`         | HTTP server port              | `4207`                                                         |
| `WEB_PLAYGROUND_DB_USER`           | Database username             | `` (empty for H2 embedded)                                     |
| `WEB_PLAYGROUND_DB_PASSWORD`       | Database password             | `` (empty for H2 embedded)                                     |
| `WEB_PLAYGROUND_DB_URL`            | JDBC connection URL           | `jdbc:h2:./build/prod;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;` |
| `WEB_PLAYGROUND_USE_SECURE_COOKIE` | Enable secure cookies (HTTPS) | `false` (local), `true` (production with HTTPS)                |

**See `.env.example` for a complete configuration template.**

### Docker Image Details

- **Base Image**: `amazoncorretto:24.0.2-alpine3.22`
- **Java Version**: OpenJDK 24.0.2 (Amazon Corretto)
- **OS**: Alpine Linux 3.22
- **Application JAR**: `web-playground-1.0-SNAPSHOT-all.jar`
- **Container Port**: 4207

## Azure Deployment

The application can be deployed to **Azure Container Apps** for production hosting with automatic scaling, HTTPS, and zero-downtime deployments.

### Quick Start

```powershell
# 1. Build and tag image
docker build -f Dockerfile -t web-playground:latest .

# 2. Push to Azure Container Registry
az acr create --resource-group web-playground-rg --name YOUR_UNIQUE_ACR_NAME --sku Basic
az acr login --name YOUR_UNIQUE_ACR_NAME
docker tag web-playground:latest YOUR_UNIQUE_ACR_NAME.azurecr.io/web-playground:latest
docker push YOUR_UNIQUE_ACR_NAME.azurecr.io/web-playground:latest

# 3. Deploy to Azure Container Apps
az containerapp create --name web-playground-app --resource-group web-playground-rg --environment web-playground-env --image YOUR_UNIQUE_ACR_NAME.azurecr.io/web-playground:latest --target-port 4207 --ingress external
```

### Complete Guide

For detailed step-by-step instructions, troubleshooting, cost optimization, and advanced configuration, see:

📘 **[AZURE-DEPLOYMENT.md](AZURE-DEPLOYMENT.md)** - Complete Azure Deployment Guide

The guide includes:
- Azure CLI installation and authentication
- Creating Azure Container Registry (ACR)
- Deploying to Azure Container Apps
- Environment variable configuration with secrets
- Monitoring, logging, and troubleshooting
- Cost optimization strategies
- CI/CD setup with GitHub Actions
- Integration with Azure Database for PostgreSQL

## Common Gradle Tasks

| Command               | Description                         |
|-----------------------|-------------------------------------|
| `gradlew build`       | Compile + run tests                 |
| `gradlew test`        | Run all tests                       |
| `gradlew run`         | Start server in development mode    |
| `gradlew shadowJar`   | Build fat JAR with all dependencies |
| `gradlew installDist` | Create distribution with scripts    |
| `gradlew distZip`     | Create distribution ZIP             |
| `gradlew clean`       | Clean build artifacts               |
| `gradlew tasks --all` | List all available tasks            |

## Testing

### Test Structure

The project includes comprehensive tests:

1. **ApplicationModuleTest.kt** - Unit tests using Ktor's `testApplication` harness (no real network)
2. **SmokeIntegrationTest.kt** - Integration tests with real embedded Netty server
3. **DatabaseTest.kt** - Database layer tests

### Running Tests

Run all tests:

```bat
gradlew.bat test
```

Run specific test class:

```bat
gradlew.bat test --tests com.vshpynta.SmokeIntegrationTest
```

Run specific test method:

```bat
gradlew.bat test --tests com.vshpynta.ApplicationModuleTest.shouldReturnHelloWorldOnRootGet
```

### Test Environment

Tests use the `test` environment configuration (`app-test.conf`) with an in-memory H2 database. Database schema is
created and populated automatically via Flyway migrations.

## Database

### Database Schema

The application uses Flyway for database migrations located in `src/main/resources/db/migration/`:

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

## Logging

The application uses **Logback** for logging (configured in `logback.xml`).

Available loggers:

- `com.vshpynta.Main` - Application lifecycle and configuration
- `com.vshpynta.db` - Database operations
- `io.ktor` - Ktor framework logs

Log levels can be adjusted in `logback.xml`.

## Single Page Application (SPA) Support

The application serves static files and supports client-side routing:

- **Development mode** (`useFileSystemAssets = true`): Serves files from `src/main/resources/public` for hot-reload
- **Production mode** (`useFileSystemAssets = false`): Serves files from JAR resources

**URL routing convention:**

- Routes starting with `/api` → Backend API endpoints
- All other routes → Served by SPA (falls back to `index.html`)

This allows the SPA to handle routes like `/home`, `/about`, `/profile` while keeping backend APIs under `/api/*`.

## Dependency & Version Management

### Ktor BOM (Bill of Materials)

All Ktor modules are managed via the Ktor BOM to ensure version consistency:

```kotlin
dependencies {
    implementation(platform("io.ktor:ktor-bom:3.3.2"))
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    // ... other Ktor modules without explicit versions
}
```

To upgrade Ktor, change only the BOM version and re-sync.

### Plugin Versions

Plugin versions are centralized in `settings.gradle.kts`:

- Kotlin: 2.2.20
- Shadow: 8.1.1
- Foojay Toolchain Resolver: 0.8.0

## JDK Toolchain

The project uses Gradle's JVM toolchain feature set to JDK 24. To switch to LTS version:

```kotlin
// In build.gradle.kts
kotlin {
    jvmToolchain(21)  // Switch to Java 21 LTS
}
```

Ensure your IDE's Gradle JVM setting matches the toolchain version.

## Future Enhancements

- [ ] Health check endpoint (`GET /api/health`)
- [ ] Code quality tools (Detekt / ktlint)
- [ ] Gradle version catalog (`libs.versions.toml`)
- Config file (HOCON) and typed settings
- [ ] Docker image with multi-stage build
- [ ] Database connection from connection string secret
- [ ] Production-grade session storage (Redis, database)
- [ ] Rate limiting middleware
- [ ] OpenAPI/Swagger documentation
- [ ] Metrics and monitoring (Micrometer)

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

## Documentation

- **AUTHENTICATION-GUIDE.md** - Comprehensive guide covering cookie and JWT authentication with examples
- **Main.kt KDoc** - Detailed documentation of application architecture and routing
- **Code comments** - Inline explanations throughout the codebase

## License

This is a playground/learning project. Feel free to use, modify, and experiment!

---

**Happy coding!** Experiment freely, extend routes, add middleware, and build something awesome.

