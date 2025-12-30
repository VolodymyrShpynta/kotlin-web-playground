package com.vshpynta.config

import com.typesafe.config.Config

/**
 * Extension functions for Typesafe Config to support additional parsing patterns.
 */

/**
 * Gets a list of strings from config, supporting both HOCON list format and comma-separated strings.
 *
 * This function provides flexibility for configuration values that can be specified in two ways:
 * 1. **HOCON list format** (in config files): `["value1", "value2", "value3"]`
 * 2. **Comma-separated string** (from environment variables): `"value1,value2,value3"`
 *
 * The function first attempts to parse as a HOCON list. If that fails (e.g., when the value
 * comes from an environment variable as a plain string), it falls back to parsing as a
 * comma-separated string.
 *
 * **Use Cases:**
 * - CORS allowed hosts configuration (can be set via env vars or config files)
 * - Feature flags lists
 * - Any configuration where environment variables need to provide list values
 *
 * **Examples:**
 *
 * Config file:
 * ```hocon
 * allowed.hosts = ["domain1.com", "domain2.com", "domain3.com"]
 * ```
 *
 * Environment variable:
 * ```bash
 * export ALLOWED_HOSTS="domain1.com,domain2.com,domain3.com"
 * ```
 *
 * Both will be parsed as: `["domain1.com", "domain2.com", "domain3.com"]`
 *
 * @param path The configuration path to retrieve
 * @return List of strings, or empty list if the path doesn't exist or the value is empty
 *
 * @see Config.getStringList
 * @see Config.getString
 */
fun Config.getStringListOrCommaSeparated(path: String): List<String> {
    return try {
        // Try to get as HOCON list first (for config files)
        getStringList(path)
    } catch (_: Exception) {
        // If that fails, try as comma-separated string (for environment variables)
        try {
            val value = getString(path)
            if (value.isBlank()) {
                emptyList()
            } else {
                value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            }
        } catch (_: Exception) {
            // If both fail, return empty list
            emptyList()
        }
    }
}

