plugins {
    // Kotlin JVM plugin (version set in settings.gradle.kts via pluginManagement)
    kotlin("jvm")
    // Application plugin provides run, installDist, distZip tasks
    application
}

// Project coordinates used for publishing or identification
// group: reverse-domain name; version: semantic or snapshot tag
// (Not critical for a simple playground, but retained for consistency.)
group = "com.vshpynta"
version = "1.0-SNAPSHOT"

repositories {
    // Primary artifact source
    mavenCentral()
    // If you need Ktor beta / EAP versions, uncomment the line below.
    // maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
}

dependencies {
    // Import Ktor BOM (Bill of Materials)
    // This ensures all Ktor modules use the same version (3.3.1) without specifying it individually.
    implementation(platform("io.ktor:ktor-bom:3.3.2"))

    // Ktor server runtime modules (version managed by BOM)
    implementation("io.ktor:ktor-server-core-jvm")    // Core server APIs
    implementation("io.ktor:ktor-server-netty-jvm")   // Netty engine for running the server

    implementation("io.ktor:ktor-server-status-pages")

    implementation("io.ktor:ktor-client-core") // Ktor client core module
    implementation("io.ktor:ktor-client-cio") // CIO engine for Ktor client

    // Ktor testing and client modules (also version-managed by BOM)
    testImplementation("io.ktor:ktor-server-test-host-jvm") // In-memory test host for fast HTTP tests

    // Logging dependencies
    implementation("ch.qos.logback:logback-classic:1.5.20")
    implementation("org.slf4j:slf4j-api:2.0.17")

    // Typesafe Config for externalized configuration management
    implementation("com.typesafe:config:1.4.3")

    // Gson for JSON serialization/deserialization
    implementation("com.google.code.gson:gson:2.13.2")

    // HikariCP for JDBC connection pooling
    implementation("com.zaxxer:HikariCP:7.0.2")

    // Flyway for database migrations
    implementation("org.flywaydb:flyway-core:11.16.0")

    // H2 Database for in-memory database
    implementation("com.h2database:h2:2.4.240")

    // Kotliquery for type-safe SQL querying
    implementation("com.github.seratch:kotliquery:1.9.1")

    // Other dependencies (Kotlin stdlib, coroutines, etc.)
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

    // AssertJ for fluent recursive comparison assertions in tests
    testImplementation("org.assertj:assertj-core:3.27.6")
}

application {
    // Main entry point (top-level main() lives in Main.kt, creating class MainKt)
    mainClass.set("com.vshpynta.MainKt")
}

tasks.test {
    // Ensures JUnit Platform usage (activated implicitly by ktor-server-tests, kept explicit for clarity)
    useJUnitPlatform()
}

kotlin {
    // Configures Gradle-managed JDK toolchain. Consider switching to an LTS (21) if 24 is not installed.
    jvmToolchain(24)
}