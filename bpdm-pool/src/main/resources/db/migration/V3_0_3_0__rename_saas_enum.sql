delete from sync_records
    where type = 'SAAS_IMPORT';

update sync_records
    set type = 'SAAS_IMPORT'
    where type = 'CDQ_IMPORT';
