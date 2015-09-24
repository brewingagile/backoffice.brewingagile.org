ALTER TABLE registrations RENAME COLUMN role TO badge;
ALTER TABLE registrations ALTER COLUMN badge SET DEFAULT '';
UPDATE registrations SET badge = '' WHERE badge IN ('DELEGATE','FREEBIE','STUDENT','SPONSOR'); 
