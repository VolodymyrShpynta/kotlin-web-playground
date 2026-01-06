plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
    alias(libs.plugins.shadow)
}

dependencies {
    // Import Ktor BOM (Bill of Materials)
    // Versions for Ktor modules are managed by the BOM
    implementation(platform(libs.ktor.bom))

    // Ktor server runtime modules (versions managed by BOM)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.server.html.builder)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.sessions)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.auth.jwt)

    // Metrics and monitoring
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.micrometer.registry.prometheus)


    // Ktor testing modules
    testImplementation(libs.ktor.server.test.host)

    // All dependencies below use versions from gradle/libs.versions.toml
    // Type-safe accessors via libs.* with IDE autocomplete!

    // Logging dependencies
    implementation(libs.logback.classic)
    implementation(libs.slf4j.api)

    // Configuration management - Hoplite (modern Kotlin config library)
    implementation(libs.hoplite.core)
    implementation(libs.hoplite.hocon)

    // JSON serialization
    implementation(libs.gson)

    // Database dependencies
    implementation(libs.hikaricp)
    implementation(libs.flyway.core)
    implementation(libs.h2.database)
    implementation(libs.kotliquery)

    // Security
    implementation(libs.bcrypt)

    // Functional programming
    implementation(libs.arrow.fx.coroutines)
    implementation(libs.arrow.fx.stm)

    // Testing dependencies
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.assertj.core)
}

application {
    mainClass.set("com.vshpynta.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

