/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.repository.LegalEntityDocRepository
import org.eclipse.tractusx.bpdm.pool.entity.ChangelogSubject
import org.eclipse.tractusx.bpdm.pool.entity.PartnerChangelogEntry
import org.eclipse.tractusx.bpdm.pool.repository.AddressPartnerRepository
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.service.PartnerChangelogService
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class OpenSearchSyncPageService(
    val legalEntityRepository: LegalEntityRepository,
    val addressPartnerRepository: AddressPartnerRepository,
    val legalEntityDocRepository: LegalEntityDocRepository,
    val documentMappingService: DocumentMappingService,
    val changelogService: PartnerChangelogService
) {
    private val logger = KotlinLogging.logger { }

    @Transactional
    fun exportPartnersToOpenSearch(fromTime: Instant, pageIndex: Int, pageSize: Int): Page<PartnerChangelogEntry> {
        logger.debug { "Export page $pageIndex" }
        val changelogEntriesPage =
            changelogService.getChangelogEntriesCreatedAfter(fromTime, listOf(ChangelogSubject.LEGAL_ENTITY, ChangelogSubject.ADDRESS), pageIndex, pageSize)
        val changelogEntriesBySubject = changelogEntriesPage.groupBy { it.changelogSubject }

        val legalEntityBpns = changelogEntriesBySubject[ChangelogSubject.LEGAL_ENTITY]?.map { it.bpn }?.toSet()
        if (!legalEntityBpns.isNullOrEmpty()) {
            val legalEntitiesToExport = legalEntityRepository.findDistinctByBpnIn(legalEntityBpns)
            logger.debug { "Exporting ${legalEntitiesToExport.size} legal entity records" }
            val partnerDocs = legalEntitiesToExport.map { documentMappingService.toDocument(it) }.toList()
            legalEntityDocRepository.saveAll(partnerDocs)
        }

        val addressBpns = changelogEntriesBySubject[ChangelogSubject.ADDRESS]?.map { it.bpn }?.toSet()
        if (!addressBpns.isNullOrEmpty()) {
            val addressesToExport = addressPartnerRepository.findDistinctByBpnIn(addressBpns)
            logger.debug { "Exporting ${addressesToExport.size} address records" }
            // TODO: map to address doc and export to OpenSearch
        }

        return changelogEntriesPage
    }
}