CREATE TABLE account (
    account text NOT NULL PRIMARY KEY UNIQUE,
    billing_recipient text NOT NULL,
    billing_address text NOT NULL
);

CREATE TABLE account_package (
    account text NOT NULL REFERENCES account(account) ON UPDATE CASCADE,
    package_number integer NOT NULL,
    description text NOT NULL,
    price NUMERIC (12,2) NOT NULL,
    PRIMARY KEY (account, package_number)
);

CREATE TABLE account_package_ticket (
    account text NOT NULL,
    package_number integer NOT NULL,
    ticket text NOT NULL,
    qty integer NOT NULL,
    PRIMARY KEY (account, package_number, ticket),
    FOREIGN KEY (account, package_number) REFERENCES account_package (account, package_number) ON UPDATE CASCADE,
    FOREIGN KEY (ticket) REFERENCES ticket (ticket) ON UPDATE CASCADE
);

INSERT INTO account (account, billing_recipient, billing_address)
SELECT DISTINCT COALESCE(
    NULLIF(substring(bucket FROM 'Sponsor: (.+)'), ''),
    NULLIF(substring(bucket FROM 'Invoice: (.+)'), ''),
    bucket),
    '',
    ''
FROM bucket;

INSERT INTO account_package (account, package_number, description, price)
SELECT
    COALESCE(
        NULLIF(substring(bucket FROM 'Sponsor: (.+)'), ''),
        NULLIF(substring(bucket FROM 'Invoice: (.+)'), ''),
        bucket),
    1,
    CASE
    WHEN price = 5000 THEN 'Sponsor: Bottle'
    WHEN price = 10000 THEN 'Sponsor: On Tap'
    ELSE 'Other'
    END,
    bundle_deal.price
FROM bucket
JOIN bundle_deal ON (bundle_deal.bundle = bucket.bucket);

INSERT INTO account_package_ticket (account, package_number, ticket, qty)
SELECT
    COALESCE(
        NULLIF(substring(bucket FROM 'Sponsor: (.+)'), ''),
        NULLIF(substring(bucket FROM 'Invoice: (.+)'), ''),
        bucket),
    1,
    'conference',
    bucket.conference
FROM bucket
JOIN bundle_deal ON (bundle_deal.bundle = bucket.bucket);

CREATE TABLE registration_account (
    registration_id uuid NOT NULL
        UNIQUE
        PRIMARY KEY
        REFERENCES registration (registration_id),
    account text NOT NULL REFERENCES account (account) ON UPDATE CASCADE
);
INSERT INTO registration_account (registration_id, account)
SELECT registration_id, COALESCE(
    NULLIF(substring(bucket FROM 'Sponsor: (.+)'), ''),
    NULLIF(substring(bucket FROM 'Invoice: (.+)'), ''),
    bucket)
FROM registration_bucket
JOIN bucket USING (bucket);

