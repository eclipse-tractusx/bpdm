do
$$
    declare
        identifier_type_values varchar[][] := array [
            ['Value added tax identification number', 'EU_VAT_ID_DE'],
            ['Value added tax identification number', 'EU_VAT_ID_FR'],
            ['Value added tax identification number', 'EU_VAT_ID_AT'],
            ['Value added tax identification number', 'EU_VAT_ID_BE'],
            ['Value added tax identification number', 'EU_VAT_ID_CH'],
            ['Value added tax identification number', 'EU_VAT_ID_CZ'],
            ['Value added tax identification number', 'EU_VAT_ID_DK'],
            ['Value added tax identification number', 'EU_VAT_ID_ES'],
            ['Value added tax identification number', 'EU_VAT_ID_GB'],
            ['Value added tax identification number', 'EU_VAT_ID_NO'],
            ['Value added tax identification number', 'EU_VAT_ID_PL'],
            ['Global Location Number', 'GS1_GLN'],
            ['Legal Entity Identifier', 'LEI_ID'],
            ['Data Universal Numbering System', 'DUNS_ID'],
            ['Handelsregister (HRB)', 'DE_BNUM'],
            ['Siren/Siret', 'FR_SIREN'],
            ['Firmenbuchnummer', 'BR_ID_AT'],
            ['Organisation number', 'BE_ENT_NO'],
            ['Company Identification Number CH', 'CH_UID'],
            ['Company Identification Number CZ', 'CZ_ICO'],
            ['Business Registration Number DK', 'CVR_DK'],
            ['Certificado de Identificación Fiscal', 'CIF_ES'],
            ['Company Registration Number', 'ID_CRN'],
            ['Organization Number', 'NO_ORGID'],
            ['REGON', 'PL_REG']
            ];
        identifier_type_value  varchar[];
    begin
        foreach identifier_type_value SLICE 1 in array identifier_type_values
            loop
                insert into identifier_types(id, uuid, created_at, updated_at, name, technical_key)
                select nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, identifier_type_value[1], identifier_type_value[2]
                where not exists(
                        select technical_key FROM identifier_types WHERE technical_key = identifier_type_value[2]
                    );
            end loop;
    end;
$$;

insert into country_identifier_types (id, created_at, updated_at, uuid, country_code, mandatory, identifier_type_id)
values (nextval('bpdm_sequence'), LOCALTIMESTAMP, LOCALTIMESTAMP, gen_random_uuid(), null, false,
        (SELECT id from identifier_types where technical_key = 'GS1_GLN'));

insert into country_identifier_types (id, created_at, updated_at, uuid, country_code, mandatory, identifier_type_id)
values (nextval('bpdm_sequence'), LOCALTIMESTAMP, LOCALTIMESTAMP, gen_random_uuid(), null, false,
        (SELECT id from identifier_types where technical_key = 'LEI_ID'));

insert into country_identifier_types (id, created_at, updated_at, uuid, country_code, mandatory, identifier_type_id)
values (nextval('bpdm_sequence'), LOCALTIMESTAMP, LOCALTIMESTAMP, gen_random_uuid(), null, false,
        (SELECT id from identifier_types where technical_key = 'DUNS_ID'));

insert into country_identifier_types (id, created_at, updated_at, uuid, country_code, mandatory, identifier_type_id)
values (nextval('bpdm_sequence'), LOCALTIMESTAMP, LOCALTIMESTAMP, gen_random_uuid(), 'DE', true,
        (SELECT id from identifier_types where technical_key = 'EU_VAT_ID_DE'));

insert into country_identifier_types (id, created_at, updated_at, uuid, country_code, mandatory, identifier_type_id)
values (nextval('bpdm_sequence'), LOCALTIMESTAMP, LOCALTIMESTAMP, gen_random_uuid(), 'DE', false,
        (SELECT id from identifier_types where technical_key = 'DE_BNUM'));

insert into country_identifier_types (id, created_at, updated_at, uuid, country_code, mandatory, identifier_type_id)
values (nextval('bpdm_sequence'), LOCALTIMESTAMP, LOCALTIMESTAMP, gen_random_uuid(), 'FR', false,
        (SELECT id from identifier_types where technical_key = 'EU_VAT_ID_FR'));

insert into country_identifier_types (id, created_at, updated_at, uuid, country_code, mandatory, identifier_type_id)
values (nextval('bpdm_sequence'), LOCALTIMESTAMP, LOCALTIMESTAMP, gen_random_uuid(), 'FR', true,
        (SELECT id from identifier_types where technical_key = 'FR_SIREN'));

insert into country_identifier_types (id, created_at, updated_at, uuid, country_code, mandatory, identifier_type_id)
values (nextval('bpdm_sequence'), LOCALTIMESTAMP, LOCALTIMESTAMP, gen_random_uuid(), 'AT', false,
        (SELECT id from identifier_types where technical_key = 'EU_VAT_ID_AT'));

insert into country_identifier_types (id, created_at, updated_at, uuid, country_code, mandatory, identifier_type_id)
values (nextval('bpdm_sequence'), LOCALTIMESTAMP, LOCALTIMESTAMP, gen_random_uuid(), 'AT', true,
        (SELECT id from identifier_types where technical_key = 'BR_ID_AT'));

