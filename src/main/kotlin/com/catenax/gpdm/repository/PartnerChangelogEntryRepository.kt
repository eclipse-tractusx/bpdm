package com.catenax.gpdm.repository

import com.catenax.gpdm.entity.PartnerChangelogEntry
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface PartnerChangelogEntryRepository : JpaRepository<PartnerChangelogEntry, Long> {
    fun findAllByIdGreaterThan(id: Long, pageable: Pageable): Page<PartnerChangelogEntry>
}