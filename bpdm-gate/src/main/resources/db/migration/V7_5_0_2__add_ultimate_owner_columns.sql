ALTER TABLE business_partners
ADD COLUMN ownership_ultimate BOOLEAN,
ADD COLUMN ultimate_owner_bpnl VARCHAR(20);
