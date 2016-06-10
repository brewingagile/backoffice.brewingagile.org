ALTER TABLE registrations ALTER COLUMN id TYPE uuid USING id::uuid;
