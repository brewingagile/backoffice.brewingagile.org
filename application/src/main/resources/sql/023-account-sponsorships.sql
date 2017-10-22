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
SELECT substring(bucket FROM 'Sponsor: (.+)'), '', '' FROM bucket
WHERE bucket ILIKE 'Sponsor: %'
UNION
SELECT substring(bucket FROM 'Invoice: (.+)'), '', '' FROM bucket
WHERE bucket ILIKE 'Invoice: %';

INSERT INTO account_package (account, package_number, description, price)
SELECT substring(bucket FROM 'Sponsor: (.+)'), 1, CASE
    WHEN price = 5000 THEN 'Sponsor: Bottle'
    WHEN price = 10000 THEN 'Sponsor: On Tap'
    END,
    bundle_deal.price
FROM bucket
JOIN bundle_deal ON (bundle_deal.bundle = bucket.bucket)
WHERE bucket ILIKE 'Sponsor: %';

INSERT INTO account_package_ticket (account, package_number, ticket, qty)
SELECT
    substring(bucket FROM 'Sponsor: (.+)'),
    1,
    'conference',
    CASE
    WHEN bundle_deal.price = 5000 THEN 1
    WHEN bundle_deal.price = 10000 THEN 2
    END
FROM bucket
JOIN bundle_deal ON (bundle_deal.bundle = bucket.bucket)
WHERE bucket ILIKE 'Sponsor: %';

INSERT INTO account (account, billing_recipient, billing_address) VALUES
('Brewing Agile', '', '');
INSERT INTO account_package (account, package_number, description, price) VALUES
('Brewing Agile', 1, 'Organisers', 0),
('Brewing Agile', 2, 'Speakers', 0);
INSERT INTO account_package_ticket (account, package_number, ticket, qty) VALUES
('Brewing Agile', 1, 'conference', 5),
('Brewing Agile', 2, 'conference', 4);

CREATE TABLE registration_account (
    registration_id uuid NOT NULL
        UNIQUE
        PRIMARY KEY
        REFERENCES registration (registration_id),
    account text NOT NULL REFERENCES account (account)
);
INSERT INTO registration_account (registration_id, account)
SELECT registration_id, COALESCE(
    NULLIF(substring(bucket FROM 'Sponsor: (.+)'), ''),
    NULLIF(substring(bucket FROM 'Invoice: (.+)'), ''),
    bucket)
FROM registration_bucket
JOIN bucket USING (bucket);

