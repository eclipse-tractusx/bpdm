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

package org.eclipse.tractusx.bpdm.pool.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.LegalEntityResponseMapper
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.service.operation.LegalEntityAssociationFetchService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for fetching business partner records from the database
 */
@Service
class BusinessPartnerFetchService(
    private val legalEntityRepository: LegalEntityRepository,
    private val legalEntityAssociationFetchService: LegalEntityAssociationFetchService,
    private val legalEntityResponseMapper: LegalEntityResponseMapper
) {

    private val logger = KotlinLogging.logger { }

    /**
     * Fetch business partners by BPN in [bpns]
     */
    @Transactional
    fun fetchByBpns(bpns: Collection<String>): Set<LegalEntityDb> {
        logger.debug { "Executing fetchByBpns() with parameters $bpns " }
        val legalEntities = legalEntityRepository.findDistinctByBpnIn(bpns)
        legalEntityAssociationFetchService.fetch(legalEntities)

        return legalEntities
    }

    /**
     * Fetch business partners by BPN in [bpns] and map to dtos
     */
    @Transactional
    fun fetchDtosByBpns(bpns: Collection<String>): Collection<LegalEntityWithLegalAddressVerboseDto> {
        logger.debug { "Executing fetchDtosByBpns() with parameters $bpns " }
        return fetchByBpns(bpns).map { legalEntityResponseMapper.toLegalEntityWithLegalAddress(it) }
    }
}
