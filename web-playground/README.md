# Kotlin Web Playground

A comprehensive multi-module Kotlin project demonstrating modern web development patterns with **Ktor** and **Spring
Boot 4.0.1**, featuring coroutines, non-blocking I/O, microservices architecture, authentication, database management,
and functional error handling.

> **Multi-Module Architecture:** This project uses a modular structure with three independent services that can be
> built, deployed, and scaled independently.

## 📋 Table of Contents

- [Overview](#overview)
- [Project Architecture](#project-architecture)
    - [Multi-Module Structure](#multi-module-structure)
    - [Service Communication](#service-communication)
- [Technology Stack](#technology-stack)
    - [Frameworks & Libraries](#frameworks--libraries)
    - [Key Dependencies](#key-dependencies)
- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Quick Start](#quick-start)
- [Module Documentation](#module-documentation)
    - [Catalog Service (Spring Boot)](#catalog-service-spring-boot)
        - [Features](#features)
        - [API Endpoints](#api-endpoints)
        - [Architecture Highlights](#architecture-highlights)
        - [Configuration](#configuration)
        - [IntelliJ HTTP Client Integration](#intellij-http-client-integration)
    - [Main App (Ktor)](#main-app-ktor)
        - [Features](#features-1)
        - [Configuration with Hoplite](#configuration-with-hoplite)
        - [API Endpoints](#api-endpoints-1)
        - [Authentication](#authentication)
        - [Database](#database)
        - [Error Handling](#error-handling)
        - [JSON Serialization Security](#json-serialization-security)
        - [Single Page Application (SPA) Support](#single-page-application-spa-support)
        - [OpenAPI & Swagger Documentation](#openapi--swagger-documentation)
        - [Monitoring & Metrics](#monitoring--metrics)
        - [IntelliJ HTTP Client Integration](#intellij-http-client-integration)
    - [Third-Party Service (Ktor)](#third-party-service-ktor)
- [Key Features & Patterns](#key-features--patterns)
    - [1. Spring Boot 4.0.1 with Kotlin Coroutines](#1-spring-boot-401-with-kotlin-coroutines)
    - [2. Non-Blocking HTTP Client (WebClient)](#2-non-blocking-http-client-webclient)
    - [3. Dispatcher Selection for I/O Operations](#3-dispatcher-selection-for-io-operations)
- [Configuration & Best Practices](#configuration--best-practices)
    - [Modern Configuration with Hoplite (Main App)](#modern-configuration-with-hoplite-main-app)
    - [1. Dependency Management](#1-dependency-management)
    - [2. Gradle Build Configuration](#2-gradle-build-configuration)
    - [3. Docker Configuration](#3-docker-configuration)
- [Development Guide](#development-guide)
    - [Building the Project](#building-the-project)
    - [Running Services Locally](#running-services-locally)
    - [Testing](#testing)
    - [Code Quality](#code-quality)
- [Deployment](#deployment)
    - [Docker Compose (Recommended)](#docker-compose-recommended)
    - [Docker Compose Configuration](#docker-compose-configuration)
- [Azure Deployment](#azure-deployment)
    - [Quick Start](#quick-start-2)
    - [Microservices Architecture on Azure](#microservices-architecture-on-azure)
    - [Complete Guide](#complete-guide)
    - [Accessing Deployed Services](#accessing-deployed-services)
- [JDK Toolchain](#jdk-toolchain)
- [Rate Limiting](#rate-limiting)
    - [Why Rate Limiting is NOT in Application Code](#why-rate-limiting-is-not-in-application-code)
    - [Recommended Approach: Infrastructure-Level](#recommended-approach-infrastructure-level)
    - [When Application-Level IS Appropriate](#when-application-level-is-appropriate)
- [Logging](#logging)
- [Troubleshooting](#troubleshooting)
    - [Common Issues](#common-issues)
- [Additional Documentation](#additional-documentation)
- [Future Enhancements](#future-enhancements)
- [Contribution Guidelines](#contribution-guidelines)
- [License](#license)
- [Further Resources](#further-resources)

---

## Overview

This project showcases **production-ready patterns** and **best practices** for building web applications and
microservices in Kotlin, including:

**Modern Frameworks & Patterns:**

- ✅ **Spring Boot 4.0.1** with Kotlin coroutines for non-blocking REST APIs
- ✅ **Ktor 3.3.2** for lightweight, async HTTP services
- ✅ **WebClient** for non-blocking HTTP client calls
- ✅ **Kotlin Coroutines** with proper `Dispatchers.IO` for I/O operations

**Authentication & Security:**

- ✅ **Cookie-based authentication** with encrypted sessions and CSRF protection
- ✅ **JWT authentication** for stateless API access
- ✅ **JSON serialization security** with explicit field exposure

**Data Management:**

- ✅ **Database management** with Flyway migrations and HikariCP connection pooling
- ✅ **Type-safe SQL queries** with Kotliquery
- ✅ **Functional error handling** with Arrow's `Either` and `Raise` DSL

**Configuration & Deployment:**

- ✅ **Hoplite configuration** with automatic environment variable mapping
- ✅ **Docker** containerization with multi-service orchestration
- ✅ **Gradle** version catalogs (BOM pattern)

**Observability & Quality:**

- ✅ **Monitoring** with Micrometer and Prometheus metrics
- ✅ **Health checks** for Kubernetes/container orchestration
- ✅ **OpenAPI/Swagger** documentation
- ✅ **Thread-safe** bean configuration

**Development Experience:**

- ✅ **SPA hosting** with client-side routing support
- ✅ **IntelliJ HTTP Client** integration with multiple environments
- ✅ **Comprehensive testing** with integration and unit tests
- ✅ **Hot-reload** for static assets in development

---

## Project Architecture

### Multi-Module Structure

This is a multi-module Gradle project with three microservices:

```
web-playground/
├── catalog-service/        # Spring Boot 4.0.1 microservice
│   ├── Spring Boot REST API
│   ├── Kotlin Coroutines with WebClient
│   ├── Non-blocking HTTP calls
│   └── Spring Actuator monitoring
├── main-app/              # Ktor main application
│   ├── Ktor HTTP server
│   ├── Cookie & JWT authentication
│   ├── Database integration (H2)
│   └── SPA hosting
├── third-party-service/   # Ktor mock external service
│   └── Simulates external API calls
└── docker-compose.yml     # Multi-service orchestration
```

### Service Communication

```
┌──────────────────┐
│  Catalog Service │  Port 9877 (container) → 9001 (host)
│  (Spring Boot)   │  Local: 9877, Docker: 9001
└────────┬─────────┘
         │
         │ HTTP (WebClient - non-blocking)
         │
         ▼
┌─────────────────────┐
│ Third-Party Service │  Port 9876 (container) → 9002 (host)
│     (Ktor)          │  Local: 9876, Docker: 9002
└─────────────────────┘
         ▲
         │
         │
         │ HTTP (Ktor Client CIO) 
         │
┌────────┴───────┐
│    Main App    │  Port 4207 (container) → 9000 (host)
│    (Ktor)      │  Local: 4207, Docker: 9000
└────────────────┘
```

**Port Mapping:**

- **Local development:** Services run on their native ports (4207, 9876, 9877)
- **Docker:** Services are mapped to 9000-9002 range on host to avoid conflicts

**Communication:**

- **Catalog Service → Third-Party Service:** Non-blocking HTTP calls via WebClient (fetches random prices)
- **Main App → Third-Party Service:** HTTP calls via Ktor Client CIO (coroutine demo endpoint uses `/random_number`,
  `/ping`, `/reverse`)

---

## Technology Stack

### Frameworks & Libraries

| Component            | Technology         | Version | Purpose                                     |
|----------------------|--------------------|---------|---------------------------------------------|
| **JDK**              | Amazon Corretto    | 24      | Java runtime                                |
| **Kotlin**           | Kotlin JVM         | 2.2.20  | Programming language                        |
| **Spring Boot**      | Spring Framework   | 4.0.1   | Microservices framework (catalog-service)   |
| **Ktor**             | Ktor Server        | 3.3.2   | HTTP server (main-app, third-party-service) |
| **Coroutines**       | kotlinx-coroutines | 1.10.2  | Async/concurrency                           |
| **WebClient**        | Spring WebFlux     | 6.2.1   | Non-blocking HTTP client                    |
| **Build Tool**       | Gradle             | 8.14    | Build automation                            |
| **Containerization** | Docker             | -       | Deployment                                  |

### Key Dependencies

#### Catalog Service (Spring Boot)

- `spring-boot-starter-web` - REST API framework
- `spring-boot-starter-webflux` - WebClient (non-blocking HTTP client)
- `spring-boot-starter-actuator` - Monitoring & health checks
- `kotlinx-coroutines-core` - Coroutine support
- `kotlinx-coroutines-reactor` - Spring/Reactor integration

#### Main App & Third-Party Service (Ktor)

- `ktor-server-netty` - HTTP server
- `ktor-server-auth` - Authentication
- `ktor-client-cio` - HTTP client
- `hikaricp` - Database connection pooling
- `h2-database` - Embedded database

---

## Getting Started

### Prerequisites

- **JDK 24** (Amazon Corretto recommended)
- **Docker** & Docker Compose (for containerized deployment)
- **Gradle** (wrapper included)

### Quick Start

#### 1. Build the Project

```bash
# Build all modules
./gradlew build

# Or build specific module
./gradlew :catalog-service:build
./gradlew :main-app:build
./gradlew :third-party-service:build
```

#### 2. Run Locally (Development)

```bash
# Terminal 1: Start third-party service
./gradlew :third-party-service:run

# Terminal 2: Start catalog service
./gradlew :catalog-service:bootRun

# Terminal 3: Start main app
./gradlew :main-app:run
```

#### 3. Run with Docker Compose (Production)

```bash
# Build JARs first
./gradlew :catalog-service:bootJar
./gradlew :main-app:shadowJar
./gradlew :third-party-service:shadowJar

# Build Docker images and start all services
docker-compose up -d --build

# Check status
docker ps

# View logs
docker logs -f catalog-service

# Stop services
docker-compose down
```

#### 4. Test the Services

```powershell
# Catalog Service - Hello World (Docker: 9001, Local: 9877)
Invoke-RestMethod -Uri "http://localhost:9001/api/hello-world"   # Docker
Invoke-RestMethod -Uri "http://localhost:9877/api/hello-world"  # Local

# Catalog Service - Catalog Endpoint (with coroutines)
Invoke-RestMethod -Uri "http://localhost:9001/api/catalog"   # Docker
Invoke-RestMethod -Uri "http://localhost:9877/api/catalog"  # Local

# Third-Party Service (Docker: 9002, Local: 9876)
Invoke-RestMethod -Uri "http://localhost:9002/random_number"   # Docker
Invoke-RestMethod -Uri "http://localhost:9876/random_number"  # Local

# Main App (Docker: 9000, Local: 4207)
Invoke-RestMethod -Uri "http://localhost:9000/api"   # Docker
Invoke-RestMethod -Uri "http://localhost:4207/api"  # Local
```

---

## Module Documentation

### Catalog Service (Spring Boot)

A **Spring Boot 4.0.1** microservice demonstrating modern Kotlin patterns with coroutines and non-blocking I/O.

#### Features

- ✅ **REST API endpoints** with Spring MVC
- ✅ **Kotlin Coroutines** for async operations
- ✅ **Non-blocking WebClient** for HTTP calls
- ✅ **Concurrent request handling** with `async`/`await`
- ✅ **Proper dispatcher usage** (`Dispatchers.IO` for network I/O)
- ✅ **Spring Actuator** for monitoring
- ✅ **Thread-safe configuration** with prototype-scoped beans

#### API Endpoints

##### GET `/api/hello-world`

Simple hello world endpoint.

**Response:**

```json
{
  "message": "Hello World from Catalog Service!",
  "service": "catalog-service",
  "timestamp": "2026-01-15T10:30:00Z"
}
```

##### GET `/api/catalog`

Returns a list of books with prices fetched from the third-party service using **Kotlin coroutines**.

**Features:**

- Concurrent price fetching using `async`/`await`
- Non-blocking HTTP calls with `WebClient`
- Uses `Dispatchers.IO` for network I/O operations
- Significantly faster than sequential execution due to concurrent API calls

**Response:**

```json
[
  {
    "id": 1,
    "title": "Kotlin in Action",
    "author": "Dmitry Jemerov",
    "price": 15.42
  },
  {
    "id": 2,
    "title": "Spring Boot: Up and Running",
    "author": "Mark Heckler",
    "price": 18.67
  }
]
```

**How It Works:**

1. **Controller** receives request (suspend function)
2. **Service** fetches book data
3. For each book, **concurrently** fetches price from third-party service
4. Uses `WebClient` for **non-blocking** HTTP calls
5. Uses `Dispatchers.IO` for **optimal thread pool** utilization
6. Returns books with prices

**Performance:**

Since both book prices are fetched concurrently rather than sequentially, the total response time is approximately equal to the slowest single API call rather than the sum of all calls. With two books, this means the operation completes in roughly half the time compared to sequential processing.

**Testing:**

```bash
# Local development
curl http://localhost:9877/api/catalog

# Docker deployment
curl http://localhost:9001/api/catalog
```

#### Architecture Highlights

##### 1. Non-Blocking HTTP Client

Uses **Spring WebFlux WebClient** instead of blocking RestClient:

```kotlin
@Service
class CatalogService(private val webClientBuilder: WebClient.Builder) {

    suspend fun getCatalog(): List<Book> = withContext(Dispatchers.IO) {
        val booksWithPrices = booksTemplate.map { book ->
            async {
                val price = fetchPriceFromThirdParty()
                book.copy(price = price)
            }
        }
        booksWithPrices.awaitAll()
    }

    private suspend fun fetchPriceFromThirdParty(): Double {
        return webClient.get()
            .uri("/random_number")
            .retrieve()
            .bodyToMono(String::class.java)
            .awaitSingle()  // Non-blocking suspension
            .toDoubleOrNull()?.div(100.0) ?: 19.99
    }
}
```

**Why WebClient?**

- ✅ Non-blocking I/O
- ✅ Integrates with Kotlin coroutines via `awaitSingle()`
- ✅ Thread doesn't block while waiting for response
- ✅ Better resource utilization

##### 2. Proper Dispatcher Usage

Uses `Dispatchers.IO` for network operations:

```kotlin
suspend fun getCatalog(): List<Book> = withContext(Dispatchers.IO) {
    // Network I/O operations
}
```

**Why Dispatchers.IO?**

| Dispatcher | Thread Pool    | Best For              | Thread Count     |
|------------|----------------|-----------------------|------------------|
| Default    | CPU cores      | CPU-intensive         | ~8 threads       |
| **IO** ✅   | I/O operations | **Network, File I/O** | Up to 64 threads |
| Main       | UI thread      | UI updates            | 1 thread         |

**Benefits:**

- ✅ Larger thread pool (64 vs 8)
- ✅ Optimized for waiting operations
- ✅ Prevents thread pool starvation
- ✅ Better scalability

**Performance Impact:**

When performing many concurrent I/O operations:

- `Dispatchers.Default` has a limited thread pool (typically equal to CPU cores, e.g., 8 threads)
- `Dispatchers.IO` has a larger thread pool (default up to 64 threads, configurable)
- With many concurrent network calls, `Dispatchers.IO` can handle more operations in parallel, significantly reducing total execution time
- The benefit scales with the number of concurrent I/O operations

##### 3. Thread-Safe Bean Configuration

Uses **prototype scope** for `WebClient.Builder` to ensure thread safety:

```kotlin
@Configuration
class WebClientConfiguration {

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    fun webClientBuilder(): WebClient.Builder {
        return WebClient.builder()
    }
}
```

**Why Prototype Scope?**

`WebClient.Builder` is **mutable** and not thread-safe:

- ❌ Singleton scope: All services share same instance → race conditions
- ✅ Prototype scope: Each service gets new instance → thread-safe

**What Happens:**

```kotlin
@Service
class Service1(private val builder: WebClient.Builder) {
    // Gets builder instance #1
}

@Service
class Service2(private val builder: WebClient.Builder) {
    // Gets builder instance #2 (different!)
}
```

**Result:**

- ✅ No shared mutable state
- ✅ Safe for concurrent initialization
- ✅ Production-ready

#### Configuration

**Environment Variables:**

- `SERVER_PORT` - Server port (default: 9877)
- `THIRD_PARTY_SERVICE_URL` - URL of third-party service
    - Local: `http://localhost:9876`
    - Docker: `http://third-party-service:9876`

**Access URLs:**

- **Local development:** http://localhost:9877
- **Docker deployment:** http://localhost:9001 (maps to container port 9877)

**Docker:**

```yaml
catalog-service:
  image: catalog-service:latest
  ports:
    - "9001:9877"    # Host:Container
  environment:
    - SERVER_PORT=9877
    - THIRD_PARTY_SERVICE_URL=http://third-party-service:9876
  depends_on:
    - third-party-service
```

#### IntelliJ HTTP Client Integration

The project includes a unified HTTP client file at the root level (`test-requests.http`) with pre-configured requests
for all services including Catalog Service.

**Features:**

- ✅ Environment-based configuration (local, docker, azure)
- ✅ All Catalog Service endpoints included
- ✅ Easy switching between environments
- ✅ Integrated with all project services

**Available Requests:**

- `GET /api/hello-world` - Hello World endpoint
- `GET /api/catalog` - Catalog with coroutines (fetches prices)
- `GET /actuator/health` - Health check endpoint

**Usage:**

1. Open `test-requests.http` in IntelliJ IDEA (at project root)
2. Select environment from dropdown:
    - `local` - Local development (http://localhost:9877)
    - `docker` - Docker deployment (http://localhost:9001)
    - `azure` - Azure deployment (configurable)
3. Navigate to "CATALOG SERVICE (SPRING BOOT) - Endpoints" section
4. Click ▶️ next to any request to execute

**Environment Configuration:**

Environments are defined in `http-client.env.json`:

```json
{
  "local": {
    "CatalogServiceUrl": "http://localhost:9877"
  },
  "docker": {
    "CatalogServiceUrl": "http://localhost:9001"
  },
  "azure": {
    "CatalogServiceUrl": "https://catalog-service.your-domain.azurecontainerapps.io"
  }
}
```

**Example Request:**

```http
### Catalog endpoint - Returns list of books with prices
GET {{CatalogServiceUrl}}/api/catalog
Accept: application/json
```

The unified HTTP client file also includes requests for Main App and Third-Party Service, making it easy to test
inter-service communication.

---

### Main App (Ktor)

A **Ktor 3.3.2** full-featured web application demonstrating authentication, database integration, functional error
handling, and Single Page Application hosting.

#### Features

**Authentication:**

- ✅ **Cookie-based session authentication** with encrypted cookies and CSRF protection
- ✅ **JWT token authentication** for stateless API access
- ✅ **Session management** in memory (UserSession data class)

**Data Layer:**

- ✅ **Database management** with Flyway migrations
- ✅ **HikariCP connection pooling** with configurable settings
- ✅ **Type-safe SQL queries** with Kotliquery
- ✅ **H2 database** with PostgreSQL compatibility mode

**API & Documentation:**

- ✅ **RESTful API endpoints** (20+ endpoints)
- ✅ **OpenAPI 3.0 specification** (manually maintained)
- ✅ **Swagger UI** for interactive API testing
- ✅ **IntelliJ HTTP Client** integration

**Configuration:**

- ✅ **Hoplite configuration** (modern Kotlin config library)
- ✅ **Automatic environment variable mapping** (SCREAMING_SNAKE_CASE → camelCase)
- ✅ **Multi-environment support** (local, prod, test)
- ✅ **Type-safe configuration** with data classes

**Observability:**

- ✅ **Health check endpoint** for Kubernetes probes
- ✅ **Prometheus metrics** with Micrometer
- ✅ **Structured logging** with Logback

**Development:**

- ✅ **SPA hosting** with client-side routing support
- ✅ **Hot-reload** for static assets in development
- ✅ **Coroutine-based async operations** with HTTP client examples

#### Configuration with Hoplite

The Main App uses **Hoplite** - a modern, zero-boilerplate Kotlin configuration library that provides:

- **Automatic data class mapping** (no manual parsing)
- **Type-safe configuration** (compiler validates everything)
- **Smart environment variable mapping** (SCREAMING_SNAKE_CASE → camelCase)
- **Nested configuration** with logical grouping
- **Multiple sources** (config files, environment variables, system properties)

**Configuration Structure:**

```kotlin
data class WebappConfig(
    val httpPort: Int = 4207,
    val db: DatabaseConfig,
    val cookie: CookieConfig,
    val cors: CorsConfig = CorsConfig(),
    val hikari: HikariConfig = HikariConfig(),
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
```

**Automatic Environment Variable Mapping:**

| Environment Variable    | Config Property        | Example Value           |
|-------------------------|------------------------|-------------------------|
| `HTTP_PORT`             | `httpPort`             | `8080`                  |
| `DB_URL`                | `db.url`               | `jdbc:postgresql://...` |
| `DB_PASSWORD`           | `db.password`          | `secret`                |
| `COOKIE_ENCRYPTION_KEY` | `cookie.encryptionKey` | `hex_key`               |
| `HIKARI_MAX_POOL_SIZE`  | `hikari.maxPoolSize`   | `50`                    |
| `CORS_ALLOWED_HOSTS`    | `cors.allowedHosts`    | `host1,host2`           |

**Environment Selection:**

```bash
export WEB_PLAYGROUND_ENV=local   # Development (default)
export WEB_PLAYGROUND_ENV=prod    # Production
export WEB_PLAYGROUND_ENV=test    # Testing
```

#### API Endpoints

All API endpoints use the `/api` prefix to avoid conflicts with SPA client-side routing.

##### Demo & Hello World Endpoints

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

##### User Management Endpoints

| Endpoint          | Method | Description     | Auth Required |
|-------------------|--------|-----------------|---------------|
| `/api/users/{id}` | GET    | Get user by ID  | No            |
| `/api/users`      | POST   | Create new user | No            |

##### Cookie-Based Authentication Endpoints

| Endpoint      | Method | Description               | Auth Required |
|---------------|--------|---------------------------|---------------|
| `/api/login`  | GET    | Display login form        | No            |
| `/api/login`  | POST   | Process login (form data) | No            |
| `/api/logout` | GET    | Logout and clear session  | No            |
| `/api/secret` | GET    | Protected endpoint (HTML) | Yes + CSRF    |

##### JWT Authentication Endpoints

| Endpoint          | Method | Description                        | Auth Required      |
|-------------------|--------|------------------------------------|--------------------|
| `/api/jwt/login`  | POST   | Login with JSON, returns JWT token | No                 |
| `/api/jwt/secret` | GET    | Protected endpoint (returns JSON)  | Yes (Bearer token) |

##### Monitoring Endpoints

| Endpoint       | Method | Description                 | Format |
|----------------|--------|-----------------------------|--------|
| `/api/health`  | GET    | Health check for K8s probes | JSON   |
| `/api/metrics` | GET    | Prometheus metrics          | Text   |

##### Documentation Endpoints

| Endpoint   | Method | Description                   | Format |
|------------|--------|-------------------------------|--------|
| `/openapi` | GET    | OpenAPI 3.0 specification     | YAML   |
| `/swagger` | GET    | Interactive API documentation | HTML   |

##### SPA Routes

| Path                 | Description                                        |
|----------------------|----------------------------------------------------|
| `/`                  | Serves `index.html` (SPA entry point)              |
| Any non-`/api` route | Falls back to `index.html` for client-side routing |

#### Authentication

The application demonstrates two authentication methods:

##### 1. Cookie-Based Session Authentication

- **Encrypted session cookies** for security
- **CSRF protection** via custom token validation
- **Session stored in memory** (UserSession data class)
- **Suitable for** traditional web applications
- **See** [AUTHENTICATION-GUIDE.md](AUTHENTICATION-GUIDE.md) for detailed documentation

**Example Login:**

```bash
# Local
curl -X POST http://localhost:4207/api/login \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=user@example.com&password=password"

# Docker
curl -X POST http://localhost:9000/api/login \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=user@example.com&password=password"
```

##### 2. JWT Token Authentication

- **Stateless authentication** with JWT tokens
- **Bearer token** in Authorization header
- **1-day token expiration**
- **Suitable for** APIs and mobile clients
- **See** [AUTHENTICATION-GUIDE.md](AUTHENTICATION-GUIDE.md) for detailed documentation

**Example Login:**

```bash
# Local
curl -X POST http://localhost:4207/api/jwt/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user@example.com","password":"password"}'

# Docker
curl -X POST http://localhost:9000/api/jwt/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user@example.com","password":"password"}'
```

**Response:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Using the Token:**

```bash
# Local
curl http://localhost:4207/api/jwt/secret \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Docker
curl http://localhost:9000/api/jwt/secret \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

#### Database

##### Schema

The application uses **Flyway** for database migrations located in `main-app/src/main/resources/db/migration/`:

- `V1__initial.sql` - Creates `user_table` with id, email, password_hash, timestamps
- `V2__add_name_to_user.sql` - Adds name column
- `V3__add_tos_accepted_backward_compatible.sql` - Adds Terms of Service acceptance flag
- `V4__make_tos_accepted_not_nullable.sql` - Makes TOS field mandatory
- `R__populate_default_user.sql` - Repeatable migration that populates default test user

##### Default User

For testing purposes, a default user is created:

- **Email:** `user@example.com`
- **Password:** `password`

##### Database Configuration

**Local Development** (H2 file-based):

```hocon
db.url = "jdbc:h2:./build/local;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
```

**Test** (H2 in-memory):

```hocon
db.url = "jdbc:h2:mem:test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
```

**Production** (Environment variable):

```bash
export DB_URL="jdbc:postgresql://localhost:5432/myapp"
export DB_USER="appuser"
export DB_PASSWORD="secure_password"
```

#### Error Handling

The application uses **Arrow's functional error handling** with `Either` and `Raise` DSL:

- **Validation errors** → Returns structured error responses (400/404)
- **Database errors** → Mapped to appropriate HTTP status codes
- **Unhandled exceptions** → Caught by StatusPages plugin (500)

**Example:**

```kotlin
either {
    val userId = call.parameters["id"]?.toLongOrNull()
        ?: raise(ValidationError("Invalid user ID", 400))
    findUserById(dbSession, userId)
        ?: raise(ValidationError("User not found", 404))
}
```

#### JSON Serialization Security

**Important:** The application uses Gson with `excludeFieldsWithoutExposeAnnotation()` for security.

- **Only fields with `@Expose` annotation are serialized to JSON**
- This prevents accidental exposure of sensitive data (e.g., password hashes)
- All DTO/response classes must have `@Expose` on fields you want in JSON

**Example:**

```kotlin
data class PublicUser(
    @Expose val id: Long,
    @Expose val email: String,
    @Expose val name: String?
    // passwordHash field without @Expose will NOT be serialized
)
```

If you see `{}` in JSON responses, check that your data classes have `@Expose` annotations!

#### Single Page Application (SPA) Support

The application serves static files and supports client-side routing:

- **Development mode** (`useFileSystemAssets = true`): Serves files from `main-app/src/main/resources/public` for
  hot-reload
- **Production mode** (`useFileSystemAssets = false`): Serves files from JAR resources

**URL routing convention:**

- Routes starting with `/api` → Backend API endpoints
- All other routes → Served by SPA (falls back to `index.html`)

This allows the SPA to handle routes like `/home`, `/about`, `/profile` while keeping backend APIs under `/api/*`.

#### OpenAPI & Swagger Documentation

This project uses a **manually maintained OpenAPI 3.0 specification** with Swagger UI.

**Why Manual?** Ktor is a micro-framework that intentionally stays lightweight. Unlike Spring Boot, it doesn't provide
automatic OpenAPI generation. This is the industry-standard approach for Ktor applications.

**Files:**

- `main-app/src/main/resources/openapi/api-docs.yaml` - OpenAPI 3.0 spec
- `main-app/src/main/resources/swagger/index.html` - Swagger UI page

**Access:**

- **Local development:**
    - OpenAPI spec: http://localhost:4207/openapi
    - Swagger UI: http://localhost:4207/swagger
- **Docker deployment:**
    - OpenAPI spec: http://localhost:9000/openapi
    - Swagger UI: http://localhost:9000/swagger

**Updating Documentation:**

1. Implement the endpoint in Ktor code
2. Update the OpenAPI spec in `api-docs.yaml`
3. Test in Swagger UI

#### Monitoring & Metrics

The application includes comprehensive monitoring using **Micrometer** with **Prometheus** metrics export.

**Health Check:**

```bash
# Local
curl http://localhost:4207/api/health

# Docker
curl http://localhost:9000/api/health
```

**Response:**

```json
{
  "status": "UP",
  "timestamp": "2026-01-15T...",
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

**Prometheus Metrics:**

```bash
# Local
curl http://localhost:4207/api/metrics

# Docker
curl http://localhost:9000/api/metrics
```

**Available Metrics:**

- **JVM Metrics**: Memory usage, GC stats, thread counts
- **System Metrics**: CPU usage, processor count
- **HTTP Metrics**: Request counts, duration, status codes
- **Custom Metrics**: Add your own using Micrometer API

**Kubernetes Integration:**

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

#### IntelliJ HTTP Client Integration

The project includes a **unified HTTP client file** at the root level (`test-requests.http`) with pre-configured API
requests for **all three services** (Main App, Catalog Service, Third-Party Service).

**Features:**

- ✅ **All services in one file** - Test all APIs from a single location
- ✅ **Environment-based configuration** - Switch between local, docker, and azure with one click
- ✅ **Organized sections** - Clearly separated requests for each service
- ✅ **Easy inter-service testing** - Test communication between services

**Environments:**

- `local` - Local development
    - Main App: `http://127.0.0.1:4207`
    - Catalog Service: `http://localhost:9877`
    - Third-Party Service: `http://localhost:9876`
- `docker` - Docker deployment
    - Main App: `http://localhost:9000`
    - Catalog Service: `http://localhost:9001`
    - Third-Party Service: `http://localhost:9002`
- `azure` - Azure deployment (configurable URLs)

**Usage:**

1. Open `test-requests.http` in IntelliJ IDEA (at project root)
2. Select environment from dropdown
3. Navigate to desired service section:
    - MAIN APP (KTOR) - Endpoints
    - CATALOG SERVICE (SPRING BOOT) - Endpoints
    - THIRD-PARTY SERVICE (KTOR) - Endpoints
4. Click ▶️ next to any request to execute

**Environment Configuration:**

File: `http-client.env.json` (at project root)

```json
{
  "local": {
    "MainAppUrl": "http://127.0.0.1:4207",
    "CatalogServiceUrl": "http://localhost:9877",
    "ThirdPartyServiceUrl": "http://localhost:9876"
  },
  "docker": {
    "MainAppUrl": "http://localhost:9000",
    "CatalogServiceUrl": "http://localhost:9001",
    "ThirdPartyServiceUrl": "http://localhost:9002"
  },
  "azure": {
    "MainAppUrl": "https://web-playground-app.your-domain.azurecontainerapps.io",
    "CatalogServiceUrl": "https://catalog-service.your-domain.azurecontainerapps.io",
    "ThirdPartyServiceUrl": "https://third-party-service.your-domain.azurecontainerapps.io"
  }
}
```

#### Configuration

**Environment Variables:**

- `WEB_PLAYGROUND_ENV` - Environment selection (local/prod/test)
- `HTTP_PORT` - Server port (default: 4207)
- `DB_URL` - Database URL
- `DB_USER` - Database username
- `DB_PASSWORD` - Database password
- `COOKIE_ENCRYPTION_KEY` - Cookie encryption key (hex)
- `COOKIE_SIGNING_KEY` - Cookie signing key (hex)
- `COOKIE_USE_SECURE` - Enable secure cookies (default: true)
- `CORS_ALLOWED_HOSTS` - Comma-separated allowed hosts
- `HIKARI_MAX_POOL_SIZE` - Max database connections (default: 10)
- And many more (see Configuration section below)

**Docker:**

```yaml
main-app:
  image: main-app:latest
  ports:
    - "9000:4207"    # Host port 9000 → Container port 4207
  environment:
    - WEB_PLAYGROUND_ENV=prod
    - WEB_PLAYGROUND_HTTP_PORT=4207
    - DB_URL=jdbc:postgresql://db:5432/myapp
    - DB_USER=appuser
    - DB_PASSWORD=secure_password
```

**Catalog Service Docker:**

```yaml
catalog-service:
  image: catalog-service:latest
  ports:
    - "9001:9877"    # Host port 9001 → Container port 9877
  environment:
    - SERVER_PORT=9877
    - THIRD_PARTY_SERVICE_URL=http://third-party-service:9876
```

**Third-Party Service Docker:**

```yaml
third-party-service:
  image: third-party-service:latest
  ports:
    - "9002:9876"    # Host port 9002 → Container port 9876
  environment:
    - THIRD_PARTY_SERVICE_PORT=9876
```

---

### Third-Party Service (Ktor)

A mock external service that simulates API calls with random delays.

#### Features

- ✅ Simulates external API behavior
- ✅ Random response delays (200-2000ms)
- ✅ Used by catalog-service for testing concurrent calls

#### Endpoints

- `GET /random_number` - Returns a random number (simulates API call)
- `GET /ping` - Returns "pong"
- `POST /reverse` - Reverses the posted body text

#### Access URLs

- **Local development:** http://localhost:9876
- **Docker deployment:** http://localhost:9002 (maps to container port 9876)

**Entry Point:** `com.vshpynta.thirdparty.ThirdPartyServiceKt`

---

## Key Features & Patterns

### 1. Spring Boot 4.0.1 with Kotlin Coroutines

#### Suspend Functions in Controllers

```kotlin
@RestController
@RequestMapping("/api")
class CatalogController(private val catalogService: CatalogService) {

    @GetMapping("/catalog")
    suspend fun getCatalog(): List<Book> {
        return catalogService.getCatalog()
    }
}
```

Spring Boot 4.0+ natively supports suspend functions in controllers.

#### Concurrent Operations with async/await

```kotlin
suspend fun getCatalog(): List<Book> = withContext(Dispatchers.IO) {
    val booksWithPrices = booksTemplate.map { book ->
        async {  // Launch concurrent operation
            val price = fetchPriceFromThirdParty()
            book.copy(price = price)
        }
    }
    booksWithPrices.awaitAll()  // Wait for all to complete
}
```

**Benefits:**

- Multiple operations run concurrently
- Non-blocking suspension
- Efficient resource usage

### 2. Non-Blocking HTTP Client (WebClient)

#### Why WebClient over RestClient?

| Feature               | RestClient            | WebClient                   |
|-----------------------|-----------------------|-----------------------------|
| Blocking              | ❌ Yes - blocks thread | ✅ No - suspends coroutine   |
| Async support         | ❌ Synchronous only    | ✅ Built for async/reactive  |
| Coroutine integration | ⚠️ Poor (blocks)      | ✅ Excellent (`awaitSingle`) |
| Performance           | ❌ Sequential          | ✅ Truly concurrent          |
| Use case              | Legacy/simple apps    | ✅ Modern async apps         |

#### Configuration

**Provide WebClient.Builder (not WebClient):**

```kotlin
@Bean
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
fun webClientBuilder(): WebClient.Builder {
    return WebClient.builder()
}
```

**Why Builder?**

- ✅ Each service can customize (baseUrl, headers, timeouts)
- ✅ Multiple clients for different APIs
- ✅ Easy to test (mock builder)
- ✅ Spring Boot recommendation
- ✅ Thread-safe with prototype scope

**Service Usage:**

```kotlin
@Service
class CatalogService(private val webClientBuilder: WebClient.Builder) {

    private val webClient = webClientBuilder
        .baseUrl(thirdPartyServiceUrl)
        .build()

    suspend fun fetchData(): String {
        return webClient.get()
            .uri("/endpoint")
            .retrieve()
            .bodyToMono(String::class.java)
            .awaitSingle()
    }
}
```

### 3. Dispatcher Selection for I/O Operations

Always use `Dispatchers.IO` for network calls, file I/O, and database operations:

```kotlin
// ❌ Wrong - Uses default dispatcher (limited threads)
suspend fun fetchData() = coroutineScope {
    async { httpCall() }
}

// ✅ Correct - Uses I/O dispatcher (optimized for I/O)
suspend fun fetchData() = withContext(Dispatchers.IO) {
    async { httpCall() }
}
```

**Guidelines:**

| Operation Type   | Dispatcher            | Example                   |
|------------------|-----------------------|---------------------------|
| Network calls    | `Dispatchers.IO`      | HTTP requests, WebSockets |
| File I/O         | `Dispatchers.IO`      | Reading/writing files     |
| Database queries | `Dispatchers.IO`      | JDBC, JPA queries         |
| CPU-intensive    | `Dispatchers.Default` | Calculations, parsing     |
| UI updates       | `Dispatchers.Main`    | Android/Desktop UI        |

---

## Configuration & Best Practices

### Modern Configuration with Hoplite (Main App)

The Main App uses **Hoplite** - the most idiomatic Kotlin configuration library, providing zero-boilerplate, type-safe
configuration with automatic environment variable mapping.

#### Why Hoplite?

✅ **Zero Boilerplate** - Automatic data class mapping (4 lines vs 30+ with TypeSafe Config)  
✅ **Type-Safe** - Compiler validates everything at compile time  
✅ **Auto Environment Variables** - Automatic SCREAMING_SNAKE_CASE → camelCase conversion  
✅ **Clean Structure** - Nested data classes for logical grouping  
✅ **Built-in Validation** - Clear error messages for configuration issues  
✅ **Most Idiomatic** - The Kotlin way to handle configuration

#### Configuration Architecture

The application uses a **hybrid configuration strategy** combining multiple sources:

**Configuration Priority (highest to lowest):**

1. **Environment Variables** - For secrets and deployment-specific settings
2. **System Properties** - JVM properties (`-Dhttp.port=8080`)
3. **Environment-Specific Config** (`app-{env}.conf`) - Environment overrides
4. **Base Config** (`app.conf`) - Sensible defaults

**Key Benefits:**

- **Security** - Secrets in environment variables, not in source control
- **Flexibility** - Easy override for different environments
- **Developer Experience** - Sensible defaults for local development
- **Cloud-Native** - Perfect for Docker, Kubernetes, Azure
- **Automatic Mapping** - No manual parsing code required

#### How Hoplite Works

```kotlin
// Automatic data class mapping - no boilerplate!
fun createAppConfig(env: String): WebappConfig =
    ConfigLoaderBuilder.default()
        .addSource(PropertySource.resource("/app-${env}.conf"))
        .addSource(PropertySource.resource("/app.conf"))
        .build()
        .loadConfigOrThrow<WebappConfig>()
```

#### Automatic Environment Variable Mapping

Hoplite uses smart naming conventions:

| Environment Variable           | Config Property              | Data Class Path                     | Example Value           |
|--------------------------------|------------------------------|-------------------------------------|-------------------------|
| `HTTP_PORT`                    | `httpPort`                   | `config.httpPort`                   | `8080`                  |
| `DB_URL`                       | `db.url`                     | `config.db.url`                     | `jdbc:postgresql://...` |
| `DB_USER`                      | `db.user`                    | `config.db.user`                    | `appuser`               |
| `DB_PASSWORD`                  | `db.password`                | `config.db.password`                | `secret`                |
| `COOKIE_ENCRYPTION_KEY`        | `cookie.encryptionKey`       | `config.cookie.encryptionKey`       | `hex_key`               |
| `COOKIE_SIGNING_KEY`           | `cookie.signingKey`          | `config.cookie.signingKey`          | `hex_key`               |
| `COOKIE_USE_SECURE`            | `cookie.useSecure`           | `config.cookie.useSecure`           | `true`                  |
| `COOKIE_SAME_SITE`             | `cookie.sameSite`            | `config.cookie.sameSite`            | `Lax`                   |
| `CORS_ALLOWED_HOSTS`           | `cors.allowedHosts`          | `config.cors.allowedHosts`          | `host1,host2`           |
| `HIKARI_MAX_POOL_SIZE`         | `hikari.maxPoolSize`         | `config.hikari.maxPoolSize`         | `50`                    |
| `HIKARI_MIN_IDLE`              | `hikari.minIdle`             | `config.hikari.minIdle`             | `10`                    |
| `HIKARI_CONNECTION_TIMEOUT_MS` | `hikari.connectionTimeoutMs` | `config.hikari.connectionTimeoutMs` | `5000`                  |

**Conversion Rules:**

- `SCREAMING_SNAKE_CASE` → `camelCase`
- Underscores indicate nesting: `DB_URL` → `db.url`
- Lists from comma-separated values: `host1,host2` → `["host1", "host2"]`

#### Configuration Properties Reference

##### Application Settings

| Property                | Environment Variable      | Default          | Description                        |
|-------------------------|---------------------------|------------------|------------------------------------|
| HTTP Port               | `HTTP_PORT`               | `4207`           | HTTP server listening port         |
| Use Filesystem Assets   | `USE_FILESYSTEM_ASSETS`   | `false`          | Serve static files from filesystem |
| Third-Party Service URL | `THIRD_PARTY_SERVICE_URL` | `localhost:9876` | Base URL for demo service          |

##### Database Settings (`db.*`)

| Property          | Environment Variable | Default      | Description         |
|-------------------|----------------------|--------------|---------------------|
| Database URL      | `DB_URL`             | H2 in-memory | JDBC connection URL |
| Database User     | `DB_USER`            | `""`         | Database username   |
| Database Password | `DB_PASSWORD`        | `""`         | Database password   |

##### Cookie Security Settings (`cookie.*`)

| Property              | Environment Variable    | Default  | Description                 |
|-----------------------|-------------------------|----------|-----------------------------|
| Use Secure Cookie     | `COOKIE_USE_SECURE`     | `true`   | Enable Secure flag (HTTPS)  |
| Cookie SameSite       | `COOKIE_SAME_SITE`      | `"Lax"`  | SameSite cookie policy      |
| Cookie Encryption Key | `COOKIE_ENCRYPTION_KEY` | Required | Cookie encryption key (hex) |
| Cookie Signing Key    | `COOKIE_SIGNING_KEY`    | Required | Cookie signing key (hex)    |

##### CORS Settings (`cors.*`)

| Property           | Environment Variable       | Default | Description                      |
|--------------------|----------------------------|---------|----------------------------------|
| CORS Allowed Hosts | `CORS_ALLOWED_HOSTS`       | `[]`    | Comma-separated allowed hosts    |
| CORS HTTPS Hosts   | `CORS_ALLOWED_HTTPS_HOSTS` | `[]`    | Comma-separated HTTPS-only hosts |

##### HikariCP Connection Pool Settings (`hikari.*`)

| Property           | Environment Variable                 | Default   | Description                  |
|--------------------|--------------------------------------|-----------|------------------------------|
| Max Pool Size      | `HIKARI_MAX_POOL_SIZE`               | `10`      | Maximum connections in pool  |
| Min Idle           | `HIKARI_MIN_IDLE`                    | `2`       | Minimum idle connections     |
| Connection Timeout | `HIKARI_CONNECTION_TIMEOUT_MS`       | `5000`    | Max wait for connection (ms) |
| Validation Timeout | `HIKARI_VALIDATION_TIMEOUT_MS`       | `3000`    | Max wait for validation (ms) |
| Idle Timeout       | `HIKARI_IDLE_TIMEOUT_MS`             | `600000`  | Remove idle connections (ms) |
| Max Lifetime       | `HIKARI_MAX_LIFETIME_MS`             | `1800000` | Recycle connections (ms)     |
| Leak Detection     | `HIKARI_LEAK_DETECTION_THRESHOLD_MS` | `60000`   | Warn if held longer (ms)     |

#### Example: Local Development

**File:** `app-local.conf`

```hocon
httpPort = 4207

db {
  url = "jdbc:h2:./build/local;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
  user = ""
  password = ""
}

useFileSystemAssets = true  # Hot-reload during development

cookie {
  useSecure = false  # Allow HTTP for local
  sameSite = "Lax"
  encryptionKey = "1d13f63b868ad26c46151245e1b5175c"
  signingKey = "d232897cbcc6cc89579bfbfc060632945e0dc519927c891733421f0f4a9ae48f"
}

cors {
  allowedHosts = ["localhost:4207", "localhost:9000"]
}

thirdPartyServiceUrl = "http://localhost:9876"
```

#### Example: Production Configuration

**Docker/Docker Compose:**

```yaml
environment:
  - WEB_PLAYGROUND_ENV=prod
  - DB_URL=jdbc:postgresql://db:5432/myapp
  - DB_USER=appuser
  - DB_PASSWORD=secure_password_from_secret
  - COOKIE_ENCRYPTION_KEY=production_encryption_key_hex
  - COOKIE_SIGNING_KEY=production_signing_key_hex
  - COOKIE_USE_SECURE=true
  - CORS_ALLOWED_HTTPS_HOSTS=myapp.com,www.myapp.com
  - HIKARI_MAX_POOL_SIZE=20
```

**Kubernetes:**

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

#### HikariCP Tuning Guide

**Development (Low Load):**

```bash
# Use defaults - fast startup
```

**Production (High Traffic):**

```bash
export HIKARI_MAX_POOL_SIZE=50
export HIKARI_MIN_IDLE=10
export HIKARI_CONNECTION_TIMEOUT_MS=3000
```

**Debugging Connection Leaks:**

```bash
export HIKARI_LEAK_DETECTION_THRESHOLD_MS=10000  # Warn after 10 seconds
```

### 1. Dependency Management

#### Version Catalog (BOM Pattern)

All versions are managed in `gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "2.2.20"
spring-boot = "4.0.1"
ktor = "3.3.2"
kotlinx-coroutines = "1.10.2"

[libraries]
# Spring Boot BOM
spring-boot-bom = { module = "org.springframework.boot:spring-boot-dependencies", version.ref = "spring-boot" }

# Spring Boot starters (versions managed by BOM)
spring-boot-starter-web = { module = "org.springframework.boot:spring-boot-starter-web" }
spring-boot-starter-webflux = { module = "org.springframework.boot:spring-boot-starter-webflux" }
```

**Benefits:**

- ✅ Single source of truth for versions
- ✅ Consistent versions across modules
- ✅ Type-safe accessors (`libs.spring.boot.starter.web`)
- ✅ Easy to update versions

#### Spring Boot BOM vs Plugin

**Spring Boot Gradle Plugin** provides:

- Build tasks (`bootRun`, `bootJar`)
- Applies dependency management (via `io.spring.dependency-management` plugin)
- Manages Spring Boot dependency versions automatically

**Two Approaches for Dependency Management:**

**Approach 1: Plugin Only (Implicit)**
```kotlin
plugins {
    id("org.springframework.boot") version "4.0.1"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")  // Version managed by plugin
}
```

**Approach 2: Explicit BOM (Our Project)** ✅
```kotlin
plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(platform(libs.spring.boot.bom))  // Explicit BOM declaration
    implementation(libs.spring.boot.starter.web)    // Version from BOM
}
```

**Why We Use Explicit BOM:**

- ✅ **Visibility** - BOM version is clearly visible in version catalog
- ✅ **Consistency** - Same pattern as Ktor (ktor-bom)
- ✅ **Gradle Best Practice** - Explicit platform dependencies are recommended
- ✅ **Version Control** - BOM version managed in `gradle/libs.versions.toml`

**Both approaches work**, but explicit BOM declaration is more transparent and follows modern Gradle conventions.

### 2. Gradle Build Configuration

#### Catalog Service (Spring Boot)

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    // No shadow plugin - Spring Boot creates executable JARs
}

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.webflux)
    implementation(kotlin("reflect"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)

    testImplementation(libs.spring.boot.starter.test)
}

kotlin {
    compilerOptions {
        // Enforce null-safety with Spring's @NonNull/@Nullable annotations
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

tasks.test {
    useJUnitPlatform()  // Required for JUnit 5
}
```

**Key Points:**

- Use `bootJar` task (not `shadowJar`)
- Spring Boot plugin handles executable JAR creation
- Include `kotlin-reflect` (required by Spring Boot)
- Add `-Xjsr305=strict` for null-safety with Spring

#### Ktor Services (Main App, Third-Party Service)

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    // No Spring Boot plugin
}

dependencies {
    implementation(platform(libs.ktor.bom))
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    // ... other Ktor dependencies
}

tasks.shadowJar {
    archiveBaseName.set("app-name")
    archiveClassifier.set("")
    archiveVersion.set("1.0-SNAPSHOT")
}
```

**Key Points:**

- Use `shadowJar` task (creates fat JAR)
- Ktor doesn't have a specific plugin like Spring Boot
- Shadow plugin combines all dependencies

### 3. Docker Configuration

#### Dockerfile Differences

**Catalog Service (Spring Boot):**

```dockerfile
FROM amazoncorretto:24.0.2-alpine3.22
RUN mkdir /app
COPY build/libs/catalog-service-1.0-SNAPSHOT.jar /app/
CMD exec java -jar /app/catalog-service-1.0-SNAPSHOT.jar
```

**Ktor Services:**

```dockerfile
FROM amazoncorretto:24.0.2-alpine3.22
RUN mkdir /app
COPY build/libs/main-app-1.0-SNAPSHOT-all.jar /app/
CMD exec java -jar /app/main-app-1.0-SNAPSHOT-all.jar
```

**Note the JAR name difference:**

- Spring Boot: `*.jar` (from `bootJar`)
- Ktor: `*-all.jar` (from `shadowJar`)

---

## Development Guide

### Building the Project

```bash
# Build all modules
./gradlew build

# Clean and build
./gradlew clean build

# Build specific module
./gradlew :catalog-service:bootJar
./gradlew :main-app:shadowJar
./gradlew :third-party-service:shadowJar
```

### Running Services Locally

#### Option 1: Gradle Tasks

```bash
# Catalog Service (Spring Boot)
./gradlew :catalog-service:bootRun

# Main App (Ktor)
./gradlew :main-app:run

# Third-Party Service (Ktor)
./gradlew :third-party-service:run
```

#### Option 2: Run JAR Directly

```bash
# Build first
./gradlew :catalog-service:bootJar

# Run
java -jar catalog-service/build/libs/catalog-service-1.0-SNAPSHOT.jar
```

### Testing

```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :catalog-service:test

# Run with logging
./gradlew :catalog-service:test --info
```

### Code Quality

```bash
# Run Detekt (static analysis)
./gradlew detekt

# View report
open build/reports/detekt/detekt.html
```

---

## Deployment

### Docker Compose (Recommended)

#### Quick Start Script

A helper script is provided to automate deployment:

```powershell
# File: restart-docker-services.ps1
# Stops Java processes, builds JARs, rebuilds Docker images, and tests endpoints

.\restart-docker-services.ps1
```

**What it does:**

1. Stops running Java processes
2. Stops Docker containers
3. Builds JARs for all services
4. Rebuilds Docker images
5. Starts all containers
6. Tests endpoints

#### Manual Deployment

```bash
# 1. Build JARs
./gradlew :catalog-service:clean :catalog-service:bootJar
./gradlew :main-app:clean :main-app:shadowJar
./gradlew :third-party-service:clean :third-party-service:shadowJar

# 2. Build and start containers
docker-compose up -d --build

# 3. Check status
docker ps

# 4. View logs
docker logs -f catalog-service
docker logs -f main-app
docker logs -f third-party-service

# 5. Stop containers
docker-compose down
```

### Docker Compose Configuration

```yaml
services:
  third-party-service:
    build: ./third-party-service
    ports:
      - "9002:9876"    # Host:Container

  catalog-service:
    build: ./catalog-service
    ports:
      - "9001:9877"    # Host:Container
    environment:
      - SERVER_PORT=9877
      - THIRD_PARTY_SERVICE_URL=http://third-party-service:9876
    depends_on:
      - third-party-service

  main-app:
    build: ./main-app
    ports:
      - "9000:4207"    # Host:Container
    environment:
      - WEB_PLAYGROUND_ENV=prod
      - WEB_PLAYGROUND_HTTP_PORT=4207
      - THIRD_PARTY_SERVICE_URL=http://third-party-service:9876
    depends_on:
      - third-party-service
```

**Key Points:**

- Services communicate via Docker network using service names and container ports
- Use service names as hostnames (e.g., `http://third-party-service:9876`)
- Port mapping format: `host:container`
- **Host ports (external):** 9000, 9001, 9002
- **Container ports (internal):** 4207, 9877, 9876

---

## Azure Deployment

The application can be deployed to **Azure Container Apps** for production hosting with automatic scaling, HTTPS, and
zero-downtime deployments.

### Quick Start

```powershell
# 1. Build and tag images for all services
docker build -f main-app/Dockerfile -t main-app:latest ./main-app
docker build -f catalog-service/Dockerfile -t catalog-service:latest ./catalog-service
docker build -f third-party-service/Dockerfile -t third-party-service:latest ./third-party-service

# 2. Create Azure Container Registry and push images
az acr create --resource-group web-playground-rg --name YOUR_UNIQUE_ACR_NAME --sku Basic
az acr login --name YOUR_UNIQUE_ACR_NAME

docker tag main-app:latest YOUR_UNIQUE_ACR_NAME.azurecr.io/main-app:latest
docker tag catalog-service:latest YOUR_UNIQUE_ACR_NAME.azurecr.io/catalog-service:latest
docker tag third-party-service:latest YOUR_UNIQUE_ACR_NAME.azurecr.io/third-party-service:latest

docker push YOUR_UNIQUE_ACR_NAME.azurecr.io/main-app:latest
docker push YOUR_UNIQUE_ACR_NAME.azurecr.io/catalog-service:latest
docker push YOUR_UNIQUE_ACR_NAME.azurecr.io/third-party-service:latest

# 3. Create Container Apps environment
az containerapp env create \
  --name web-playground-env \
  --resource-group web-playground-rg \
  --location northeurope

# 4. Deploy third-party-service (internal)
az containerapp create \
  --name third-party-service \
  --resource-group web-playground-rg \
  --environment web-playground-env \
  --image YOUR_UNIQUE_ACR_NAME.azurecr.io/third-party-service:latest \
  --target-port 9876 \
  --ingress internal

# 5. Deploy catalog-service (external)
az containerapp create \
  --name catalog-service \
  --resource-group web-playground-rg \
  --environment web-playground-env \
  --image YOUR_UNIQUE_ACR_NAME.azurecr.io/catalog-service:latest \
  --target-port 9877 \
  --ingress external \
  --env-vars "THIRD_PARTY_SERVICE_URL=http://third-party-service:9876"

# 6. Deploy main-app (external)
az containerapp create \
  --name main-app \
  --resource-group web-playground-rg \
  --environment web-playground-env \
  --image YOUR_UNIQUE_ACR_NAME.azurecr.io/main-app:latest \
  --target-port 4207 \
  --ingress external \
  --env-vars "WEB_PLAYGROUND_ENV=prod" "WEB_PLAYGROUND_HTTP_PORT=4207" "THIRD_PARTY_SERVICE_URL=http://third-party-service:9876"
```

**Note:** Replace `YOUR_UNIQUE_ACR_NAME` with your actual Azure Container Registry name.

### Microservices Architecture on Azure

**Service Configuration:**

| Service                 | Ingress  | Port | Purpose                                       |
|-------------------------|----------|------|-----------------------------------------------|
| **third-party-service** | Internal | 9876 | Mock API (only accessible within environment) |
| **catalog-service**     | External | 9877 | Public API with Spring Boot                   |
| **main-app**            | External | 4207 | Public web application with Ktor              |

**Communication:**

- Catalog Service and Main App → Third-Party Service (via internal DNS)
- External users → Catalog Service and Main App (via HTTPS)

### Environment Variables for Azure

**Catalog Service:**

```bash
SERVER_PORT=9877
THIRD_PARTY_SERVICE_URL=http://third-party-service:9876
```

**Main App:**

```bash
WEB_PLAYGROUND_ENV=prod
WEB_PLAYGROUND_HTTP_PORT=4207
THIRD_PARTY_SERVICE_URL=http://third-party-service:9876
DB_URL=jdbc:postgresql://your-db-server:5432/myapp
DB_USER=appuser
DB_PASSWORD=<stored-in-azure-secret>
COOKIE_ENCRYPTION_KEY=<stored-in-azure-secret>
COOKIE_SIGNING_KEY=<stored-in-azure-secret>
```

### Complete Guide

For detailed step-by-step instructions, troubleshooting, cost optimization, and advanced configuration, see:

📘 **[AZURE-DEPLOYMENT.md](AZURE-DEPLOYMENT.md)** - Complete Azure Deployment Guide

The guide includes:

- Azure CLI installation and authentication
- Creating Azure Container Registry (ACR)
- Deploying to Azure Container Apps (single service or microservices)
- Deploying all three services (main-app, catalog-service, third-party-service)
- Inter-service communication configuration
- Internal vs external ingress patterns
- Environment variable configuration with Azure Key Vault secrets
- Monitoring, logging, and troubleshooting with Azure Monitor
- Cost optimization strategies
- Scaling configuration
- Custom domains and SSL certificates

### Accessing Deployed Services

After deployment, Azure provides HTTPS URLs:

```bash
# Get application URLs
az containerapp show --name main-app --resource-group web-playground-rg --query properties.configuration.ingress.fqdn -o tsv
az containerapp show --name catalog-service --resource-group web-playground-rg --query properties.configuration.ingress.fqdn -o tsv

# Example URLs:
# https://main-app.orangemushroom-daf89c2d.northeurope.azurecontainerapps.io
# https://catalog-service.orangemushroom-daf89c2d.northeurope.azurecontainerapps.io
```

Update your `http-client.env.json` with these URLs:

```json
{
  "azure": {
    "MainAppUrl": "https://main-app.orangemushroom-daf89c2d.northeurope.azurecontainerapps.io",
    "CatalogServiceUrl": "https://catalog-service.orangemushroom-daf89c2d.northeurope.azurecontainerapps.io",
    "ThirdPartyServiceUrl": "http://third-party-service:9876"
  }
}
```

**Note:** Third-Party Service uses internal URL since it has internal ingress.

---

## JDK Toolchain

The project uses Gradle's JVM toolchain feature set to **JDK 24**. The toolchain is configured centrally in the root
`build.gradle.kts` for all modules.

**To switch to an LTS version**, update the root build file:

```kotlin
// In root build.gradle.kts -> subprojects block
configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
    jvmToolchain(21)  // Switch to Java 21 LTS
}
```

Ensure your IDE's Gradle JVM setting matches the toolchain version.

---

## Rate Limiting

### Why Rate Limiting is NOT in Application Code

This application **intentionally does not include** application-level rate limiting. Here's why:

#### ❌ Problems with Application-Level Rate Limiting

**In-Memory Limitations:**

- ❌ **Lost on restart** - Rate limit state disappears when pod restarts
- ❌ **Not distributed** - Each pod has independent limits (bypass by hitting different pods)
- ❌ **Memory leaks** - Unbounded growth with unique keys
- ❌ **No persistence** - Can't track long-term abuse patterns

**Architectural Issues:**

```
User → Pod 1 (100 req/min) ✅ Allowed
User → Pod 2 (100 req/min) ✅ Allowed  
User → Pod 3 (100 req/min) ✅ Allowed
Total: 300 req/min ❌ Limit bypassed!
```

#### ✅ Recommended Approach: Infrastructure-Level

Rate limiting should be implemented **before requests reach your application** using infrastructure components.

**Option 1: API Gateway**

**Kong Gateway:**

```yaml
plugins:
  - name: rate-limiting
    config:
      minute: 100
      hour: 5000
      policy: redis  # Distributed
      redis:
        host: redis.example.com
```

**NGINX:**

```nginx
limit_req_zone $binary_remote_addr zone=api_limit:10m rate=100r/m;

location /api {
    limit_req zone=api_limit burst=20 nodelay;
    proxy_pass http://backend;
}
```

**Option 2: Load Balancer (HAProxy, Traefik)**

**Option 3: CDN / DDoS Protection (Cloudflare, Akamai)**

#### When Application-Level IS Appropriate

Use Redis-based rate limiting ONLY for:

- ✅ Business logic limits (e.g., premium user quotas)
- ✅ Per-tenant limits in multi-tenant systems
- ✅ API endpoint-specific limits (expensive operations)

**Key Takeaways:**

- ✅ **DO:** Implement at API Gateway/Load Balancer
- ✅ **DO:** Use CDN for DDoS protection
- ✅ **DO:** Use Redis for distributed app limits (if needed)
- ❌ **DON'T:** Use in-memory in microservices
- ❌ **DON'T:** Implement in app code for basic protection

**Remember:** Rate limiting is an **infrastructure concern**, not application logic.

---

## Logging

The application uses **Logback** for logging (configured in `logback.xml`).

**Available loggers:**

- `com.vshpynta.Main` - Application lifecycle and configuration
- `com.vshpynta.db` - Database operations
- `io.ktor` - Ktor framework logs

Log levels can be adjusted in `logback.xml`.

---

## Troubleshooting

### Common Issues

#### 1. Port Already in Use

**Error:**

```
bind: Only one usage of each socket address is normally permitted
```

**Solution:**

```powershell
# Stop Java processes
Get-Process java | Stop-Process -Force

# Stop Docker containers
docker-compose down

# Restart
docker-compose up -d --build
```

#### 2. "No Main Manifest Attribute" Error

**Error:**

```
no main manifest attribute, in /app/catalog-service.jar
```

**Cause:** Using wrong JAR task (shadowJar instead of bootJar for Spring Boot)

**Solution:**

- Spring Boot (catalog-service): Use `bootJar` task
- Ktor (main-app, third-party-service): Use `shadowJar` task
- Update Dockerfile to use correct JAR name

#### 3. Test Failures After Code Changes

**Error:**

```
org.springframework.beans.factory.NoSuchBeanDefinitionException
```

**Common causes:**

- Bean scope issues (use `@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)` for builders)
- Missing dependencies in `build.gradle.kts`
- IDE cache issues

**Solution:**

```bash
# Clean rebuild
./gradlew clean build

# Invalidate IDE caches (IntelliJ)
# File → Invalidate Caches → Invalidate and Restart
```

#### 4. Coroutines Not Working

**Issue:** Suspend functions not compiling or running

**Checklist:**

- ✅ Add `kotlinx-coroutines-core` dependency
- ✅ Add `kotlinx-coroutines-reactor` for Spring integration
- ✅ Use `suspend` keyword on functions
- ✅ Use `withContext(Dispatchers.IO)` for I/O operations
- ✅ Reload Gradle project in IDE

#### 5. WebClient Bean Not Found

**Error:**

```
No beans of 'WebClient.Builder' found
```

**Solution:**

```kotlin
@Configuration
class WebClientConfiguration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    fun webClientBuilder(): WebClient.Builder {
        return WebClient.builder()
    }
}
```

#### 6. Ktor Mixed Version Issues

**Error:**

```
NoSuchMethodError with Ktor
```

**Cause:** Mixed Ktor versions across dependencies

**Solution:** Verify BOM is used and remove explicit versions:

```kotlin
dependencies {
    implementation(platform(libs.ktor.bom))  // Use BOM
    implementation(libs.ktor.server.core)    // No version - comes from BOM
}
```

#### 7. Java Command Not Found (Windows)

**Solution:**

1. Set `JAVA_HOME` as System environment variable
2. Add `%JAVA_HOME%\bin` to PATH (not just `%JAVA_HOME%`)
3. Verify:
   ```cmd
   echo %JAVA_HOME%
   java --version
   ```

#### 8. Static Files Not Found in JAR

**Solution:** Ensure `useFileSystemAssets = false` in production config

#### 9. Database Migration Errors

**Solution:** Delete `build/local.mv.db` and restart for fresh database

#### 10. Cookie Authentication Not Working

**Solution:** Check `COOKIE_ENCRYPTION_KEY` and `COOKIE_SIGNING_KEY` are set correctly

#### 11. CORS Errors

**Solution:** Verify CORS configuration in `Main.kt` and `CORS_ALLOWED_HOSTS` environment variable

#### 12. 404 for API Endpoints

**Solution:** Ensure routes are prefixed with `/api`

#### 13. Configuration Not Loading

**Symptom:** Application fails to start with Hoplite error

**Solution:** Hoplite provides clear error messages:

```
Error loading config:
- Missing required field: cookie.encryptionKey
- Type mismatch at hikari.maxPoolSize: expected Int, got String
```

Fix by:

1. Checking the error message
2. Verifying required fields are set
3. Ensuring correct types

#### 14. Environment Variables Not Working

**Common Causes:**

1. Wrong naming: `DB.URL` ❌ should be `DB_URL` ✓
2. Not exported: `DB_URL=value` ❌ should be `export DB_URL=value` ✓
3. Wrong shell: PowerShell uses `$env:DB_URL="value"`

**Verify:**

```bash
# Linux/macOS
echo $DB_URL

# Windows PowerShell
echo $env:DB_URL
```

---

## Additional Documentation

- **[AUTHENTICATION-GUIDE.md](AUTHENTICATION-GUIDE.md)** - Detailed guide for cookie and JWT authentication
- **[AZURE-DEPLOYMENT.md](AZURE-DEPLOYMENT.md)** - Step-by-step Azure deployment instructions

---

## Future Enhancements

- [x] ~~Health check endpoint~~ ✅ **Implemented**
- [x] ~~Code quality tools (Detekt)~~ ✅ **Implemented**
- [x] ~~Gradle version catalog~~ ✅ **Implemented**
- [x] ~~Config file (HOCON) and typed settings~~ ✅ **Implemented**
- [x] ~~OpenAPI/Swagger documentation~~ ✅ **Implemented**
- [x] ~~Metrics and monitoring (Micrometer)~~ ✅ **Implemented**
- [ ] Rate limiting (API Gateway level)
- [ ] Docker multi-stage build
- [ ] Production session storage (Redis/database)
- [ ] Distributed tracing (OpenTelemetry)
- [ ] Circuit breakers and resilience patterns

---

## Contribution Guidelines

1. **Keep BOM aligned** - Avoid mixing versions; use BOM for consistency
2. **Add KDoc** - Document public APIs and complex functions
3. **Run tests before commits** - `./gradlew test`
4. **Update README** - Document new endpoints, configuration, or changes
5. **Follow project structure** - Keep separation between layers
6. **Use functional error handling** - Leverage Arrow's `Either` and `Raise`
7. **Add migrations for schema changes** - Create Flyway migration files
8. **Test authentication methods** - Ensure changes work with both auth types
9. **Module-specific changes** - Build and test only affected modules

---

## License

This is a playground/learning project. Feel free to use, modify, and experiment!

---

**Happy coding!** Experiment freely, extend routes, add middleware, and build something awesome.

