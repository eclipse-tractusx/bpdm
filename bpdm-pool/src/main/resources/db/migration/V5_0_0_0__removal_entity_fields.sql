ALTER TABLE logistic_addresses
DROP COLUMN IF EXISTS alt_district_l1,
DROP COLUMN IF EXISTS alt_district_l2,
DROP COLUMN IF EXISTS alt_admin_area_l2,
DROP COLUMN IF EXISTS alt_admin_area_l3,
DROP COLUMN IF EXISTS alt_admin_area_l4;


-- DROP COLUMN IF EXISTS alt_street_name,
-- DROP COLUMN IF EXISTS alt_street_number,
-- DROP COLUMN IF EXISTS alt_street_milestone,
-- DROP COLUMN IF EXISTS alt_street_direction;