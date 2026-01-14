package com.vshpynta.catalog.service

import com.vshpynta.catalog.model.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

/**
 * Service for managing catalog operations.
 * Uses coroutines to fetch prices from the third-party service.
 * Uses non-blocking WebClient for true async execution.
 */
@Service
class CatalogService(
    webClientBuilder: WebClient.Builder
) {
    private val thirdPartyServiceUrl = System.getenv("THIRD_PARTY_SERVICE_URL")
        ?: "http://localhost:9876"

    private val webClient = webClientBuilder
        .baseUrl(thirdPartyServiceUrl)
        .build()

    /**
     * Fetches a list of books with prices from the third-party service.
     * Uses coroutines with Dispatchers.IO to fetch prices concurrently for better performance.
     * Dispatchers.IO is optimized for I/O operations like network calls.
     */
    suspend fun getCatalog(): List<Book> = withContext(Dispatchers.IO) {
        // Hardcoded book data
        val booksTemplates = listOf(
            Book(id = 1, title = "Kotlin in Action", author = "Dmitry Jemerov", price = 0.0),
            Book(id = 2, title = "Spring Boot: Up and Running", author = "Mark Heckler", price = 0.0)
        )

        // Fetch prices concurrently using async
        val booksWithPrices = booksTemplates.map { book ->
            async {
                val price = fetchPriceFromThirdParty()
                book.copy(price = price)
            }
        }

        // Await all async operations and return the results
        booksWithPrices.awaitAll()
    }

    /**
     * Fetches a random price from the third-party service.
     * Uses non-blocking WebClient with coroutine suspension.
     * The third-party service returns a random number (200-2000ms delay).
     */
    private suspend fun fetchPriceFromThirdParty(): Double {
        val randomNumber = webClient.get()
            .uri("/random_number")
            .retrieve()
            .bodyToMono(String::class.java)
            .awaitSingle()  // Suspend without blocking the thread

        // Convert the random number to a price (divide by 100 to get reasonable book prices)
        return randomNumber.toDoubleOrNull()?.div(100.0) ?: 19.99
    }
}
