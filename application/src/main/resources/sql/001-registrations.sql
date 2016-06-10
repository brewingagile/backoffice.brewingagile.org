CREATE TABLE registrations (
	id text NOT NULL, 
	participant_name text NOT NULL, 
	participant_email text NOT NULL,
	billing_company text NOT NULL,
	billing_address text NOT NULL,
	registered timestamp DEFAULT 'NOW()' NOT NULL
	);

