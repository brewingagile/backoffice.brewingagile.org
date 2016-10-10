CREATE TABLE budget_fixed_costs (
    cost text NOT NULL CHECK (cost <> '') PRIMARY KEY,
    amount 	NUMERIC (12,2) NOT NULL
);