UPDATE changelog_entries
SET business_partner_type = 'LEGAL_ENTITY'
WHERE business_partner_type = 'LegalEntity';

UPDATE changelog_entries
SET business_partner_type = 'SITE'
WHERE business_partner_type = 'Site';

UPDATE changelog_entries
SET business_partner_type = 'ADDRESS'
WHERE business_partner_type = 'Address';

UPDATE sharing_states
SET lsa_type = 'LEGAL_ENTITY'
WHERE lsa_type = 'LegalEntity';

UPDATE sharing_states
SET lsa_type = 'SITE'
WHERE lsa_type = 'Site';

UPDATE sharing_states
SET lsa_type = 'ADDRESS'
WHERE lsa_type = 'Address';