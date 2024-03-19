ALTER TABLE business_partners
ADD COLUMN associated_owner_bpnl VARCHAR(255);


ALTER TABLE changelog_entries
ADD COLUMN associated_owner_bpnl VARCHAR(255);


ALTER TABLE sharing_states
ADD COLUMN associated_owner_bpnl VARCHAR(255);