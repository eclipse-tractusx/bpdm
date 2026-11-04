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

import org.eclipse.tractusx.bpdm.common.dto.BusinessStateDates
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityState
import org.eclipse.tractusx.bpdm.pool.model.raw.legalentity.LegalEntityCreateRaw
import org.eclipse.tractusx.bpdm.pool.model.request.*
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.eclipse.tractusx.bpdm.pool.api.v6.model.businesspartner.ConfidenceCriteriaV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityStateV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.LogisticAddressV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerUpdateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerCreateV6Request

/**
 * Maps a **v6** legal-entity request into the shared loose [LegalEntityCreateRaw] / [LegalEntityUpdateRequest]. v6
 * differs from v7: the legal entity is flat (no `header` wrapper), the legal address is a sibling field (not nested),
 * there are no script variants, and membership is `isCatenaXMemberData` (→ domain `isDataSpaceParticipant`).
 */
@Component
class LegalEntityDtoRequestMapperV6(
    private val addressDtoRequestMapperV6: AddressDtoRequestMapperV6
) {

    /**
     * Returns the raw create for the legal entity and legal address a client sent.
     */
    fun toCreateRaw(request: LegalEntityPartnerCreateV6Request): LegalEntityCreateRaw =
        LegalEntityCreateRaw(content = toContentRequest(request.legalEntity, request.legalAddress))

    /**
     * Returns the update request for the legal entity a client sent, addressed by its BPN.
     */
    fun toUpdateRequest(request: LegalEntityPartnerUpdateV6Request): LegalEntityUpdateRequest =
        LegalEntityUpdateRequest(legalEntityBpn = request.bpnl, content = toContentRequest(request.legalEntity, request.legalAddress))

    private fun toContentRequest(legalEntity: LegalEntityV6Dto, legalAddress: LogisticAddressV6Dto): LegalEntityContentRequest =
        LegalEntityContentRequest(
            header = toHeaderRequest(legalEntity),
            legalAddress = addressDtoRequestMapperV6.toContentRequest(legalAddress)
        )

    private fun toHeaderRequest(legalEntity: LegalEntityV6Dto): LegalEntityHeaderRequest =
        LegalEntityHeaderRequest(
            legalName = legalEntity.legalName,
            legalShortName = legalEntity.legalShortName,
            legalForm = legalEntity.legalForm,
            identifiers = legalEntity.identifiers.map { LegalEntityIdentifier(it.value, it.type, it.issuingBody) },
            states = legalEntity.states.map { toStateRequest(it) },
            confidenceCriteria = toConfidenceRequest(legalEntity.confidenceCriteria),
            isDataSpaceParticipant = legalEntity.isCatenaXMemberData,
            // V6 has no ownership flag, so a V6 write must never change it.
            ownershipUltimate = null,
            scriptVariants = emptyList()
        )

    private fun toStateRequest(state: LegalEntityStateV6Dto): LegalEntityState =
        LegalEntityState(state.validFrom.toValidFromInstant(), state.validTo.toValidToInstant(), state.type)

    private fun toConfidenceRequest(confidence: ConfidenceCriteriaV6Dto): ConfidenceCriteriaRequest =
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
