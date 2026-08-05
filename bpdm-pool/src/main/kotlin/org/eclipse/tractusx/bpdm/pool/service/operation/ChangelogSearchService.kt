/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.service.operation

import org.eclipse.tractusx.bpdm.pool.entity.PartnerChangelogEntryDb
import org.eclipse.tractusx.bpdm.pool.model.parsed.ChangelogSearchParsed
import org.eclipse.tractusx.bpdm.pool.repository.PartnerChangelogEntryRepository
import org.eclipse.tractusx.bpdm.pool.repository.PartnerChangelogEntryRepository.Specs.byBpnsIn
import org.eclipse.tractusx.bpdm.pool.repository.PartnerChangelogEntryRepository.Specs.byBusinessPartnerTypesIn
import org.eclipse.tractusx.bpdm.pool.repository.PartnerChangelogEntryRepository.Specs.byIsMember
import org.eclipse.tractusx.bpdm.pool.repository.PartnerChangelogEntryRepository.Specs.byUpdatedGreaterThan
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Queries changelog entries of business partners by search criteria.
 */
@Service
class ChangelogSearchService(
    private val partnerChangelogEntryRepository: PartnerChangelogEntryRepository
) {

    /**
     * Returns the requested page of changelog entries matching every given criterion, oldest change first, where a
     * criterion left empty matches all.
     */
    @Transactional(readOnly = true)
    fun search(criteria: ChangelogSearchParsed, pageable: Pageable): Page<PartnerChangelogEntryDb> {
        val specification = Specification.allOf(
            byBpnsIn(criteria.bpns),
            byBusinessPartnerTypesIn(criteria.businessPartnerTypes),
            byUpdatedGreaterThan(criteria.timestampAfter),
            byIsMember(criteria.isCatenaXMemberData)
        )

        val chronologically = PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by(PartnerChangelogEntryDb::updatedAt.name))

        return partnerChangelogEntryRepository.findAll(specification, chronologically)
    }
}