insert into country_identifier_types (id, created_at, updated_at, uuid, country_code, mandatory, identifier_type_id)
values (nextval('bpdm_sequence'), LOCALTIMESTAMP, LOCALTIMESTAMP, gen_random_uuid(), 'BE', false,
        (SELECT id from identifier_types where technical_key = 'EU_VAT_ID_BE'));

insert into country_identifier_types (id, created_at, updated_at, uuid, country_code, mandatory, identifier_type_id)
values (nextval('bpdm_sequence'), LOCALTIMESTAMP, LOCALTIMESTAMP, gen_random_uuid(), 'BE', true,
        (SELECT id from identifier_types where technical_key = 'BE_ENT_NO'));

insert into country_identifier_types (id, created_at, updated_at, uuid, country_code, mandatory, identifier_type_id)
values (nextval('bpdm_sequence'), LOCALTIMESTAMP, LOCALTIMESTAMP, gen_random_uuid(), 'CH', false,
        (SELECT id from identifier_types where technical_key = 'EU_VAT_ID_CH'));

insert into country_identifier_types (id, created_at, updated_at, uuid, country_code, mandatory, identifier_type_id)
values (nextval('bpdm_sequence'), LOCALTIMESTAMP, LOCALTIMESTAMP, gen_random_uuid(), 'CH', true,
        (SELECT id from identifier_types where technical_key = 'CH_UID'));

insert into country_identifier_types (id, created_at, updated_at, uuid, country_code, mandatory, identifier_type_id)
values (nextval('bpdm_sequence'), LOCALTIMESTAMP, LOCALTIMESTAMP, gen_random_uuid(), 'CZ', false,
        (SELECT id from identifier_types where technical_key = 'EU_VAT_ID_CZ'));

insert into country_identifier_types (id, created_at, updated_at, uuid, country_code, mandatory, identifier_type_id)
values (nextval('bpdm_sequence'), LOCALTIMESTAMP, LOCALTIMESTAMP, gen_random_uuid(), 'CZ', true,
        (SELECT id from identifier_types where technical_key = 'CZ_ICO'));

insert into country_identifier_types (id, created_at, updated_at, uuid, country_code, mandatory, identifier_type_id)
values (nextval('bpdm_sequence'), LOCALTIMESTAMP, LOCALTIMESTAMP, gen_random_uuid(), 'DK', false,
        (SELECT id from identifier_types where technical_key = 'EU_VAT_ID_DK'));

insert into country_identifier_types (id, created_at, updated_at, uuid, country_code, mandatory, identifier_type_id)
values (nextval('bpdm_sequence'), LOCALTIMESTAMP, LOCALTIMESTAMP, gen_random_uuid(), 'DK', true,
        (SELECT id from identifier_types where technical_key = 'CVR_DK'));

insert into country_identifier_types (id, created_at, updated_at, uuid, country_code, mandatory, identifier_type_id)
values (nextval('bpdm_sequence'), LOCALTIMESTAMP, LOCALTIMESTAMP, gen_random_uuid(), 'ES', false,
        (SELECT id from identifier_types where technical_key = 'EU_VAT_ID_ES'));

insert into country_identifier_types (id, created_at, updated_at, uuid, country_code, mandatory, identifier_type_id)
values (nextval('bpdm_sequence'), LOCALTIMESTAMP, LOCALTIMESTAMP, gen_random_uuid(), 'ES', true,
        (SELECT id from identifier_types where technical_key = 'CIF_ES'));

insert into country_identifier_types (id, created_at, updated_at, uuid, country_code, mandatory, identifier_type_id)
values (nextval('bpdm_sequence'), LOCALTIMESTAMP, LOCALTIMESTAMP, gen_random_uuid(), 'GB', false,
        (SELECT id from identifier_types where technical_key = 'EU_VAT_ID_GB'));

insert into country_identifier_types (id, created_at, updated_at, uuid, country_code, mandatory, identifier_type_id)
values (nextval('bpdm_sequence'), LOCALTIMESTAMP, LOCALTIMESTAMP, gen_random_uuid(), 'GB', true,
        (SELECT id from identifier_types where technical_key = 'ID_CRN'));

insert into country_identifier_types (id, created_at, updated_at, uuid, country_code, mandatory, identifier_type_id)
values (nextval('bpdm_sequence'), LOCALTIMESTAMP, LOCALTIMESTAMP, gen_random_uuid(), 'NO', false,
        (SELECT id from identifier_types where technical_key = 'EU_VAT_ID_NO'));

insert into country_identifier_types (id, created_at, updated_at, uuid, country_code, mandatory, identifier_type_id)
values (nextval('bpdm_sequence'), LOCALTIMESTAMP, LOCALTIMESTAMP, gen_random_uuid(), 'NO', true,
        (SELECT id from identifier_types where technical_key = 'NO_ORGID'));

insert into country_identifier_types (id, created_at, updated_at, uuid, country_code, mandatory, identifier_type_id)
values (nextval('bpdm_sequence'), LOCALTIMESTAMP, LOCALTIMESTAMP, gen_random_uuid(), 'PL', false,
        (SELECT id from identifier_types where technical_key = 'EU_VAT_ID_PL'));

insert into country_identifier_types (id, created_at, updated_at, uuid, country_code, mandatory, identifier_type_id)
values (nextval('bpdm_sequence'), LOCALTIMESTAMP, LOCALTIMESTAMP, gen_random_uuid(), 'PL', true,
        (SELECT id from identifier_types where technical_key = 'PL_REG'));
