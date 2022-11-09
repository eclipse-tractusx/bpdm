alter table partner_changelog_entries
    add column changelog_subject varchar(255);

update partner_changelog_entries
set changelog_subject = (case
                             when starts_with(bpn, 'BPNL') then 'LEGAL_ENTITY'
                             when starts_with(bpn, 'BPNA') then 'ADDRESS'
                             when starts_with(bpn, 'BPNS') then 'SITE'
    end);

alter table partner_changelog_entries
    alter column changelog_subject set not null;
