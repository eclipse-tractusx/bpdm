INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'AT_FBN', 'LEGAL_ENTITY', 'Firmenbuchnummer', NULL, 'FBN', NULL, '^d{1,6}[a-z]$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Firmenbuchnummer', transliterated_name = NULL, abbreviation = 'FBN', transliterated_abbreviation = NULL, format = '^d{1,6}[a-z]$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'AT_EU_VAT_ID', 'LEGAL_ENTITY', 'European Union Value-added Tax Identification Number', NULL, 'EU VAT ID (number)', NULL, '^ATUd{7}d{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'European Union Value-added Tax Identification Number', transliterated_name = NULL, abbreviation = 'EU VAT ID (number)', transliterated_abbreviation = NULL, format = '^ATUd{7}d{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'BE_OND', 'LEGAL_ENTITY', 'Ondernemingsnummer / Numéro d''entreprise', NULL, 'OND-(nummer) / (numéro) ENT', 'OND-(nummer) / (numero) ENT', '^[0,1]{1}d{3}d{3}d{1}d{2}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Ondernemingsnummer / Numéro d''entreprise', transliterated_name = NULL, abbreviation = 'OND-(nummer) / (numéro) ENT', transliterated_abbreviation = 'OND-(nummer) / (numero) ENT', format = '^[0,1]{1}d{3}d{3}d{1}d{2}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'BE_BTW', 'LEGAL_ENTITY', 'Belasting over de toegevoegde waarde nummer / Numéro de taxe sur la valeur ajoutée', NULL, 'BTW(-nummer) / (numéro) TVA', 'BTW(-nummer) / (numero) TVA', '^[0,1]d{3}d{3}d{1}d{2}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Belasting over de toegevoegde waarde nummer / Numéro de taxe sur la valeur ajoutée', transliterated_name = NULL, abbreviation = 'BTW(-nummer) / (numéro) TVA', transliterated_abbreviation = 'BTW(-nummer) / (numero) TVA', format = '^[0,1]d{3}d{3}d{1}d{2}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'BE_EU_VAT_ID', 'LEGAL_ENTITY', 'European Union Value-added Tax Identification Number', NULL, 'EU VAT ID (number)', NULL, '^BE[0,1]d{3}d{3}d{1}d{2}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'European Union Value-added Tax Identification Number', transliterated_name = NULL, abbreviation = 'EU VAT ID (number)', transliterated_abbreviation = NULL, format = '^BE[0,1]d{3}d{3}d{1}d{2}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'BG_EIK', 'LEGAL_ENTITY', 'Единен идентификационен код', NULL, 'ЕИК', 'EIK', '^d{8,9}(d{1})?$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Единен идентификационен код', transliterated_name = NULL, abbreviation = 'ЕИК', transliterated_abbreviation = 'EIK', format = '^d{8,9}(d{1})?$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'BG_EU_VAT_ID', 'LEGAL_ENTITY', 'European Union Value-added Tax Identification Number', NULL, 'EU VAT ID (number)', NULL, '^BGd{8,9}(d{1})?$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'European Union Value-added Tax Identification Number', transliterated_name = NULL, abbreviation = 'EU VAT ID (number)', transliterated_abbreviation = NULL, format = '^BGd{8,9}(d{1})?$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'CH_UID', 'LEGAL_ENTITY', 'Unternehmens-Identifikationsnummer', NULL, 'UID', NULL, '^CHE-d{3}.d{3}.d{3}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Unternehmens-Identifikationsnummer', transliterated_name = NULL, abbreviation = 'UID', transliterated_abbreviation = NULL, format = '^CHE-d{3}.d{3}.d{3}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'CH_EHRA_ID', 'LEGAL_ENTITY', 'Eidgenössisches Handelsregisteramt-Identifikationsnummer', NULL, 'EHRA-ID', NULL, '^d{6}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Eidgenössisches Handelsregisteramt-Identifikationsnummer', transliterated_name = NULL, abbreviation = 'EHRA-ID', transliterated_abbreviation = NULL, format = '^d{6}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'CH_UID_MWST', 'LEGAL_ENTITY', 'Unternehmens-Identifikationsnummer (Mehrwertsteuer)', NULL, 'UID MWSt.', NULL, '^CHE-d{3}.d{3}.d{3}s(MWST|TVA|IPA)$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Unternehmens-Identifikationsnummer (Mehrwertsteuer)', transliterated_name = NULL, abbreviation = 'UID MWSt.', transliterated_abbreviation = NULL, format = '^CHE-d{3}.d{3}.d{3}s(MWST|TVA|IPA)$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'CY_AEE', 'LEGAL_ENTITY', 'Αριθμός Εγγραφής στο Τμήμα Εφόρου Εταιρειών', NULL, 'AEE', NULL, '^[A-Z]{1,2}d{1,8}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Αριθμός Εγγραφής στο Τμήμα Εφόρου Εταιρειών', transliterated_name = NULL, abbreviation = 'AEE', transliterated_abbreviation = NULL, format = '^[A-Z]{1,2}d{1,8}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'CY_AFT', 'LEGAL_ENTITY', 'Αριθμός φορολογικής ταυτότητας', NULL, 'ΑΦΤ', 'AFT', '^[013945]{1}d{7}[A-Z]{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Αριθμός φορολογικής ταυτότητας', transliterated_name = NULL, abbreviation = 'ΑΦΤ', transliterated_abbreviation = 'AFT', format = '^[013945]{1}d{7}[A-Z]{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'CY_EU_VAT_ID', 'LEGAL_ENTITY', 'European Union Value-added Tax Identification Number', NULL, 'EU VAT ID (number)', NULL, '^CY[013945]{1}d{7}[A-Z]{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'European Union Value-added Tax Identification Number', transliterated_name = NULL, abbreviation = 'EU VAT ID (number)', transliterated_abbreviation = NULL, format = '^CY[013945]{1}d{7}[A-Z]{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'CZ_ICO', 'LEGAL_ENTITY', 'Identifikační číslo osoby', NULL, 'IČO', 'ICO', '^[1-9]{1}d{6,8}d{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Identifikační číslo osoby', transliterated_name = NULL, abbreviation = 'IČO', transliterated_abbreviation = 'ICO', format = '^[1-9]{1}d{6,8}d{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'CZ_DIC', 'LEGAL_ENTITY', 'Daňové identifikační číslo', NULL, 'DIČ', 'DIC', '^[1-9]{1}d{6,8}d{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Daňové identifikační číslo', transliterated_name = NULL, abbreviation = 'DIČ', transliterated_abbreviation = 'DIC', format = '^[1-9]{1}d{6,8}d{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'CZ_EU_VAT_ID', 'LEGAL_ENTITY', 'European Union Value-added Tax Identification Number', NULL, 'EU VAT ID (number)', NULL, '^CZ[1-9]{1}d{6,8}d{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'European Union Value-added Tax Identification Number', transliterated_name = NULL, abbreviation = 'EU VAT ID (number)', transliterated_abbreviation = NULL, format = '^CZ[1-9]{1}d{6,8}d{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'DE_HR', 'LEGAL_ENTITY', 'Handelsregisternummer', NULL, 'HR(-nummer)', NULL, '^([BDFGHKMNPRTUVWXY]{1}d{1,4}[VR]?.)?((HRA)|(G(n|N)R)|(HRB)|(PR)|(VR)|(G(s|S)R))[1-9]{1}[A-Z0-9]{1,5}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Handelsregisternummer', transliterated_name = NULL, abbreviation = 'HR(-nummer)', transliterated_abbreviation = NULL, format = '^([BDFGHKMNPRTUVWXY]{1}d{1,4}[VR]?.)?((HRA)|(G(n|N)R)|(HRB)|(PR)|(VR)|(G(s|S)R))[1-9]{1}[A-Z0-9]{1,5}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'DE_EU_VAT_ID', 'LEGAL_ENTITY', 'European Union Value-added Tax Identification Number', NULL, 'EU VAT ID (number)', NULL, '^DEd{8}d{1}(-d{5})?$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'European Union Value-added Tax Identification Number', transliterated_name = NULL, abbreviation = 'EU VAT ID (number)', transliterated_abbreviation = NULL, format = '^DEd{8}d{1}(-d{5})?$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'DK_CVR', 'LEGAL_ENTITY', 'Centrale Virksomhedsregister Nummer', NULL, 'CVR(-nummer)', NULL, '^d{7}d{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Centrale Virksomhedsregister Nummer', transliterated_name = NULL, abbreviation = 'CVR(-nummer)', transliterated_abbreviation = NULL, format = '^d{7}d{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'DK_EU_VAT_ID', 'LEGAL_ENTITY', 'European Union Value-added Tax Identification Number', NULL, 'EU VAT ID (number)', NULL, '^DKd{7}d{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'European Union Value-added Tax Identification Number', transliterated_name = NULL, abbreviation = 'EU VAT ID (number)', transliterated_abbreviation = NULL, format = '^DKd{7}d{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'EE_RG', 'LEGAL_ENTITY', 'Äriregistri kood', NULL, 'RG(-kood)', NULL, '^d{8}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Äriregistri kood', transliterated_name = NULL, abbreviation = 'RG(-kood)', transliterated_abbreviation = NULL, format = '^d{8}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'EE_EU_VAT_ID', 'LEGAL_ENTITY', 'European Union Value-added Tax Identification Number', NULL, 'EU VAT ID (number)', NULL, '^EEd{8}d{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'European Union Value-added Tax Identification Number', transliterated_name = NULL, abbreviation = 'EU VAT ID (number)', transliterated_abbreviation = NULL, format = '^EEd{8}d{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'ES_RM', 'LEGAL_ENTITY', 'Número de inscripción en el registro mercantil', NULL, '(número) RM', '(numero) RM', '^d{8}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Número de inscripción en el registro mercantil', transliterated_name = NULL, abbreviation = '(número) RM', transliterated_abbreviation = '(numero) RM', format = '^d{8}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'ES_NIF', 'LEGAL_ENTITY', 'Número de identificación fiscal (fka Código de identificación fiscal)', NULL, 'NIF', NULL, '^[A-HJ-NP-SUVW]{1}d{2}d{5}[A-Z0-9]{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Número de identificación fiscal (fka Código de identificación fiscal)', transliterated_name = NULL, abbreviation = 'NIF', transliterated_abbreviation = NULL, format = '^[A-HJ-NP-SUVW]{1}d{2}d{5}[A-Z0-9]{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'ES_EU_VAT_ID', 'LEGAL_ENTITY', 'European Union Value-added Tax Identification Number', NULL, 'EU VAT ID (number)', NULL, '^ES[A-HJ-NP-SUVW]{1}d{2}d{5}[A-Z0-9]{1}$ ')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'European Union Value-added Tax Identification Number', transliterated_name = NULL, abbreviation = 'EU VAT ID (number)', transliterated_abbreviation = NULL, format = '^ES[A-HJ-NP-SUVW]{1}d{2}d{5}[A-Z0-9]{1}$ ';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'FI_Y', 'LEGAL_ENTITY', 'Yritys- ja yhteisötunnus', NULL, 'Y(-tunnus)', NULL, '^d{7}-d{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Yritys- ja yhteisötunnus', transliterated_name = NULL, abbreviation = 'Y(-tunnus)', transliterated_abbreviation = NULL, format = '^d{7}-d{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'FI_EU_VAT_ID', 'LEGAL_ENTITY', 'European Union Value-added Tax Identification Number', NULL, 'EU VAT ID (number)', NULL, '^FId{7}d{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'European Union Value-added Tax Identification Number', transliterated_name = NULL, abbreviation = 'EU VAT ID (number)', transliterated_abbreviation = NULL, format = '^FId{7}d{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'FR_SIREN', 'LEGAL_ENTITY', 'Numéro des système d''identification du répertoire des entreprises', NULL, '(numéro) SIREN', '(numero) SIREN', '^d{8}d{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Numéro des système d''identification du répertoire des entreprises', transliterated_name = NULL, abbreviation = '(numéro) SIREN', transliterated_abbreviation = '(numero) SIREN', format = '^d{8}d{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'FR_SIRET', 'ADDRESS', 'Numéro du système d''identification du répertoire des établissements', NULL, '(numéro) SIRET', '(numero) SIRET', '^d{8}d{1}d{5}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Numéro du système d''identification du répertoire des établissements', transliterated_name = NULL, abbreviation = '(numéro) SIRET', transliterated_abbreviation = '(numero) SIRET', format = '^d{8}d{1}d{5}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'FR_EU_VAT_ID', 'LEGAL_ENTITY', 'European Union Value-added Tax Identification Number', NULL, 'EU VAT ID (number)', NULL, '^FR[A-Z0-9]{2}d{8}d{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'European Union Value-added Tax Identification Number', transliterated_name = NULL, abbreviation = 'EU VAT ID (number)', transliterated_abbreviation = NULL, format = '^FR[A-Z0-9]{2}d{8}d{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'GB_CRN', 'LEGAL_ENTITY', 'Company Registration Number', NULL, 'CRN', NULL, '^((AC|CE|CS|FC|FE|GE|GS|IC|LP|NC|NF|NI|NL|NO|NP|OC|OE|PC|R0|RC|SA|SC|SE|SF|SG|SI|SL|SO|SR|SZ|ZC|d{2})d{6})|((IP|SP|RS)[A-Zd]{6})|(SLd{5}[dA])$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Company Registration Number', transliterated_name = NULL, abbreviation = 'CRN', transliterated_abbreviation = NULL, format = '^((AC|CE|CS|FC|FE|GE|GS|IC|LP|NC|NF|NI|NL|NO|NP|OC|OE|PC|R0|RC|SA|SC|SE|SF|SG|SI|SL|SO|SR|SZ|ZC|d{2})d{6})|((IP|SP|RS)[A-Zd]{6})|(SLd{5}[dA])$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'GB_UTR', 'LEGAL_ENTITY', 'Unique Taxpayer Reference', NULL, 'UTR', NULL, '^d{10}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Unique Taxpayer Reference', transliterated_name = NULL, abbreviation = 'UTR', transliterated_abbreviation = NULL, format = '^d{10}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'GB_VAT_REG', 'LEGAL_ENTITY', 'Value-added Tax Registration Number', NULL, 'VAT Reg. (number)', NULL, '^GBd{9}(d{3})?$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Value-added Tax Registration Number', transliterated_name = NULL, abbreviation = 'VAT Reg. (number)', transliterated_abbreviation = NULL, format = '^GBd{9}(d{3})?$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'GR_GEMI', 'LEGAL_ENTITY', 'Αριθμός του γενικού εμπορικού μητρώου', NULL, '(Αρ.) Γ.Ε.ΜΗ.', '(Ar.) G.E.MI.', '^[0-9]{1,12}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Αριθμός του γενικού εμπορικού μητρώου', transliterated_name = NULL, abbreviation = '(Αρ.) Γ.Ε.ΜΗ.', transliterated_abbreviation = '(Ar.) G.E.MI.', format = '^[0-9]{1,12}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'GR_AFM', 'LEGAL_ENTITY', 'Αριθμός φορολογικού μητρώου', NULL, 'ΑΦΜ', 'AFM', '^d{8}d{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Αριθμός φορολογικού μητρώου', transliterated_name = NULL, abbreviation = 'ΑΦΜ', transliterated_abbreviation = 'AFM', format = '^d{8}d{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'GR_EU_VAT_ID', 'LEGAL_ENTITY', 'European Union Value-added Tax Identification Number', NULL, 'EU VAT ID (number)', NULL, '^ELd{8}d{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'European Union Value-added Tax Identification Number', transliterated_name = NULL, abbreviation = 'EU VAT ID (number)', transliterated_abbreviation = NULL, format = '^ELd{8}d{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'HR_MBS', 'LEGAL_ENTITY', 'Matični broj subjekta trgovačkog suda', NULL, 'MBS', NULL, '^[0-1]d{8}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Matični broj subjekta trgovačkog suda', transliterated_name = NULL, abbreviation = 'MBS', transliterated_abbreviation = NULL, format = '^[0-1]d{8}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'HR_OIB', 'LEGAL_ENTITY', 'Osobni identifikacijski broj', NULL, 'OIB', NULL, '^d{9}d{2}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Osobni identifikacijski broj', transliterated_name = NULL, abbreviation = 'OIB', transliterated_abbreviation = NULL, format = '^d{9}d{2}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'HR_EU_VAT_ID', 'LEGAL_ENTITY', 'European Union Value-added Tax Identification Number', NULL, 'EU VAT ID (number)', NULL, '^HRd{9}d{2}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'European Union Value-added Tax Identification Number', transliterated_name = NULL, abbreviation = 'EU VAT ID (number)', transliterated_abbreviation = NULL, format = '^HRd{9}d{2}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'HU_CJS', 'LEGAL_ENTITY', 'Cégjegyzékszám', NULL, 'CJS', NULL, '^d{2}-d{2}-d{6}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Cégjegyzékszám', transliterated_name = NULL, abbreviation = 'CJS', transliterated_abbreviation = NULL, format = '^d{2}-d{2}-d{6}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'HU_AS', 'LEGAL_ENTITY', 'Adószám', NULL, 'AS', NULL, '^d{8}-d{1}-d{2}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Adószám', transliterated_name = NULL, abbreviation = 'AS', transliterated_abbreviation = NULL, format = '^d{8}-d{1}-d{2}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'HU_EU_VAT_ID', 'LEGAL_ENTITY', 'European Union Value-added Tax Identification Number', NULL, 'EU VAT ID (number)', NULL, '^HUd{8}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'European Union Value-added Tax Identification Number', transliterated_name = NULL, abbreviation = 'EU VAT ID (number)', transliterated_abbreviation = NULL, format = '^HUd{8}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'HU_CSPO_AFA_AZ', 'LEGAL_ENTITY', 'Csoportos általános forgalmi adó azonosító szám', NULL, 'Csop. ÁFA az. (sz.)', 'Csop. AFA az. (sz.)', '^HUd{8}d{3}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Csoportos általános forgalmi adó azonosító szám', transliterated_name = NULL, abbreviation = 'Csop. ÁFA az. (sz.)', transliterated_abbreviation = 'Csop. AFA az. (sz.)', format = '^HUd{8}d{3}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'IE_CRO', 'LEGAL_ENTITY', 'Company Registration Office Number', NULL, 'CRO (number)', NULL, '^[1-9]d{1,6}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Company Registration Office Number', transliterated_name = NULL, abbreviation = 'CRO (number)', transliterated_abbreviation = NULL, format = '^[1-9]d{1,6}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'IE_EU_VAT_ID', 'LEGAL_ENTITY', 'European Union Value-added Tax Identification Number', NULL, 'EU VAT ID (number)', NULL, '^IEd{7}[A-Z]{2}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'European Union Value-added Tax Identification Number', transliterated_name = NULL, abbreviation = 'EU VAT ID (number)', transliterated_abbreviation = NULL, format = '^IEd{7}[A-Z]{2}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'IT_REA', 'LEGAL_ENTITY', 'Numero del registro economico amministrativo', NULL, '(numero) REA', NULL, '^[A-Z]{2}-d{7}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Numero del registro economico amministrativo', transliterated_name = NULL, abbreviation = '(numero) REA', transliterated_abbreviation = NULL, format = '^[A-Z]{2}-d{7}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'IT_CF', 'LEGAL_ENTITY', 'Codice fiscale', NULL, 'CF', NULL, '^d{7}d{3}d{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Codice fiscale', transliterated_name = NULL, abbreviation = 'CF', transliterated_abbreviation = NULL, format = '^d{7}d{3}d{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'IT_IVA', 'LEGAL_ENTITY', 'Codice imposta sul valore aggiunto', NULL, '(codice) IVA', NULL, '^d{7}d{3}d{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Codice imposta sul valore aggiunto', transliterated_name = NULL, abbreviation = '(codice) IVA', transliterated_abbreviation = NULL, format = '^d{7}d{3}d{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'IT_EU_VAT_ID', 'LEGAL_ENTITY', 'European Union Value-added Tax Identification Number', NULL, 'EU VAT ID (number)', NULL, '^ITd{7}d{3}d{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'European Union Value-added Tax Identification Number', transliterated_name = NULL, abbreviation = 'EU VAT ID (number)', transliterated_abbreviation = NULL, format = '^ITd{7}d{3}d{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'LT_JAR', 'LEGAL_ENTITY', 'Juridinių asmenų registro kodas', NULL, 'JAR (kodas)', NULL, '^d{1}d{7}d{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Juridinių asmenų registro kodas', transliterated_name = NULL, abbreviation = 'JAR (kodas)', transliterated_abbreviation = NULL, format = '^d{1}d{7}d{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'LT_EU_VAT_ID', 'LEGAL_ENTITY', 'European Union Value-added Tax Identification Number', NULL, 'EU VAT ID (number)', NULL, '^LTd{1}d{7}d{1}(d{3})?$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'European Union Value-added Tax Identification Number', transliterated_name = NULL, abbreviation = 'EU VAT ID (number)', transliterated_abbreviation = NULL, format = '^LTd{1}d{7}d{1}(d{3})?$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'LU_RCS', 'LEGAL_ENTITY', 'Numéro registre de commerce et des sociétés', NULL, '(numéro) RCS', '(numero) RCS', '^[B-Z]d+$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Numéro registre de commerce et des sociétés', transliterated_name = NULL, abbreviation = '(numéro) RCS', transliterated_abbreviation = '(numero) RCS', format = '^[B-Z]d+$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'LU_EU_VAT_ID', 'LEGAL_ENTITY', 'European Union Value-added Tax Identification Number', NULL, 'EU VAT ID (number)', NULL, '^LUd{6}d{2}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'European Union Value-added Tax Identification Number', transliterated_name = NULL, abbreviation = 'EU VAT ID (number)', transliterated_abbreviation = NULL, format = '^LUd{6}d{2}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'LV_URN', 'LEGAL_ENTITY', 'Uzņēmuma reģistrācijas numur', NULL, 'URN', NULL, '^[1-9]{1}d{7}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Uzņēmuma reģistrācijas numur', transliterated_name = NULL, abbreviation = 'URN', transliterated_abbreviation = NULL, format = '^[1-9]{1}d{7}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'LV_EU_VAT_ID', 'LEGAL_ENTITY', 'European Union Value-added Tax Identification Number', NULL, 'EU VAT ID (number)', NULL, '^LV4d{1}[1-9]{1}d{7}d{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'European Union Value-added Tax Identification Number', transliterated_name = NULL, abbreviation = 'EU VAT ID (number)', transliterated_abbreviation = NULL, format = '^LV4d{1}[1-9]{1}d{7}d{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'MT_CRN', 'LEGAL_ENTITY', 'Company Registration Number', NULL, 'CRN', NULL, '^(C|P|SV|SE|LP|SO|NF|RPF|FD|G)d{1,5}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Company Registration Number', transliterated_name = NULL, abbreviation = 'CRN', transliterated_abbreviation = NULL, format = '^(C|P|SV|SE|LP|SO|NF|RPF|FD|G)d{1,5}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'MT_EU_VAT_ID', 'LEGAL_ENTITY', 'European Union Value-added Tax Identification Number', NULL, 'EU VAT ID (number)', NULL, '^MTd{6}d{2}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'European Union Value-added Tax Identification Number', transliterated_name = NULL, abbreviation = 'EU VAT ID (number)', transliterated_abbreviation = NULL, format = '^MTd{6}d{2}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'NL_KVK', 'LEGAL_ENTITY', 'Kamer van Koophandel nummer', NULL, 'KVK (nummer)', NULL, '^d{8}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Kamer van Koophandel nummer', transliterated_name = NULL, abbreviation = 'KVK (nummer)', transliterated_abbreviation = NULL, format = '^d{8}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'NL_RSIN', 'LEGAL_ENTITY', 'Rechtspersonen en Samenwerkingsverbanden Informatie Nummer', NULL, 'RSIN (nummer)', NULL, '^[1-9]{1}d{8}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Rechtspersonen en Samenwerkingsverbanden Informatie Nummer', transliterated_name = NULL, abbreviation = 'RSIN (nummer)', transliterated_abbreviation = NULL, format = '^[1-9]{1}d{8}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'NL_EU_VAT_ID', 'LEGAL_ENTITY', 'European Union Value-added Tax Identification Number', NULL, 'EU VAT ID (number)', NULL, '^NL[1-9]{1}d{8}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'European Union Value-added Tax Identification Number', transliterated_name = NULL, abbreviation = 'EU VAT ID (number)', transliterated_abbreviation = NULL, format = '^NL[1-9]{1}d{8}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'NO_ORG', 'LEGAL_ENTITY', 'Organisasjonsnummer', NULL, 'ORG(-nummer)', NULL, '^d{9}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Organisasjonsnummer', transliterated_name = NULL, abbreviation = 'ORG(-nummer)', transliterated_abbreviation = NULL, format = '^d{9}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'NO_ORG_MVA', 'LEGAL_ENTITY', 'Organisasjonsnummer (Merverdiavgift)', NULL, 'ORG(-nummer) Mva.', NULL, '^d{9}sMVA$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Organisasjonsnummer (Merverdiavgift)', transliterated_name = NULL, abbreviation = 'ORG(-nummer) Mva.', transliterated_abbreviation = NULL, format = '^d{9}sMVA$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'PL_KRS', 'LEGAL_ENTITY', 'Numer w krajowym rejestrze sądowym', NULL, 'KRS (numer)', NULL, '^KRSd{10}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Numer w krajowym rejestrze sądowym', transliterated_name = NULL, abbreviation = 'KRS (numer)', transliterated_abbreviation = NULL, format = '^KRSd{10}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'PL_REGON', 'LEGAL_ENTITY', 'Numer identyfikacyjny rejestru gospodarki narodowe', NULL, '(numer) REGON', NULL, '^(d{9}|d{14})$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Numer identyfikacyjny rejestru gospodarki narodowe', transliterated_name = NULL, abbreviation = '(numer) REGON', transliterated_abbreviation = NULL, format = '^(d{9}|d{14})$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'PL_NIP', 'LEGAL_ENTITY', 'Numer identyfikacji podatkowej', NULL, 'NIP', NULL, '^d{3}d{6}d{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Numer identyfikacji podatkowej', transliterated_name = NULL, abbreviation = 'NIP', transliterated_abbreviation = NULL, format = '^d{3}d{6}d{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'PL_EU_VAT_ID', 'LEGAL_ENTITY', 'European Union Value-added Tax Identification Number', NULL, 'EU VAT ID (number)', NULL, '^PLd{3}d{6}d{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'European Union Value-added Tax Identification Number', transliterated_name = NULL, abbreviation = 'EU VAT ID (number)', transliterated_abbreviation = NULL, format = '^PLd{3}d{6}d{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'PT_NIPC', 'LEGAL_ENTITY', 'Número de Identificação de Pessoa Coletiva (aka Número de Identificação Fiscal)', NULL, 'NIPC', NULL, '^PT5d{7}d{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Número de Identificação de Pessoa Coletiva (aka Número de Identificação Fiscal)', transliterated_name = NULL, abbreviation = 'NIPC', transliterated_abbreviation = NULL, format = '^PT5d{7}d{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'PT_EU_VAT_ID', 'LEGAL_ENTITY', 'European Union Value-added Tax Identification Number', NULL, 'EU VAT ID (number)', NULL, '^PT5d{7}d{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'European Union Value-added Tax Identification Number', transliterated_name = NULL, abbreviation = 'EU VAT ID (number)', transliterated_abbreviation = NULL, format = '^PT5d{7}d{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'RO_NRC', 'LEGAL_ENTITY', 'Numărul de ordine în registrul comerţului', NULL, 'NRC', NULL, '^J[0-5]{1}[0-9]{1}/d{4}/d{1,6}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Numărul de ordine în registrul comerţului', transliterated_name = NULL, abbreviation = 'NRC', transliterated_abbreviation = NULL, format = '^J[0-5]{1}[0-9]{1}/d{4}/d{1,6}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'RO_CUI', 'LEGAL_ENTITY', 'Codul Unic de Înregistrare', NULL, 'CUI', NULL, '^[1-9]d{0,8}d{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Codul Unic de Înregistrare', transliterated_name = NULL, abbreviation = 'CUI', transliterated_abbreviation = NULL, format = '^[1-9]d{0,8}d{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'RO_EU_VAT_ID', 'LEGAL_ENTITY', 'European Union Value-added Tax Identification Number', NULL, 'EU VAT ID (number)', NULL, '^RO[1-9]d{0,8}d{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'European Union Value-added Tax Identification Number', transliterated_name = NULL, abbreviation = 'EU VAT ID (number)', transliterated_abbreviation = NULL, format = '^RO[1-9]d{0,8}d{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'SE_ORG', 'LEGAL_ENTITY', 'Organisationsnummer', NULL, 'ORG(-nummer)', NULL, '^d{2}[2-9]{1}d{3}-?d{4}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Organisationsnummer', transliterated_name = NULL, abbreviation = 'ORG(-nummer)', transliterated_abbreviation = NULL, format = '^d{2}[2-9]{1}d{3}-?d{4}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'SE_EU_VAT_ID', 'LEGAL_ENTITY', 'European Union Value-added Tax Identification Number', NULL, 'EU VAT ID (number)', NULL, '^SEd{2}[2-9]{1}d{3}d{4}(d{2})?$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'European Union Value-added Tax Identification Number', transliterated_name = NULL, abbreviation = 'EU VAT ID (number)', transliterated_abbreviation = NULL, format = '^SEd{2}[2-9]{1}d{3}d{4}(d{2})?$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'SI_MAT', 'LEGAL_ENTITY', 'Matična številka', NULL, 'MAT (št.)', 'MAT (st.)', '^d{10}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Matična številka', transliterated_name = NULL, abbreviation = 'MAT (št.)', transliterated_abbreviation = 'MAT (st.)', format = '^d{10}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'SI_DAV', 'LEGAL_ENTITY', 'Davčna številka', NULL, 'DAV (št.)', 'DAV (st.)', '^d{7}d{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Davčna številka', transliterated_name = NULL, abbreviation = 'DAV (št.)', transliterated_abbreviation = 'DAV (st.)', format = '^d{7}d{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'SI_EU_VAT_ID', 'LEGAL_ENTITY', 'European Union Value-added Tax Identification Number', NULL, 'EU VAT ID (number)', NULL, '^SId{7}d{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'European Union Value-added Tax Identification Number', transliterated_name = NULL, abbreviation = 'EU VAT ID (number)', transliterated_abbreviation = NULL, format = '^SId{7}d{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'SK_ICO', 'LEGAL_ENTITY', 'Identifikačné číslo organizácie', NULL, 'IČO', 'ICO', '^[1-9]{1}d{7}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Identifikačné číslo organizácie', transliterated_name = NULL, abbreviation = 'IČO', transliterated_abbreviation = 'ICO', format = '^[1-9]{1}d{7}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'SK_DIC', 'LEGAL_ENTITY', 'Daňové identifikačné číslo', NULL, 'DIČ', 'DIC', '^[1-9]{1}d{8}d{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Daňové identifikačné číslo', transliterated_name = NULL, abbreviation = 'DIČ', transliterated_abbreviation = 'DIC', format = '^[1-9]{1}d{8}d{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'SK_EU_VAT_ID', 'LEGAL_ENTITY', 'European Union Value-added Tax Identification Number', NULL, 'EU VAT ID (number)', NULL, '^SK[1-9]{1}d{8}d{1}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'European Union Value-added Tax Identification Number', transliterated_name = NULL, abbreviation = 'EU VAT ID (number)', transliterated_abbreviation = NULL, format = '^SK[1-9]{1}d{8}d{1}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'XI_CRN', 'LEGAL_ENTITY', 'Company Registration Number', NULL, 'CRN', NULL, '^((AC|CE|CS|FC|FE|GE|GS|IC|LP|NC|NF|NI|NL|NO|NP|OC|OE|PC|R0|RC|SA|SC|SE|SF|SG|SI|SL|SO|SR|SZ|ZC|d{2})d{6})|((IP|SP|RS)[A-Zd]{6})|(SLd{5}[dA])$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Company Registration Number', transliterated_name = NULL, abbreviation = 'CRN', transliterated_abbreviation = NULL, format = '^((AC|CE|CS|FC|FE|GE|GS|IC|LP|NC|NF|NI|NL|NO|NP|OC|OE|PC|R0|RC|SA|SC|SE|SF|SG|SI|SL|SO|SR|SZ|ZC|d{2})d{6})|((IP|SP|RS)[A-Zd]{6})|(SLd{5}[dA])$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'XI_UTR', 'LEGAL_ENTITY', 'Unique Taxpayer Reference', NULL, 'UTR', NULL, '^d{10}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Unique Taxpayer Reference', transliterated_name = NULL, abbreviation = 'UTR', transliterated_abbreviation = NULL, format = '^d{10}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'XI_VAT_REG', 'LEGAL_ENTITY', 'Value-added Tax Registration Number', NULL, 'VAT Reg. (number)', NULL, '^(XI|GB)d{9}(d{3})?$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Value-added Tax Registration Number', transliterated_name = NULL, abbreviation = 'VAT Reg. (number)', transliterated_abbreviation = NULL, format = '^(XI|GB)d{9}(d{3})?$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'GLEIF_LEI', 'LEGAL_ENTITY', 'Legal Entity Identifier', NULL, 'LEI', NULL, '^[A-Z0-9]{18}[0-9]{2}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Legal Entity Identifier', transliterated_name = NULL, abbreviation = 'LEI', transliterated_abbreviation = NULL, format = '^[A-Z0-9]{18}[0-9]{2}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'EU_EORI', 'LEGAL_ENTITY', 'Economic Operators'' Registration and Identification Number', NULL, 'EORI (number)', NULL, '^[A-Z]{2}[0-9A-Z]+$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Economic Operators'' Registration and Identification Number', transliterated_name = NULL, abbreviation = 'EORI (number)', transliterated_abbreviation = NULL, format = '^[A-Z]{2}[0-9A-Z]+$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'GS1_GLN', 'LEGAL_ENTITY', 'Global Location Number', NULL, 'GLN', NULL, '^d{13}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Global Location Number', transliterated_name = NULL, abbreviation = 'GLN', transliterated_abbreviation = NULL, format = '^d{13}$';

INSERT INTO identifier_types (id, uuid, created_at, updated_at, technical_key, business_partner_type, name, transliterated_name, abbreviation, transliterated_abbreviation, format)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'DNB_DUNS', 'LEGAL_ENTITY', 'Data Universal Numbering System Number', NULL, 'DUNS (number)', NULL, '^d{9}$')
 ON CONFLICT (technical_key, business_partner_type)
DO UPDATE SET name = 'Data Universal Numbering System Number', transliterated_name = NULL, abbreviation = 'DUNS (number)', transliterated_abbreviation = NULL, format = '^d{9}$';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'AT_FBN' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'AT_EU_VAT_ID' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'BE_OND' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'BE_BTW' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'BE_EU_VAT_ID' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'BG_EIK' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'BG_EU_VAT_ID' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'TIN' FROM identifier_types WHERE technical_key = 'CH_UID' AND business_partner_type = 'LEGAL_ENTITY';
INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'CH_UID' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'CH_EHRA_ID' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'CH_UID_MWST' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'CY_AEE' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'TIN' FROM identifier_types WHERE technical_key = 'CY_AFT' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'CY_EU_VAT_ID' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'CZ_ICO' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'TIN' FROM identifier_types WHERE technical_key = 'CZ_DIC' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'CZ_EU_VAT_ID' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'DE_HR' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'DE_EU_VAT_ID' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'DK_CVR' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'DK_EU_VAT_ID' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'EE_RG' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'EE_EU_VAT_ID' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'ES_RM' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'TIN' FROM identifier_types WHERE technical_key = 'ES_NIF' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'ES_EU_VAT_ID' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'FI_Y' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'FI_EU_VAT_ID' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'FR_SIREN' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'FR_SIRET' AND business_partner_type = 'ADDRESS';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'FR_EU_VAT_ID' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'GB_CRN' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'TIN' FROM identifier_types WHERE technical_key = 'GB_UTR' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'GB_VAT_REG' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'GR_GEMI' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'TIN' FROM identifier_types WHERE technical_key = 'GR_AFM' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'GR_EU_VAT_ID' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'HR_MBS' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'TIN' FROM identifier_types WHERE technical_key = 'HR_OIB' AND business_partner_type = 'LEGAL_ENTITY';
INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'HR_OIB' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'HR_EU_VAT_ID' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'HU_CJS' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'TIN' FROM identifier_types WHERE technical_key = 'HU_AS' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'HU_EU_VAT_ID' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'HU_CSPO_AFA_AZ' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'IE_CRO' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'IE_EU_VAT_ID' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'IT_REA' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'TIN' FROM identifier_types WHERE technical_key = 'IT_CF' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'IT_IVA' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'IT_EU_VAT_ID' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'LT_JAR' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'LT_EU_VAT_ID' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'LU_RCS' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'LU_EU_VAT_ID' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'LV_URN' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'LV_EU_VAT_ID' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'MT_CRN' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'MT_EU_VAT_ID' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'NL_KVK' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'TIN' FROM identifier_types WHERE technical_key = 'NL_RSIN' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'NL_EU_VAT_ID' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'TIN' FROM identifier_types WHERE technical_key = 'NO_ORG' AND business_partner_type = 'LEGAL_ENTITY';
INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'NO_ORG' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'NO_ORG_MVA' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'PL_KRS' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'PL_REGON' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'TIN' FROM identifier_types WHERE technical_key = 'PL_NIP' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'PL_EU_VAT_ID' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'TIN' FROM identifier_types WHERE technical_key = 'PT_NIPC' AND business_partner_type = 'LEGAL_ENTITY';
INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'PT_NIPC' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'PT_EU_VAT_ID' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'RO_NRC' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'TIN' FROM identifier_types WHERE technical_key = 'RO_CUI' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'RO_EU_VAT_ID' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'SE_ORG' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'SE_EU_VAT_ID' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'SI_MAT' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'TIN' FROM identifier_types WHERE technical_key = 'SI_DAV' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'SI_EU_VAT_ID' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'SK_ICO' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'TIN' FROM identifier_types WHERE technical_key = 'SK_DIC' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'SK_EU_VAT_ID' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'NBR' FROM identifier_types WHERE technical_key = 'XI_CRN' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'TIN' FROM identifier_types WHERE technical_key = 'XI_UTR' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'VAT' FROM identifier_types WHERE technical_key = 'XI_VAT_REG' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'IBR' FROM identifier_types WHERE technical_key = 'GLEIF_LEI' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'IBR' FROM identifier_types WHERE technical_key = 'EU_EORI' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'OTH' FROM identifier_types WHERE technical_key = 'GS1_GLN' AND business_partner_type = 'LEGAL_ENTITY';

INSERT INTO identifier_type_categories (identifier_type_id, category)
SELECT identifier_types.id, 'OTH' FROM identifier_types WHERE technical_key = 'DNB_DUNS' AND business_partner_type = 'LEGAL_ENTITY';

