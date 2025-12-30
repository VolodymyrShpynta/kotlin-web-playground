package com.vshpynta.web.dto

data class ValidationError(
    val error: String,
    val statusCode: Int = 400
)
