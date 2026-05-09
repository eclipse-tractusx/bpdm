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
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.TypeKeyNameVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.api.model.AddressIdentifierDto
import org.eclipse.tractusx.bpdm.pool.api.model.AlternativePostalAddressDto
import org.eclipse.tractusx.bpdm.pool.api.model.PhysicalPostalAddressDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.*
import org.eclipse.tractusx.bpdm.pool.api.model.response.*
import org.eclipse.tractusx.bpdm.test.testdata.pool.TestMetadataV7
import java.time.Instant

class PoolResponseFactoryV7(
    private val testMetadata: TestMetadataV7
) {

    private val anyStringValue = "ANY"
    private val anyDate = Instant.now()

    fun <T> buildSinglePageResponse(content: Collection<T>): PageDto<T> {
        return PageDto(content.size.toLong(), 1, 0, content.size, content)
    }

    fun buildLegalEntityCreate(withValuesFrom: LegalEntityPartnerCreateRequest): LegalEntityPartnerCreateVerboseDto{
        return LegalEntityPartnerCreateVerboseDto(
            buildLegalEntityWithLegalAddress(withValuesFrom.legalEntity),
            withValuesFrom.index
        )
    }

    fun buildLegalEntityUpdate(withValuesFrom: LegalEntityPartnerUpdateRequest, bpnAFrom: LegalEntityWithLegalAddressVerboseDto): LegalEntityPartnerCreateVerboseDto{
        return buildLegalEntityUpdate(withValuesFrom, bpnAFrom.legalAddress.bpna)
    }

    fun buildLegalEntityUpdate(withValuesFrom: LegalEntityPartnerUpdateRequest, bpnA: String = anyStringValue): LegalEntityPartnerCreateVerboseDto{
        return LegalEntityPartnerCreateVerboseDto(
            buildLegalEntityWithLegalAddress(withValuesFrom.legalEntity, withValuesFrom.bpnl, bpnA),
            index = withValuesFrom.bpnl
        )
    }

    fun buildLegalEntityWithLegalAddress(withValuesFrom: LegalEntityDto, bpnL: String = anyStringValue, bpnA: String = anyStringValue): LegalEntityWithLegalAddressVerboseDto{
        return LegalEntityWithLegalAddressVerboseDto(
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
        withValuesFrom: LegalEntityHeaderDto,
        bpnL: String = anyStringValue,
        currentness: Instant = anyDate,
        createdAt: Instant = anyDate,
        updatedAt: Instant = anyDate
    ): LegalEntityHeaderVerboseDto{
        return with(withValuesFrom){
            LegalEntityHeaderVerboseDto(
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
                updatedAt = updatedAt
            )
        }
    }

    fun buildLegalForm(technicalKey: String?): LegalFormDto?{
        return technicalKey?.let { testMetadata.legalForms.find { it.technicalKey == technicalKey }!! }
    }

    fun buildSiteUpdate(
        withValuesFrom: SitePartnerUpdateRequest,
        existingSite: SitePartnerCreateVerboseDto
    ): SitePartnerCreateVerboseDto {
        return SitePartnerCreateVerboseDto(
            site = buildSite(
                withValuesFrom = withValuesFrom.site,
                bpnS = existingSite.site.bpns,
                bpnLegalEntity = existingSite.site.bpnLegalEntity
            ),
            mainAddress = buildInvariantAddress(
                withValuesFrom = withValuesFrom.site.mainAddress,
                addressType = AddressType.SiteMainAddress,
                isParticipantData = true,
                givenBpnA = existingSite.mainAddress.bpna,
                bpnLegalEntity = existingSite.site.bpnLegalEntity,
                bpnSite = existingSite.site.bpns
            ).withConfidence(TestDataV7.SharedByOwnerConfidence),
            index = withValuesFrom.bpns
        )
    }

    fun buildSiteSearchResponse(siteCreate: SitePartnerCreateVerboseDto): SiteWithMainAddressVerboseDto {
        return SiteWithMainAddressVerboseDto(
            site = siteCreate.site,
            mainAddress = siteCreate.mainAddress
        )
    }

    fun buildSiteSiteCreate(
        withValuesFrom: SitePartnerCreateRequest,
        bpnS: String = anyStringValue,
        bpnA: String = anyStringValue
    ): SitePartnerCreateVerboseDto{
        return SitePartnerCreateVerboseDto(
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
        withValuesFrom: SiteCreateRequestWithLegalAddressAsMain,
        legalEntityParent: LegalEntityWithLegalAddressVerboseDto,
        bpnS: String = anyStringValue
    ): SitePartnerCreateVerboseDto{
        return SitePartnerCreateVerboseDto(
            site = buildSite(
                name = withValuesFrom.name,
                states = withValuesFrom.states,
                confidenceCriteria = withValuesFrom.confidenceCriteria,
                scriptVariants = emptyList(),
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
        withValuesFrom: SiteDto,
        bpnS: String = anyStringValue,
        bpnLegalEntity: String = anyStringValue,
        createdAt: Instant = anyDate,
        updatedAt: Instant = anyDate
    ): SiteVerboseDto{
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
        states: Collection<SiteStateDto>,
        confidenceCriteria: ConfidenceCriteriaDto,
        scriptVariants: List<SiteScriptVariantDto>,
        bpnS: String = anyStringValue,
        bpnLegalEntity: String = anyStringValue,
        createdAt: Instant = anyDate,
        updatedAt: Instant = anyDate
    ): SiteVerboseDto{
        return SiteVerboseDto(
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
        withValuesFrom: AddressPartnerCreateRequest,
        legalEntity: LegalEntityWithLegalAddressVerboseDto,
        bpnA: String = anyStringValue
    ): AddressPartnerCreateVerboseDto {
        val invariantAddress = buildInvariantAddress(
            withValuesFrom = withValuesFrom.address,
            addressType = AddressType.AdditionalAddress,
            isParticipantData = legalEntity.header.isParticipantData,
            givenBpnA = bpnA,
            bpnLegalEntity = legalEntity.header.bpnl,
            bpnSite = null
        )
        return AddressPartnerCreateVerboseDto(
            address = invariantAddress,
            scriptVariants = withValuesFrom.scriptVariants,
            index = withValuesFrom.index
        )
    }

    fun buildAdditionalAddressCreate(
        withValuesFrom: AddressPartnerCreateRequest,
        site: SitePartnerCreateVerboseDto,
        bpnA: String = anyStringValue
    ): AddressPartnerCreateVerboseDto {
        val invariantAddress = buildInvariantAddress(
            withValuesFrom = withValuesFrom.address,
            addressType = AddressType.AdditionalAddress,
            isParticipantData = true,
            givenBpnA = bpnA,
            bpnLegalEntity = site.site.bpnLegalEntity,
            bpnSite = site.site.bpns
        ).withConfidence(TestDataV7.SharedByOwnerConfidence)
        return AddressPartnerCreateVerboseDto(
            address = invariantAddress,
            scriptVariants = withValuesFrom.scriptVariants,
            index = withValuesFrom.index
        )
    }

    fun buildLegalAddressUpdate(
        withValuesFrom: AddressPartnerUpdateRequest,
        legalEntity: LegalEntityWithLegalAddressVerboseDto
    ): AddressPartnerUpdateVerboseDto {
        val invariantAddress = buildInvariantAddress(
            withValuesFrom = withValuesFrom.address,
            addressType = AddressType.LegalAddress,
            isParticipantData = legalEntity.header.isParticipantData,
            givenBpnA = legalEntity.legalAddress.bpna,
            bpnLegalEntity = legalEntity.header.bpnl,
            bpnSite = null
        )
        return AddressPartnerUpdateVerboseDto(
            address = invariantAddress,
            scriptVariants = withValuesFrom.scriptVariants
        )
    }

    fun buildSiteMainAddressUpdate(
        withValuesFrom: AddressPartnerUpdateRequest,
        site: SitePartnerCreateVerboseDto
    ): AddressPartnerUpdateVerboseDto {
        val invariantAddress = buildInvariantAddress(
            withValuesFrom = withValuesFrom.address,
            addressType = site.mainAddress.addressType ?: AddressType.SiteMainAddress,
            isParticipantData = true,
            givenBpnA = site.mainAddress.bpna,
            bpnLegalEntity = site.site.bpnLegalEntity,
            bpnSite = site.site.bpns
        ).withConfidence(TestDataV7.SharedByOwnerConfidence)
        return AddressPartnerUpdateVerboseDto(
            address = invariantAddress,
            scriptVariants = withValuesFrom.scriptVariants
        )
    }

    fun buildAdditionalAddressUpdate(
        withValuesFrom: AddressPartnerUpdateRequest,
        existingAddress: AddressPartnerCreateVerboseDto
    ): AddressPartnerUpdateVerboseDto {
        val isParticipantData = existingAddress.address.isParticipantData
        val invariantAddress = buildInvariantAddress(
            withValuesFrom = withValuesFrom.address,
            addressType = existingAddress.address.addressType ?: AddressType.AdditionalAddress,
            isParticipantData = isParticipantData,
            givenBpnA = existingAddress.address.bpna,
            bpnLegalEntity = existingAddress.address.bpnLegalEntity,
            bpnSite = existingAddress.address.bpnSite
        )
        return AddressPartnerUpdateVerboseDto(
            address = invariantAddress,
            scriptVariants = withValuesFrom.scriptVariants
        )
    }

    fun buildAddressSearchResponse(created: AddressPartnerCreateVerboseDto): LogisticAddressVerboseDto =
        LogisticAddressVerboseDto(address = created.address, scriptVariants = created.scriptVariants)

    fun buildAddressSearchResponse(site: SitePartnerCreateVerboseDto): LogisticAddressVerboseDto =
        LogisticAddressVerboseDto(address = site.mainAddress, scriptVariants = site.site.scriptVariants.map { buildSiteMainAddressScriptVariant(it) })

    fun buildAddressSearchResponseFromLegalSite(site: SitePartnerCreateVerboseDto, legalEntity: LegalEntityWithLegalAddressVerboseDto): LogisticAddressVerboseDto =
        LogisticAddressVerboseDto(address = site.mainAddress, scriptVariants = legalEntity.scriptVariants.map { buildLegalAddressScriptVariant(it) })


    fun buildAddressSearchResponse(legalEntity: LegalEntityWithLegalAddressVerboseDto): LogisticAddressVerboseDto =
        LogisticAddressVerboseDto(address = legalEntity.legalAddress, scriptVariants = legalEntity.scriptVariants.map { buildLegalAddressScriptVariant(it) })

    private fun buildInvariantAddress(
        withValuesFrom: LogisticAddressDto,
        addressType: AddressType,
        isParticipantData: Boolean,
        givenBpnA: String = anyStringValue,
        bpnLegalEntity: String? = anyStringValue,
        bpnSite: String? = anyStringValue,
        createdAt: Instant = anyDate,
        updatedAt: Instant = anyDate,
    ): LogisticAddressInvariantVerboseDto {
        return with(withValuesFrom) {
            LogisticAddressInvariantVerboseDto(
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

    private fun buildLegalIdentifier(withValuesFrom: LegalEntityIdentifierDto): LegalEntityIdentifierVerboseDto {
        val identifierType = testMetadata.legalEntityIdentifierTypes.find { withValuesFrom.type == it.technicalKey }
            ?: throw IllegalArgumentException("Legal Entity identifier with Key '${withValuesFrom.type}' is not expected")
        return LegalEntityIdentifierVerboseDto(
            value = withValuesFrom.value,
            typeVerbose = TypeKeyNameVerboseDto(identifierType.technicalKey, identifierType.name),
            issuingBody = withValuesFrom.issuingBody
        )
    }

    private fun buildAddressIdentifier(withValuesFrom: AddressIdentifierDto): AddressIdentifierVerboseDto {
        val identifierType = testMetadata.addressIdentifierTypes.find { withValuesFrom.type == it.technicalKey }
            ?: throw IllegalArgumentException("Address identifier with Key '${withValuesFrom.type}' is not expected")
        return AddressIdentifierVerboseDto(
            value = withValuesFrom.value,
            typeVerbose = TypeKeyNameVerboseDto(identifierType.technicalKey, identifierType.name)
        )
    }

    private fun buildLegalBusinessState(withValuesFrom: LegalEntityStateDto): LegalEntityStateVerboseDto {
        return with(withValuesFrom) {
            LegalEntityStateVerboseDto(validFrom = validFrom, validTo = validTo, typeVerbose = TypeKeyNameVerboseDto(type, type.getTypeName()))
        }
    }

    private fun buildSiteBusinessState(withValuesFrom: SiteStateDto): SiteStateVerboseDto {
        return with(withValuesFrom) {
            SiteStateVerboseDto(validFrom = validFrom, validTo = validTo, typeVerbose = TypeKeyNameVerboseDto(type, type.getTypeName()))
        }
    }

    private fun buildAddressBusinessState(withValuesFrom: AddressStateDto): AddressStateVerboseDto {
        return with(withValuesFrom) {
            AddressStateVerboseDto(validFrom = validFrom, validTo = validTo, typeVerbose = TypeKeyNameVerboseDto(type, type.getTypeName()))
        }
    }

    private fun buildPhysicalAddress(withValuesFrom: PhysicalPostalAddressDto): PhysicalPostalAddressVerboseDto {
        return with(withValuesFrom) {
            PhysicalPostalAddressVerboseDto(
                geographicCoordinates = geographicCoordinates,
                countryVerbose = TypeKeyNameVerboseDto(country, country.getName()),
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

    private fun buildAlternativeAddress(withValuesFrom: AlternativePostalAddressDto): AlternativePostalAddressVerboseDto {
        return with(withValuesFrom) {
            AlternativePostalAddressVerboseDto(
                geographicCoordinates = geographicCoordinates,
                countryVerbose = TypeKeyNameVerboseDto(country, country.getName()),
                administrativeAreaLevel1Verbose = administrativeAreaLevel1?.let { buildAdminArea(it) },
                postalCode, city, deliveryServiceType, deliveryServiceQualifier, deliveryServiceNumber
            )
        }
    }

    private fun buildAdminArea(givenAdminAreaCode: String): RegionDto {
        return with(testMetadata.adminAreas.find { it.code == givenAdminAreaCode }!!) {
            RegionDto(countryCode, code, name)
        }
    }

    private fun buildLegalAddressScriptVariant(legalEntityScriptVariant: LegalEntityScriptVariantDto) =
        LogisticAddressScriptVariantDto(legalEntityScriptVariant.scriptCode, legalEntityScriptVariant.legalAddress)

    private fun buildSiteMainAddressScriptVariant(siteScriptVariant: SiteScriptVariantDto) =
        LogisticAddressScriptVariantDto(siteScriptVariant.scriptCode, siteScriptVariant.mainAddress)
}