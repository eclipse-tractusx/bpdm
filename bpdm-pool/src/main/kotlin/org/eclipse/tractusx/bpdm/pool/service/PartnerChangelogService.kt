/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.pool.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.ChangelogEntryVerboseDto
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.entity.PartnerChangelogEntry
import org.eclipse.tractusx.bpdm.pool.repository.PartnerChangelogEntryRepository
import org.eclipse.tractusx.bpdm.pool.repository.PartnerChangelogEntryRepository.Specs.byBpnsIn
import org.eclipse.tractusx.bpdm.pool.repository.PartnerChangelogEntryRepository.Specs.byBusinessPartnerTypesIn
import org.eclipse.tractusx.bpdm.pool.repository.PartnerChangelogEntryRepository.Specs.byUpdatedGreaterThan
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Provides access to changelog entries of business partners. Changelog entries must be created manually via this service, when business partner (including
 * related child entities) are created/updated/deleted.
 *
 * The changelog entries can be used during synchronization of business partner data in order to know which business partners need to be synchronized.
 */
@Service
class PartnerChangelogService(
    private val partnerChangelogEntryRepository: PartnerChangelogEntryRepository,
) {
    private val logger = KotlinLogging.logger { }

    @Transactional
    fun createChangelogEntries(changelogEntries: Collection<ChangelogEntryCreateRequest>): List<PartnerChangelogEntry> {
        logger.debug { "Create ${changelogEntries.size} new change log entries" }
        val entities = changelogEntries.map { it.toEntity() }
        return partnerChangelogEntryRepository.saveAll(entities)
    }

    fun getChangelogEntriesCreatedAfter(
        fromTime: Instant,
        businessPartnerTypesFilters: Collection<BusinessPartnerType> = BusinessPartnerType.values().asList(),
        pageIndex: Int,
        pageSize: Int
    ): Page<PartnerChangelogEntry> {
        return partnerChangelogEntryRepository.findByCreatedAtAfterAndBusinessPartnerTypeIn(
            fromTime,
            businessPartnerTypesFilters,
            PageRequest.of(pageIndex, pageSize, Sort.by(PartnerChangelogEntry::createdAt.name).ascending())
        )
    }

    fun getChangeLogEntries(
        bpns: Set<String>?,
        businessPartnerTypes: Set<BusinessPartnerType>?,
        fromTime: Instant?,
        pageIndex: Int,
        pageSize: Int
    ): PageDto<ChangelogEntryVerboseDto> {
        val spec = Specification.allOf(byBpnsIn(bpns), byBusinessPartnerTypesIn(businessPartnerTypes), byUpdatedGreaterThan(fromTime))
        val pageRequest = PageRequest.of(pageIndex, pageSize, Sort.by(PartnerChangelogEntry::updatedAt.name).ascending())
        val page = partnerChangelogEntryRepository.findAll(spec, pageRequest)
        return page.toDto(page.content.map { it.toDto() })
    }

    private fun ChangelogEntryCreateRequest.toEntity(): PartnerChangelogEntry {
        return PartnerChangelogEntry(this.bpn, this.businessPartnerType, this.changelogType)
    }
}