package com.vshpynta.catalog.controller

import com.ninjasquad.springmockk.MockkBean
import com.vshpynta.catalog.model.Book
import com.vshpynta.catalog.service.CatalogService
import io.mockk.coEvery
import io.mockk.coVerify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.client.RestClient

/**
 * Tests for CatalogController using MockK with SpringBootTest.
 * Pattern matches the working HelloWorldControllerTest.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CatalogControllerTest {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var restClient: RestClient

    @MockkBean
    private lateinit var catalogService: CatalogService

    @BeforeEach
    fun setup() {
        restClient = RestClient.builder()
            .baseUrl("http://localhost:$port")
            .build()
    }

    @Test
    fun `getCatalog should return list of books`() {
        // Given
        val expectedBooks = listOf(
            Book(id = 1, title = "Kotlin in Action", author = "Dmitry Jemerov", price = 39.99),
            Book(id = 2, title = "Spring Boot: Up and Running", author = "Mark Heckler", price = 45.50)
        )
        coEvery { catalogService.getCatalog() } returns expectedBooks

        // When
        val response = restClient.get()
            .uri("/api/catalog")
            .retrieve()
            .body(object : ParameterizedTypeReference<List<Book>>() {})

        // Then
        assertThat(response).isNotNull
        assertThat(response).hasSize(2)
        assertThat(response).containsExactlyInAnyOrderElementsOf(expectedBooks)

        coVerify(exactly = 1) { catalogService.getCatalog() }
    }

    @Test
    fun `getCatalog should return empty list when no books available`() {
        // Given
        coEvery { catalogService.getCatalog() } returns emptyList()

        // When
        val response = restClient.get()
            .uri("/api/catalog")
            .retrieve()
            .body(object : ParameterizedTypeReference<List<Book>>() {})

        // Then
        assertThat(response).isNotNull
        assertThat(response).isEmpty()

        coVerify(exactly = 1) { catalogService.getCatalog() }
    }

    @Test
    fun `getCatalog should return single book`() {
        // Given
        val book = Book(id = 42, title = "Effective Kotlin", author = "Marcin Moskala", price = 29.99)
        coEvery { catalogService.getCatalog() } returns listOf(book)

        // When
        val response = restClient.get()
            .uri("/api/catalog")
            .retrieve()
            .body(object : ParameterizedTypeReference<List<Book>>() {})

        // Then
        assertThat(response).isNotNull
        assertThat(response).hasSize(1)
        assertThat(response!![0]).isEqualTo(book)

        coVerify(exactly = 1) { catalogService.getCatalog() }
    }

    @Test
    fun `getCatalog should handle large book lists`() {
        // Given
        val largeBookList = (1..100).map { id ->
            Book(id = id.toLong(), title = "Book $id", author = "Author $id", price = id * 10.0)
        }
        coEvery { catalogService.getCatalog() } returns largeBookList

        // When
        val response = restClient.get()
            .uri("/api/catalog")
            .retrieve()
            .body(object : ParameterizedTypeReference<List<Book>>() {})

        // Then
        assertThat(response).isNotNull
        assertThat(response).hasSize(100)

        coVerify(exactly = 1) { catalogService.getCatalog() }
    }
}
