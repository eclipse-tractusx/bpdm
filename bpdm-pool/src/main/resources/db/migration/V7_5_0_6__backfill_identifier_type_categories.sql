-- Identifier types created through the v6 endpoint, which has no categories field, were stored without a category and
-- had one substituted while being read. That substitution now happens when the identifier type is created, so the
-- types already stored without a category are given theirs here.
INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'OTH'
FROM identifier_types
WHERE NOT EXISTS (SELECT 1
                  FROM identifier_type_categories
                  WHERE identifier_type_categories.identifier_type_id = identifier_types.id);
