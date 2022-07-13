package org.eclipse.tractusx.bpdm.pool.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryDto
import org.eclipse.tractusx.bpdm.pool.dto.response.ChangelogEntryResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.entity.PartnerChangelogEntry
import org.eclipse.tractusx.bpdm.pool.repository.BusinessPartnerRepository
import org.eclipse.tractusx.bpdm.pool.repository.PartnerChangelogEntryRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Provides access to changelog entries of business partners. Changelog entries must be created manually via this service, when business partner (including
 * related child entities) are created/updated/deleted.
 *
 * The changelog entries can be used during synchronization of business partner data in order to know which business partners need to be synchronized.
 */
@Service
class PartnerChangelogService(
    val partnerChangelogEntryRepository: PartnerChangelogEntryRepository,
    val businessPartnerRepository: BusinessPartnerRepository
) {
    private val logger = KotlinLogging.logger { }

    @Transactional
    fun createChangelogEntry(changelogEntry: ChangelogEntryDto): PartnerChangelogEntry {
        logger.debug { "Create new change log entry for BPN ${changelogEntry.bpn}" }
        return createChangelogEntries(listOf(changelogEntry)).single()
    }

    @Transactional
    fun createChangelogEntries(changelogEntries: Collection<ChangelogEntryDto>): List<PartnerChangelogEntry> {
        logger.debug { "Create ${changelogEntries.size} new change log entries" }
        val entities = changelogEntries.map { it.toEntity() }
        return partnerChangelogEntryRepository.saveAll(entities)
    }

    fun getChangelogEntriesStartingAfterId(startId: Long = -1, pageIndex: Int, pageSize: Int): Page<PartnerChangelogEntry> {
        return partnerChangelogEntryRepository.findAllByIdGreaterThan(
            startId,
            PageRequest.of(pageIndex, pageSize, Sort.by(PartnerChangelogEntry::id.name).ascending())
        )
    }

    fun getChangelogEntriesByBpn(bpn: String, pageIndex: Int, pageSize: Int): PageResponse<ChangelogEntryResponse> {
        if (!businessPartnerRepository.existsByBpn(bpn)) {
            throw BpdmNotFoundException("Business Partner", bpn)
        }
        val page = partnerChangelogEntryRepository.findAllByBpn(bpn, PageRequest.of(pageIndex, pageSize, Sort.by(PartnerChangelogEntry::id.name).ascending()))
        return page.toDto(page.content.map { it.toDto() })
    }

    private fun ChangelogEntryDto.toEntity(): PartnerChangelogEntry {
        return PartnerChangelogEntry(this.changelogType, this.bpn)
    }
}