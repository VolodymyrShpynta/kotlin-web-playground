package com.vshpynta.catalog.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST controller providing the hello-world endpoint.
 */
@RestController
@RequestMapping("/api")
class HelloWorldController {

    @GetMapping("/hello-world")
    fun helloWorld(): Map<String, String> {
        return mapOf(
            "message" to "Hello World from Catalog Service!",
            "service" to "catalog-service",
            "timestamp" to System.currentTimeMillis().toString()
        )
    }
}
