CREATE TABLE registration_invoice_2 (
    registration_id UUID NOT NULL PRIMARY KEY REFERENCES registration (registration_id),
    invoice_number text NOT NULL CHECK (invoice_number <> ''),
    pdf bytea NOT NULL
);