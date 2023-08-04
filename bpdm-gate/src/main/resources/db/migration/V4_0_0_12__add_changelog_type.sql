alter table changelog_entries
    add changelog_type varchar(255) default 'UPDATE';
