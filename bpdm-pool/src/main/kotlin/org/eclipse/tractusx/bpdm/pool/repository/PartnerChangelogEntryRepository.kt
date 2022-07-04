package org.eclipse.tractusx.bpdm.pool.repository

import org.eclipse.tractusx.bpdm.pool.entity.PartnerChangelogEntry
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface PartnerChangelogEntryRepository : JpaRepository<PartnerChangelogEntry, Long> {
    fun findAllByIdGreaterThan(id: Long, pageable: Pageable): Page<PartnerChangelogEntry>

    fun findAllByBpn(bpn: String, pageable: Pageable): Page<PartnerChangelogEntry>
}