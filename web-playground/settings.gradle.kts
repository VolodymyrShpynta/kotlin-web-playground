// Plugin versions are now managed by version catalog (gradle/libs.versions.toml)
// This provides type-safe plugin accessors via alias(libs.plugins.*)

plugins {
    // Foojay resolver plugin lets Gradle auto-provision JDKs declared via jvmToolchain()
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

// Root project name (affects distribution folder names & generated scripts)
rootProject.name = "web-playground"

// Include submodules
include("main-app")
include("third-party-service")
