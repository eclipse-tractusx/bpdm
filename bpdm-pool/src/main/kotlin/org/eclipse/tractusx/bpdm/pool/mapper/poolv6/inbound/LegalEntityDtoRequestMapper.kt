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

import org.eclipse.tractusx.bpdm.pool.api.v6.model.ConfidenceCriteriaDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LegalEntityDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LogisticAddressDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalEntityPartnerCreateRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalEntityPartnerUpdateRequestV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.toV7
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound.AddressDtoRequestMapper
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityState
import org.eclipse.tractusx.bpdm.pool.model.request.*
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

    fun toCreateRequest(request: LegalEntityPartnerCreateRequestV6): LegalEntityCreateRequest =
        LegalEntityCreateRequest(content = toContentRequest(request.legalEntity, request.legalAddress))

    fun toUpdateRequest(request: LegalEntityPartnerUpdateRequestV6): LegalEntityUpdateRequest =
        LegalEntityUpdateRequest(legalEntityBpn = request.bpnl, content = toContentRequest(request.legalEntity, request.legalAddress))

    private fun toContentRequest(legalEntity: LegalEntityDtoV6, legalAddress: LogisticAddressDtoV6): LegalEntityContentRequest =
        LegalEntityContentRequest(
            header = toHeaderRequest(legalEntity),
            legalAddress = addressDtoRequestMapper.toContentRequest(legalAddress.toV7(), emptyList())
        )

    private fun toHeaderRequest(legalEntity: LegalEntityDtoV6): LegalEntityHeaderRequest =
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

    private fun toConfidenceRequest(confidence: ConfidenceCriteriaDtoV6): ConfidenceCriteriaRequest =
        ConfidenceCriteriaRequest(
            sharedByOwner = confidence.sharedByOwner,
            checkedByExternalDataSource = confidence.checkedByExternalDataSource,
            lastConfidenceCheckAt = confidence.lastConfidenceCheckAt.toUtcInstant(),
            nextConfidenceCheckAt = confidence.nextConfidenceCheckAt.toUtcInstant()
        )

    private fun LocalDateTime.toUtcInstant() = toInstant(ZoneOffset.UTC)
}
