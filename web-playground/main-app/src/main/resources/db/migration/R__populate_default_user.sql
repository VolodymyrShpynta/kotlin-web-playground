-- Repeatable Flyway migration (R__ prefix): reruns when file changes.
-- Purpose: Ensure a default application user exists for local/dev environments.
-- Idempotency: MERGE ... KEY(email) inserts or updates the row without duplicates.
-- Security: Replace the placeholder password_hash with a real salted hash (never store plain or weak values).
-- Production: Remove or disable this default user outside local/testing environments.

MERGE INTO user_table (email, password_hash, name, tos_accepted)
KEY (email)
VALUES
-- Password (1234) is hashed using a com.vshpynta.security.hashPasswordAsHex function.
  ('vshpynta@crud.business', x'243261243130244d4f7652486f303059764d3946415942364834374a656e454b5768706e665863644b48634152597a68535459396a704b594677456d', 'Volodymyr Shpynta', true);
