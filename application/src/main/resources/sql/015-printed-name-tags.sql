CREATE TABLE printed_nametags (
    registration_id UUID NOT NULL PRIMARY KEY REFERENCES registrations (id) ON DELETE CASCADE ON UPDATE CASCADE
);