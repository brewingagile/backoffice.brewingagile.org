ALTER TABLE registrations ADD COLUMN state varchar NOT NULL DEFAULT 'RECEIVED' CHECK (state <> '');
ALTER TABLE registrations ALTER COLUMN state DROP DEFAULT;

UPDATE registrations SET state = 'INVOICING' WHERE id IN (SELECT registration_id FROM registration_invoices);
