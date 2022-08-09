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
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.BusinessPartnerDoc
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.repository.BusinessPartnerDocRepository
import org.eclipse.tractusx.bpdm.pool.repository.BusinessPartnerRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class OpenSearchSyncPageService(
    val businessPartnerRepository: BusinessPartnerRepository,
    val businessPartnerDocRepository: BusinessPartnerDocRepository,
    val documentMappingService: DocumentMappingService
) {
    private val logger = KotlinLogging.logger { }

    @Transactional
    fun exportPartnersToOpenSearch(fromTime: Instant, pageRequest: PageRequest): Page<BusinessPartnerDoc> {
        logger.debug { "Export page ${pageRequest.pageNumber}" }
        val partnersToExport = businessPartnerRepository.findByUpdatedAtAfter(fromTime, pageRequest)
        logger.debug { "Exporting ${partnersToExport.size} records" }
        val partnerDocs = partnersToExport.map { documentMappingService.toDocument(it) }.toList()
        businessPartnerDocRepository.saveAll(partnerDocs)
        return PageImpl(partnerDocs, partnersToExport.pageable, partnersToExport.totalElements)
    }
}