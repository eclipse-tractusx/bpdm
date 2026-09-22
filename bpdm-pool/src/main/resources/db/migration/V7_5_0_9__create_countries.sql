CREATE TABLE countries
(
    id                          BIGINT                      NOT NULL,
    uuid                        UUID                        NOT NULL,
    created_at                  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at                  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    country_code                VARCHAR(2)                  NOT NULL,
    name                        VARCHAR(255)                NOT NULL,
    description                 VARCHAR(255),
    CONSTRAINT pk_countries PRIMARY KEY (id),
    CONSTRAINT uc_countries_country_code UNIQUE (country_code)
);

CREATE INDEX idx_countries_country_code ON countries (country_code);
