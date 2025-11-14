package com.vshpynta.db

import kotliquery.Row

/**
 * Converts a Kotliquery [Row] into a Map<String, Any?> keyed by column names.
 *
 * Each column name (as reported by JDBC metadata) is associated with its value (nullable).
 * This generic representation is useful for ad-hoc queries or lightweight dynamic mapping.
 * For domain objects, prefer dedicated mappers (e.g. `User.fromRow`).
 *
 * Performance: Suitable for small result sets; for large streaming operations consider `forEach` with
 * targeted extraction to avoid building many intermediate maps.
 */
fun mapFromRow(row: Row): Map<String, Any?> {
    return row.underlying.metaData
        .let {
            (1..it.columnCount)
                .map(it::getColumnName)
        }
        .associateWith { row.anyOrNull(it) }
}
