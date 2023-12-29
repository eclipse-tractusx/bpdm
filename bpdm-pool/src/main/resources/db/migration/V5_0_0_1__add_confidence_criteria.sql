ALTER TABLE legal_entities
ADD COLUMN shared_by_owner                  boolean     NOT NULL DEFAULT FALSE,
ADD COLUMN checked_by_external_data_source  boolean     NOT NULL DEFAULT FALSE,
ADD COLUMN number_of_business_partners      INT         NOT NULL DEFAULT 1,
ADD COLUMN last_confidence_check_at         TIMESTAMP   NOT NULL DEFAULT NOW()::timestamp,
ADD COLUMN next_confidence_check_at         TIMESTAMP   NOT NULL DEFAULT NOW()::timestamp,
ADD COLUMN confidence_level                 INT         NOT NULL DEFAULT 0;

ALTER TABLE sites
ADD COLUMN shared_by_owner                  boolean     NOT NULL DEFAULT FALSE,
ADD COLUMN checked_by_external_data_source  boolean     NOT NULL DEFAULT FALSE,
ADD COLUMN number_of_business_partners      INT         NOT NULL DEFAULT 1,
ADD COLUMN last_confidence_check_at         TIMESTAMP   NOT NULL DEFAULT NOW()::timestamp,
ADD COLUMN next_confidence_check_at         TIMESTAMP   NOT NULL DEFAULT NOW()::timestamp,
ADD COLUMN confidence_level                 INT         NOT NULL DEFAULT 0;

ALTER TABLE logistic_addresses
ADD COLUMN shared_by_owner                  boolean     NOT NULL DEFAULT FALSE,
ADD COLUMN checked_by_external_data_source  boolean     NOT NULL DEFAULT FALSE,
ADD COLUMN number_of_business_partners      INT         NOT NULL DEFAULT 1,
ADD COLUMN last_confidence_check_at         TIMESTAMP   NOT NULL DEFAULT NOW()::timestamp,
ADD COLUMN next_confidence_check_at         TIMESTAMP   NOT NULL DEFAULT NOW()::timestamp,
ADD COLUMN confidence_level                 INT         NOT NULL DEFAULT 0;

