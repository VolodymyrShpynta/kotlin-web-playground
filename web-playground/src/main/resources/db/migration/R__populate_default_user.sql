-- Repeatable Flyway migration (R__ prefix): reruns when file changes.
-- Purpose: Ensure a default application user exists for local/dev environments.
-- Idempotency: MERGE ... KEY(email) inserts or updates the row without duplicates.
-- Security: Replace the placeholder password_hash with a real salted hash (never store plain or weak values).
-- Production: Remove or disable this default user outside local/testing environments.

MERGE INTO user_table (email, password_hash, name, tos_accepted)
KEY (email)
VALUES
  ('vshpynta@crud.business', '456def', 'Volodymyr Shpynta', true);
