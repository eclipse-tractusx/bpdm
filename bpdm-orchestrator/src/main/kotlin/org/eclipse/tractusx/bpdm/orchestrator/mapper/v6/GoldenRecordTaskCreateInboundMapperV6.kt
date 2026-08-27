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

import org.eclipse.tractusx.bpdm.orchestrator.model.BusinessPartnerRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.GoldenRecordTaskCreateRequest
import org.eclipse.tractusx.orchestrator.api.model.PostalAddressWithScriptVariants
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskCreateRequestEntry
import org.springframework.stereotype.Component
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartner as BusinessPartnerV7
import org.eclipse.tractusx.orchestrator.api.model.LegalEntity as LegalEntityV7
import org.eclipse.tractusx.orchestrator.api.v6.model.BusinessPartner as BusinessPartnerV6
import org.eclipse.tractusx.orchestrator.api.v6.model.LegalEntity as LegalEntityV6

@Component
class GoldenRecordTaskCreateInboundMapperV6 {

    fun toRequest(entry: TaskCreateRequestEntry): GoldenRecordTaskCreateRequest =
        GoldenRecordTaskCreateRequest(
            recordId = entry.recordId,
            businessPartner = toBusinessPartnerRequest(entry.businessPartner)
        )

    private fun toBusinessPartnerRequest(businessPartner: BusinessPartnerV6): BusinessPartnerRequest =
        with(businessPartner) {
            val v7LegalEntity = toLegalEntityV7(legalEntity)
            BusinessPartnerRequest(
                nameParts = nameParts,
                owningCompany = owningCompany,
                uncategorized = uncategorized,
                legalEntity = v7LegalEntity,
                site = site,
                additionalAddress = additionalAddress?.let { PostalAddressWithScriptVariants(it, emptyList()) },
                additionalSites = emptyList()
            )
        }

    private fun toLegalEntityV7(legalEntity: LegalEntityV6): LegalEntityV7 =
        with(legalEntity) {
            LegalEntityV7(
                bpnReference = bpnReference,
                legalName = legalName,
                legalShortName = legalShortName,
                legalForm = legalForm,
                identifiers = identifiers,
                states = states,
                confidenceCriteria = confidenceCriteria,
                isParticipantData = isCatenaXMemberData,
                hasChanged = hasChanged,
                ownershipUltimate = null,
                ultimateOwnerBpnl = null,
                legalAddress = legalAddress,
                scriptVariants = emptyList(),
                goldenRecordRelations = emptyList(),
                updatedAt = null
            )
        }
}
