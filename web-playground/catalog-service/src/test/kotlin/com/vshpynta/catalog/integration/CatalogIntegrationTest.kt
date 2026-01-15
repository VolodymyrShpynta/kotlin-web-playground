package com.vshpynta.catalog.integration

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.vshpynta.catalog.model.Book
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.client.RestClient

/**
 * Integration test for CatalogController that validates HTTP communication with third-party service.
 *
 * Uses modern WireMock 3.x + Spring Boot 4.x approach:
 * - @RegisterExtension for WireMock lifecycle management (JUnit 5 standard)
 * - @DynamicPropertySource to inject WireMock URL BEFORE Spring context starts
 * - Tests the complete HTTP flow from controller through service to external service
 *
 * This validates the ENTIRE integration:
 * HTTP Request → CatalogController → CatalogService → WebClient → Third-Party Service (WireMock)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CatalogIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var restClient: RestClient

    companion object {
        /**
         * WireMock server using JUnit 5 extension (modern approach).
         * Starts on dynamic port before Spring context to avoid conflicts.
         */
        @JvmField
        @RegisterExtension
        val wireMock: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        /**
         * KEY: Inject WireMock URL into Spring configuration BEFORE context starts.
         * This sets the third-party.service.url property that ThirdPartyServiceProperties reads.
         */
        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("third-party.service.url") { wireMock.baseUrl() }
        }
    }

    @BeforeEach
    fun setup() {
        // Initialize RestClient pointing to our test server
        restClient = RestClient.builder()
            .baseUrl("http://localhost:$port")
            .build()

        // Reset WireMock stubs before each test for isolation
        wireMock.resetAll()
    }

    @Test
    fun `getCatalog should fetch books with prices from third-party service`() {
        // Given - Stub WireMock to return specific price
        wireMock.stubFor(
            get(urlEqualTo("/random_number"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("1299")  // Will become 12.99 (divided by 100)
                )
        )

        // When - Call catalog endpoint
        val books = restClient.get()
            .uri("/api/catalog")
            .retrieve()
            .body(object : ParameterizedTypeReference<List<Book>>() {})

        // Then - Verify response
        assertThat(books).isNotNull().hasSize(2)

        // All books should have the price from third-party service
        books!!.forEach { book ->
            assertThat(book.price).isEqualTo(12.99)
        }

        // Verify book titles
        assertThat(books.map { it.title }).containsExactlyInAnyOrder(
            "Kotlin in Action",
            "Spring Boot: Up and Running"
        )

        // Verify WireMock received exactly 2 HTTP calls (one per book)
        wireMock.verify(2, getRequestedFor(urlEqualTo("/random_number")))
    }

    @Test
    fun `getCatalog should handle invalid response from third-party service`() {
        // Given - Stub returns non-numeric response
        wireMock.stubFor(
            get(urlEqualTo("/random_number"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody("invalid-data")
                )
        )

        // When
        val books = restClient.get()
            .uri("/api/catalog")
            .retrieve()
            .body(object : ParameterizedTypeReference<List<Book>>() {})

        // Then - Should use fallback price (19.99)
        assertThat(books).isNotNull().hasSize(2)

        books!!.forEach { book ->
            assertThat(book.price).isEqualTo(19.99)  // Fallback price from service
        }
    }

    @Test
    fun `getCatalog should handle third-party service error`() {
        // Given - Stub returns 500 error
        wireMock.stubFor(
            get(urlEqualTo("/random_number"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")
                )
        )

        // When/Then - Service should propagate the error
        val exception = org.junit.jupiter.api.assertThrows<Exception> {
            restClient.get()
                .uri("/api/catalog")
                .retrieve()
                .body(object : ParameterizedTypeReference<List<Book>>() {})
        }

        assertThat(exception).isNotNull()
    }

    @Test
    fun `getCatalog should make concurrent calls to third-party service`() {
        // Given - Add delay to verify concurrent execution
        wireMock.stubFor(
            get(urlEqualTo("/random_number"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody("2999")  // 29.99
                        .withFixedDelay(500)  // 500ms delay per call
                )
        )

        // When - Measure execution time
        val startTime = System.currentTimeMillis()

        val books = restClient.get()
            .uri("/api/catalog")
            .retrieve()
            .body(object : ParameterizedTypeReference<List<Book>>() {})

        val duration = System.currentTimeMillis() - startTime

        // Then
        assertThat(books).isNotNull().hasSize(2)

        // If calls were sequential: 2 * 500ms = 1000ms
        // If calls were concurrent: ~500ms (both happen in parallel)
        // Allow some overhead, so check duration < 900ms
        assertThat(duration).isLessThan(900)
            .describedAs("Concurrent calls should take < 900ms, not > 1000ms (sequential)")

        wireMock.verify(2, getRequestedFor(urlEqualTo("/random_number")))
    }

    @Test
    fun `getCatalog should handle timeout from third-party service`() {
        // Given - Stub with very long delay to simulate timeout
        wireMock.stubFor(
            get(urlEqualTo("/random_number"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody("1999")
                        .withFixedDelay(10000)  // 10 second delay
                )
        )

        // When/Then - Should timeout
        val exception = org.junit.jupiter.api.assertThrows<Exception> {
            restClient.get()
                .uri("/api/catalog")
                .retrieve()
                .body(object : ParameterizedTypeReference<List<Book>>() {})
        }

        assertThat(exception).isNotNull()
    }
}
