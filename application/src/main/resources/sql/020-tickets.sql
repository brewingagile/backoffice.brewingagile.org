CREATE TABLE ticket (
    ticket text NOT NULL CHECK (ticket <> '') PRIMARY KEY,
    product_text text NOT NULL,
    price  NUMERIC (12,2) NOT NULL,
    seats INTEGER NOT NULL
);

INSERT INTO ticket (ticket, product_text, price, seats) VALUES
    ('conference', 'Conference: You go to the conference on Friday afternoon and the discussion groups on Saturday. Starts at 13:00.', 1200, 110),
    ('workshop1', 'Workshop: "#NoEstimates" with Vasco Duarte (Thursday, all day).', 3500, 22),
    ('workshop2', 'Workshop: "Agile Retrospectives" with Luis Goncalves (Friday morning).', 1750, 20);

ALTER TABLE registration_ticket ADD CONSTRAINT registration_ticket_ticket FOREIGN KEY (ticket) REFERENCES ticket (ticket) ON UPDATE CASCADE;