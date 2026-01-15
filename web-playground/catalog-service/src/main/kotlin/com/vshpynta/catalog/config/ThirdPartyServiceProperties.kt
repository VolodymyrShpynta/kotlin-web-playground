package com.vshpynta.catalog.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration properties for third-party service integration.
 *
 * Usage in application.yml:
 * ```yaml
 * third-party:
 *   service:
 *     url: http://localhost:9876
 * ```
 *
 * Or via environment variable:
 * THIRD_PARTY_SERVICE_URL=http://localhost:9876
 */
@Configuration
@ConfigurationProperties(prefix = "third-party.service")
data class ThirdPartyServiceProperties(
    /**
     * Base URL of the third-party service.
     */
    var url: String = "http://localhost:9876"
)
