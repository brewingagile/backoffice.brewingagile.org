UPDATE registrations SET billing_method = 'EMAIL' WHERE billing_method = 'email';
UPDATE registrations SET billing_method = 'SNAILMAIL' WHERE billing_method = 'snailmail';

CREATE TABLE registration_invoices (
	registration_id uuid PRIMARY KEY NOT NULL REFERENCES registrations(id), 
	invoice_reference_id uuid NOT NULL
);