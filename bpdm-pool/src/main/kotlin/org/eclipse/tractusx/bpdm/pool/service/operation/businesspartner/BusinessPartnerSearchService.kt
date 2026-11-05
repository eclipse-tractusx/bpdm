/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.service.operation.businesspartner

import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.model.parsed.businesspartner.BusinessPartnerSearchParsed
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.eclipse.tractusx.bpdm.pool.service.operation.address.AddressAssociationFetchService
import org.eclipse.tractusx.bpdm.pool.service.operation.legalentity.LegalEntityAssociationFetchService
import org.eclipse.tractusx.bpdm.pool.service.operation.site.SiteAssociationFetchService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Queries business partners of every kind through the addresses that represent them.
 */
@Service
class BusinessPartnerSearchService(
    private val logisticAddressRepository: LogisticAddressRepository,
    private val addressAssociationFetchService: AddressAssociationFetchService,
    private val legalEntityAssociationFetchService: LegalEntityAssociationFetchService,
    private val siteAssociationFetchService: SiteAssociationFetchService
) {

    /**
     * Returns the requested page of legal addresses, site main addresses and additional addresses, as far as
     * [criteria] includes them, whose legal entity and address match every given criterion.
     */
    @Transactional(readOnly = true)
    fun search(criteria: BusinessPartnerSearchParsed, pageable: Pageable): Page<LogisticAddressDb> {
        val addressPage = logisticAddressRepository.searchBusinessPartner(
            bpn = criteria.bpn,
            legalName = criteria.legalName,
            streetName = criteria.streetName,
            postalCode = criteria.postalCode,
            city = criteria.city,
            country = criteria.country,
            includesLegalEntities = criteria.includesLegalEntities,
            includesSites = criteria.includesSites,
            includesAdditionalAddresses = criteria.includesAdditionalAddresses,
            pageable = pageable
        )

        val addresses = addressPage.content.toSet()
        if (addresses.isNotEmpty()) {
            addressAssociationFetchService.fetch(addresses)
            logisticAddressRepository.joinCountries(addresses)
            legalEntityAssociationFetchService.fetch(addresses.mapNotNull { it.legalEntity }.toSet())
            siteAssociationFetchService.fetch(addresses.mapNotNull { it.mainSite }.toSet())
        }

        return addressPage
    }
}
