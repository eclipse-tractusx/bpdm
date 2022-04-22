CREATE INDEX idx_27a3d3e2dc744e624f86c42ac ON thoroughfares (address_id);

CREATE INDEX idx_27e28cee3b3c196d04d92dd16 ON localities (address_id);

CREATE INDEX idx_40c1cd3586bff0d8838b5a42f ON classifications (partner_id);

CREATE INDEX idx_68d0ddefca3d78782f306205a ON names (partner_id);

CREATE INDEX idx_68e587d8695feeb07ae0e4306 ON relations (start_node_id);

CREATE INDEX idx_6b9d462c775e3411dd2dfef5c ON administrative_areas (address_id);

CREATE INDEX idx_7773e8f689fcaef3f45fe0255 ON addresses (partner_id);

CREATE INDEX idx_7cd3dbc21ea393a2e3f579844 ON premises (address_id);

CREATE INDEX idx_8f40df2ce03f76bc9923a683a ON bank_accounts (partner_id);

CREATE INDEX idx_94ce64d22fe3bd5d4658efdfa ON post_codes (address_id);

CREATE INDEX idx_9de08b456309ac30a77546592 ON identifiers (partner_id);

CREATE INDEX idx_a91204c0e84c71629e3639ba7 ON relations (end_node_id);

CREATE INDEX idx_e645e8b157b7bbaed58397144 ON business_stati (partner_id);

CREATE INDEX idx_f8a9c5185fbb7e54bdd96edb8 ON postal_delivery_points (address_id);