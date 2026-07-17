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

package org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.inbound

import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityState
import org.eclipse.tractusx.bpdm.pool.model.request.*
import org.springframework.stereotype.Component
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityScriptVariant as LegalEntityScriptVariantRequest
import org.eclipse.tractusx.orchestrator.api.model.BusinessState as TaskBusinessState
import org.eclipse.tractusx.orchestrator.api.model.LegalEntity as TaskLegalEntity
import org.eclipse.tractusx.orchestrator.api.model.PostalAddressScriptVariantWithScriptCode as TaskScriptVariant

/**
 * Maps a cleaning task's legal entity into the loose [LegalEntityCreateRequest] / [LegalEntityUpdateRequest]; the legal
 * address is delegated to [GoldenRecordTaskAddressRequestMapper]. Unlike the pass-through elsewhere, a missing
 * [LegalEntityState] type throws here — the loose request's type is non-null by contract (matches the task path's prior
 * behavior).
 */
@Component
class GoldenRecordTaskLegalEntityRequestMapper(
    private val addressRequestMapper: GoldenRecordTaskAddressRequestMapper
) {

    fun toCreateRequest(legalEntity: TaskLegalEntity): LegalEntityCreateRequest =
        LegalEntityCreateRequest(content = toContentRequest(legalEntity))

    fun toUpdateRequest(legalEntityBpn: String, legalEntity: TaskLegalEntity): LegalEntityUpdateRequest =
        LegalEntityUpdateRequest(legalEntityBpn = legalEntityBpn, content = toContentRequest(legalEntity))

    private fun toContentRequest(legalEntity: TaskLegalEntity): LegalEntityContentRequest =
        LegalEntityContentRequest(
            header = toHeaderRequest(legalEntity),
            legalAddress = addressRequestMapper.toContentRequest(
                legalEntity.legalAddress,
                legalEntity.scriptVariants.map { TaskScriptVariant(it.scriptCode, it.legalAddress) }
            )
        )

    private fun toHeaderRequest(legalEntity: TaskLegalEntity): LegalEntityHeaderRequest =
        LegalEntityHeaderRequest(
            legalName = legalEntity.legalName,
            legalShortName = legalEntity.legalShortName,
            legalForm = legalEntity.legalForm,
            identifiers = legalEntity.identifiers.map { LegalEntityIdentifier(it.value, it.type, it.issuingBody) },
            states = legalEntity.states.map { toState(it) },
            confidenceCriteria = addressRequestMapper.toConfidenceRequest(legalEntity.confidenceCriteria),
            isParticipantData = legalEntity.isParticipantData ?: false,
            scriptVariants = legalEntity.scriptVariants.map { LegalEntityScriptVariantRequest(it.scriptCode, it.legalName, it.legalShortName) }
        )

    private fun toState(state: TaskBusinessState): LegalEntityState =
        LegalEntityState(
            validFrom = state.validFrom,
            validTo = state.validTo,
            type = state.type ?: throw BpdmValidationException("Business Partner state type is null")
        )
}
