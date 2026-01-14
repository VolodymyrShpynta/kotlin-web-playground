/**
 * Catalog Service - A Spring Boot application for managing catalog data.
 *
 * This service provides REST endpoints for catalog operations.
 * Runs on port 9877 by default, configurable via SERVER_PORT environment variable.
 */
package com.vshpynta.catalog

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CatalogServiceApplication

fun main(args: Array<String>) {
    runApplication<CatalogServiceApplication>(*args)
}
