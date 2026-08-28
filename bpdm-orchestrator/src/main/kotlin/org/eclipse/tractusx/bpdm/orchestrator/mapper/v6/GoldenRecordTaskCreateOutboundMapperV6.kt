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

import org.springframework.stereotype.Component
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartner as BusinessPartnerV7
import org.eclipse.tractusx.orchestrator.api.model.LegalEntity as LegalEntityV7
import org.eclipse.tractusx.orchestrator.api.model.TaskClientStateDto as TaskClientStateDtoV7
import org.eclipse.tractusx.orchestrator.api.v6.model.BusinessPartner as BusinessPartnerV6
import org.eclipse.tractusx.orchestrator.api.v6.model.LegalEntity as LegalEntityV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskClientStateDto as TaskClientStateDtoV6

/**
 * Translates the V7 golden record task client state into the reduced V6 shape. `processingState` is already the same
 * type shared by both API versions and passes through unchanged; only `businessPartnerResult` needs the reverse of
 * [org.eclipse.tractusx.bpdm.orchestrator.mapper.v6.GoldenRecordTaskCreateInboundMapperV6]'s translation, dropping the
 * V7-only fields V6 has no room for (`additionalSites`, ultimate owner, script variants, golden record relations).
 */
@Component
class GoldenRecordTaskCreateOutboundMapperV6 {

    fun toClientState(clientState: TaskClientStateDtoV7): TaskClientStateDtoV6 =
        with(clientState) {
            TaskClientStateDtoV6(
                taskId = taskId,
                recordId = recordId,
                businessPartnerResult = toBusinessPartnerV6(businessPartnerResult),
                processingState = processingState
            )
        }

    private fun toBusinessPartnerV6(businessPartner: BusinessPartnerV7): BusinessPartnerV6 =
        with(businessPartner) {
            BusinessPartnerV6(
                nameParts = nameParts,
                owningCompany = owningCompany,
                uncategorized = uncategorized,
                legalEntity = toLegalEntityV6(legalEntity),
                site = site,
                additionalAddress = additionalAddress?.toPostalAddress()
            )
        }

    private fun toLegalEntityV6(legalEntity: LegalEntityV7): LegalEntityV6 =
        with(legalEntity) {
            LegalEntityV6(
                bpnReference = bpnReference,
                legalName = legalName,
                legalShortName = legalShortName,
                legalForm = legalForm,
                identifiers = identifiers,
                states = states,
                confidenceCriteria = confidenceCriteria,
                isCatenaXMemberData = isParticipantData,
                hasChanged = hasChanged,
                legalAddress = legalAddress
            )
        }
}
