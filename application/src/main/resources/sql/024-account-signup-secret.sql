CREATE TABLE account_signup_secret (
    account_signup_secret uuid NOT NULL PRIMARY KEY,
    account text NOT NULL REFERENCES account (account) ON UPDATE CASCADE ON DELETE CASCADE
);
