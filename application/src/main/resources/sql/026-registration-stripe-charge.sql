CREATE TABLE registration_stripe_charge (
    charge_id text NOT NULL PRIMARY KEY,
    registration_id UUID NOT NULL REFERENCES registration (registration_id),
    amount NUMERIC(12,2) NOT NULL,
    "when" TIMESTAMP NOT NULL
);