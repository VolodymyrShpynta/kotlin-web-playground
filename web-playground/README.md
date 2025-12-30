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

## Future Enhancements

- [ ] Health check endpoint (`GET /api/health`)
- [ ] Code quality tools (Detekt / ktlint)
- [x] ~~Gradle version catalog (`libs.versions.toml`)~~ - **Implemented!**
- [x] ~~Config file (HOCON) and typed settings~~ - **Implemented!**
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
9. **Module-specific changes** - Build and test only affected modules during development

## Additional Documentation

- **[AUTHENTICATION-GUIDE.md](AUTHENTICATION-GUIDE.md)** - Detailed guide for cookie and JWT authentication
- **[AZURE-DEPLOYMENT.md](AZURE-DEPLOYMENT.md)** - Step-by-step Azure deployment instructions with microservices support

## License

This is a playground/learning project. Feel free to use, modify, and experiment!

---

**Happy coding!** Experiment freely, extend routes, add middleware, and build something awesome.

