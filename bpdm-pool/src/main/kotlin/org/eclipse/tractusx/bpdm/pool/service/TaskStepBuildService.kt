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

import jakarta.transaction.Transactional
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.pool.api.model.AddressIdentifierDto
import org.eclipse.tractusx.bpdm.pool.api.model.AddressStateDto
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityIdentifierDto
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityStateDto
import org.eclipse.tractusx.bpdm.pool.api.model.SiteStateDto
import org.eclipse.tractusx.bpdm.pool.api.model.StreetDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.*
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorCode
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.orchestrator.api.model.*
import org.eclipse.tractusx.orchestrator.api.model.AlternativePostalAddressDto
import org.eclipse.tractusx.orchestrator.api.model.ConfidenceCriteriaDto
import org.eclipse.tractusx.orchestrator.api.model.LegalEntityDto
import org.eclipse.tractusx.orchestrator.api.model.LogisticAddressDto
import org.eclipse.tractusx.orchestrator.api.model.PhysicalPostalAddressDto
import org.eclipse.tractusx.orchestrator.api.model.SiteDto
import org.springframework.stereotype.Service
import org.eclipse.tractusx.bpdm.pool.api.model.AlternativePostalAddressDto as AlternativePostalAddressPoolDto
import org.eclipse.tractusx.bpdm.pool.api.model.ConfidenceCriteriaDto as ConfidenceCriteriaPoolDto
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityDto as LegalEntityPoolDto
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressDto as LogisticAddressPoolDto
import org.eclipse.tractusx.bpdm.pool.api.model.PhysicalPostalAddressDto as PhysicalPostalAddressPoolDto
import org.eclipse.tractusx.bpdm.pool.api.model.SiteDto as SitePoolDto


