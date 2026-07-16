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

package org.eclipse.tractusx.bpdm.pool.mapper.poolv6.inbound

import org.eclipse.tractusx.bpdm.pool.api.model.ConfidenceCriteriaDto
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LegalEntityDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalEntityPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalEntityPartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound.AddressDtoRequestMapper
import org.eclipse.tractusx.bpdm.pool.model.request.ConfidenceCriteriaRequest
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityContentRequest
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityCreateRequest
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityHeaderRequest
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityIdentifier
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityState
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityUpdateRequest
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Maps a **v6** legal-entity request into the shared loose [LegalEntityCreateRequest] / [LegalEntityUpdateRequest]. v6
 * differs from v7: the legal entity is flat (no `header` wrapper), the legal address is a sibling field (not nested),
 * there are no script variants, and membership is `isCatenaXMemberData` (→ domain `isParticipantData`).
 */
@Component("legalEntityDtoRequestMapperLegacy")
class LegalEntityDtoRequestMapper(
    private val addressDtoRequestMapper: AddressDtoRequestMapper
) {

    fun toCreateRequest(request: LegalEntityPartnerCreateRequest): LegalEntityCreateRequest =
        LegalEntityCreateRequest(content = toContentRequest(request.legalEntity, request.legalAddress))

    fun toUpdateRequest(request: LegalEntityPartnerUpdateRequest): LegalEntityUpdateRequest =
        LegalEntityUpdateRequest(legalEntityBpn = request.bpnl, content = toContentRequest(request.legalEntity, request.legalAddress))

    private fun toContentRequest(legalEntity: LegalEntityDto, legalAddress: LogisticAddressDto): LegalEntityContentRequest =
        LegalEntityContentRequest(
            header = toHeaderRequest(legalEntity),
            legalAddress = addressDtoRequestMapper.toContentRequest(legalAddress, emptyList())
        )

    private fun toHeaderRequest(legalEntity: LegalEntityDto): LegalEntityHeaderRequest =
        LegalEntityHeaderRequest(
            legalName = legalEntity.legalName,
            legalShortName = legalEntity.legalShortName,
            legalForm = legalEntity.legalForm,
            identifiers = legalEntity.identifiers.map { LegalEntityIdentifier(it.value, it.type, it.issuingBody) },
            states = legalEntity.states.map { LegalEntityState(it.validFrom?.toUtcInstant(), it.validTo?.toUtcInstant(), it.type) },
            confidenceCriteria = toConfidenceRequest(legalEntity.confidenceCriteria),
            isParticipantData = legalEntity.isCatenaXMemberData,
            scriptVariants = emptyList()
        )

    private fun toConfidenceRequest(confidence: ConfidenceCriteriaDto): ConfidenceCriteriaRequest =
        ConfidenceCriteriaRequest(
            sharedByOwner = confidence.sharedByOwner,
            checkedByExternalDataSource = confidence.checkedByExternalDataSource,
            lastConfidenceCheckAt = confidence.lastConfidenceCheckAt.toUtcInstant(),
            nextConfidenceCheckAt = confidence.nextConfidenceCheckAt.toUtcInstant()
        )

    private fun LocalDateTime.toUtcInstant() = toInstant(ZoneOffset.UTC)
}
