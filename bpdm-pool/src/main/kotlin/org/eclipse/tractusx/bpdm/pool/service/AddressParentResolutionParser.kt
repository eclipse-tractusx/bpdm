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

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateTypedParentsRequest
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateUntypedParentRequest
import org.eclipse.tractusx.bpdm.pool.model.InvalidParentBpn
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.UnresolvableLegalEntity
import org.eclipse.tractusx.bpdm.pool.model.UnresolvableSite
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.springframework.stereotype.Service

/**
 * Resolves each request's single, *untyped* `bpnParent` into the explicit (legalEntity, site) parents the typed create
 * stage needs, validating existence in the same pass: it determines whether the BPN is a legal entity or a site —
 * reporting the precise [InvalidParentBpn]/[UnresolvableLegalEntity]/[UnresolvableSite] errors the typed stage cannot
 * distinguish — where a legal-entity parent resolves to itself and a site parent contributes its own legal entity.
 * Order-preserving positional contract (see [ParseResult]): result[i] corresponds to requests[i].
 */
@Service
class AddressParentResolutionParser(
    private val legalEntityRepository: LegalEntityRepository,
    private val siteRepository: SiteRepository,
    private val bpnIssuingService: BpnIssuingService
) {

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
                    else ParseResult.ofSingleFailure(UnresolvableLegalEntity(parent))
                BusinessPartnerType.SITE ->
                    sitesByBpn[parent]?.let {
                        ParseResult.Success(AddressCreateTypedParentsRequest(legalEntityBpn = it.legalEntity.bpn, siteBpn = parent, content = request.content))
                    } ?: ParseResult.ofSingleFailure(UnresolvableSite(parent))
                else -> ParseResult.ofSingleFailure(InvalidParentBpn(parent))
            }
        }
    }
}
