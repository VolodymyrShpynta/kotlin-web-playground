// Central plugin version declarations. The Kotlin plugin version set here propagates to build.gradle.kts
pluginManagement {
    plugins {
        // Kotlin JVM plugin version. Bump here to upgrade Kotlin (e.g., "2.3.0" when released)
        kotlin("jvm") version "2.2.20"
    }
}

plugins {
    // Foojay resolver plugin lets Gradle auto-provision JDKs declared via jvmToolchain()
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

// Root project name (affects distribution folder names & generated scripts)
rootProject.name = "web-playground"