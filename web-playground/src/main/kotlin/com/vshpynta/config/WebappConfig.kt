package com.vshpynta.config

data class WebappConfig(
    val httpPort: Int,
    val dbUrl: String,
    val dbUser: String,
    val dbPassword: String
)
