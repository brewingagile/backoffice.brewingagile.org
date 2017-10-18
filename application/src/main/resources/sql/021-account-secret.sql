CREATE TABLE account_secret (
    secret_id uuid NOT NULL PRIMARY KEY,
    bundle text NOT NULL REFERENCES bucket (bucket) DEFERRABLE
);
