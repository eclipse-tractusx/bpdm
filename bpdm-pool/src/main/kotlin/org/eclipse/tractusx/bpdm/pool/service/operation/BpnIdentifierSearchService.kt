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

import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.model.BpnIdentifierMatch
import org.eclipse.tractusx.bpdm.pool.model.parsed.BpnIdentifierSearchParsed
import org.eclipse.tractusx.bpdm.pool.repository.AddressIdentifierRepository
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityIdentifierRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Queries the BPNs of business partners carrying given identifiers.
 */
@Service
class BpnIdentifierSearchService(
    private val legalEntityIdentifierRepository: LegalEntityIdentifierRepository,
    private val addressIdentifierRepository: AddressIdentifierRepository
) {

    /**
     * Returns a match for every requested identifier value that is held by a business partner of the identifier type's
     * business partner type, omitting the values no business partner carries.
     */
    @Transactional(readOnly = true)
    fun search(criteria: BpnIdentifierSearchParsed): Set<BpnIdentifierMatch> =
        when (criteria.identifierType.businessPartnerType) {
            IdentifierBusinessPartnerType.LEGAL_ENTITY ->
                legalEntityIdentifierRepository.findBpnsByIdentifierTypeAndValues(criteria.identifierType, criteria.identifierValues)

            IdentifierBusinessPartnerType.ADDRESS ->
                addressIdentifierRepository.findBpnsByIdentifierTypeAndValues(criteria.identifierType, criteria.identifierValues)
        }
}
