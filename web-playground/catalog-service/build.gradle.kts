plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
}

dependencies {
    // Spring Boot BOM (platform) for dependency management
    implementation(platform(libs.spring.boot.bom))

    // Spring Boot starter dependencies (versions managed by BOM)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.webflux)  // For non-blocking WebClient
    implementation(libs.spring.boot.starter.actuator)

    // Kotlin standard library and reflection
    implementation(kotlin("reflect"))

    // Coroutines support for async operations
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)  // For Spring WebFlux coroutine support

    // Logging is provided by spring-boot-starter-web (includes logback-classic and slf4j-api)

    // Testing
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.mockk)  // MockK for Kotlin mocking
    testImplementation(libs.springmockk)  // Spring + MockK integration (@MockkBean support)
    testImplementation(libs.kotlinx.coroutines.test)  // Coroutine testing utilities
    testImplementation(libs.wiremock)  // WireMock for stubbing HTTP calls to external services
}

kotlin {
    compilerOptions {
        // Strictly enforce JSR-305 nullability annotations from Java libraries (e.g., Spring Framework)
        // This enables compile-time null-safety when calling Java code that uses @NonNull/@Nullable annotations
        // Spring Framework 5.0+ annotates all APIs, so this catches potential NPEs at compile time
        // Example: Spring's @Nullable forces Kotlin to use User? instead of User
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

// Configure test task to use JUnit Platform (JUnit 5)
// This is required for Gradle to properly discover and execute JUnit 5 tests
// Without this, Gradle would look for JUnit 4 tests and wouldn't find any JUnit 5 tests
// spring-boot-starter-test includes JUnit 5 (Jupiter), so we need to enable the JUnit Platform engine
tasks.test {
    useJUnitPlatform()
}
