ALTER TABLE address_relations
    ADD COLUMN kind VARCHAR(255) NOT NULL DEFAULT 'Succession'
        CHECK (kind IN ('Succession', 'HeadquarterRelocation'));

-- A relocation that already took effect is no longer recognisable by its start address, because the headquarter
-- synchronization moved the legal address to its end. It is found by walking back from each current legal address
-- over the relations that synchronization walks today.
WITH RECURSIVE started_relations AS (
    SELECT DISTINCT relation.id, relation.start_address_id, relation.end_address_id
    FROM address_relations relation
    JOIN address_relation_validity_periods period ON period.relation_id = relation.id
    WHERE period.valid_from <= CURRENT_DATE AND (period.valid_to IS NULL OR period.valid_to >= CURRENT_DATE)
),
applied_relocations AS (
    SELECT started.id, started.start_address_id
    FROM started_relations started
    JOIN legal_entities legal_entity ON legal_entity.legal_address_id = started.end_address_id
    UNION
    SELECT started.id, started.start_address_id
    FROM started_relations started
    JOIN applied_relocations applied ON started.end_address_id = applied.start_address_id
)
UPDATE address_relations
SET kind = 'HeadquarterRelocation'
WHERE id IN (SELECT id FROM applied_relocations)
   OR start_address_id IN (SELECT legal_address_id FROM legal_entities);
