plugins {
    alias(libs.plugins.kotlin.jvm)
    application
    alias(libs.plugins.shadow)
}

dependencies {
    // Import Ktor BOM (Bill of Materials)
    implementation(platform(libs.ktor.bom))

    // Minimal Ktor dependencies for the third-party service
    // Versions managed by BOM and version catalog
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)

    // Logging dependencies (versions from gradle/libs.versions.toml)
    implementation(libs.logback.classic)
    implementation(libs.slf4j.api)
}

application {
    mainClass.set("com.vshpynta.thirdparty.ThirdPartyServiceKt")
}
