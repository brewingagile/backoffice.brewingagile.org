CREATE TABLE registration_ticket (
    registration_id uuid NOT NULL REFERENCES registration (registration_id),
    ticket text NOT NULL CHECK (ticket <> ''),
    PRIMARY KEY (registration_id, ticket)
);
INSERT INTO registration_ticket (registration_id, ticket) SELECT registration_id, 'conference' FROM registration;
INSERT INTO registration_ticket (registration_id, ticket) SELECT registration_id, 'workshop1' FROM registration WHERE (position('conference+workshop' in ticket) > 0);
INSERT INTO registration_ticket (registration_id, ticket) SELECT registration_id, 'workshop2' FROM registration WHERE (position('conference+workshop2' in ticket) > 0);
ALTER TABLE registration DROP COLUMN ticket;