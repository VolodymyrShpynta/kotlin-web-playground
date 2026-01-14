package com.vshpynta.catalog.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.client.RestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HelloWorldControllerTest {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var restClient: RestClient

    @BeforeEach
    fun setup() {
        restClient = RestClient.builder()
            .baseUrl("http://localhost:$port")
            .build()
    }

    @Test
    fun helloWorldEndpointShouldReturnValidResponse() {
        // When
        val response = restClient.get()
            .uri("/api/hello-world")
            .retrieve()
            .body(Map::class.java)

        // Then
        assertThat(response).isNotNull
        assertThat(response!!["message"]).isEqualTo("Hello World from Catalog Service!")
        assertThat(response["service"]).isEqualTo("catalog-service")
        assertThat(response["timestamp"]).isNotNull()
    }
}
