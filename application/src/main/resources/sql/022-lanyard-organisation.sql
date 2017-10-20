ALTER TABLE registration ADD COLUMN organisation text;
UPDATE registration SET organisation = billing_company;
ALTER TABLE registration ALTER COLUMN organisation SET NOT NULL;
