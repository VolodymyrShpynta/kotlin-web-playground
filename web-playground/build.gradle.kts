// Root build file for multi-module project
// Individual module configurations are in their respective build.gradle.kts files
// - main-app/build.gradle.kts: Main web application
// - third-party-service/build.gradle.kts: Demo third-party service

// All versions are now managed in gradle/libs.versions.toml
// This provides type-safe accessors via libs.* (e.g., libs.logback.classic)

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.shadow) apply false
}

allprojects {
    group = "com.vshpynta"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        // If you need Ktor beta / EAP versions, uncomment the line below.
        // maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
    }
}

// Common configuration applied to all subprojects
subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    // Configure JVM toolchain for all modules
    configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(24)
    }

    dependencies {
        // Common dependencies for all modules
        add("implementation", kotlin("stdlib"))
    }
}
