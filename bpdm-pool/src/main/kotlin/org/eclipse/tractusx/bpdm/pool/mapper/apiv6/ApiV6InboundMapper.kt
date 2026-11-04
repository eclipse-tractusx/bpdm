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

package org.eclipse.tractusx.bpdm.pool.mapper.apiv6

import org.eclipse.tractusx.bpdm.common.dto.BusinessStateDates
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.LogisticAddressV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.bpn.BpnRequestIdentifierSearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.bpn.IdentifiersSearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.businesspartner.ConfidenceCriteriaV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.changelog.ChangelogSearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerCreateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerUpdateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntitySearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityStateV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.IdentifierBusinessPartnerTypeV6
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.IdentifierBusinessPartnerTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.IdentifierTypeV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.LegalFormV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.participant.CxMembershipSearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.participant.CxMembershipUpdateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteSearchV6Request
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.inbound.AddressDtoRequestMapperV6
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
import org.eclipse.tractusx.bpdm.pool.model.raw.site.LegalEntitySiteSearchRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.site.SiteGetRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.site.SiteSearchRaw
import org.eclipse.tractusx.bpdm.pool.model.request.ConfidenceCriteriaRequest
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityContentRequest
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityHeaderRequest
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityIdentifier
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Maps API v6 requests to their raw model.
 */
@Component
class ApiV6InboundMapper(
    private val addressDtoRequestMapperV6: AddressDtoRequestMapperV6
) {

    /**
     * Returns the raw identifier type search criteria a client sent.
     */
    fun toIdentifierTypeSearchRaw(businessPartnerType: IdentifierBusinessPartnerTypeV6, country: String?): IdentifierTypeSearchRaw =
        IdentifierTypeSearchRaw(
            businessPartnerType = IdentifierBusinessPartnerTypeV7.valueOf(businessPartnerType.name),
            country = country
        )

    /**
     * Returns the raw field quality rule search for [country].
     */
    fun toFieldQualityRuleSearchRaw(country: String): FieldQualityRuleSearchRaw =
        FieldQualityRuleSearchRaw(country = country)

    /**
     * Returns the raw identifier type create holding [identifierType].
     */
    fun toIdentifierTypeCreateRaw(identifierType: IdentifierTypeV6Dto): IdentifierTypeCreateRaw =
        IdentifierTypeCreateRaw(
            technicalKey = identifierType.technicalKey,
            businessPartnerType = IdentifierBusinessPartnerTypeV7.valueOf(identifierType.businessPartnerType.name),
            name = identifierType.name,
            abbreviation = identifierType.abbreviation,
            transliteratedName = identifierType.transliteratedName,
            transliteratedAbbreviation = identifierType.transliteratedAbbreviation,
            format = null,
            categories = emptySet(),
            details = identifierType.details.map { IdentifierTypeDetailRaw(it.country, it.mandatory) }
        )

    /**
     * Returns the raw legal form create holding [legalForm].
     */
    fun toLegalFormCreateRaw(legalForm: LegalFormV6Request): LegalFormCreateRaw =
        LegalFormCreateRaw(
            technicalKey = legalForm.technicalKey,
            name = legalForm.name,
            transliteratedName = legalForm.transliteratedName,
            abbreviations = legalForm.abbreviation,
            transliteratedAbbreviations = legalForm.transliteratedAbbreviations,
            country = legalForm.country,
            language = legalForm.language,
            administrativeAreaLevel1 = legalForm.administrativeAreaLevel1,
            isActive = legalForm.isActive
        )

    /**
     * Returns the raw BPN-by-identifier search holding [request].
     */
    fun toBpnIdentifierSearchRaw(request: IdentifiersSearchV6Request): BpnIdentifierSearchRaw =
        BpnIdentifierSearchRaw(
            businessPartnerType = IdentifierBusinessPartnerTypeV7.valueOf(request.businessPartnerType.name),
            identifierTypeKey = request.idType,
            identifierValues = request.idValues
        )

    /**
     * Returns the raw BPN-by-request-identifier search holding [request].
     */
    fun toBpnRequestIdentifierSearchRaw(request: BpnRequestIdentifierSearchV6Request): BpnRequestIdentifierSearchRaw =
        BpnRequestIdentifierSearchRaw(requestedIdentifiers = request.requestedIdentifiers)

    /**
     * Returns the raw changelog search holding [request].
     */
    fun toChangelogSearchRaw(request: ChangelogSearchV6Request): ChangelogSearchRaw =
        ChangelogSearchRaw(
            bpns = request.bpns,
            businessPartnerTypes = request.businessPartnerTypes,
            timestampAfter = request.timestampAfter
        )

    /**
     * Returns the raw data space participant search holding [request].
     */
    fun toDataSpaceParticipantSearchRaw(request: CxMembershipSearchV6Request): DataSpaceParticipantSearchRaw =
        DataSpaceParticipantSearchRaw(legalEntityBpns = request.bpnLs, isDataSpaceParticipant = request.isCatenaXMember)

    /**
     * Returns one raw participation update per membership in [request], in the order they were sent.
     */
    fun toDataSpaceParticipantUpdateRaws(request: CxMembershipUpdateV6Request): List<DataSpaceParticipantUpdateRaw> =
        request.memberships.map {
            DataSpaceParticipantUpdateRaw(legalEntityBpn = it.bpnL, isDataSpaceParticipant = it.isCatenaXMember)
        }

    /**
     * Returns the raw legal entity fetch for the legal entity carrying [identifierValue] of [identifierType].
     */
    fun toLegalEntityGetRaw(identifierValue: String, identifierType: String?): LegalEntityGetRaw =
        LegalEntityGetRaw(identifierValue = identifierValue, identifierType = identifierType)

    /**
     * Returns the raw legal entity search holding [request].
     */
    fun toLegalEntitySearchRaw(request: LegalEntitySearchV6Request): LegalEntitySearchRaw =
        LegalEntitySearchRaw(legalEntityBpns = request.bpnLs, legalName = request.legalName)

    /**
     * Returns the raw create for the legal entity and legal address a client sent.
     */
    fun toLegalEntityCreateRaw(request: LegalEntityPartnerCreateV6Request): LegalEntityCreateRaw =
        LegalEntityCreateRaw(content = toContentRequest(request.legalEntity, request.legalAddress))

    /**
     * Returns the raw update for the legal entity and legal address a client sent, addressed by its BPN.
     */
    fun toLegalEntityUpdateRaw(request: LegalEntityPartnerUpdateV6Request): LegalEntityUpdateRaw =
        LegalEntityUpdateRaw(legalEntityBpn = request.bpnl, content = toContentRequest(request.legalEntity, request.legalAddress))

    /**
     * Returns the raw site fetch for the site carrying [bpn].
     */
    fun toSiteGetRaw(bpn: String): SiteGetRaw =
        SiteGetRaw(bpn = bpn)

    /**
     * Returns the raw site search holding [request].
     */
    fun toSiteSearchRaw(request: SiteSearchV6Request): SiteSearchRaw =
        SiteSearchRaw(siteBpns = request.siteBpns, legalEntityBpns = request.legalEntityBpns, name = request.name)

    /**
     * Returns the raw search for the sites of the legal entity carrying [legalEntityBpn].
     */
    fun toLegalEntitySiteSearchRaw(legalEntityBpn: String): LegalEntitySiteSearchRaw =
        LegalEntitySiteSearchRaw(legalEntityBpn = legalEntityBpn)

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
