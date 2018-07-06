CREATE TABLE registration_stripe_charge_receipt (
    charge_id text NOT NULL PRIMARY KEY REFERENCES registration_stripe_charge (charge_id),
    receipt_pdf_source bytea NOT NULL
);