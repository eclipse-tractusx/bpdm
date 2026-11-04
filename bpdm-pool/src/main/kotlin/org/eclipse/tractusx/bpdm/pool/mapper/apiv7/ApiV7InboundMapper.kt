/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.mapper.apiv7

import org.eclipse.tractusx.bpdm.common.dto.BusinessStateDates
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.bpn.BpnRequestIdentifierSearchV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.bpn.IdentifiersSearchV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.ConfidenceCriteriaV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.changelog.ChangelogSearchV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerUpdateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntitySearchV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.IdentifierBusinessPartnerTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.IdentifierTypeV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.LegalFormV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.participant.DataSpaceParticipantSearchV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.participant.DataSpaceParticipantUpdateV7Request
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound.AddressDtoRequestMapper
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityState
import org.eclipse.tractusx.bpdm.pool.model.raw.bpn.BpnIdentifierSearchRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.bpn.BpnRequestIdentifierSearchRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.changelog.ChangelogSearchRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.legalentity.LegalEntityCreateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.legalentity.LegalEntityGetRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.legalentity.LegalEntitySearchRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.legalentity.LegalEntityUpdateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.metadata.FieldQualityRuleSearchRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.metadata.IdentifierTypeCreateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.metadata.IdentifierTypeDetailRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.metadata.IdentifierTypeSearchRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.metadata.LegalFormCreateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.participation.DataSpaceParticipantSearchRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.participation.DataSpaceParticipantUpdateRaw
import org.eclipse.tractusx.bpdm.pool.model.request.ConfidenceCriteriaRequest
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityContentRequest
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityHeaderRequest
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityIdentifier
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityScriptVariant
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Maps API v7 requests to their raw model.
 */
@Component
class ApiV7InboundMapper(
    private val addressDtoRequestMapper: AddressDtoRequestMapper
) {

    /**
     * Returns the raw identifier type search criteria a client sent.
     */
    fun toIdentifierTypeSearchRaw(businessPartnerType: IdentifierBusinessPartnerTypeV7, country: String?): IdentifierTypeSearchRaw =
        IdentifierTypeSearchRaw(businessPartnerType = businessPartnerType, country = country)

    /**
     * Returns the raw field quality rule search for [country].
     */
    fun toFieldQualityRuleSearchRaw(country: String): FieldQualityRuleSearchRaw =
        FieldQualityRuleSearchRaw(country = country)

    /**
     * Returns the raw identifier type create holding [identifierType].
     */
    fun toIdentifierTypeCreateRaw(identifierType: IdentifierTypeV7Dto): IdentifierTypeCreateRaw =
        IdentifierTypeCreateRaw(
            technicalKey = identifierType.technicalKey,
            businessPartnerType = identifierType.businessPartnerType,
            name = identifierType.name,
            abbreviation = identifierType.abbreviation,
            transliteratedName = identifierType.transliteratedName,
            transliteratedAbbreviation = identifierType.transliteratedAbbreviation,
            format = identifierType.format,
            categories = identifierType.categories,
            details = identifierType.details.map { IdentifierTypeDetailRaw(it.country, it.mandatory) }
        )

    /**
     * Returns the raw legal form create holding [legalForm].
     */
    fun toLegalFormCreateRaw(legalForm: LegalFormV7Request): LegalFormCreateRaw =
        LegalFormCreateRaw(
            technicalKey = legalForm.technicalKey,
            name = legalForm.name,
            transliteratedName = legalForm.transliteratedName,
            abbreviations = legalForm.abbreviations,
            transliteratedAbbreviations = legalForm.transliteratedAbbreviations,
            country = legalForm.country,
            language = legalForm.language,
            administrativeAreaLevel1 = legalForm.administrativeAreaLevel1,
            isActive = legalForm.isActive
        )

    /**
     * Returns the raw BPN-by-identifier search holding [request].
     */
    fun toBpnIdentifierSearchRaw(request: IdentifiersSearchV7Request): BpnIdentifierSearchRaw =
        BpnIdentifierSearchRaw(
            businessPartnerType = request.businessPartnerType,
            identifierTypeKey = request.idType,
            identifierValues = request.idValues
        )

    /**
     * Returns the raw BPN-by-request-identifier search holding [request].
     */
    fun toBpnRequestIdentifierSearchRaw(request: BpnRequestIdentifierSearchV7Request): BpnRequestIdentifierSearchRaw =
        BpnRequestIdentifierSearchRaw(requestedIdentifiers = request.requestedIdentifiers)

    /**
     * Returns the raw changelog search holding [request].
     */
    fun toChangelogSearchRaw(request: ChangelogSearchV7Request): ChangelogSearchRaw =
        ChangelogSearchRaw(
            bpns = request.bpns,
            businessPartnerTypes = request.businessPartnerTypes,
            timestampAfter = request.timestampAfter
        )

    /**
     * Returns the raw data space participant search holding [request].
     */
    fun toDataSpaceParticipantSearchRaw(request: DataSpaceParticipantSearchV7Request): DataSpaceParticipantSearchRaw =
        DataSpaceParticipantSearchRaw(legalEntityBpns = request.bpnLs, isDataSpaceParticipant = request.isDataSpaceParticipant)

    /**
     * Returns one raw participation update per participation in [request], in the order they were sent.
     */
    fun toDataSpaceParticipantUpdateRaws(request: DataSpaceParticipantUpdateV7Request): List<DataSpaceParticipantUpdateRaw> =
        request.participants.map {
            DataSpaceParticipantUpdateRaw(legalEntityBpn = it.bpnL, isDataSpaceParticipant = it.isDataSpaceParticipant)
        }

    /**
     * Returns the raw legal entity fetch for the legal entity carrying [identifierValue] of [identifierType].
     */
    fun toLegalEntityGetRaw(identifierValue: String, identifierType: String?): LegalEntityGetRaw =
        LegalEntityGetRaw(identifierValue = identifierValue, identifierType = identifierType)

    /**
     * Returns the raw legal entity search holding [request].
     */
    fun toLegalEntitySearchRaw(request: LegalEntitySearchV7Request): LegalEntitySearchRaw =
        LegalEntitySearchRaw(legalEntityBpns = request.bpnLs, legalName = request.legalName)

    /**
     * Returns the raw create for the legal entity and legal address a client sent.
     */
    fun toLegalEntityCreateRaw(request: LegalEntityPartnerCreateV7Request): LegalEntityCreateRaw =
        LegalEntityCreateRaw(content = toContentRequest(request.legalEntity))

    /**
     * Returns the raw update for the legal entity a client sent, addressed by its BPN.
     */
    fun toLegalEntityUpdateRaw(request: LegalEntityPartnerUpdateV7Request): LegalEntityUpdateRaw =
        LegalEntityUpdateRaw(legalEntityBpn = request.bpnl, content = toContentRequest(request.legalEntity))

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
