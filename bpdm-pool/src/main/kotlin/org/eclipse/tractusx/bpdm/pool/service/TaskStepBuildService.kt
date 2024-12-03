/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.service

import com.neovisionaries.i18n.CountryCode
import jakarta.transaction.Transactional
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.api.model.request.*
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.repository.BpnRequestIdentifierRepository
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneOffset
import org.eclipse.tractusx.bpdm.pool.api.model.AlternativePostalAddressDto as AlternativePostalAddressPoolDto
import org.eclipse.tractusx.bpdm.pool.api.model.ConfidenceCriteriaDto as ConfidenceCriteriaPoolDto
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityDto as LegalEntityPoolDto
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressDto as LogisticAddressPoolDto
import org.eclipse.tractusx.bpdm.pool.api.model.PhysicalPostalAddressDto as PhysicalPostalAddressPoolDto
import org.eclipse.tractusx.bpdm.pool.api.model.SiteDto as SitePoolDto


@Service
class TaskStepBuildService(
    private val businessPartnerBuildService: BusinessPartnerBuildService,
    private val businessPartnerFetchService: BusinessPartnerFetchService,
    private val siteService: SiteService,
    private val addressService: AddressService,
    private val bpnRequestIdentifierRepository: BpnRequestIdentifierRepository
) {

    enum class CleaningError(val message: String) {
        LEGAL_NAME_IS_NULL("Legal name is null"),
        COUNTRY_CITY_IS_NULL("Country or city in physicalAddress is null"),
        LEGAL_ENTITY_IS_NULL("Legal entity or BpnL Reference is null"),
        LEGAL_ADDRESS_IS_NULL("Legal Address is null"),
        PHYSICAL_ADDRESS_IS_NULL("Physical Address is null"),
        ALTERNATIVE_ADDRESS_DATA_IS_NULL("Country or city or deliveryServiceType or deliveryServiceNumber in alternativeAddress is null"),
        MAINE_ADDRESS_IS_NULL("Main address is null"),
        BPNS_IS_NULL("BpnS Reference is null"),
        BPNA_IS_NULL("BpnA Reference is null"),
        SITE_NAME_IS_NULL("Site name is null"),
        INVALID_LEGAL_ENTITY_BPN("Invalid legal entity BPN"),
        CLASSIFICATION_TYPE_IS_NULL("Classification type is null"),
        PHYSICAL_ADDRESS_COUNTRY_MISSING("Physical Address has no country"),
        PHYSICAL_ADDRESS_CITY_MISSING("Physical Address has no city"),
        ALTERNATIVE_ADDRESS_COUNTRY_MISSING("Alternative Address has no country"),
        ALTERNATIVE_ADDRESS_CITY_MISSING("Alternative Address has no city"),
        ALTERNATIVE_ADDRESS_DELIVERY_SERVICE_TYPE_MISSING("Alternative Address has no delivery service type"),
        ALTERNATIVE_ADDRESS_DELIVERY_SERVICE_NUMBER_MISSING("Alternative Address has no delivery service number"),
        ADDRESS_CONFIDENCE_CRITERIA_MISSING("Logistic address is missing confidence criteria"),
        SITE_CONFIDENCE_CRITERIA_MISSING("Site is missing confidence criteria"),
        SITE_NAME_MISSING("Site has no name"),
        LEGAL_ENTITY_CONFIDENCE_CRITERIA_MISSING("Legal Entity has no confidence criteria"),
        MAIN_ADDRESS_BPN_REFERENCE_MISSING("The BpnA Reference of the site main address is missing"),
        LEGAL_ADDRESS_BPN_REFERENCE_MISSING("The BpnA Reference of the legal address is missing"),
    }

    @Transactional
    fun upsertBusinessPartner(taskEntry: TaskStepReservationEntryDto): TaskStepResultEntryDto {
        val taskEntryBpnMapping = TaskEntryBpnMapping(listOf(taskEntry), bpnRequestIdentifierRepository)
        val businessPartnerDto = taskEntry.businessPartner

        val legalEntityBpns = processLegalEntity(businessPartnerDto, taskEntryBpnMapping)
        val siteBpns = processSite(businessPartnerDto, legalEntityBpns.legalEntityBpn, taskEntryBpnMapping)
        val addressBpn = processAdditionalAddress(businessPartnerDto, legalEntityBpns.legalEntityBpn, siteBpns?.siteBpn, taskEntryBpnMapping)

        val businessPartnerWithBpns = with(businessPartnerDto){
            copy(
                legalEntity = legalEntity.copy(
                    bpnReference = toBpnReference(legalEntityBpns.legalEntityBpn),
                    legalAddress = legalEntity.legalAddress.copy(
                        bpnReference = toBpnReference(legalEntityBpns.legalAddressBpn)
                    ),
                    isCatenaXMemberData = legalEntityBpns.isCatenaXMember
                ),
                site = siteBpns?.let { site?.copy(
                    bpnReference = toBpnReference(siteBpns.siteBpn),
                    siteMainAddress = site!!.siteMainAddress?.copy(bpnReference = toBpnReference(siteBpns.mainAddressBpn))
                ) },
                additionalAddress = addressBpn?.let { additionalAddress?.copy(bpnReference = toBpnReference(addressBpn)) }
            )
        }

        taskEntryBpnMapping.writeCreatedMappingsToDb(bpnRequestIdentifierRepository)
        return TaskStepResultEntryDto(
            taskId = taskEntry.taskId,
            businessPartner = businessPartnerWithBpns,
            errors = emptyList()
        )
    }

    private fun processLegalEntity(
        businessPartner: BusinessPartner, taskEntryBpnMapping: TaskEntryBpnMapping
    ): LegalEntityBpns{
        val legalEntity = businessPartner.legalEntity
        val bpnLReference = legalEntity.bpnReference
        val bpnL = taskEntryBpnMapping.getBpn(bpnLReference)

        val existingLegalEntityInformation by lazy {
            businessPartnerFetchService.fetchByBpns(listOf(bpnL!!))
                .firstOrNull()
                ?.let { LegalEntityBpns(it.bpn, it.legalAddress.bpn, it.isCatenaXMemberData) } ?:
            throw BpdmValidationException("Legal entity with specified BPNL $bpnL not found")
        }

        val isCatenaXMember = legalEntity.isCatenaXMemberData ?: if(bpnL != null) existingLegalEntityInformation.isCatenaXMember else false

        val bpnResults = if(bpnL != null && legalEntity.hasChanged == false){
            //No need to upsert, just fetch the information
            existingLegalEntityInformation
        }else{
            upsertLegalEntity(legalEntity.copy(isCatenaXMemberData = isCatenaXMember), taskEntryBpnMapping)
        }

        return bpnResults
    }


    private fun upsertLegalEntity(
        legalEntity: LegalEntity, taskEntryBpnMapping: TaskEntryBpnMapping
    ): LegalEntityBpns {
        val legalAddress = legalEntity.legalAddress
        val bpnLReference = legalEntity.bpnReference
        val bpnL = taskEntryBpnMapping.getBpn(bpnLReference)

        val poolLegalEntity = toPoolDto(legalEntity)
        val poolLegalAddress = toPoolDto(legalAddress)

        val bpnResults = if (bpnL == null) {
            createLegalEntity(poolLegalEntity, poolLegalAddress)
        }
        else{
            updateLegalEntity(bpnL, poolLegalEntity, poolLegalAddress)
        }

        taskEntryBpnMapping.addMapping(bpnLReference, bpnResults.legalEntityBpn)
        taskEntryBpnMapping.addMapping(legalAddress.bpnReference, bpnResults.legalAddressBpn)

        return bpnResults
    }

    private fun createLegalEntity(legalEntityDto: LegalEntityPoolDto, legalAddressDto: LogisticAddressPoolDto): LegalEntityBpns {

        val createRequest = LegalEntityPartnerCreateRequest(
            legalEntity = legalEntityDto,
            legalAddress = legalAddressDto,
            index = ""
        )
        val result = businessPartnerBuildService.createLegalEntities(listOf(createRequest))
        if(result.errors.isNotEmpty())
            throw BpdmValidationException("Errors on creating Legal Entity: ${result.errors.joinToString()}")

        val legalEntityResult = result.entities.firstOrNull() ?: throw BpdmValidationException("Unknown error when trying to create legal entity")

        val bpnL = legalEntityResult.legalEntity.bpnl
        val legalAddressBpnA = legalEntityResult.legalAddress.bpna

        return LegalEntityBpns(bpnL, legalAddressBpnA, legalEntityResult.legalEntity.isCatenaXMemberData)
    }

    private fun updateLegalEntity(
        bpnL: String,
        legalEntityDto: LegalEntityPoolDto,
        legalAddressDto: LogisticAddressPoolDto
    ): LegalEntityBpns {
        val updateRequest = LegalEntityPartnerUpdateRequest(
            bpnl = bpnL,
            legalEntity = legalEntityDto,
            legalAddress = legalAddressDto
        )
        val result = businessPartnerBuildService.updateLegalEntities(listOf(updateRequest))
        if(result.errors.isNotEmpty())
            throw BpdmValidationException("Errors on updating Legal Entity: ${result.errors.joinToString()}")

        val legalEntityResult = result.entities.firstOrNull() ?: throw BpdmValidationException("Unknown error when trying to update legal entity")

        return LegalEntityBpns(legalEntityResult.legalEntity.bpnl, legalEntityResult.legalAddress.bpna, legalEntityResult.legalEntity.isCatenaXMemberData)
    }

    private fun processSite(
        businessPartner: BusinessPartner,
        legalEntityBpn: String,
        taskEntryBpnMapping: TaskEntryBpnMapping
    ): SiteBpns? {
        val site = businessPartner.site ?: return null

        val bpnSReference = site.bpnReference
        val bpnS = taskEntryBpnMapping.getBpn(bpnSReference)

        val bpnResults = if(bpnS != null && site.hasChanged == false){
            //No need to upsert, just fetch the information
            siteService.searchSites(SiteService.SiteSearchRequest(siteBpns = listOf(bpnS), null, null, null, null), PaginationRequest(0, 1))
                .content.firstOrNull()
                ?.let { SiteBpns(it.site.bpns, it.mainAddress.bpna) }
                ?: throw BpdmValidationException(CleaningError.MAINE_ADDRESS_IS_NULL.message)
        }else {
            upsertSite(site, businessPartner, legalEntityBpn, taskEntryBpnMapping)
        }

        return bpnResults
    }

    private fun upsertSite(
        site: Site,
        businessPartner: BusinessPartner,
        legalEntityBpn: String,
        taskEntryBpnMapping: TaskEntryBpnMapping
    ): SiteBpns {
        val siteMainAddress = if(site.siteMainIsLegalAddress) businessPartner.legalEntity.legalAddress else site.siteMainAddress
            ?: throw BpdmValidationException(CleaningError.MAINE_ADDRESS_IS_NULL.message)

        val bpnSReference = site.bpnReference
        val bpnS = taskEntryBpnMapping.getBpn(bpnSReference)

        val poolSite = toPoolDto(site, siteMainAddress)

        val bpnResults = if (bpnS == null) {
            createSite(poolSite, legalEntityBpn, site.siteMainAddress == null)
        }
        else {
            updateSite(bpnS, poolSite)
        }

        taskEntryBpnMapping.addMapping(bpnSReference, bpnResults.siteBpn)
        taskEntryBpnMapping.addMapping(siteMainAddress.bpnReference, bpnResults.mainAddressBpn)

        return bpnResults
    }

    private fun createSite(
        poolSite: SitePoolDto,
        legalEntityBpn: String,
        isSiteMainAndLegalAddress: Boolean
    ): SiteBpns {

        val result = if(isSiteMainAndLegalAddress){
            val createRequest = SiteCreateRequestWithLegalAddressAsMain(
                name = poolSite.name,
                states = poolSite.states,
                confidenceCriteria = poolSite.confidenceCriteria,
                bpnLParent = legalEntityBpn
            )
            businessPartnerBuildService.createSitesWithLegalAddressAsMain(listOf(createRequest))
        }else{
            val createRequest = SitePartnerCreateRequest(
                bpnlParent = legalEntityBpn,
                site = poolSite,
                index = ""
            )
            businessPartnerBuildService.createSitesWithMainAddress(listOf(createRequest))
        }

        if(result.errors.isNotEmpty())
            throw BpdmValidationException("Errors on creating Site: ${result.errors.joinToString()}")

        val siteResult = result.entities.firstOrNull() ?: throw BpdmValidationException("Unknown error when trying to creating site")

        return SiteBpns(siteResult.site.bpns, siteResult.mainAddress.bpna)
    }

    private fun updateSite(
        bpnS: String,
        poolSite: SitePoolDto,
    ): SiteBpns {
        val updateRequest = SitePartnerUpdateRequest(
            bpns = bpnS,
            site = poolSite
        )
        val result = businessPartnerBuildService.updateSites(listOf(updateRequest))
        if(result.errors.isNotEmpty())
            throw BpdmValidationException("Errors on updating Site: ${result.errors.joinToString()}")

        val siteResult = result.entities.firstOrNull() ?: throw BpdmValidationException("Unknown error when trying to updating site")

        return SiteBpns(siteResult.site.bpns, siteResult.mainAddress.bpna)
    }

    private fun processAdditionalAddress(
        businessPartner: BusinessPartner,
        legalEntityBpn: String,
        siteBpn: String?,
        taskEntryBpnMapping: TaskEntryBpnMapping
    ): String?{
        val additionalAddress = businessPartner.additionalAddress ?: return null

        val bpnAReference = additionalAddress.bpnReference
        val bpnA = taskEntryBpnMapping.getBpn(bpnAReference)

        val addressBpn = if(bpnA != null && additionalAddress.hasChanged == false){
            // No need to upsert just fetch the data
            addressService.searchAddresses(AddressService.AddressSearchRequest(addressBpns = listOf(bpnA), null, null, null, null, null), PaginationRequest(0, 1))
                .content.firstOrNull()?.bpna
                ?: throw BpdmValidationException(CleaningError.BPNA_IS_NULL.message)
        }else{
            upsertAdditionalAddress(additionalAddress, legalEntityBpn, siteBpn, taskEntryBpnMapping)
        }

        return addressBpn
    }

    private fun upsertAdditionalAddress(
        additionalAddress: PostalAddress,
        legalEntityBpn: String,
        siteBpn: String?,
        taskEntryBpnMapping: TaskEntryBpnMapping
    ): String {
        val bpnAReference = additionalAddress.bpnReference
        val bpnA = taskEntryBpnMapping.getBpn(bpnAReference)

        val poolAddress = toPoolDto(additionalAddress)

        val addressBpn = if (bpnA == null) {
            createLogisticAddress(poolAddress, legalEntityBpn, siteBpn)
        }
        else {
            updateLogisticAddress(bpnA, poolAddress)
        }

        taskEntryBpnMapping.addMapping(bpnAReference, addressBpn)

        return addressBpn
    }

    private fun createLogisticAddress(
        poolAddress: LogisticAddressPoolDto,
        legalEntityBpn: String,
        siteBpn: String?
    ): String {
        val addressCreateRequest = AddressPartnerCreateRequest(
            bpnParent = siteBpn ?: legalEntityBpn,
            index = "",
            address = poolAddress
        )
        val result = businessPartnerBuildService.createAddresses(listOf(addressCreateRequest))

        if(result.errors.isNotEmpty())
            throw BpdmValidationException("Errors on creating Address: ${result.errors.joinToString()}")

        val addressResult = result.entities.firstOrNull() ?: throw BpdmValidationException("Unknown error when trying to creating address")

        return addressResult.address.bpna
    }

    private fun updateLogisticAddress(
        bpnA: String,
        poolAddress: LogisticAddressPoolDto,
    ): String {
        val addressUpdateRequest = AddressPartnerUpdateRequest(
            bpna = bpnA,
            address =  poolAddress
        )
        val result = businessPartnerBuildService.updateAddresses(listOf(addressUpdateRequest))

        if(result.errors.isNotEmpty())
            throw BpdmValidationException("Errors on updating Address: ${result.errors.joinToString()}")

        val addressResult = result.entities.firstOrNull() ?: throw BpdmValidationException("Unknown error when trying to updating address")

        return addressResult.bpna
    }

    private fun toPoolDto(legalEntity: LegalEntity) =
        with(legalEntity) {
            LegalEntityPoolDto(
                legalName = legalName ?: throw BpdmValidationException(CleaningError.LEGAL_NAME_IS_NULL.message),
                legalShortName = legalShortName,
                legalForm = legalForm,
                identifiers = identifiers.map { assertNotNull(it).let { LegalEntityIdentifierDto(it.value!!, it.type!!, it.issuingBody) } },
                states = states.map { assertNotNull(it).let { LegalEntityStateDto(it.validFrom.toLocalDateTime(), it.validTo.toLocalDateTime(), it.type!!) }   },
                confidenceCriteria = toPoolDto(confidenceCriteria, CleaningError.LEGAL_ENTITY_CONFIDENCE_CRITERIA_MISSING),
                isCatenaXMemberData = isCatenaXMemberData ?: false
            )
        }

    private fun toPoolDto(site: Site, siteMainAddress: PostalAddress) =
        with(site) {
            SitePoolDto(
                name = site.siteName ?: throw BpdmValidationException(CleaningError.SITE_NAME_MISSING.message),
                states = states.map { assertNotNull(it).let { SiteStateDto(it.validFrom.toLocalDateTime(), it.validTo.toLocalDateTime(), it.type!!) }},
                mainAddress = toPoolDto(siteMainAddress),
                confidenceCriteria = toPoolDto(confidenceCriteria, CleaningError.SITE_CONFIDENCE_CRITERIA_MISSING)
            )
        }


    private fun toPoolDto(logisticAddress: PostalAddress) =
        with(logisticAddress) {
            LogisticAddressPoolDto(
                name = addressName,
                states = states.map { assertNotNull(it).let {  AddressStateDto(it.validFrom.toLocalDateTime(), it.validTo.toLocalDateTime(), it.type!!)} },
                identifiers = identifiers.map { assertNotNull(it).let { AddressIdentifierDto(it.value!!, it.type!!) } },
                physicalPostalAddress = toPoolDto(physicalAddress),
                alternativePostalAddress = alternativeAddress?.let { toPoolDto(it) },
                confidenceCriteria = toPoolDto(confidenceCriteria, CleaningError.ADDRESS_CONFIDENCE_CRITERIA_MISSING)
            )
        }

    private fun toPoolDto(physicalPostalAddressDto: PhysicalAddress) =
        with(physicalPostalAddressDto) {
            PhysicalPostalAddressPoolDto(
                geographicCoordinates = with(geographicCoordinates) { longitude?.let { lon -> latitude?.let { lat ->  GeoCoordinateDto(lon, lat, altitude) } } },
                country =  country?.let { toCountryCode(it) } ?: throw BpdmValidationException(CleaningError.PHYSICAL_ADDRESS_COUNTRY_MISSING.message),
                administrativeAreaLevel1 = administrativeAreaLevel1,
                administrativeAreaLevel2 = administrativeAreaLevel2,
                administrativeAreaLevel3 = administrativeAreaLevel3,
                postalCode = postalCode,
                city = city ?: throw BpdmValidationException(CleaningError.PHYSICAL_ADDRESS_CITY_MISSING.message),
                district = district,
                companyPostalCode = companyPostalCode,
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door,
                street = with(street) {
                        StreetDto(
                            name,
                            houseNumber,
                            houseNumberSupplement,
                            milestone,
                            direction,
                            namePrefix,
                            additionalNamePrefix,
                            nameSuffix,
                            additionalNameSuffix
                        )
                },
                taxJurisdictionCode = taxJurisdictionCode
            )
        }

    private fun toPoolDto(alternativeAddress: AlternativeAddress) =
        with(alternativeAddress) {
            AlternativePostalAddressPoolDto(
                geographicCoordinates = with(geographicCoordinates) { longitude?.let { lon -> latitude?.let { lat ->  GeoCoordinateDto(lon, lat, altitude) } } },
                country = country?.let { toCountryCode(it) } ?: throw BpdmValidationException(CleaningError.ALTERNATIVE_ADDRESS_COUNTRY_MISSING.message),
                administrativeAreaLevel1 = administrativeAreaLevel1,
                postalCode = postalCode,
                city = city ?: throw BpdmValidationException(CleaningError.ALTERNATIVE_ADDRESS_CITY_MISSING.message),
                deliveryServiceType = deliveryServiceType
                    ?: throw BpdmValidationException(CleaningError.ALTERNATIVE_ADDRESS_DELIVERY_SERVICE_TYPE_MISSING.message),
                deliveryServiceQualifier = deliveryServiceQualifier,
                deliveryServiceNumber = deliveryServiceNumber
                    ?: throw BpdmValidationException(CleaningError.ALTERNATIVE_ADDRESS_DELIVERY_SERVICE_NUMBER_MISSING.message),
            )
        }

    private fun toPoolDto(confidenceCriteria: ConfidenceCriteria, cleaningError: CleaningError) =
        with(confidenceCriteria) {
            ConfidenceCriteriaPoolDto(
                sharedByOwner ?:  throw BpdmValidationException(cleaningError.message),
                checkedByExternalDataSource ?:  throw BpdmValidationException(cleaningError.message),
                numberOfSharingMembers ?:  throw BpdmValidationException(cleaningError.message),
                lastConfidenceCheckAt?.atZone(ZoneOffset.UTC)?.toLocalDateTime() ?:  throw BpdmValidationException(cleaningError.message),
                nextConfidenceCheckAt?.atZone(ZoneOffset.UTC)?.toLocalDateTime() ?:  throw BpdmValidationException(cleaningError.message),
                confidenceLevel ?:  throw BpdmValidationException(cleaningError.message)
            )
        }


    private fun assertNotNull(identifier: Identifier): Identifier {
        identifier.value ?: throw BpdmValidationException("Identifier value is null")
        identifier.type ?: throw BpdmValidationException("Identifier type is null")

        return identifier
    }

    private fun assertNotNull(state: BusinessState): BusinessState {
        state.type ?: throw BpdmValidationException("Business Partner state type is null")
        return state
    }

    private fun toBpnReference(bpn: String) = BpnReference(bpn, null, BpnReferenceType.Bpn)

    private fun Instant?.toLocalDateTime() =
        this?.atZone(ZoneOffset.UTC)?.toLocalDateTime()

    private fun toCountryCode(code: String): CountryCode{
        try{
            return CountryCode.getByAlpha2Code(code)
        }catch (e: IllegalArgumentException){
            throw BpdmValidationException("Country Code not recognized")
        }
    }

    data class LegalEntityBpns(
        val legalEntityBpn: String,
        val legalAddressBpn: String,
        val isCatenaXMember: Boolean
    )

    data class SiteBpns(
        val siteBpn: String,
        val mainAddressBpn: String
    )
}