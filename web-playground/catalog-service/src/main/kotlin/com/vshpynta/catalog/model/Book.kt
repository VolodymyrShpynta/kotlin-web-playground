package com.vshpynta.catalog.model

/**
 * Data class representing a book in the catalog.
 */
data class Book(
    val id: Long,
    val title: String,
    val author: String,
    val price: Double
)
