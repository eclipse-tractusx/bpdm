UPDATE business_partners bp
SET    sharing_state_id = sh.id
FROM   sharing_states sh
WHERE  bp.external_id IS NOT DISTINCT FROM sh.external_id AND bp.associated_owner_bpnl IS NOT DISTINCT FROM sh.associated_owner_bpnl;