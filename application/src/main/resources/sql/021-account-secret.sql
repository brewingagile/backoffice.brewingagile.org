CREATE TABLE account_secret (
    secret_id uuid NOT NULL PRIMARY KEY,
    bundle text NOT NULL REFERENCES bucket (bucket) DEFERRABLE
);

CREATE TABLE stripe_charge (
    charge_id text NOT NULL UNIQUE PRIMARY KEY,
    bundle text NOT NULL REFERENCES bucket (bucket) DEFERRABLE,
    amount NUMERIC (12,2) NOT NULL,
    "when" timestamp NOT NULL
);
