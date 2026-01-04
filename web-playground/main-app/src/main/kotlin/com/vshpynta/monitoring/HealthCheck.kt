package com.vshpynta.monitoring

import com.google.gson.annotations.Expose
import com.vshpynta.db.mapFromRow
import com.vshpynta.web.JsonWebResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotliquery.queryOf
import kotliquery.sessionOf
import org.slf4j.LoggerFactory
import java.time.Instant
import javax.sql.DataSource

private val log = LoggerFactory.getLogger("com.vshpynta.monitoring.HealthCheck")

/**
 * Health check status representing the overall application health.
 */
data class HealthCheckResponse(
    @Expose val status: String,
    @Expose val timestamp: String,
    @Expose val checks: Map<String, CheckStatus>
)

/**
 * Individual component health check status.
 */
data class CheckStatus(
    @Expose val status: String,
    @Expose val message: String? = null
)

/**
 * Health check handler that verifies application components.
 *
 * Checks:
 * - Application status (always UP if code is running)
 * - Database connectivity
 *
 * Returns:
 * - 200 OK with "UP" status if all checks pass
 * - 503 Service Unavailable with "DOWN" status if any check fails
 */
suspend fun handleHealthCheck(dataSource: DataSource): JsonWebResponse {
    val checks = mutableMapOf<String, CheckStatus>()

    // Check application status (always UP if we can execute this code)
    checks["application"] = CheckStatus(status = "UP")

    // Check database connectivity
    checks["database"] = checkDatabaseHealth(dataSource)

    // Determine overall status
    val allHealthy = checks.values.all { it.status == "UP" }
    val overallStatus = if (allHealthy) "UP" else "DOWN"
    val statusCode = if (allHealthy) HttpStatusCode.OK.value else HttpStatusCode.ServiceUnavailable.value

    val response = HealthCheckResponse(
        status = overallStatus,
        timestamp = Instant.now().toString(),
        checks = checks
    )

    log.debug("Health check: {}", response)
    return JsonWebResponse(body = response, statusCode = statusCode)
}

/**
 * Checks database connectivity by executing a simple query.
 */
private suspend fun checkDatabaseHealth(dataSource: DataSource): CheckStatus = withContext(Dispatchers.IO) {
    try {
        sessionOf(dataSource).use { session ->
            session.single(queryOf("SELECT 1 AS result"), ::mapFromRow)
                ?.get("result")
                ?.takeIf { it == 1 }
                ?.let { CheckStatus(status = "UP", message = "Database is accessible") }
                ?: CheckStatus(status = "DOWN", message = "Database query failed or returned unexpected result")
        }
    } catch (e: Exception) {
        log.error("Database health check failed", e)
        CheckStatus(status = "DOWN", message = "Database connection failed: ${e.message}")
    }
}
