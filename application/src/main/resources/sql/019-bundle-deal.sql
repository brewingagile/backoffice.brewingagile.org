CREATE TABLE bundle_deal (
	bundle text NOT NULL PRIMARY KEY REFERENCES bucket (bucket) DEFERRABLE,
	price NUMERIC (12,2) NOT NULL CHECK (price >= 0.0)
);