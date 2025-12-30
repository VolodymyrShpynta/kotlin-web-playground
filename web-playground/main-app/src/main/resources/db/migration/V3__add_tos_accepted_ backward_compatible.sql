-- This migration adds a new column 'tos_accepted' to the 'user_table' to track whether users have accepted the terms of service.
-- The column is of type BOOLEAN and is nullable to maintain backward compatibility with existing records and code.
-- The next migration (which is the part of the next service deployment) should handle setting default values and enforcing non-null constraints.
ALTER TABLE user_table
  ADD COLUMN tos_accepted BOOLEAN;
