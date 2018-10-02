CREATE TABLE registration_invoice_method (
    registration_id UUID NOT NULL PRIMARY KEY REFERENCES registration (registration_id),
    billing_company text NOT NULL,
    billing_address text NOT NULL
);

INSERT INTO registration_invoice_method
SELECT registration_id, billing_company, billing_address FROM registration
LEFT JOIN registration_account USING (registration_id)
WHERE registered < '2018-08-07' AND registration_account IS NULL;

ALTER TABLE registration
	DROP COLUMN billing_company,
	DROP COLUMN billing_address,
	DROP COLUMN billing_method;
