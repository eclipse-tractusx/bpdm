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

import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntitySearchParsed
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Queries legal entities by search criteria.
 */
@Service
class LegalEntitySearchService(
    private val legalEntityRepository: LegalEntityRepository,
    private val legalEntityAssociationFetchService: LegalEntityAssociationFetchService
) {

    /**
     * Returns the requested page of legal entities matching every given criterion, where a criterion left empty matches
     * all.
     */
    @Transactional(readOnly = true)
    fun search(criteria: LegalEntitySearchParsed, pageable: Pageable): Page<LegalEntityDb> {
        val specification = Specification.allOf(
            LegalEntityRepository.byBpns(criteria.legalEntityBpns),
            LegalEntityRepository.byLegalName(criteria.legalName),
            LegalEntityRepository.byIsDataSpaceParticipant(criteria.isDataSpaceParticipant)
        )

        val legalEntityPage = legalEntityRepository.findAll(specification, pageable)
        legalEntityAssociationFetchService.fetch(legalEntityPage.content.toSet())

        return legalEntityPage
    }
}
