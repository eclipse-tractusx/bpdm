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

package org.eclipse.tractusx.bpdm.pool.service.parser

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.error.AddressCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.error.InvalidParentBpn
import org.eclipse.tractusx.bpdm.pool.model.error.UnresolvableLegalEntity
import org.eclipse.tractusx.bpdm.pool.model.error.UnresolvableSite
import org.eclipse.tractusx.bpdm.pool.model.request.AddressCreateTypedParentsRequest
import org.eclipse.tractusx.bpdm.pool.model.request.AddressCreateUntypedParentRequest
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.eclipse.tractusx.bpdm.pool.service.BpnIssuingService
import org.springframework.stereotype.Service

/**
 * Resolves an address-create request's single untyped parent BPN into the explicit legal-entity and site parents the
 * typed create stage needs, telling a BPN that resolves to nothing apart from one of a kind that cannot parent an
 * address.
 */
@Service
class AddressParentResolutionParser(
    private val legalEntityRepository: LegalEntityRepository,
    private val siteRepository: SiteRepository,
    private val bpnIssuingService: BpnIssuingService
) {

    /**
     * Resolves each request's parent BPN — a site parent contributing its own legal entity — and fails the entry when the
     * BPN names neither a known legal entity nor a known site.
     */
    fun parse(
        requests: List<AddressCreateUntypedParentRequest>
    ): List<ParseResult<AddressCreateTypedParentsRequest, AddressCreateParseError>> {
        val typeByBpn = requests.map { it.bpnParent }.associateWith { bpnIssuingService.translateToBusinessPartnerType(it) }
        val legalEntityParentBpns = typeByBpn.filterValues { it == BusinessPartnerType.LEGAL_ENTITY }.keys
        val siteParentBpns = typeByBpn.filterValues { it == BusinessPartnerType.SITE }.keys
        val existingLegalEntityBpns = legalEntityRepository.findDistinctByBpnIn(legalEntityParentBpns).map { it.bpn }.toSet()
        val sitesByBpn = siteRepository.findDistinctByBpnIn(siteParentBpns).associateBy { it.bpn }

        return requests.map { request ->
            val parent = request.bpnParent
            when (typeByBpn[parent]) {
                BusinessPartnerType.LEGAL_ENTITY ->
                    if (parent in existingLegalEntityBpns)
                        ParseResult.Success(AddressCreateTypedParentsRequest(legalEntityBpn = parent, siteBpn = null, content = request.content))
                    else ParseResult.Companion.ofSingleFailure(UnresolvableLegalEntity(parent))
                BusinessPartnerType.SITE ->
                    sitesByBpn[parent]?.let {
                        ParseResult.Success(AddressCreateTypedParentsRequest(legalEntityBpn = it.legalEntity.bpn, siteBpn = parent, content = request.content))
                    } ?: ParseResult.Companion.ofSingleFailure(UnresolvableSite(parent))
                else -> ParseResult.Companion.ofSingleFailure(InvalidParentBpn(parent))
            }
        }
    }
}