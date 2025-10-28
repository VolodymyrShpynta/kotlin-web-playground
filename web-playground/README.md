# Kotlin Web Playground

A minimal Kotlin (JVM) playground project running a simple Ktor HTTP server (Netty engine) exposing a single `GET /` endpoint that returns `Hello, World!`.

## Tech Stack
- Kotlin: 2.2.20 (configured via `pluginManagement` in `settings.gradle.kts`)
- Ktor: 3.3.1 (managed via the Ktor BOM) – modules: server-core, server-netty, server-test-host, client-cio
- Gradle Kotlin DSL build (`build.gradle.kts`)
- JVM Toolchain: 24 (switch to 21 LTS if desired)

## Documentation & Code Comments
Inline comments explain the purpose of plugins, dependencies, test setup, and routing. See:
- `settings.gradle.kts` – plugin & Foojay resolver rationale
- `build.gradle.kts` – BOM use, dependency grouping, version alignment
- `Main.kt` – KDoc for entry point and module
- `HelloWorldRouteTest.kt` – integration test explanation

## Source Entry Point
`src/main/kotlin/com/vshpynta/Main.kt` defines:
- Netty server on port **4207**
- `Application.module()` (testable configuration)
- Private route builder for `GET /`

## Requirements
- JDK 24 (or JDK 21 LTS)
- Internet access for dependency resolution

## Run the Application
Windows (cmd.exe):
```bat
gradlew.bat run
```
Unix-like:
```bash
./gradlew run
```
Server URL: `http://localhost:4207/`

Test the endpoint:
```bash
curl http://localhost:4207/
```
Expected:
```
Hello, World!
```

## Common Gradle Tasks
```bat
gradlew.bat build      # Compile + test
gradlew.bat test       # Run tests
gradlew.bat run        # Start server
gradlew.bat installDist
gradlew.bat distZip
gradlew.bat clean
gradlew.bat tasks --all
```

## Running the Installed Distribution
```bat
build\install\web-playground\bin\web-playground.bat
```
Unix-like:
```bash
./build/install/web-playground/bin/web-playground
```

## Project Structure
```
web-playground/
  build.gradle.kts
  settings.gradle.kts
  src/
    main/kotlin/com/vshpynta/Main.kt
    test/kotlin/com/vshpynta/HelloWorldRouteTest.kt
```

## Port Configuration
Hard-coded in `Main.kt`:
```kotlin
embeddedServer(Netty, port = 4207, module = Application::module)
```
Environment override:
```kotlin
val port = System.getenv("PORT")?.toIntOrNull() ?: 4207
```
Run examples:
```bash
PORT=8080 ./gradlew run
set PORT=8080 && gradlew.bat run
$env:PORT=8080; gradlew.bat run
```

## Dependency & Version Management
Using the Ktor BOM keeps all referenced Ktor artifacts at the same version:
```kotlin
dependencies {
    implementation(platform("io.ktor:ktor-bom:3.3.1"))
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    testImplementation("io.ktor:ktor-server-test-host-jvm")
    testImplementation("io.ktor:ktor-client-cio-jvm")
    implementation(kotlin("stdlib"))
}
```
To upgrade Ktor, change only the BOM line (e.g. `ktor-bom:3.4.0`) and re-sync.

### Beta / EAP Versions
Uncomment the EAP repository if using previews:
```kotlin
// repositories {
//   maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
// }
```
Then update BOM and ensure all modules still resolve. If a module 404s, revert to stable.

## Tests
Integration test (`HelloWorldRouteTest.kt`) uses Ktor `testApplication {}` and the test host module. Run:
```bat
gradlew.bat test
```
Targeted run:
```bat
gradlew.bat test --tests com.vshpynta.HelloWorldRouteTest
```

Coroutines test utilities are available via `kotlinx-coroutines-test` for structured concurrency & virtual time when needed.

## Logging (Optional)
SLF4J warning appears (no provider). Add a simple backend:
```kotlin
dependencies {
    runtimeOnly("org.slf4j:slf4j-simple:2.0.16") // or logback-classic
}
```
Then use `application.log.info("Started")` or Kotlin Logging.

## Packaging
Standard (non-fat) jar:
```bash
./gradlew jar
java -jar build/libs/web-playground-1.0-SNAPSHOT.jar
```
Dependencies aren’t bundled; use:
```bash
./gradlew installDist
```
or add Shadow plugin for an uber-jar.

## JDK Toolchain
Switch to LTS 21 for broader compatibility:
```kotlin
kotlin { jvmToolchain(21) }
```
Ensure IDE Gradle JVM matches.

## Future Enhancements
- Logging backend (logback or slf4j-simple)
- Health check route (`GET /health`)
- Detekt / ktlint
- Version catalog (`libs.versions.toml`)
- Config file (HOCON) and typed settings
- Shadow plugin / Docker image

## Contribution Guidelines
1. Keep BOM aligned; avoid mixing versions.
2. Add KDoc for public APIs.
3. Run tests before commits.
4. Update README when adding endpoints or ports.
5. Prefer explicit configuration in `module()`.

## Troubleshooting
| Issue | Action |
|-------|--------|
| `NoSuchMethodError` | Mixed Ktor versions – verify BOM and remove explicit versions. |
| SLF4J warnings | Add a logging backend dependency. |
| 404 resolving artifacts | `gradlew --refresh-dependencies`; check repository lines. |
| Port conflict | Change port or free the process. |
| Missing hints in IDE | Enable Kotlin inlay hints in Settings. |

---
Experiment freely; extend routes; add middleware. Happy coding!
