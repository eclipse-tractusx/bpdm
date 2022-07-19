package org.eclipse.tractusx.bpdm.pool.entity

import javax.persistence.*

@Entity
@Table(name = "partner_changelog_entries")
class PartnerChangelogEntry(
    @Enumerated(EnumType.STRING)
    @Column(name = "changelog_type", nullable = false, updatable = false)
    val changelogType: ChangelogType,
    @Column(name = "bpn", nullable = false, updatable = false)
    val bpn: String
) : BaseEntity()

enum class ChangelogType() {
    CREATE,
    UPDATE
}