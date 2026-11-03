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

package org.eclipse.tractusx.bpdm.test.testdata.pool.v7

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessStateDates
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.TypeKeyNameVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressIdentifierV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressIdentifierVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerCreateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerUpdateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerUpdateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressStateV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressStateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AlternativePostalAddressV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AlternativePostalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressInvariantVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.PhysicalPostalAddressV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.PhysicalPostalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.ConfidenceCriteriaV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityHeaderV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityHeaderVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityIdentifierV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityIdentifierVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerCreateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerUpdateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityStateV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityStateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityWithLegalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.LegalFormV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.RegionV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteCreateWithLegalAddressAsMainV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SitePartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SitePartnerCreateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SitePartnerUpdateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteStateV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteStateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteWithMainAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.test.testdata.pool.TestMetadataV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.toCountryVerbose
import java.time.Instant

class PoolResponseFactoryV7(
    private val testMetadata: TestMetadataV7
) {

    private val anyStringValue = "ANY"
    private val anyDate = Instant.now()

    fun <T> buildSinglePageResponse(content: Collection<T>): PageDto<T> {
        return PageDto(content.size.toLong(), 1, 0, content.size, content)
    }

    fun buildLegalEntityCreate(withValuesFrom: LegalEntityPartnerCreateV7Request): LegalEntityPartnerCreateVerboseV7Dto{
        return LegalEntityPartnerCreateVerboseV7Dto(
            buildLegalEntityWithLegalAddress(withValuesFrom.legalEntity),
            withValuesFrom.index
        )
    }

    fun buildLegalEntityUpdate(withValuesFrom: LegalEntityPartnerUpdateV7Request, bpnAFrom: LegalEntityWithLegalAddressVerboseV7Dto): LegalEntityPartnerCreateVerboseV7Dto{
        val expected = buildLegalEntityUpdate(withValuesFrom, bpnAFrom.legalAddress.bpna)
        return expected.copy(legalEntity = expected.legalEntity.copy(
            header = expected.legalEntity.header.copy(createdAt = bpnAFrom.header.createdAt, updatedAt = bpnAFrom.header.updatedAt),
            legalAddress = expected.legalEntity.legalAddress.copy(createdAt = bpnAFrom.legalAddress.createdAt, updatedAt = bpnAFrom.legalAddress.updatedAt)
        ))
    }

    fun buildLegalEntityUpdate(withValuesFrom: LegalEntityPartnerUpdateV7Request, bpnA: String = anyStringValue): LegalEntityPartnerCreateVerboseV7Dto{
        return LegalEntityPartnerCreateVerboseV7Dto(
            buildLegalEntityWithLegalAddress(withValuesFrom.legalEntity, withValuesFrom.bpnl, bpnA),
            index = withValuesFrom.bpnl
        )
    }

    fun buildLegalEntityWithLegalAddress(withValuesFrom: LegalEntityV7Dto, bpnL: String = anyStringValue, bpnA: String = anyStringValue): LegalEntityWithLegalAddressVerboseV7Dto{
        return LegalEntityWithLegalAddressVerboseV7Dto(
            header = buildLegalEntityHeader(withValuesFrom.header, bpnL),
            legalAddress = buildInvariantAddress(
                withValuesFrom = withValuesFrom.legalAddress,
                addressType = AddressType.LegalAddress,
                isParticipantData = withValuesFrom.header.isParticipantData,
                givenBpnA = bpnA,
                bpnLegalEntity = bpnL
            ),
            scriptVariants = withValuesFrom.scriptVariants
        )
    }

    fun buildLegalEntityHeader(
        withValuesFrom: LegalEntityHeaderV7Dto,
        bpnL: String = anyStringValue,
        currentness: Instant = anyDate,
        createdAt: Instant = anyDate,
        updatedAt: Instant = anyDate,
        ownershipUltimate: Boolean? = false,
        ultimateOwnerBpnl: String? = null
    ): LegalEntityHeaderVerboseV7Dto{
        return with(withValuesFrom){
            LegalEntityHeaderVerboseV7Dto(
                bpnl = bpnL,
                legalName = legalName,
                legalShortName = legalShortName,
                legalFormVerbose = buildLegalForm(legalForm),
                identifiers = identifiers.map { buildLegalIdentifier(it) },
                states = states.map { buildLegalBusinessState(it) },
                relations = emptyList(),
                currentness = currentness,
                confidenceCriteria = confidenceCriteria,
                isParticipantData = isParticipantData,
                createdAt = createdAt,
                updatedAt = updatedAt,
                ownershipUltimate = ownershipUltimate,
                ultimateOwnerBpnl = ultimateOwnerBpnl
            )
        }
    }

    fun buildLegalForm(technicalKey: String?): LegalFormV7Dto?{
        return technicalKey?.let { testMetadata.legalForms.find { it.technicalKey == technicalKey }!! }
    }

    fun buildSiteUpdate(
        withValuesFrom: SitePartnerUpdateV7Request,
        existingSite: SitePartnerCreateVerboseV7Dto
    ): SitePartnerCreateVerboseV7Dto {
        return SitePartnerCreateVerboseV7Dto(
            site = buildSite(
                withValuesFrom = withValuesFrom.site,
                bpnS = existingSite.site.bpns,
                bpnLegalEntity = existingSite.site.bpnLegalEntity,
                createdAt = existingSite.site.createdAt,
                updatedAt = existingSite.site.updatedAt
            ),
            mainAddress = buildInvariantAddress(
                withValuesFrom = withValuesFrom.site.mainAddress,
                addressType = AddressType.SiteMainAddress,
                isParticipantData = true,
                givenBpnA = existingSite.mainAddress.bpna,
                bpnLegalEntity = existingSite.site.bpnLegalEntity,
                bpnSite = existingSite.site.bpns,
                createdAt = existingSite.mainAddress.createdAt,
                updatedAt = existingSite.mainAddress.updatedAt
            ).withConfidence(TestDataV7.SharedByOwnerConfidence),
            index = withValuesFrom.bpns
        )
    }

    fun buildSiteSearchResponse(siteCreate: SitePartnerCreateVerboseV7Dto): SiteWithMainAddressVerboseV7Dto {
        return SiteWithMainAddressVerboseV7Dto(
            site = siteCreate.site,
            mainAddress = siteCreate.mainAddress
        )
    }

    fun buildSiteSiteCreate(
        withValuesFrom: SitePartnerCreateV7Request,
        bpnS: String = anyStringValue,
        bpnA: String = anyStringValue
    ): SitePartnerCreateVerboseV7Dto{
        return SitePartnerCreateVerboseV7Dto(
            site = buildSite(
                withValuesFrom = withValuesFrom.site,
                bpnS = bpnS,
                bpnLegalEntity = withValuesFrom.bpnlParent
            ),
            mainAddress = buildInvariantAddress(
                withValuesFrom = withValuesFrom.site.mainAddress,
                addressType = AddressType.SiteMainAddress,
                //Site data is always ParticipantData
                isParticipantData = true,
                givenBpnA = bpnA,
                bpnLegalEntity = withValuesFrom.bpnlParent,
                bpnSite = bpnS
                //Site data is always shared by owner
            ).withConfidence(TestDataV7.SharedByOwnerConfidence),
            index = withValuesFrom.index
        )
    }

    fun buildLegalSiteCreate(
        withValuesFrom: SiteCreateWithLegalAddressAsMainV7Request,
        legalEntityParent: LegalEntityWithLegalAddressVerboseV7Dto,
        bpnS: String = anyStringValue
    ): SitePartnerCreateVerboseV7Dto{
        return SitePartnerCreateVerboseV7Dto(
            site = buildSite(
                name = withValuesFrom.name,
                states = withValuesFrom.states,
                confidenceCriteria = withValuesFrom.confidenceCriteria,
                scriptVariants = withValuesFrom.scriptVariants.zip(legalEntityParent.scriptVariants){ siteVariant, leVariant -> SiteScriptVariantV7Dto(siteVariant.scriptCode, siteVariant.name, leVariant.legalAddress) },
                bpnS = bpnS,
                bpnLegalEntity = withValuesFrom.bpnLParent
            ),
            mainAddress = legalEntityParent
                .legalAddress
                .withConfidence(TestDataV7.SharedByOwnerConfidence)
                .copy(addressType = AddressType.LegalAndSiteMainAddress),
            index = "0"
        )
    }

    fun buildSite(
        withValuesFrom: SiteV7Dto,
        bpnS: String = anyStringValue,
        bpnLegalEntity: String = anyStringValue,
        createdAt: Instant = anyDate,
        updatedAt: Instant = anyDate
    ): SiteVerboseV7Dto{
        return with(withValuesFrom){
            buildSite(
                bpnS = bpnS,
                name = name,
                states = states,
                bpnLegalEntity = bpnLegalEntity,
                createdAt = createdAt,
                updatedAt = updatedAt,
                confidenceCriteria = confidenceCriteria,
                scriptVariants = scriptVariants
            )
        }
    }


    fun buildSite(
        name: String,
        states: Collection<SiteStateV7Dto>,
        confidenceCriteria: ConfidenceCriteriaV7Dto,
        scriptVariants: List<SiteScriptVariantV7Dto>,
        bpnS: String = anyStringValue,
        bpnLegalEntity: String = anyStringValue,
        createdAt: Instant = anyDate,
        updatedAt: Instant = anyDate
    ): SiteVerboseV7Dto{
        return SiteVerboseV7Dto(
                bpns = bpnS,
                name = name,
                states = states.map { buildSiteBusinessState(it) },
                //Site data is always ParticipantData
                isParticipantData = true,
                bpnLegalEntity = bpnLegalEntity,
                createdAt = createdAt,
                updatedAt = updatedAt,
                //Site data is always shared by owner
                confidenceCriteria = confidenceCriteria.withCalculatedConfidence(TestDataV7.DefaultSiteConfidence),
                scriptVariants = scriptVariants
            )
    }

    fun buildAdditionalAddressCreate(
        withValuesFrom: AddressPartnerCreateV7Request,
        legalEntity: LegalEntityWithLegalAddressVerboseV7Dto,
        bpnA: String = anyStringValue
    ): AddressPartnerCreateVerboseV7Dto {
        val invariantAddress = buildInvariantAddress(
            withValuesFrom = withValuesFrom.address,
            addressType = AddressType.AdditionalAddress,
            isParticipantData = legalEntity.header.isParticipantData,
            givenBpnA = bpnA,
            bpnLegalEntity = legalEntity.header.bpnl,
            bpnSite = null
        )
        return AddressPartnerCreateVerboseV7Dto(
            address = invariantAddress,
            scriptVariants = withValuesFrom.scriptVariants,
            index = withValuesFrom.index
        )
    }

    fun buildAdditionalAddressCreate(
        withValuesFrom: AddressPartnerCreateV7Request,
        site: SitePartnerCreateVerboseV7Dto,
        bpnA: String = anyStringValue
    ): AddressPartnerCreateVerboseV7Dto {
        val invariantAddress = buildInvariantAddress(
            withValuesFrom = withValuesFrom.address,
            addressType = AddressType.AdditionalAddress,
            isParticipantData = true,
            givenBpnA = bpnA,
            bpnLegalEntity = site.site.bpnLegalEntity,
            bpnSite = site.site.bpns
        ).withConfidence(TestDataV7.SharedByOwnerConfidence)
        return AddressPartnerCreateVerboseV7Dto(
            address = invariantAddress,
            scriptVariants = withValuesFrom.scriptVariants,
            index = withValuesFrom.index
        )
    }

    fun buildLegalAddressUpdate(
        withValuesFrom: AddressPartnerUpdateV7Request,
        legalEntity: LegalEntityWithLegalAddressVerboseV7Dto
    ): AddressPartnerUpdateVerboseV7Dto {
        val invariantAddress = buildInvariantAddress(
            withValuesFrom = withValuesFrom.address,
            addressType = AddressType.LegalAddress,
            isParticipantData = legalEntity.header.isParticipantData,
            givenBpnA = legalEntity.legalAddress.bpna,
            bpnLegalEntity = legalEntity.header.bpnl,
            bpnSite = null,
            createdAt = legalEntity.legalAddress.createdAt,
            updatedAt = legalEntity.legalAddress.updatedAt
        )
        return AddressPartnerUpdateVerboseV7Dto(
            address = invariantAddress,
            scriptVariants = withValuesFrom.scriptVariants
        )
    }

    fun buildSiteMainAddressUpdate(
        withValuesFrom: AddressPartnerUpdateV7Request,
        site: SitePartnerCreateVerboseV7Dto
    ): AddressPartnerUpdateVerboseV7Dto {
        val invariantAddress = buildInvariantAddress(
            withValuesFrom = withValuesFrom.address,
            addressType = site.mainAddress.addressType ?: AddressType.SiteMainAddress,
            isParticipantData = true,
            givenBpnA = site.mainAddress.bpna,
            bpnLegalEntity = site.site.bpnLegalEntity,
            bpnSite = site.site.bpns,
            createdAt = site.mainAddress.createdAt,
            updatedAt = site.mainAddress.updatedAt
        ).withConfidence(TestDataV7.SharedByOwnerConfidence)
        return AddressPartnerUpdateVerboseV7Dto(
            address = invariantAddress,
            scriptVariants = withValuesFrom.scriptVariants
        )
    }

    fun buildAdditionalAddressUpdate(
        withValuesFrom: AddressPartnerUpdateV7Request,
        existingAddress: AddressPartnerCreateVerboseV7Dto
    ): AddressPartnerUpdateVerboseV7Dto {
        val isParticipantData = existingAddress.address.isParticipantData
        val invariantAddress = buildInvariantAddress(
            withValuesFrom = withValuesFrom.address,
            addressType = existingAddress.address.addressType ?: AddressType.AdditionalAddress,
            isParticipantData = isParticipantData,
            givenBpnA = existingAddress.address.bpna,
            bpnLegalEntity = existingAddress.address.bpnLegalEntity,
            bpnSite = existingAddress.address.bpnSite,
            createdAt = existingAddress.address.createdAt,
            updatedAt = existingAddress.address.updatedAt
        )
        return AddressPartnerUpdateVerboseV7Dto(
            address = invariantAddress,
            scriptVariants = withValuesFrom.scriptVariants
        )
    }

    fun buildAddressSearchResponse(created: AddressPartnerCreateVerboseV7Dto): LogisticAddressVerboseV7Dto =
        LogisticAddressVerboseV7Dto(address = created.address, scriptVariants = created.scriptVariants)

    fun buildAddressSearchResponse(site: SitePartnerCreateVerboseV7Dto): LogisticAddressVerboseV7Dto =
        LogisticAddressVerboseV7Dto(address = site.mainAddress, scriptVariants = site.site.scriptVariants.map { buildSiteMainAddressScriptVariant(it) })

    fun buildAddressSearchResponseFromLegalSite(site: SitePartnerCreateVerboseV7Dto, legalEntity: LegalEntityWithLegalAddressVerboseV7Dto): LogisticAddressVerboseV7Dto =
        LogisticAddressVerboseV7Dto(address = site.mainAddress, scriptVariants = legalEntity.scriptVariants.map { buildLegalAddressScriptVariant(it) })


    fun buildAddressSearchResponse(legalEntity: LegalEntityWithLegalAddressVerboseV7Dto): LogisticAddressVerboseV7Dto =
        LogisticAddressVerboseV7Dto(address = legalEntity.legalAddress, scriptVariants = legalEntity.scriptVariants.map { buildLegalAddressScriptVariant(it) })

    private fun buildInvariantAddress(
        withValuesFrom: LogisticAddressV7Dto,
        addressType: AddressType,
        isParticipantData: Boolean,
        givenBpnA: String = anyStringValue,
        bpnLegalEntity: String? = anyStringValue,
        bpnSite: String? = anyStringValue,
        createdAt: Instant = anyDate,
        updatedAt: Instant = anyDate,
    ): LogisticAddressInvariantVerboseV7Dto {
        return with(withValuesFrom) {
            LogisticAddressInvariantVerboseV7Dto(
                bpna = givenBpnA,
                name = name,
                states = states.map { buildAddressBusinessState(it) },
                identifiers = identifiers.map { buildAddressIdentifier(it) },
                physicalPostalAddress = buildPhysicalAddress(physicalPostalAddress),
                alternativePostalAddress = alternativePostalAddress?.let { buildAlternativeAddress(it) },
                bpnLegalEntity = bpnLegalEntity,
                bpnSite = bpnSite,
                isParticipantData = isParticipantData,
                createdAt = createdAt,
                updatedAt = updatedAt,
                confidenceCriteria = confidenceCriteria,
                addressType = addressType
            )
        }
    }

    private fun buildLegalIdentifier(withValuesFrom: LegalEntityIdentifierV7Dto): LegalEntityIdentifierVerboseV7Dto {
        val identifierType = testMetadata.legalEntityIdentifierTypes.find { withValuesFrom.type == it.technicalKey }
            ?: throw IllegalArgumentException("Legal Entity identifier with Key '${withValuesFrom.type}' is not expected")
        return LegalEntityIdentifierVerboseV7Dto(
            value = withValuesFrom.value,
            typeVerbose = TypeKeyNameVerboseDto(identifierType.technicalKey, identifierType.name),
            issuingBody = withValuesFrom.issuingBody
        )
    }

    private fun buildAddressIdentifier(withValuesFrom: AddressIdentifierV7Dto): AddressIdentifierVerboseV7Dto {
        val identifierType = testMetadata.addressIdentifierTypes.find { withValuesFrom.type == it.technicalKey }
            ?: throw IllegalArgumentException("Address identifier with Key '${withValuesFrom.type}' is not expected")
        return AddressIdentifierVerboseV7Dto(
            value = withValuesFrom.value,
            typeVerbose = TypeKeyNameVerboseDto(identifierType.technicalKey, identifierType.name)
        )
    }

    private fun buildLegalBusinessState(withValuesFrom: LegalEntityStateV7Dto): LegalEntityStateVerboseV7Dto {
        return with(withValuesFrom) {
            LegalEntityStateVerboseV7Dto(validFrom = validFrom ?: BusinessStateDates.EARLIEST, validTo = validTo ?: BusinessStateDates.LATEST, typeVerbose = TypeKeyNameVerboseDto(type, type.getTypeName()))
        }
    }

    private fun buildSiteBusinessState(withValuesFrom: SiteStateV7Dto): SiteStateVerboseV7Dto {
        return with(withValuesFrom) {
            SiteStateVerboseV7Dto(validFrom = validFrom ?: BusinessStateDates.EARLIEST, validTo = validTo ?: BusinessStateDates.LATEST, typeVerbose = TypeKeyNameVerboseDto(type, type.getTypeName()))
        }
    }

    private fun buildAddressBusinessState(withValuesFrom: AddressStateV7Dto): AddressStateVerboseV7Dto {
        return with(withValuesFrom) {
            AddressStateVerboseV7Dto(validFrom = validFrom ?: BusinessStateDates.EARLIEST, validTo = validTo ?: BusinessStateDates.LATEST, typeVerbose = TypeKeyNameVerboseDto(type, type.getTypeName()))
        }
    }

    private fun buildPhysicalAddress(withValuesFrom: PhysicalPostalAddressV7Dto): PhysicalPostalAddressVerboseV7Dto {
        return with(withValuesFrom) {
            PhysicalPostalAddressVerboseV7Dto(
                geographicCoordinates = geographicCoordinates,
                countryVerbose = testMetadata.countries.toCountryVerbose(country),
                administrativeAreaLevel1Verbose = administrativeAreaLevel1?.let { buildAdminArea(it) },
                administrativeAreaLevel2,
                administrativeAreaLevel3,
                postalCode,
                city,
                district,
                street,
                companyPostalCode,
                industrialZone,
                building,
                floor,
                door,
                taxJurisdictionCode
            )
        }
    }

    private fun buildAlternativeAddress(withValuesFrom: AlternativePostalAddressV7Dto): AlternativePostalAddressVerboseV7Dto {
        return with(withValuesFrom) {
            AlternativePostalAddressVerboseV7Dto(
                geographicCoordinates = geographicCoordinates,
                countryVerbose = testMetadata.countries.toCountryVerbose(country),
                administrativeAreaLevel1Verbose = administrativeAreaLevel1?.let { buildAdminArea(it) },
                postalCode, city, deliveryServiceType, deliveryServiceQualifier, deliveryServiceNumber
            )
        }
    }

    private fun buildAdminArea(givenAdminAreaCode: String): RegionV7Dto {
        return with(testMetadata.adminAreas.find { it.code == givenAdminAreaCode }!!) {
            RegionV7Dto(countryCode, code, name)
        }
    }

    private fun buildLegalAddressScriptVariant(legalEntityScriptVariant: LegalEntityScriptVariantV7Dto) =
        LogisticAddressScriptVariantV7Dto(legalEntityScriptVariant.scriptCode, legalEntityScriptVariant.legalAddress)

    private fun buildSiteMainAddressScriptVariant(siteScriptVariant: SiteScriptVariantV7Dto) =
        LogisticAddressScriptVariantV7Dto(siteScriptVariant.scriptCode, siteScriptVariant.mainAddress)
}