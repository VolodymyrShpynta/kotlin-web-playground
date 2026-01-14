package com.vshpynta.catalog.controller

import com.vshpynta.catalog.model.Book
import com.vshpynta.catalog.service.CatalogService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST controller for catalog operations.
 * Provides endpoints to retrieve book catalog with prices from third-party service.
 */
@RestController
@RequestMapping("/api")
class CatalogController(
    private val catalogService: CatalogService
) {

    /**
     * GET /api/catalog
     * Returns a list of books with prices fetched from third-party service.
     * Uses coroutines for concurrent price fetching.
     */
    @GetMapping("/catalog")
    suspend fun getCatalog(): List<Book> {
        return catalogService.getCatalog()
    }
}
