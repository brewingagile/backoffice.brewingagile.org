ALTER TABLE account ADD COLUMN billing_email text DEFAULT '' NOT NULL;
ALTER TABLE account ALTER COLUMN billing_email DROP DEFAULT;
