package com.vshpynta.config

import com.typesafe.config.ConfigFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [ConfigExtensions] extension functions.
 *
 * Tests the [Config.getStringListOrCommaSeparated] function with various input formats:
 * - HOCON list format (from config files)
 * - Comma-separated strings (from environment variables)
 * - Empty values
 * - Missing configuration paths
 * - Edge cases (whitespace, empty elements, single values)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfigExtensionsTest {

    @Test
    fun `getStringListOrCommaSeparated should parse HOCON list format`() {
        // Given: Config with HOCON list
        val config = ConfigFactory.parseString(
            """
            hosts = ["domain1.com", "domain2.com", "domain3.com"]
            """.trimIndent()
        )

        // When
        val result = config.getStringListOrCommaSeparated("hosts")

        // Then
        assertEquals(listOf("domain1.com", "domain2.com", "domain3.com"), result)
    }

    @Test
    fun `getStringListOrCommaSeparated should parse comma-separated string`() {
        // Given: Config with comma-separated string (environment variable format)
        val config = ConfigFactory.parseString(
            """
            hosts = "domain1.com,domain2.com,domain3.com"
            """.trimIndent()
        )

        // When
        val result = config.getStringListOrCommaSeparated("hosts")

        // Then
        assertEquals(listOf("domain1.com", "domain2.com", "domain3.com"), result)
    }

    @Test
    fun `getStringListOrCommaSeparated should handle single value without comma`() {
        // Given: Config with single value
        val config = ConfigFactory.parseString(
            """
            host = "single-domain.com"
            """.trimIndent()
        )

        // When
        val result = config.getStringListOrCommaSeparated("host")

        // Then
        assertEquals(listOf("single-domain.com"), result)
    }

    @Test
    fun `getStringListOrCommaSeparated should trim whitespace around values`() {
        // Given: Config with comma-separated string with extra whitespace
        val config = ConfigFactory.parseString(
            """
            hosts = "  domain1.com  ,  domain2.com  ,  domain3.com  "
            """.trimIndent()
        )

        // When
        val result = config.getStringListOrCommaSeparated("hosts")

        // Then
        assertEquals(listOf("domain1.com", "domain2.com", "domain3.com"), result)
    }

    @Test
    fun `getStringListOrCommaSeparated should filter out empty elements`() {
        // Given: Config with comma-separated string containing empty elements
        val config = ConfigFactory.parseString(
            """
            hosts = "domain1.com,,domain2.com,  ,domain3.com"
            """.trimIndent()
        )

        // When
        val result = config.getStringListOrCommaSeparated("hosts")

        // Then
        assertEquals(listOf("domain1.com", "domain2.com", "domain3.com"), result)
    }

    @Test
    fun `getStringListOrCommaSeparated should return empty list for blank string`() {
        // Given: Config with blank string
        val config = ConfigFactory.parseString(
            """
            hosts = "   "
            """.trimIndent()
        )

        // When
        val result = config.getStringListOrCommaSeparated("hosts")

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getStringListOrCommaSeparated should return empty list for empty string`() {
        // Given: Config with empty string
        val config = ConfigFactory.parseString(
            """
            hosts = ""
            """.trimIndent()
        )

        // When
        val result = config.getStringListOrCommaSeparated("hosts")

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getStringListOrCommaSeparated should return empty list for empty HOCON list`() {
        // Given: Config with empty HOCON list
        val config = ConfigFactory.parseString(
            """
            hosts = []
            """.trimIndent()
        )

        // When
        val result = config.getStringListOrCommaSeparated("hosts")

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getStringListOrCommaSeparated should return empty list for missing path`() {
        // Given: Config without the specified path
        val config = ConfigFactory.parseString(
            """
            other = "value"
            """.trimIndent()
        )

        // When
        val result = config.getStringListOrCommaSeparated("missing.path")

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getStringListOrCommaSeparated should handle values with ports`() {
        // Given: Config with hosts including ports
        val config = ConfigFactory.parseString(
            """
            hosts = "localhost:4207,localhost:9000,127.0.0.1:8080"
            """.trimIndent()
        )

        // When
        val result = config.getStringListOrCommaSeparated("hosts")

        // Then
        assertEquals(listOf("localhost:4207", "localhost:9000", "127.0.0.1:8080"), result)
    }

    @Test
    fun `getStringListOrCommaSeparated should handle HOCON list with ports`() {
        // Given: Config with HOCON list including ports
        val config = ConfigFactory.parseString(
            """
            hosts = ["localhost:4207", "localhost:9000", "127.0.0.1:8080"]
            """.trimIndent()
        )

        // When
        val result = config.getStringListOrCommaSeparated("hosts")

        // Then
        assertEquals(listOf("localhost:4207", "localhost:9000", "127.0.0.1:8080"), result)
    }

    @Test
    fun `getStringListOrCommaSeparated should handle nested path`() {
        // Given: Config with nested path
        val config = ConfigFactory.parseString(
            """
            cors {
              allowedHosts = ["domain1.com", "domain2.com"]
            }
            """.trimIndent()
        )

        // When
        val result = config.getStringListOrCommaSeparated("cors.allowedHosts")

        // Then
        assertEquals(listOf("domain1.com", "domain2.com"), result)
    }

    @Test
    fun `getStringListOrCommaSeparated should handle comma-separated with special characters`() {
        // Given: Config with domains containing hyphens and subdomains
        val config = ConfigFactory.parseString(
            """
            hosts = "my-app.example.com,api.sub-domain.example.org,test-123.com"
            """.trimIndent()
        )

        // When
        val result = config.getStringListOrCommaSeparated("hosts")

        // Then
        assertEquals(
            listOf("my-app.example.com", "api.sub-domain.example.org", "test-123.com"),
            result
        )
    }

    @Test
    fun `getStringListOrCommaSeparated should handle trailing comma`() {
        // Given: Config with trailing comma
        val config = ConfigFactory.parseString(
            """
            hosts = "domain1.com,domain2.com,"
            """.trimIndent()
        )

        // When
        val result = config.getStringListOrCommaSeparated("hosts")

        // Then: Trailing empty element should be filtered out
        assertEquals(listOf("domain1.com", "domain2.com"), result)
    }

    @Test
    fun `getStringListOrCommaSeparated should handle leading comma`() {
        // Given: Config with leading comma
        val config = ConfigFactory.parseString(
            """
            hosts = ",domain1.com,domain2.com"
            """.trimIndent()
        )

        // When
        val result = config.getStringListOrCommaSeparated("hosts")

        // Then: Leading empty element should be filtered out
        assertEquals(listOf("domain1.com", "domain2.com"), result)
    }

    @Test
    fun `getStringListOrCommaSeparated should handle real-world CORS configuration`() {
        // Given: Config simulating real production CORS setup
        val config = ConfigFactory.parseString(
            """
            cors {
              allowedHosts = []
              allowedHttpsHosts = ["web-playground-app.ashypebble-e19f1304.northeurope.azurecontainerapps.io", "app.mycompany.com"]
            }
            """.trimIndent()
        )

        // When
        val httpHosts = config.getStringListOrCommaSeparated("cors.allowedHosts")
        val httpsHosts = config.getStringListOrCommaSeparated("cors.allowedHttpsHosts")

        // Then
        assertTrue(httpHosts.isEmpty())
        assertEquals(
            listOf(
                "web-playground-app.ashypebble-e19f1304.northeurope.azurecontainerapps.io",
                "app.mycompany.com"
            ),
            httpsHosts
        )
    }

    @Test
    fun `getStringListOrCommaSeparated should handle environment variable override simulation`() {
        // Given: Config with fallback pattern (simulating ${?VAR} with fallback)
        val baseConfig = ConfigFactory.parseString(
            """
            hosts = ["default1.com", "default2.com"]
            """.trimIndent()
        )

        val overrideConfig = ConfigFactory.parseString(
            """
            hosts = "override1.com,override2.com,override3.com"
            """.trimIndent()
        )

        val mergedConfig = overrideConfig.withFallback(baseConfig)

        // When
        val result = mergedConfig.getStringListOrCommaSeparated("hosts")

        // Then: Override should win
        assertEquals(listOf("override1.com", "override2.com", "override3.com"), result)
    }
}

