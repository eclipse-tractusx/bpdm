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

package org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound

import org.eclipse.tractusx.bpdm.pool.api.model.ConfidenceCriteriaDto
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityDto
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressScriptVariantDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityState
import org.eclipse.tractusx.bpdm.pool.model.request.*
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset

@Component
class LegalEntityDtoRequestMapper(
    private val addressDtoRequestMapper: AddressDtoRequestMapper
) {

    fun toCreateRequest(request: LegalEntityPartnerCreateRequest): LegalEntityCreateRequest =
        LegalEntityCreateRequest(content = toContentRequest(request.legalEntity))

    fun toUpdateRequest(request: LegalEntityPartnerUpdateRequest): LegalEntityUpdateRequest =
        LegalEntityUpdateRequest(legalEntityBpn = request.bpnl, content = toContentRequest(request.legalEntity))

    private fun toContentRequest(legalEntity: LegalEntityDto): LegalEntityContentRequest =
        LegalEntityContentRequest(
            header = toHeaderRequest(legalEntity),
            legalAddress = addressDtoRequestMapper.toContentRequest(
                legalEntity.legalAddress,
                legalEntity.scriptVariants.map { LogisticAddressScriptVariantDto(it.scriptCode, it.legalAddress) }
            )
        )

    private fun toHeaderRequest(legalEntity: LegalEntityDto): LegalEntityHeaderRequest {
        val header = legalEntity.header
        return LegalEntityHeaderRequest(
            legalName = header.legalName,
            legalShortName = header.legalShortName,
            legalForm = header.legalForm,
            identifiers = header.identifiers.map { LegalEntityIdentifier(it.value, it.type, it.issuingBody) },
            states = header.states.map { LegalEntityState(it.validFrom?.toUtcInstant(), it.validTo?.toUtcInstant(), it.type) },
            confidenceCriteria = toConfidenceRequest(header.confidenceCriteria),
            isDataSpaceParticipant = header.isParticipantData,
            ownershipUltimate = header.ownershipUltimate,
            scriptVariants = legalEntity.scriptVariants.map { LegalEntityScriptVariant(it.scriptCode, it.legalName, it.shortName) }
        )
    }

    private fun toConfidenceRequest(confidence: ConfidenceCriteriaDto): ConfidenceCriteriaRequest =
        ConfidenceCriteriaRequest(
            sharedByOwner = confidence.sharedByOwner,
            checkedByExternalDataSource = confidence.checkedByExternalDataSource,
            lastConfidenceCheckAt = confidence.lastConfidenceCheckAt.toUtcInstant(),
            nextConfidenceCheckAt = confidence.nextConfidenceCheckAt.toUtcInstant()
        )

    private fun LocalDateTime.toUtcInstant() = toInstant(ZoneOffset.UTC)
}
