ALTER TABLE logistic_addresses
ADD COLUMN phy_street_number_supplement VARCHAR(255);

ALTER TABLE postal_addresses
ADD COLUMN phy_street_number_supplement VARCHAR(255);