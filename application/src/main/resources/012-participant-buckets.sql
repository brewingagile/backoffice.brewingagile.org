CREATE TABLE bucket (
	bucket text NOT NULL CHECK (bucket <> '') PRIMARY KEY,
	conference integer NOT NULL,
	workshop1 integer NOT NULL,
	workshop2 integer NOT NULL
);

CREATE TABLE registration_bucket (
	registration_id UUID NOT NULL PRIMARY KEY 
		REFERENCES registrations (id),
	bucket text NOT NULL REFERENCES bucket (bucket) ON UPDATE CASCADE
); 
