ALTER TABLE registrations DROP COLUMN workshop;
ALTER TABLE registrations DROP COLUMN dietary_requirements;
ALTER TABLE registrations ADD COLUMN ticket text NOT NULL DEFAULT '';
ALTER TABLE registrations ADD COLUMN dietary_requirements text NOT NULL DEFAULT '';

