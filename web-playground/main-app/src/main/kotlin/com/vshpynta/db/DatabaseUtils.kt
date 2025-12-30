package com.vshpynta.db

import kotliquery.Row
import kotliquery.Session

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

/**
 * Executes the given [body] within a JDBC savepoint on the provided Kotliquery [Session].
 *
 * Mechanics:
 * - Creates a savepoint (`Connection.setSavepoint()`).
 * - Invokes [body].
 * - On success releases the savepoint (to allow the driver to discard related resources).
 * - On exception rolls back to the savepoint (partial rollback) and rethrows the original exception.
 *
 * Use Cases:
 * - Protecting a subset of statements inside a larger transaction so you can recover locally without aborting the outer transaction.
 * - Implementing "try one strategy then fallback" patterns where the first attempt may partially write data.
 * - Grouping optional / best-effort side effects (e.g. logging rows) that should not poison the enclosing commit if they fail.
 *
 * Requirements & Notes:
 * - Works best when the session is inside an explicit transaction (e.g. via `session.transaction { ... }`). Some drivers
 *   may allow savepoints in auto-commit mode but semantics can vary.
 * - Nested savepoints are typically supported; each invocation creates and manages its own scope.
 * - If the driver does not support savepoints, a `SQLFeatureNotSupportedException` will be thrown by `setSavepoint()`.
 * - The original exception raised inside [body] is not wrapped; callers can handle it as usual.
 *
 * Example (without catching — any exception aborts the whole transaction):
 * ```kotlin
 * session.transaction { tx ->
 *     // Outer atomic unit
 *     tx.update(queryOf("INSERT INTO batch_run (started_at) VALUES (CURRENT_TIMESTAMP)"))
 *
 *     // If either of the inserts below fails AND we do not catch the exception,
 *     // dbSavePoint will rollback only these inserts, then rethrow — the rethrow causes the outer
 *     // transaction to rollback the earlier batch_run insert as well.
 *     dbSavePoint(tx) {
 *         tx.update(queryOf("INSERT INTO batch_items (name) VALUES (?)", "alpha"))
 *         tx.update(queryOf("INSERT INTO batch_items (name) VALUES (?)", "beta"))
 *     }
 *
 *     tx.update(queryOf("UPDATE batch_run SET finished_at = CURRENT_TIMESTAMP"))
 * }
 * ```
 *
 * Example (catching to allow partial rollback and keep earlier work):
 * ```kotlin
 * session.transaction { tx ->
 *     tx.update(queryOf("INSERT INTO batch_run (started_at) VALUES (CURRENT_TIMESTAMP)"))
 *
 *     try {
 *         dbSavePoint(tx) {
 *             tx.update(queryOf("INSERT INTO batch_items (name) VALUES (?)", "alpha"))
 *             tx.update(queryOf("INSERT INTO batch_items (name) VALUES (?)", "beta"))
 *         }
 *     } catch (e: Exception) {
 *         // We decided these items are optional; log and continue.
 *         logger.warn("Optional batch_items failed, continuing outer tx", e)
 *     }
 *
 *     // This update still commits because we swallowed the exception.
 *     tx.update(queryOf("UPDATE batch_run SET finished_at = CURRENT_TIMESTAMP"))
 * }
 * ```
 *
 * What happens on an exception inside the savepoint block?
 * 1. Statements executed BEFORE dbSavePoint (in the outer transaction) are still pending and unaffected.
 * 2. Statements executed AFTER the savepoint and BEFORE the failure are rolled back to the savepoint.
 * 3. The exception is rethrown. If uncaught, the outer transaction rolls back EVERYTHING.
 * 4. If caught, the outer transaction can proceed and eventually commit earlier work.
 *
 * Edge Cases:
 * - Ensure that logic inside [body] does not depend on side effects that vanish after rollback (e.g. generated keys).
 * - Generated keys from rolled back statements are invalid; re-execute statements after rollback if needed.
 */
fun <T> dbSavePoint(dbSession: Session, body: () -> T): T {
    val sp = dbSession.connection.underlying.setSavepoint()
    return try {
        body().also {
            dbSession.connection.underlying.releaseSavepoint(sp)
        }
    } catch (e: Exception) {
        dbSession.connection.underlying.rollback(sp)
        throw e
    }
}
