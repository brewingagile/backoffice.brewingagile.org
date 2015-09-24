ALTER TABLE registration_bucket DROP CONSTRAINT registration_bucket_bucket_fkey;
ALTER TABLE registration_bucket ADD CONSTRAINT registration_bucket_bucket_fkey FOREIGN KEY (bucket) REFERENCES bucket (bucket) DEFERRABLE;

