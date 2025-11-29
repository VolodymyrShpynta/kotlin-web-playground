package com.vshpynta.db

import kotliquery.TransactionalSession
import kotliquery.sessionOf
import javax.sql.DataSource

/**
 * Test harness executing [handler] inside a JDBC transaction and ALWAYS rolling back afterwards.
 *
 * Implementation detail: uses Kotliquery's transaction support then explicitly calls `rollback()`
 * on the underlying connection in a finally block ensuring no changes persist even if the handler
 * completes normally.
 *
 * Use Cases:
 *  - Verifying isolation and visibility of uncommitted changes.
 *  - Creating deterministic starting points without cleanup code.
 *
 * Notes:
 *  - Do not nest calls to testTx unless you fully understand underlying driver transaction semantics.
 *  - Exceptions inside the handler still trigger rollback (redundant but harmless) before propagating.
 */
fun testTx(dataSource: DataSource, handler: (TransactionalSession) -> Unit) {
    sessionOf(dataSource, returnGeneratedKey = true)
        .use { dbSession ->
            dbSession.transaction { txSession ->
                try {
                    handler(txSession)
                } finally {
                    dbSession.connection.rollback()
                }
            }
        }
}
