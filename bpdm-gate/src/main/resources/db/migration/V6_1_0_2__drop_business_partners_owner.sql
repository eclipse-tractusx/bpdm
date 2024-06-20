ALTER TABLE business_partners
DROP COLUMN associated_owner_bpnl;

ALTER TABLE business_partners
DROP COLUMN external_id;

ALTER TABLE sharing_states
RENAME COLUMN associated_owner_bpnl TO tenant_bpnl;

ALTER TABLE changelog_entries
RENAME COLUMN associated_owner_bpnl TO tenant_bpnl;

ALTER TABLE business_partners
ALTER COLUMN sharing_state_id SET NOT NULL;
