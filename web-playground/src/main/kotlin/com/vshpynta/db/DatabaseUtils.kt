package com.vshpynta.db

import kotliquery.Row

fun mapFromRow(row: Row): Map<String, Any?> {
    return row.underlying.metaData
        .let {
            (1..it.columnCount)
                .map(it::getColumnName)
        }
        .associateWith { row.anyOrNull(it) }
}
