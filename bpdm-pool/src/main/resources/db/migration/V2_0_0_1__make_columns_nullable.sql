alter table bank_accounts
    alter column international_account_identifier drop not null;
alter table bank_accounts
    alter column international_bank_identifier drop not null;
alter table bank_accounts
    alter column national_account_identifier drop not null;
alter table bank_accounts
    alter column national_bank_identifier drop not null;

alter table business_stati
    alter column denotation drop not null;
alter table business_stati
    alter column valid_from drop not null;

alter table classifications
    alter column value drop not null;

alter table legal_forms
    alter column name drop not null;
