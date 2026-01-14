package com.vshpynta.catalog.config

import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.web.reactive.function.client.WebClient

/**
 * Configuration for HTTP clients used in the application.
 */
@Configuration
class WebClientConfiguration {

    /**
     * Provides a WebClient.Builder bean for dependency injection.
     *
     * IMPORTANT: Uses prototype scope for thread safety!
     * - Each service injection gets a NEW builder instance
     * - Prevents race conditions during concurrent configuration
     * - Safe for multiple services building clients simultaneously
     *
     * Why Builder instead of WebClient?
     * 1. Flexibility - Each service can customize the client (baseUrl, headers, timeouts)
     * 2. Multiple clients - Services can create different clients for different APIs
     * 3. Testing - Easier to mock and configure for tests
     * 4. Spring Boot recommendation - Follows official best practices
     * 5. No shared state - Each service builds its own isolated WebClient instance
     *
     * WebClient is non-blocking and works seamlessly with Kotlin coroutines.
     * Used by services to create WebClient instances for calling external APIs.
     */
    @Bean
    @Scope(SCOPE_PROTOTYPE)  // CRITICAL: Each injection gets a fresh instance for thread safety
    fun webClientBuilder(): WebClient.Builder {
        return WebClient.builder()
        // Can add common defaults here that all clients will inherit
        // e.g., .defaultHeader("X-Application", "catalog-service")
    }
}