@Service
class TaskStepBuildService(
    private val businessPartnerBuildService: BusinessPartnerBuildService
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
    fun upsertBusinessPartner(taskEntry: TaskStepReservationEntryDto, taskEntryBpnMapping: TaskEntryBpnMapping): TaskStepResultEntryDto {
        val businessPartnerDto = taskEntry.businessPartner

        val legalEntityResult = upsertLegalEntity(businessPartnerDto.legalEntity, taskEntryBpnMapping)

        val siteResult = if (legalEntityResult.errors.isEmpty() && businessPartnerDto.site != null) {
            upsertSite(
                businessPartnerDto.site,
                legalEntityResult.legalEntityBpn!!,
                businessPartnerDto.legalEntity?.legalAddress?.bpnAReference,
                taskEntryBpnMapping
            )
        } else {
            null
        }

        val addressResult = if (legalEntityResult.errors.isEmpty() && siteResult?.errors?.isEmpty() != false && businessPartnerDto.address != null)
            upsertLogisticAddress(businessPartnerDto.address, legalEntityResult.legalEntityBpn!!, siteResult?.siteBpn, taskEntryBpnMapping)
        else
            null

        val taskErrors = legalEntityResult.errors
            .plus(siteResult?.errors ?: emptyList())
            .plus(addressResult?.errors ?: emptyList())
            .map { TaskErrorDto(TaskErrorType.Unspecified, it.message) }

        val bpna = addressResult?.addressBpn ?: siteResult?.mainAddressBpn ?: legalEntityResult.legalAddressBpn
        val genericWithBpn = with(businessPartnerDto.generic) {
            copy(
                legalEntity = this.legalEntity.copy(legalEntityBpn = legalEntityResult.legalEntityBpn),
                site = this.site.copy(siteBpn = siteResult?.siteBpn),
                address = this.address.copy(addressBpn = bpna)
            )
        }

        val legalEntityWithBpn = with(businessPartnerDto.legalEntity) {
            this?.let {
                copy(
                    bpnLReference = toBpnReference(legalEntityResult.legalEntityBpn),
                    legalAddress = legalAddress!!.copy(bpnAReference = toBpnReference(legalEntityResult.legalAddressBpn))
                )
            }
        }

        val siteWithBpn = with(businessPartnerDto.site) {
            this?.let {
                copy(
                    bpnSReference = toBpnReference(siteResult?.siteBpn),
                    mainAddress = mainAddress!!.copy(bpnAReference = toBpnReference(siteResult?.mainAddressBpn))
            )
            }
        }

        val addressWithBpn = with(businessPartnerDto.address) {
            this?.let {
                copy(bpnAReference = toBpnReference(addressResult?.addressBpn))
            }
        }

        return TaskStepResultEntryDto(
            taskId = taskEntry.taskId,
            businessPartner = BusinessPartnerFullDto(
                generic = genericWithBpn,
                legalEntity = legalEntityWithBpn,
                site = siteWithBpn,
                address = addressWithBpn
            ),
            errors = taskErrors
        )
    }


    fun upsertLegalEntity(
        legalEntityDto: LegalEntityDto?, taskEntryBpnMapping: TaskEntryBpnMapping
    ): LegalEntityUpsertResponse {
        val bpnLReference = legalEntityDto?.bpnLReference ?: throw BpdmValidationException(CleaningError.LEGAL_ENTITY_IS_NULL.message)

        val bpn = taskEntryBpnMapping.getBpn(bpnLReference)
        val bpnWithError = if (bpn == null) {
            createLegalEntity(legalEntityDto, taskEntryBpnMapping)
        } else {
            updateLegalEntity(bpn, legalEntityDto)
        }

        return bpnWithError
    }

    private fun createLegalEntity(
        legalEntityDto: LegalEntityDto, taskEntryBpnMapping: TaskEntryBpnMapping
    ): LegalEntityUpsertResponse {
        val legalAddress = legalEntityDto.legalAddress ?: throw BpdmValidationException(CleaningError.LEGAL_ADDRESS_IS_NULL.message)
        val bpnAReference =
            legalEntityDto.legalAddress?.bpnAReference ?: throw BpdmValidationException(CleaningError.LEGAL_ADDRESS_BPN_REFERENCE_MISSING.message)

        val createRequest = LegalEntityPartnerCreateRequest(
            legalEntity = toPoolDto(legalEntityDto),
            legalAddress = toPoolDto(legalAddress),
            index = ""
        )
        val result = businessPartnerBuildService.createLegalEntities(listOf(createRequest))
        val legalEntityResult = result.entities.firstOrNull()
        val bpnL = legalEntityResult?.legalEntity?.bpnl
        val legalAddressBpnA = legalEntityResult?.legalAddress?.bpna

        bpnL?.run { taskEntryBpnMapping.addMapping(legalEntityDto.bpnLReference!!, bpnL) }
        legalAddressBpnA?.run { taskEntryBpnMapping.addMapping(bpnAReference, legalAddressBpnA) }

        return LegalEntityUpsertResponse(bpnL, legalAddressBpnA, result.errors)
    }

    private fun updateLegalEntity(
        bpnL: String,
        legalEntityDto: LegalEntityDto
    ): LegalEntityUpsertResponse {
        val legalAddress = legalEntityDto.legalAddress ?: throw BpdmValidationException(CleaningError.LEGAL_ADDRESS_IS_NULL.message)

        val updateRequest = LegalEntityPartnerUpdateRequest(
            bpnl = bpnL,
            legalEntity = toPoolDto(legalEntityDto),
            legalAddress = toPoolDto(legalAddress)
        )
        val result = businessPartnerBuildService.updateLegalEntities(listOf(updateRequest))
        val legalEntityResult = result.entities.firstOrNull()
        return LegalEntityUpsertResponse(legalEntityResult?.legalEntity?.bpnl, legalEntityResult?.legalAddress?.bpna, result.errors)
    }

    private fun upsertSite(
        siteDto: SiteDto?,
        legalEntityBpn: String,
        legalAddressReference: BpnReferenceDto?,
        taskEntryBpnMapping: TaskEntryBpnMapping
    ): SiteUpsertResponse {
        val bpnSReference = siteDto?.bpnSReference ?: throw BpdmValidationException(CleaningError.BPNS_IS_NULL.message)
        val bpn = taskEntryBpnMapping.getBpn(bpnSReference)

        val upsertSite = if (bpn == null) {
            createSite(siteDto, legalEntityBpn, legalAddressReference, taskEntryBpnMapping)
        } else {
            updateSite(bpn, siteDto)
        }
        return upsertSite
    }

    private fun createSite(
        siteDto: SiteDto,
        legalEntityBpn: String,
        legalAddressReference: BpnReferenceDto?,
        taskEntryBpnMapping: TaskEntryBpnMapping
    ): SiteUpsertResponse {
        val mainAddressReference = siteDto.mainAddress?.bpnAReference ?: throw BpdmValidationException(CleaningError.MAIN_ADDRESS_BPN_REFERENCE_MISSING.message)

        val result = if (mainAddressReference.referenceValue == legalAddressReference?.referenceValue) {
            val createRequest = BusinessPartnerBuildService.SiteCreateRequestWithLegalAddressAsMain(
                name = siteDto.name ?: throw BpdmValidationException(CleaningError.SITE_NAME_MISSING.message),
                states = siteDto.states.map { SiteStateDto(it.validFrom, it.validTo, it.type) },
                confidenceCriteria = siteDto.confidenceCriteria?.let { toPoolDto(it) }
                    ?: throw BpdmValidationException(CleaningError.SITE_CONFIDENCE_CRITERIA_MISSING.message),
                bpnLParent = legalEntityBpn
            )
            businessPartnerBuildService.createSitesWithLegalAddressAsMain(listOf(createRequest))
        } else {
            val createRequest = SitePartnerCreateRequest(
                bpnlParent = legalEntityBpn,
                site = toPoolDto(siteDto),
                index = ""
            )
            businessPartnerBuildService.createSitesWithMainAddress(listOf(createRequest))
        }

        val siteResult = result.entities.firstOrNull()
        val bpnS = siteResult?.site?.bpns
        val mainAddressBpnA = siteResult?.mainAddress?.bpna

        bpnS?.run { taskEntryBpnMapping.addMapping(siteDto.bpnSReference!!, bpnS) }
        mainAddressBpnA?.run { taskEntryBpnMapping.addMapping(mainAddressReference, mainAddressBpnA) }

        return SiteUpsertResponse(bpnS, mainAddressBpnA, result.errors)
    }

    private fun updateSite(
        bpnS: String,
        siteDto: SiteDto
    ): SiteUpsertResponse {
        val updateRequest = SitePartnerUpdateRequest(
            bpns = bpnS,
            site = toPoolDto(siteDto)
        )
        val result = businessPartnerBuildService.updateSites(listOf(updateRequest))
        val siteResult = result.entities.firstOrNull()

        return SiteUpsertResponse(siteResult?.site?.bpns, siteResult?.mainAddress?.bpna, result.errors)
    }

    private fun upsertLogisticAddress(
        addressDto: LogisticAddressDto?,
        legalEntityBpn: String,
        siteBpn: String?,
        taskEntryBpnMapping: TaskEntryBpnMapping
    ): AddressUpsertResponse {
        val bpnAReference = addressDto?.bpnAReference ?: throw BpdmValidationException(CleaningError.BPNA_IS_NULL.message)
        val bpn = taskEntryBpnMapping.getBpn(bpnAReference)

        val upsertAddress = if (bpn == null) {
            createLogisticAddress(addressDto, legalEntityBpn, siteBpn, taskEntryBpnMapping)
        } else {
            updateLogisticAddress(bpn, addressDto)
        }

        return upsertAddress
    }

    private fun createLogisticAddress(
        addressDto: LogisticAddressDto,
        legalEntityBpn: String,
        siteBpn: String?,
        taskEntryBpnMapping: TaskEntryBpnMapping
    ): AddressUpsertResponse {
        val addressCreateRequest = AddressPartnerCreateRequest(
            bpnParent = siteBpn ?: legalEntityBpn,
            index = "",
            address = toPoolDto(addressDto)
        )
        val result = businessPartnerBuildService.createAddresses(listOf(addressCreateRequest))
        val bpna = result.entities.firstOrNull()?.address?.bpna
        bpna?.run { taskEntryBpnMapping.addMapping(addressDto.bpnAReference!!, bpna) }

        return AddressUpsertResponse(bpna, result.errors)
    }

    private fun updateLogisticAddress(
        bpnA: String,
        addressDto: LogisticAddressDto
    ): AddressUpsertResponse {
        val addressUpdateRequest = AddressPartnerUpdateRequest(
            bpna = bpnA,
            address = toPoolDto(addressDto)
        )
        val result = businessPartnerBuildService.updateAddresses(listOf(addressUpdateRequest))

        return AddressUpsertResponse(result.entities.firstOrNull()?.bpna, result.errors)
    }

    private fun toPoolDto(legalEntity: LegalEntityDto) =
        with(legalEntity) {
            LegalEntityPoolDto(
                legalName = legalName ?: throw BpdmValidationException(CleaningError.LEGAL_NAME_IS_NULL.message),
                legalShortName = legalShortName,
                legalForm = legalForm,
                identifiers = identifiers.map { LegalEntityIdentifierDto(it.value, it.type, it.issuingBody) },
                states = states.map { LegalEntityStateDto(it.validFrom, it.validTo, it.type) },
                confidenceCriteria = confidenceCriteria?.let { toPoolDto(it) }
                    ?: throw BpdmValidationException(CleaningError.LEGAL_ENTITY_CONFIDENCE_CRITERIA_MISSING.message),
            )
        }

    private fun toPoolDto(site: SiteDto) =
        with(site) {
            SitePoolDto(
                name = name ?: throw BpdmValidationException(CleaningError.SITE_NAME_MISSING.message),
                states = states.map { SiteStateDto(it.validFrom, it.validTo, it.type) },
                mainAddress = mainAddress?.let { toPoolDto(it) } ?: throw BpdmValidationException(CleaningError.MAINE_ADDRESS_IS_NULL.message),
                confidenceCriteria = confidenceCriteria?.let { toPoolDto(it) }
                    ?: throw BpdmValidationException(CleaningError.SITE_CONFIDENCE_CRITERIA_MISSING.message)
            )
        }


    private fun toPoolDto(logisticAddress: LogisticAddressDto) =
        with(logisticAddress) {
            LogisticAddressPoolDto(
                name = name,
                states = states.map { AddressStateDto(it.validFrom, it.validTo, it.type) },
                identifiers = identifiers.map { AddressIdentifierDto(it.value, it.type) },
                physicalPostalAddress = physicalPostalAddress?.let { toPoolDto(it) }
                    ?: throw BpdmValidationException(CleaningError.PHYSICAL_ADDRESS_IS_NULL.message),
                alternativePostalAddress = alternativePostalAddress?.let { toPoolDto(it) },
                confidenceCriteria = with(confidenceCriteria) {
                    this?.let {
                        ConfidenceCriteriaPoolDto(
                            sharedByOwner,
                            checkedByExternalDataSource,
                            numberOfBusinessPartners,
                            lastConfidenceCheckAt,
                            nextConfidenceCheckAt,
                            confidenceLevel
                        )
                    }
                } ?: throw BpdmValidationException(CleaningError.ADDRESS_CONFIDENCE_CRITERIA_MISSING.message)
            )
        }

    private fun toPoolDto(physicalPostalAddressDto: PhysicalPostalAddressDto) =
        with(physicalPostalAddressDto) {
            PhysicalPostalAddressPoolDto(
                geographicCoordinates = with(geographicCoordinates) { this?.let { GeoCoordinateDto(longitude, latitude, altitude) } },
                country = country ?: throw BpdmValidationException(CleaningError.PHYSICAL_ADDRESS_COUNTRY_MISSING.message),
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
                    this?.let {
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
                    }
                }
            )
        }

    private fun toPoolDto(alternativeAddress: AlternativePostalAddressDto) =
        with(alternativeAddress) {
            AlternativePostalAddressPoolDto(
                geographicCoordinates = with(geographicCoordinates) { this?.let { GeoCoordinateDto(longitude, latitude, altitude) } },
                country = country ?: throw BpdmValidationException(CleaningError.ALTERNATIVE_ADDRESS_COUNTRY_MISSING.message),
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

    private fun toPoolDto(confidenceCriteria: ConfidenceCriteriaDto) =
        with(confidenceCriteria) {
            ConfidenceCriteriaPoolDto(
                sharedByOwner,
                checkedByExternalDataSource,
                numberOfBusinessPartners,
                lastConfidenceCheckAt,
                nextConfidenceCheckAt,
                confidenceLevel
            )
        }

    private fun toBpnReference(bpn: String?) =
        bpn?.let { BpnReferenceDto(bpn, BpnReferenceType.Bpn) }

    data class LegalEntityUpsertResponse(
        val legalEntityBpn: String?,
        val legalAddressBpn: String?,
        val errors: Collection<ErrorInfo<ErrorCode>>
    )

    data class SiteUpsertResponse(
        val siteBpn: String?,
        val mainAddressBpn: String?,
        val errors: Collection<ErrorInfo<ErrorCode>>
    )

    data class AddressUpsertResponse(
        val addressBpn: String?,
        val errors: Collection<ErrorInfo<ErrorCode>>
    )
}