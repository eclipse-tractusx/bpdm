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

package org.eclipse.tractusx.bpdm.orchestrator.mapper.v6

import org.eclipse.tractusx.bpdm.orchestrator.mapper.BusinessPartnerRequestMapper
import org.eclipse.tractusx.bpdm.orchestrator.model.request.BusinessPartnerRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.LegalEntityRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.PostalAddressWithScriptVariantsRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.GoldenRecordTaskCreateRequest
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskCreateRequestEntry
import org.springframework.stereotype.Component
import org.eclipse.tractusx.orchestrator.api.v6.model.LegalEntity as LegalEntityV6
import org.eclipse.tractusx.orchestrator.api.v6.model.BusinessPartner as BusinessPartnerV6

/**
 * Translates the V6 create-task request entry into the unified [GoldenRecordTaskCreateRequest]. V6's `BusinessPartner`
 * and `LegalEntity` are near-identical, slightly reduced variants of the V7 ones (no `additionalSites`, no ultimate
 * owner or script variant/golden record relation information, and `isCatenaXMemberData` instead of
 * `isParticipantData`); every other nested type (site, addresses, identifiers, ...) is already the very same V7 type
 * reused by V6, so this is a pure, narrow translation with the missing V7-only fields defaulted.
 */
@Component
class GoldenRecordTaskCreateInboundMapperV6(
    private val businessPartnerRequestMapper: BusinessPartnerRequestMapper
) {

    fun toRequest(entry: TaskCreateRequestEntry): GoldenRecordTaskCreateRequest =
        GoldenRecordTaskCreateRequest(
            recordId = entry.recordId,
            businessPartner = toBusinessPartnerRequest(entry.businessPartner)
        )

    private fun toBusinessPartnerRequest(businessPartner: BusinessPartnerV6): BusinessPartnerRequest =
        with(businessPartner) {
            BusinessPartnerRequest(
                nameParts = nameParts.map(businessPartnerRequestMapper::toNamePartRequest),
                owningCompany = owningCompany,
                uncategorized = businessPartnerRequestMapper.toUncategorizedPropertiesRequest(uncategorized),
                legalEntity = toLegalEntityRequest(legalEntity),
                site = site?.let(businessPartnerRequestMapper::toSiteRequest),
                additionalAddress = additionalAddress?.let {
                    PostalAddressWithScriptVariantsRequest(
                        postalProperties = businessPartnerRequestMapper.toPostalAddressRequest(it),
                        scriptVariants = emptyList()
                    )
                },
                additionalSites = emptyList()
            )
        }

    private fun toLegalEntityRequest(legalEntity: LegalEntityV6): LegalEntityRequest =
        with(legalEntity) {
            LegalEntityRequest(
                bpnReference = businessPartnerRequestMapper.toBpnReferenceRequest(bpnReference),
                legalName = legalName,
                legalShortName = legalShortName,
                legalForm = legalForm,
                identifiers = identifiers.map(businessPartnerRequestMapper::toIdentifierRequest),
                states = states.map(businessPartnerRequestMapper::toBusinessStateRequest),
                confidenceCriteria = businessPartnerRequestMapper.toConfidenceCriteriaRequest(confidenceCriteria),
                isParticipantData = isCatenaXMemberData,
                hasChanged = hasChanged,
                ownershipUltimate = null,
                ultimateOwnerBpnl = null,
                legalAddress = businessPartnerRequestMapper.toPostalAddressRequest(legalAddress),
                scriptVariants = emptyList(),
                goldenRecordRelations = emptyList(),
                updatedAt = null
            )
        }
}
