UPDATE bpdmgate.business_partner_relations r
SET sharing_state_type = 'Ready',
    sharing_state_updated_at = now(),
	is_staged = false
FROM bpdmgate.business_partner_relation_stages st
WHERE r.sharing_state_type IS NULL
AND st.relation_type = 'IsManagedBy'
AND st.relation_id = r.id;
