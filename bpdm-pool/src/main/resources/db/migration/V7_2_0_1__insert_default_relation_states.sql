INSERT INTO relation_validity_periods
( SELECT id as relation_id, updated_at as valid_from, NULL FROM relations )

