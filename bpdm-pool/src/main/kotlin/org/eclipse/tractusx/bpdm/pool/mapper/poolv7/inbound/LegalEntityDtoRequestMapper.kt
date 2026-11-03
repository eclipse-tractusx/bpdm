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

import org.eclipse.tractusx.bpdm.common.dto.BusinessStateDates
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.ConfidenceCriteriaV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerUpdateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityV7Dto
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityState
import org.eclipse.tractusx.bpdm.pool.model.request.*
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset

@Component
class LegalEntityDtoRequestMapper(
    private val addressDtoRequestMapper: AddressDtoRequestMapper
) {

    fun toCreateRequest(request: LegalEntityPartnerCreateV7Request): LegalEntityCreateRequest =
        LegalEntityCreateRequest(content = toContentRequest(request.legalEntity))

    fun toUpdateRequest(request: LegalEntityPartnerUpdateV7Request): LegalEntityUpdateRequest =
        LegalEntityUpdateRequest(legalEntityBpn = request.bpnl, content = toContentRequest(request.legalEntity))

    private fun toContentRequest(legalEntity: LegalEntityV7Dto): LegalEntityContentRequest =
        LegalEntityContentRequest(
            header = toHeaderRequest(legalEntity),
            legalAddress = addressDtoRequestMapper.toContentRequest(
                legalEntity.legalAddress,
                legalEntity.scriptVariants.map { LogisticAddressScriptVariantV7Dto(it.scriptCode, it.legalAddress) }
            )
        )

    private fun toHeaderRequest(legalEntity: LegalEntityV7Dto): LegalEntityHeaderRequest {
        val header = legalEntity.header
        return LegalEntityHeaderRequest(
            legalName = header.legalName,
            legalShortName = header.legalShortName,
            legalForm = header.legalForm,
            identifiers = header.identifiers.map { LegalEntityIdentifier(it.value, it.type, it.issuingBody) },
            states = header.states.map { LegalEntityState(it.validFrom.toValidFromInstant(), it.validTo.toValidToInstant(), it.type) },
            confidenceCriteria = toConfidenceRequest(header.confidenceCriteria),
            isDataSpaceParticipant = header.isParticipantData,
            ownershipUltimate = header.ownershipUltimate,
            scriptVariants = legalEntity.scriptVariants.map { LegalEntityScriptVariant(it.scriptCode, it.legalName, it.shortName) }
        )
    }

    private fun toConfidenceRequest(confidence: ConfidenceCriteriaV7Dto): ConfidenceCriteriaRequest =
        ConfidenceCriteriaRequest(
            sharedByOwner = confidence.sharedByOwner,
            checkedByExternalDataSource = confidence.checkedByExternalDataSource,
            lastConfidenceCheckAt = confidence.lastConfidenceCheckAt.toUtcInstant(),
            nextConfidenceCheckAt = confidence.nextConfidenceCheckAt.toUtcInstant()
        )

    private fun LocalDateTime.toUtcInstant() = toInstant(ZoneOffset.UTC)

    private fun LocalDateTime?.toValidFromInstant() = BusinessStateDates.receiveValidFrom(this)?.toUtcInstant()

    private fun LocalDateTime?.toValidToInstant() = BusinessStateDates.receiveValidTo(this)?.toUtcInstant()
}
