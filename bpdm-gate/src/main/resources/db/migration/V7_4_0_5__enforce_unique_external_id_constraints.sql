CREATE UNIQUE INDEX IF NOT EXISTS ux_sharing_states_external_id_null_tenant
ON sharing_states (external_id)
WHERE tenant_bpnl IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_sharing_states_tenant_external_id
ON sharing_states (tenant_bpnl, external_id)
WHERE tenant_bpnl IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_business_partners_sharing_state_stage
ON business_partners (sharing_state_id, stage);

