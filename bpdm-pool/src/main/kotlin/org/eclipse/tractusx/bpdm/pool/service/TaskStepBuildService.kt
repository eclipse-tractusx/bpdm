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
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
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

@Service
class TaskStepBuildService(
    private val metadataService: MetadataService,
    private val bpnIssuingService: BpnIssuingService,
    private val changelogService: PartnerChangelogService,
    private val legalEntityRepository: LegalEntityRepository,
    private val logisticAddressRepository: LogisticAddressRepository,
    private val siteRepository: SiteRepository,
    private val businessPartnerEquivalenceService: BusinessPartnerEquivalenceService,
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
        INVALID_SITE_BPN("Invalid site BPN"),
        CLASSIFICATION_TYPE_IS_NULL("Classification type is null")
    }

    @Transactional
    fun upsertBusinessPartner(taskEntry: TaskStepReservationEntryDto, taskEntryBpnMapping: TaskEntryBpnMapping): TaskStepResultEntryDto {

        val businessPartnerDto = taskEntry.businessPartner
        var siteResult: SiteDto? = null
        var addressResult: LogisticAddressDto? = null

        val legalEntity = upsertLegalEntity(businessPartnerDto.legalEntity, taskEntryBpnMapping)
        var genericBpnA = legalEntity.legalAddress.bpn

        var siteEntity: Site? = null
        if (businessPartnerDto.site != null) {
            siteEntity = upsertSite(businessPartnerDto.site, legalEntity, taskEntryBpnMapping, businessPartnerDto)
            siteResult = businessPartnerDto.site!!.copy(
                bpnSReference = BpnReferenceDto(referenceValue = siteEntity.bpn, referenceType = BpnReferenceType.Bpn),
                mainAddress = businessPartnerDto.site!!.mainAddress!!.copy(
                    bpnAReference = BpnReferenceDto(referenceValue = siteEntity.mainAddress.bpn, referenceType = BpnReferenceType.Bpn)
                )
            )
            genericBpnA = siteEntity.mainAddress.bpn
        }
        val addressEntity: LogisticAddress?
        if (businessPartnerDto.address != null) {
            addressEntity = upsertLogisticAddress(businessPartnerDto.address, legalEntity, siteEntity, taskEntryBpnMapping)
            addressResult = businessPartnerDto.address!!.copy(
                bpnAReference = BpnReferenceDto(referenceValue = addressEntity.bpn, referenceType = BpnReferenceType.Bpn)
            )
            genericBpnA = addressEntity.bpn
        }

        return TaskStepResultEntryDto(
            taskId = taskEntry.taskId,
            businessPartner = BusinessPartnerFullDto(
                generic = with(businessPartnerDto.generic) {
                    copy(
                        legalEntity = this.legalEntity.copy(legalEntityBpn = legalEntity.bpn),
                        site = this.site.copy(siteBpn = siteEntity?.bpn),
                        address = this.address.copy(addressBpn = genericBpnA)
                    )
                },
                legalEntity = businessPartnerDto.legalEntity!!.copy(
                    bpnLReference = BpnReferenceDto(referenceValue = legalEntity.bpn, referenceType = BpnReferenceType.Bpn),
                    legalAddress = businessPartnerDto.legalEntity!!.legalAddress!!.copy(
                        bpnAReference = BpnReferenceDto(referenceValue = legalEntity.legalAddress.bpn, referenceType = BpnReferenceType.Bpn)
                    )
                ),
                site = siteResult,
                address = addressResult
            )
        )
    }

    private fun upsertLogisticAddress(
        addressDto: LogisticAddressDto?,
        legalEntity: LegalEntity,
        siteEntity: Site?,
        taskEntryBpnMapping: TaskEntryBpnMapping
    ): LogisticAddress {
        val bpnAReference = addressDto?.bpnAReference ?: throw BpdmValidationException(CleaningError.BPNA_IS_NULL.message)

        val bpn = taskEntryBpnMapping.getBpn(bpnAReference)
        var hasChanges = false
        val upsertAddress = if (bpn == null) {
            val bpnA = bpnIssuingService.issueAddressBpns(1).single()
            taskEntryBpnMapping.addMapping(bpnAReference, bpnA)
            createLogisticAddressInternal(addressDto, bpnA)
        } else {
            val addressMetadataMap = metadataService.getMetadata(listOf(addressDto)).toMapping()
            val updateAddress = logisticAddressRepository.findByBpn(bpn)

            if (updateAddress != null) {
                val newAddress = createLogisticAddressInternal(addressDto, updateAddress.bpn)
                updateLogisticAddress(newAddress, addressDto, addressMetadataMap)
                hasChanges = businessPartnerEquivalenceService.isEquivalent(updateAddress, newAddress)
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

        if (hasChanges || bpn == null) {
            logisticAddressRepository.save(upsertAddress)
        }


        val changelogType =
            if (bpn == null) ChangelogType.CREATE else ChangelogType.UPDATE
        if (hasChanges || bpn == null) {
            changelogService.createChangelogEntries(
                listOf(
                    ChangelogEntryCreateRequest(upsertAddress.bpn, changelogType, BusinessPartnerType.ADDRESS)
                )
            )
        }


        return upsertAddress
    }

    private fun createLogisticAddress(addressDto: LogisticAddressDto?, taskEntryBpnMapping: TaskEntryBpnMapping): LogisticAddress {

        val bpnAReference = addressDto?.bpnAReference
        val bpnA = bpnIssuingService.issueAddressBpns(1).single()

        if (bpnAReference != null && bpnAReference.referenceType == BpnReferenceType.BpnRequestIdentifier) {
            taskEntryBpnMapping.addMapping(bpnAReference, bpnA)
        }
        val newAddress = createLogisticAddressInternal(addressDto, bpnA)
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
            physicalPostalAddress = BusinessPartnerBuildService.createPhysicalAddress(dto.physicalPostalAddress!!, addressMetadataMap.regions),
            alternativePostalAddress = dto.alternativePostalAddress?.let {
                BusinessPartnerBuildService.createAlternativeAddress(
                    it,
                    addressMetadataMap.regions
                )
            },
            name = dto.name,
            confidenceCriteria = BusinessPartnerBuildService.createConfidenceCriteria(dto.confidenceCriteria!!)
        )
        updateAddressIdentifiersAndStates(address, dto, addressMetadataMap.idTypes)

        return address
    }

    private fun updateLogisticAddress(address: LogisticAddress, dto: LogisticAddressDto, metadataMap: BusinessPartnerBuildService.AddressMetadataMapping) {

        if (dto.physicalPostalAddress == null) {

            throw BpdmValidationException(CleaningError.PHYSICAL_ADDRESS_IS_NULL.message)
        }

        address.name = dto.name
        address.physicalPostalAddress = BusinessPartnerBuildService.createPhysicalAddress(dto.physicalPostalAddress!!, metadataMap.regions)
        address.alternativePostalAddress = dto.alternativePostalAddress?.let { BusinessPartnerBuildService.createAlternativeAddress(it, metadataMap.regions) }
        address.confidenceCriteria = BusinessPartnerBuildService.createConfidenceCriteria(dto.confidenceCriteria!!)

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
                    validFrom = it.validFrom,
                    validTo = it.validTo,
                    type = it.type,
                    address = address
                )
            })
        }
    }

    fun upsertLegalEntity(
        legalEntityDto: LegalEntityDto?, taskEntryBpnMapping: TaskEntryBpnMapping
    ): LegalEntity {

        val bpnLReference = legalEntityDto?.bpnLReference ?: throw BpdmValidationException(CleaningError.LEGAL_ENTITY_IS_NULL.message)
        val legalAddress = legalEntityDto.legalAddress ?: throw BpdmValidationException(CleaningError.LEGAL_ADDRESS_IS_NULL.message)
        val legalEntityMetadataMap = metadataService.getMetadata(listOf(legalEntityDto)).toMapping()

        val bpn = taskEntryBpnMapping.getBpn(bpnLReference)
        var hasChanges = false
        val upsertLe = if (bpn == null) {
            val bpnL = bpnIssuingService.issueLegalEntityBpns(1).single()
            taskEntryBpnMapping.addMapping(bpnLReference, bpnL)
            val createdLe = BusinessPartnerBuildService.createLegalEntity(legalEntityDto, bpnL, legalEntityDto.legalName, legalEntityMetadataMap)
            val address = createLogisticAddress(legalAddress, taskEntryBpnMapping)
            createdLe.legalAddress = address
            address.legalEntity = createdLe
            createdLe
        } else {

            val updateLe = legalEntityRepository.findByBpn(bpn)
            if (updateLe != null) {
                if (legalEntityDto.hasChanged == true) {

                    val createdLe =
                        BusinessPartnerBuildService.createLegalEntity(legalEntityDto, updateLe.bpn, legalEntityDto.legalName, legalEntityMetadataMap)
                    val address = createLogisticAddress(legalAddress, taskEntryBpnMapping)
                    createdLe.legalAddress = address
                    address.legalEntity = createdLe
                    hasChanges = businessPartnerEquivalenceService.isEquivalent(updateLe, createdLe)

                    BusinessPartnerBuildService.updateLegalEntity(updateLe, legalEntityDto, legalEntityDto.legalName, legalEntityMetadataMap)
                    val addressMetadataMap = metadataService.getMetadata(listOf(legalAddress)).toMapping()
                    updateLogisticAddress(updateLe.legalAddress, legalAddress, addressMetadataMap)
                }
            } else {
                throw BpdmValidationException(CleaningError.INVALID_LEGAL_ENTITY_BPN.message)
            }
            updateLe
        }
        val changelogType =
            if (bpn == null) ChangelogType.CREATE else ChangelogType.UPDATE

        if (hasChanges || bpn == null) {
            legalEntityRepository.save(upsertLe)
            changelogService.createChangelogEntries(
                listOf(
                    ChangelogEntryCreateRequest(upsertLe.bpn, changelogType, BusinessPartnerType.LEGAL_ENTITY)
                )
            )
        }
        return upsertLe
    }

    private fun upsertSite(
        siteDto: SiteDto?,
        legalEntity: LegalEntity,
        taskEntryBpnMapping: TaskEntryBpnMapping,
        genericBusinessPartner: BusinessPartnerFullDto
    ): Site {

        val bpnSReference = siteDto?.bpnSReference ?: throw BpdmValidationException(CleaningError.BPNS_IS_NULL.message)
        val mainAddress = siteDto.mainAddress ?: throw BpdmValidationException(CleaningError.MAINE_ADDRESS_IS_NULL.message)

        val bpn = taskEntryBpnMapping.getBpn(bpnSReference)
        val changelogType = if (bpn == null) ChangelogType.CREATE else ChangelogType.UPDATE
        var hasChanges = false
        val upsertSite = if (bpn == null) {
            val bpnS = bpnIssuingService.issueSiteBpns(1).single()
            val createSite = BusinessPartnerBuildService.createSite(siteDto, bpnS, legalEntity)
            taskEntryBpnMapping.addMapping(bpnSReference, bpnS)

            val siteMainAddress =
                if (genericBusinessPartner.generic.address.addressType == AddressType.LegalAndSiteMainAddress)
                    legalEntity.legalAddress
                else
                    createLogisticAddress(mainAddress, taskEntryBpnMapping)

            createSite.mainAddress = siteMainAddress
            siteMainAddress.site = createSite
            createSite

        } else {

            val updateSite = siteRepository.findByBpn(bpn)
            if (updateSite != null) {
                if (siteDto.hasChanged == true) {
                    val createSite = BusinessPartnerBuildService.createSite(siteDto, updateSite.bpn, legalEntity)
                    val siteMainAddress =
                        if (genericBusinessPartner.generic.address.addressType == AddressType.LegalAndSiteMainAddress)
                            legalEntity.legalAddress
                        else
                            createLogisticAddress(mainAddress, taskEntryBpnMapping)
                    createSite.mainAddress = siteMainAddress
                    siteMainAddress.site = createSite
                    hasChanges = businessPartnerEquivalenceService.isEquivalent(updateSite, createSite)

                    BusinessPartnerBuildService.updateSite(updateSite, siteDto)
                    val addressMetadataMap = metadataService.getMetadata(listOf(mainAddress)).toMapping()
                    updateLogisticAddress(updateSite.mainAddress, mainAddress, addressMetadataMap)
                }
            } else {
                throw BpdmValidationException(CleaningError.INVALID_SITE_BPN.message)
            }
            updateSite
        }

        if (hasChanges || bpn == null) {
            siteRepository.save(upsertSite)
            changelogService.createChangelogEntries(
                listOf(
                    ChangelogEntryCreateRequest(upsertSite.bpn, changelogType, BusinessPartnerType.SITE)
                )
            )
        }

        return upsertSite
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