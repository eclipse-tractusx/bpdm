/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

import jakarta.transaction.Transactional
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.dto.AddressMetadataDto
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.dto.LegalEntityMetadataDto
import org.eclipse.tractusx.bpdm.pool.entity.*
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class TaskStepBuildService(
    private val metadataService: MetadataService,
    private val bpnIssuingService: BpnIssuingService,
    private val changelogService: PartnerChangelogService,
    private val legalEntityRepository: LegalEntityRepository,
    private val logisticAddressRepository: LogisticAddressRepository,
    private val siteRepository: SiteRepository,
) {

    enum class CleaningError(val message: String) {
        LEGAL_NAME_IS_NULL("Legal name is null"),
        COUNTRY_CITY_IS_NULL("Country or city in physicalAddress is null"),
        LEGAL_ENTITY_IS_NULL("Legal entity or BpnL Reference is null"),
        LEGAL_ADDRESS_IS_NULL("Legal Address is null"),
        LOGISTIC_ADDRESS_IS_NULL("Logistic Address or Physical Address is null"),
        PHYSICAL_ADDRESS_IS_NULL("Physical Address is null"),
        ALTERNATIVE_ADDRESS_DATA_IS_NULL("Country or city or deliveryServiceType or deliveryServiceNumber in alternativeAddress is null"),
        MAINE_ADDRESS_IS_NULL("Main address is null"),
        BPNS_IS_NULL("BpnS Reference is null"),
        BPNA_IS_NULL("BpnA Reference is null"),
        SITE_NAME_IS_NULL("Site name is null"),
        INVALID_LOGISTIC_ADDRESS_BPN("Invalid Logistic Address BPN"),
        INVALID_LEGAL_ENTITY_BPN("Invalid legal entity BPN"),
        INVALID_SITE_BPN("Invalid site BPN")

    }

    @Transactional
    fun upsertBusinessPartner(taskEntry: TaskStepReservationEntryDto): TaskStepResultEntryDto {

        // TODO associate generated BPN with BPN request identifier
        val businessPartnerDto = taskEntry.businessPartner
        var siteResult: SiteDto? = null
        var addressResult: LogisticAddressDto? = null

        val legalEntity = upsertLegalEntity(businessPartnerDto.legalEntity)
        var siteEntity: Site? = null
        if (businessPartnerDto.site != null) {
            siteEntity = upsertSite(businessPartnerDto.site, legalEntity)
            siteResult = businessPartnerDto.site!!.copy(
                bpnSReference = BpnReferenceDto(referenceValue = siteEntity.bpn, referenceType = BpnReferenceType.Bpn)
            )
        }
        if (businessPartnerDto.address != null) {
            val addressEntity = upsertLogisticAddress(businessPartnerDto.address, legalEntity, siteEntity)
            addressResult = businessPartnerDto.address!!.copy(
                bpnAReference = BpnReferenceDto(referenceValue = addressEntity.bpn, referenceType = BpnReferenceType.Bpn)
            )
        }

        return TaskStepResultEntryDto(
            taskId = taskEntry.taskId,
            businessPartner = BusinessPartnerFullDto(
                generic = businessPartnerDto.generic,
                legalEntity = businessPartnerDto.legalEntity!!.copy(
                    bpnLReference = BpnReferenceDto(referenceValue = legalEntity.bpn, referenceType = BpnReferenceType.Bpn)
                ),
                site = siteResult,
                address = addressResult
            )
        )
    }

    private fun upsertLogisticAddress(
        addressDto: LogisticAddressDto?,
        legalEntity: LegalEntity,
        siteEntity: Site?
    ): LogisticAddress {

        val bpnAReference = addressDto?.bpnAReference ?: throw BpdmValidationException(CleaningError.BPNA_IS_NULL.message)

        val isCreate = bpnAReference.referenceType == BpnReferenceType.BpnRequestIdentifier
        val changelogType =
            if (isCreate) ChangelogType.CREATE else ChangelogType.UPDATE

        val upsertAddress = if (isCreate) {
            val bpnLA = bpnIssuingService.issueAddressBpns(1).single()
            createLogisticAddressInternal(addressDto, bpnLA)
        } else {
            val addressMetadataMap = metadataService.getMetadata(listOf(addressDto)).toMapping()

            val updateAddress = logisticAddressRepository.findByBpn(bpnAReference.referenceValue)
            if (updateAddress != null) {
                updateLogisticAddress(updateAddress, addressDto, addressMetadataMap)
            } else {
                throw BpdmValidationException(CleaningError.INVALID_LOGISTIC_ADDRESS_BPN.message)
            }
            updateAddress
        }
        if (siteEntity != null) {
            upsertAddress.site = siteEntity
        } else {
            upsertAddress.legalEntity = legalEntity
        }
        logisticAddressRepository.save(upsertAddress)
        changelogService.createChangelogEntries(
            listOf(
                ChangelogEntryCreateRequest(upsertAddress.bpn, changelogType, BusinessPartnerType.ADDRESS)
            )
        )

        return upsertAddress
    }

    private fun createLogisticAddress(
        addressDto: LogisticAddressDto?
    ): LogisticAddress {

        val bpnLA = bpnIssuingService.issueAddressBpns(1)
        val newAddress = createLogisticAddressInternal(addressDto, bpnLA[0])
        changelogService.createChangelogEntries(
            listOf(
                ChangelogEntryCreateRequest(newAddress.bpn, ChangelogType.CREATE, BusinessPartnerType.ADDRESS)
            )
        )

        return newAddress
    }

    private fun createLogisticAddressInternal(
        dto: LogisticAddressDto?,
        bpn: String
    ): LogisticAddress {

        if (dto?.physicalPostalAddress == null) {
            throw BpdmValidationException(CleaningError.LOGISTIC_ADDRESS_IS_NULL.message)
        }

        val addressMetadataMap = metadataService.getMetadata(listOf(dto)).toMapping()
        val address = LogisticAddress(
            bpn = bpn,
            legalEntity = null,
            site = null,
            physicalPostalAddress = createPhysicalAddress(dto.physicalPostalAddress!!, addressMetadataMap.regions),
            alternativePostalAddress = dto.alternativePostalAddress?.let { createAlternativeAddress(it, addressMetadataMap.regions) },
            name = dto.name
        )
        updateAddressIdentifiersAndStates(address, dto, addressMetadataMap.idTypes)

        return address
    }

    private fun updateLogisticAddress(address: LogisticAddress, dto: LogisticAddressDto, metadataMap: BusinessPartnerBuildService.AddressMetadataMapping) {

        if (dto.physicalPostalAddress == null) {

            throw BpdmValidationException(CleaningError.PHYSICAL_ADDRESS_IS_NULL.message)
        }

        address.name = dto.name
        address.physicalPostalAddress = createPhysicalAddress(dto.physicalPostalAddress!!, metadataMap.regions)
        address.alternativePostalAddress = dto.alternativePostalAddress?.let { createAlternativeAddress(it, metadataMap.regions) }

        updateAddressIdentifiersAndStates(address, dto, metadataMap.idTypes)
    }

    private fun updateAddressIdentifiersAndStates(
        address: LogisticAddress,
        dto: LogisticAddressDto,
        idTypes: Map<String, IdentifierType>
    ) {
        address.identifiers.apply {
            clear()
            addAll(dto.identifiers.map {
                AddressIdentifier(
                    value = it.value,
                    type = idTypes[it.type]!!,
                    address = address
                )
            })
        }
        address.states.apply {
            clear()
            addAll(dto.states.map {
                AddressState(
                    description = it.description,
                    validFrom = it.validFrom,
                    validTo = it.validTo,
                    type = it.type,
                    address = address
                )
            })
        }
    }

    private fun createAlternativeAddress(alternativeAddress: AlternativePostalAddressDto, regions: Map<String, Region>): AlternativePostalAddress {

        if (alternativeAddress.country == null || alternativeAddress.city == null ||
            alternativeAddress.deliveryServiceType == null || alternativeAddress.deliveryServiceNumber == null
        ) {

            throw BpdmValidationException(CleaningError.ALTERNATIVE_ADDRESS_DATA_IS_NULL.message)
        }

        return AlternativePostalAddress(
            geographicCoordinates = alternativeAddress.geographicCoordinates?.let { GeographicCoordinate(it.latitude, it.longitude, it.altitude) },
            country = alternativeAddress.country!!,
            administrativeAreaLevel1 = regions[alternativeAddress.administrativeAreaLevel1],
            postCode = alternativeAddress.postalCode,
            city = alternativeAddress.city!!,
            deliveryServiceType = alternativeAddress.deliveryServiceType!!,
            deliveryServiceNumber = alternativeAddress.deliveryServiceNumber!!,
            deliveryServiceQualifier = alternativeAddress.deliveryServiceQualifier
        )
    }

    private fun createPhysicalAddress(physicalAddress: PhysicalPostalAddressDto, regions: Map<String, Region>): PhysicalPostalAddress {

        if (physicalAddress.country == null || physicalAddress.city == null) {
            throw BpdmValidationException(CleaningError.COUNTRY_CITY_IS_NULL.message)
        }

        return PhysicalPostalAddress(
            geographicCoordinates = physicalAddress.geographicCoordinates?.let { GeographicCoordinate(it.latitude, it.longitude, it.altitude) },
            country = physicalAddress.country!!,
            administrativeAreaLevel1 = regions[physicalAddress.administrativeAreaLevel1],
            administrativeAreaLevel2 = physicalAddress.administrativeAreaLevel2,
            administrativeAreaLevel3 = physicalAddress.administrativeAreaLevel3,
            postCode = physicalAddress.postalCode,
            city = physicalAddress.city!!,
            districtLevel1 = physicalAddress.district,
            street = physicalAddress.street?.let {
                Street(
                    name = it.name,
                    houseNumber = it.houseNumber,
                    milestone = it.milestone,
                    direction = it.direction
                )
            },
            companyPostCode = physicalAddress.companyPostalCode,
            industrialZone = physicalAddress.industrialZone,
            building = physicalAddress.building,
            floor = physicalAddress.floor,
            door = physicalAddress.door
        )
    }

    private fun createLegalEntity(
        legalEntityDto: LegalEntityDto,
        bpnL: String,
        metadataMap: BusinessPartnerBuildService.LegalEntityMetadataMapping
    ): LegalEntity {

        if (legalEntityDto.legalName == null) {
            throw BpdmValidationException(CleaningError.LEGAL_NAME_IS_NULL.message)
        }

        // it has to be validated that the legalForm exits
        val legalForm = legalEntityDto.legalForm?.let { metadataMap.legalForms[it]!! }
        val legalName = Name(value = legalEntityDto.legalName!!, shortName = legalEntityDto.legalShortName)
        val newLegalEntity = LegalEntity(
            bpn = bpnL,
            legalName = legalName,
            legalForm = legalForm,
            currentness = Instant.now().truncatedTo(ChronoUnit.MICROS),
        )
        updateLegalEntity(newLegalEntity, legalEntityDto,
            legalEntityDto.identifiers.map { BusinessPartnerBuildService.toLegalEntityIdentifier(it, metadataMap.idTypes, newLegalEntity) })

        return newLegalEntity
    }

    private fun updateLegalEntity(
        legalEntity: LegalEntity,
        legalEntityDto: LegalEntityDto,
        identifiers: List<LegalEntityIdentifier>
    ) {
        val legalName = legalEntityDto.legalName ?: throw BpdmValidationException(CleaningError.LEGAL_NAME_IS_NULL.message)

        legalEntity.currentness = BusinessPartnerBuildService.createCurrentnessTimestamp()

        legalEntity.legalName = Name(value = legalName, shortName = legalEntityDto.legalShortName)

        legalEntity.identifiers.replace(identifiers)

        legalEntity.states.replace(legalEntityDto.states
            .map { BusinessPartnerBuildService.toLegalEntityState(it, legalEntity) })

        legalEntity.classifications.replace(
            legalEntityDto.classifications
                .map { BusinessPartnerBuildService.toLegalEntityClassification(it, legalEntity) }.toSet()
        )
    }


    fun upsertLegalEntity(
        legalEntityDto: LegalEntityDto?
    ): LegalEntity {

        val bpnLReference = legalEntityDto?.bpnLReference ?: throw BpdmValidationException(CleaningError.LEGAL_ENTITY_IS_NULL.message)
        val legalAddress = legalEntityDto.legalAddress ?: throw BpdmValidationException(CleaningError.LEGAL_ADDRESS_IS_NULL.message)

        val isCreate = bpnLReference.referenceType == BpnReferenceType.BpnRequestIdentifier
        val changelogType =
            if (isCreate) ChangelogType.CREATE else ChangelogType.UPDATE

        val legalEntityMetadataMap = metadataService.getMetadata(listOf(legalEntityDto)).toMapping()

        val upsertLe = if (isCreate) {
            val bpnL = bpnIssuingService.issueLegalEntityBpns(1).single()
            val createdLe = createLegalEntity(legalEntityDto, bpnL, legalEntityMetadataMap)
            val address = createLogisticAddress(legalAddress)
            createdLe.legalAddress = address
            address.legalEntity = createdLe
            createdLe
        } else {

            val updateLe = legalEntityRepository.findByBpn(bpnLReference.referenceValue)
            if (updateLe != null) {
                if (legalEntityDto.hasChanged == false) {
                    updateLegalEntity(updateLe, legalEntityDto,
                        legalEntityDto.identifiers.map { BusinessPartnerBuildService.toLegalEntityIdentifier(it, legalEntityMetadataMap.idTypes, updateLe) })
                    val addressMetadataMap = metadataService.getMetadata(listOf(legalAddress)).toMapping()
                    updateLogisticAddress(updateLe.legalAddress, legalAddress, addressMetadataMap)
                }
            } else {
                throw BpdmValidationException(CleaningError.INVALID_LEGAL_ENTITY_BPN.message)
            }
            updateLe
        }
        legalEntityRepository.save(upsertLe)
        changelogService.createChangelogEntries(
            listOf(
                ChangelogEntryCreateRequest(upsertLe.bpn, changelogType, BusinessPartnerType.LEGAL_ENTITY)
            )
        )

        return upsertLe
    }

    private fun upsertSite(
        siteDto: SiteDto?,
        legalEntity: LegalEntity
    ): Site {
        val bpnSReference = siteDto?.bpnSReference ?: throw BpdmValidationException(CleaningError.BPNS_IS_NULL.message)
        val mainAddress = siteDto.mainAddress ?: throw BpdmValidationException(CleaningError.MAINE_ADDRESS_IS_NULL.message)

        val isCreate = bpnSReference.referenceType == BpnReferenceType.BpnRequestIdentifier
        val changelogType = if (isCreate) ChangelogType.CREATE else ChangelogType.UPDATE

        val upsertSite = if (isCreate) {
            val bpnS = bpnIssuingService.issueSiteBpns(1).single()
            val createSite = createSite(siteDto, bpnS, legalEntity)
            val address = createLogisticAddress(mainAddress)
            createSite.mainAddress = address
            address.site = createSite
            createSite
        } else {

            val updateSite = siteRepository.findByBpn(siteDto.bpnSReference?.referenceValue!!)
            if (updateSite != null) {
                if (siteDto.hasChanged == false) {
                    updateSite(updateSite, siteDto)
                    val addressMetadataMap = metadataService.getMetadata(listOf(mainAddress)).toMapping()
                    updateLogisticAddress(updateSite.mainAddress, mainAddress, addressMetadataMap)
                }
            } else {
                throw BpdmValidationException(CleaningError.INVALID_SITE_BPN.message)
            }
            updateSite
        }
        siteRepository.save(upsertSite)
        changelogService.createChangelogEntries(
            listOf(
                ChangelogEntryCreateRequest(upsertSite.bpn, changelogType, BusinessPartnerType.SITE)
            )
        )

        return upsertSite
    }

    private fun createSite(
        siteDto: SiteDto,
        bpnS: String,
        partner: LegalEntity
    ): Site {

        val name = siteDto.name ?: throw BpdmValidationException(CleaningError.SITE_NAME_IS_NULL.message)

        val site = Site(
            bpn = bpnS,
            name = name,
            legalEntity = partner,
        )

        site.states.addAll(siteDto.states
            .map { BusinessPartnerBuildService.toSiteState(it, site) })

        return site
    }

    private fun updateSite(site: Site, siteDto: SiteDto) {

        val name = siteDto.name ?: throw BpdmValidationException(CleaningError.SITE_NAME_IS_NULL.message)

        site.name = name

        site.states.clear()
        site.states.addAll(siteDto.states
            .map { BusinessPartnerBuildService.toSiteState(it, site) })
    }

    private fun AddressMetadataDto.toMapping() =
        BusinessPartnerBuildService.AddressMetadataMapping(
            idTypes = idTypes.associateBy { it.technicalKey },
            regions = regions.associateBy { it.regionCode }
        )

    private fun LegalEntityMetadataDto.toMapping() =
        BusinessPartnerBuildService.LegalEntityMetadataMapping(
            idTypes = idTypes.associateBy { it.technicalKey },
            legalForms = legalForms.associateBy { it.technicalKey }
        )
}