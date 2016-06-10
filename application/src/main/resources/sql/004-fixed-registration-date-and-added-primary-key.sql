ALTER TABLE registrations ALTER COLUMN registered SET DEFAULT now();
ALTER TABLE registrations ADD CONSTRAINT registrations_pk PRIMARY KEY (id);

