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

import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressSearchParsed
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Queries addresses by search criteria.
 */
@Service
class AddressSearchService(
    private val logisticAddressRepository: LogisticAddressRepository,
    private val addressAssociationFetchService: AddressAssociationFetchService
) {

    /**
     * Returns the requested page of addresses matching every given criterion, where a criterion left empty matches all.
     */
    @Transactional(readOnly = true)
    fun search(criteria: AddressSearchParsed, pageable: Pageable): Page<LogisticAddressDb> {
        val specification = Specification.allOf(
            LogisticAddressRepository.byBpns(criteria.addressBpns),
            LogisticAddressRepository.bySiteBpns(criteria.siteBpns),
            LogisticAddressRepository.byLegalEntityBpns(criteria.legalEntityBpns),
            LogisticAddressRepository.byName(criteria.name),
            LogisticAddressRepository.byIsMember(criteria.isCatenaXMemberData),
            LogisticAddressRepository.withoutSites(criteria.excludesSiteAddresses)
        )

        val addressPage = logisticAddressRepository.findAll(specification, pageable)
        addressAssociationFetchService.fetch(addressPage.content.toSet())

        return addressPage
    }
}
