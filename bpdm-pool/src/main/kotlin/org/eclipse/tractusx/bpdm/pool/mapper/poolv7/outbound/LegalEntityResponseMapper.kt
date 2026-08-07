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

package org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound

import org.eclipse.tractusx.bpdm.common.service.toDto
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityHeaderVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityIdentifierVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityScriptVariantDto
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityStateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityIdentifierDb
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityStateDb
import org.springframework.stereotype.Component

/**
 * Maps stored legal entities to the v7 API legal entity DTOs.
 */
@Component
class LegalEntityResponseMapper(
    private val addressResponseMapper: AddressResponseMapper,
    private val confidenceCriteriaResponseMapper: ConfidenceCriteriaResponseMapper,
    private val identifierTypeResponseMapper: IdentifierTypeResponseMapper,
    private val legalFormResponseMapper: LegalFormResponseMapper,
    private val relationResponseMapper: RelationResponseMapper
) {

    /**
     * Returns the script-invariant part of the given legal entity as the API reports it.
     */
    fun toHeader(legalEntity: LegalEntityDb): LegalEntityHeaderVerboseDto =
        with(legalEntity) {
            LegalEntityHeaderVerboseDto(
                bpnl = bpn,
                legalName = legalName.value,
                legalShortName = legalName.shortName,
                legalFormVerbose = legalForm?.let { legalFormResponseMapper.toLegalForm(it) },
                identifiers = identifiers.map { toIdentifier(it) },
                states = states.map { toState(it) },
                relations = startNodeRelations.plus(endNodeRelations).map { relationResponseMapper.toRelation(it) },
                currentness = currentness,
                confidenceCriteria = confidenceCriteriaResponseMapper.toConfidenceCriteria(confidenceCriteria),
                isParticipantData = isDataSpaceParticipant,
                createdAt = createdAt,
                updatedAt = updatedAt,
                ownershipUltimate = ownershipUltimate,
                ultimateOwnerBpnl = ultimateOwnerBpnl
            )
        }

    /**
     * Returns the given legal entity together with its legal address as the API reports them.
     */
    fun toLegalEntityWithLegalAddress(legalEntity: LegalEntityDb): LegalEntityWithLegalAddressVerboseDto =
        LegalEntityWithLegalAddressVerboseDto(
            legalAddress = addressResponseMapper.toInvariantAddress(legalEntity.legalAddress),
            header = toHeader(legalEntity),
            scriptVariants = toScriptVariants(legalEntity)
        )

    /**
     * Returns the given created or updated legal entity as the API reports it, tagged with the key of the request that
     * wrote it.
     */
    fun toUpsertResponse(legalEntity: LegalEntityDb, entryId: String?): LegalEntityPartnerCreateVerboseDto =
        LegalEntityPartnerCreateVerboseDto(
            legalEntity = toLegalEntityWithLegalAddress(legalEntity),
            index = entryId
        )

    private fun toIdentifier(identifier: LegalEntityIdentifierDb): LegalEntityIdentifierVerboseDto =
        LegalEntityIdentifierVerboseDto(
            identifier.value,
            identifierTypeResponseMapper.toTypeKeyName(identifier.type),
            identifier.issuingBody
        )

    private fun toState(state: LegalEntityStateDb): LegalEntityStateVerboseDto =
        LegalEntityStateVerboseDto(state.validFrom, state.validTo, state.type.toDto())

    // The legal address covers every script its legal entity is named in: the parsers reject a variant it does not
    // cover and ScriptVariantCoverageService prunes any the legal address stops covering.
    private fun toScriptVariants(legalEntity: LegalEntityDb): List<LegalEntityScriptVariantDto> {
        val legalAddressVariantsByCode = legalEntity.legalAddress.scriptVariants.associateBy { it.scriptCode.technicalKey }

        return legalEntity.scriptVariants.map { variant ->
            val scriptCode = variant.scriptCode.technicalKey
            val legalAddressVariant = legalAddressVariantsByCode[scriptCode]
                ?: throw IllegalStateException("Legal entity script variant of script code '$scriptCode' is not covered by the legal address.")
            LegalEntityScriptVariantDto(
                scriptCode,
                variant.legalName,
                variant.shortName,
                addressResponseMapper.toPostalAddressScriptVariant(legalAddressVariant)
            )
        }
    }
}
