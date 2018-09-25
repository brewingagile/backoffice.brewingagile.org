ALTER TABLE account_package_ticket DROP CONSTRAINT account_package_ticket_ticket_fkey;
ALTER TABLE account_package_ticket ADD CONSTRAINT account_package_ticket_ticket_fkey FOREIGN KEY (ticket) REFERENCES ticket(ticket) ON UPDATE CASCADE DEFERRABLE;
