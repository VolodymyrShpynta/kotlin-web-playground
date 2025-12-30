-- Populate existing records with a default value before making the column NOT NULL
UPDATE user_table SET tos_accepted = false;

-- Now alter the column to set it as NOT NULL
ALTER TABLE user_table
  ALTER COLUMN tos_accepted
  SET NOT NULL;
