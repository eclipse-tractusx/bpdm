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

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.IBaseLegalEntityDto
import org.eclipse.tractusx.bpdm.common.dto.IBaseLogisticAddressDto
import org.eclipse.tractusx.bpdm.common.dto.RequestWithKey
import org.eclipse.tractusx.bpdm.common.util.findDuplicates
import org.eclipse.tractusx.bpdm.pool.api.model.request.*
import org.eclipse.tractusx.bpdm.pool.api.model.response.*
import org.eclipse.tractusx.bpdm.pool.dto.LegalEntityMetadataDto
import org.eclipse.tractusx.bpdm.pool.repository.*
import org.eclipse.tractusx.orchestrator.api.model.TaskStepReservationEntryDto
import org.springframework.stereotype.Service

@Service
class RequestValidationService(
    private val legalEntityRepository: LegalEntityRepository,
    private val legalEntityIdentifierRepository: LegalEntityIdentifierRepository,
    private val addressIdentifierRepository: AddressIdentifierRepository,
    private val bpnIssuingService: BpnIssuingService,
    private val siteRepository: SiteRepository,
    private val addressRepository: LogisticAddressRepository,
    private val metadataService: MetadataService
) {

    fun validateLegalEntityCreatesOrchestrator(requests: List<TaskStepReservationEntryDto>): Map<RequestWithKey, Collection<ErrorInfo<LegalEntityCreateError>>> {

        val legalEntitiesErrorsByRequest = validateLegalEntityCreatesInternal(requests.map {
            LegalEntityBridge(
                legalEntity = it.businessPartner.legalEntity,
                request = it
            )
        })

        val addressByTask = requests
            .filter { it.businessPartner.legalEntity?.legalAddress != null }
            .associateWith { it.businessPartner.legalEntity?.legalAddress as IBaseLogisticAddressDto }
            .toMap()
        val errorsByRequestAddress = validateLegalEntityCreatesAddresses(addressByTask)

        return mergeErrorMaps(legalEntitiesErrorsByRequest, errorsByRequestAddress)
    }

    fun validateLegalEntityCreates(requests: Collection<LegalEntityPartnerCreateRequest>): Map<RequestWithKey, Collection<ErrorInfo<LegalEntityCreateError>>> {

        val requestByLeError = validateLegalEntityCreatesInternal(requests.map {
            LegalEntityBridge(
                legalEntity = it.legalEntity,
                request = it
            )
        })
        val errorsByRequestAddress = validateLegalEntityCreatesAddresses(requests.associateWith { it.legalAddress })
        return mergeErrorMaps(requestByLeError, errorsByRequestAddress)
    }

    private fun validateLegalEntityCreatesInternal(
        requests: List<LegalEntityBridge>
    ): Map<RequestWithKey, List<ErrorInfo<LegalEntityCreateError>>> {

        val legalEntityRequests = requests.map { it.legalEntity!! }
        val duplicatesValidator = ValidateLegalEntityIdentifiersDuplicated(legalEntityRequests, LegalEntityCreateError.LegalEntityDuplicateIdentifier)
        val legalFormValidator = ValidateLegalFormExists(legalEntityRequests, LegalEntityCreateError.LegalFormNotFound)
        val identifierValidator = ValidateIdentifierLeTypesExists(legalEntityRequests, LegalEntityCreateError.LegalEntityIdentifierNotFound)

        return requests.map {
            val legalEntity = it.legalEntity!!
            val request = it.request

            val validationErrors =
                legalFormValidator.validate(legalEntity, request) +
                        identifierValidator.validate(legalEntity, request) +
                        duplicatesValidator.validate(legalEntity, request, bpn = null)

            request to validationErrors
        }.toMap()
            .filterValues { it.isNotEmpty() }

    }

    fun validateLegalEntityCreatesAddresses(
        addressByRequest: Map<out RequestWithKey, IBaseLogisticAddressDto>
    ): Map<RequestWithKey, List<ErrorInfo<LegalEntityCreateError>>> {

        val legalAddressRequests = addressByRequest.values

        val regionValidator = ValidateRegionExists(legalAddressRequests, LegalEntityCreateError.LegalAddressRegionNotFound)
        val identifiersValidator = ValidateIdentifierTypesExists(legalAddressRequests, LegalEntityCreateError.LegalAddressIdentifierNotFound)
        val identifiersDuplicateValidator = ValidateAddressIdentifiersDuplicated(legalAddressRequests, LegalEntityCreateError.LegalAddressDuplicateIdentifier)

        return addressByRequest.map {
            val legalAddress = it.value
            val request = it.key
            val validationErrors =
                regionValidator.validate(legalAddress, request) +
                        identifiersValidator.validate(legalAddress, request) +
                        identifiersDuplicateValidator.validate(legalAddress, request, null)
            request to validationErrors
        }.toMap()
            .filterValues { it.isNotEmpty() }
    }

    fun validateLegalEntityUpdates(
        requests: Collection<LegalEntityPartnerUpdateRequest>
    ): Map<LegalEntityPartnerUpdateRequest, Collection<ErrorInfo<LegalEntityUpdateError>>> {

        val requestByLeError = validateLegalEntityUpdatesInternal(requests)
        val errorsByRequestAddress = validateLegalEntityAddressUpdates(requests)
        return mergeErrorMaps(requestByLeError, errorsByRequestAddress)
    }

    fun validateLegalEntityUpdatesInternal(
        requests: Collection<LegalEntityPartnerUpdateRequest>
    ): Map<LegalEntityPartnerUpdateRequest, Collection<ErrorInfo<LegalEntityUpdateError>>> {

        val legalEntityRequests = requests.map { it.legalEntity }
        val duplicatesValidator = ValidateLegalEntityIdentifiersDuplicated(legalEntityRequests, LegalEntityUpdateError.LegalEntityDuplicateIdentifier)
        val existingLegalEntityBpns = legalEntityRepository.findDistinctByBpnIn(requests.map { it.bpnl }).map { it.bpn }.toSet()
        val existingBpnValidator = ValidateUpdateBpnExists(existingLegalEntityBpns, LegalEntityUpdateError.LegalEntityNotFound)
        val legalFormValidator = ValidateLegalFormExists(legalEntityRequests, LegalEntityUpdateError.LegalFormNotFound)
        val identifierValidator = ValidateIdentifierLeTypesExists(legalEntityRequests, LegalEntityUpdateError.LegalEntityIdentifierNotFound)

        return requests.associateWith { request ->
            val legalEntity = request.legalEntity

            legalFormValidator.validate(legalEntity, request) +
                    identifierValidator.validate(legalEntity, request) +
                    duplicatesValidator.validate(legalEntity, request, request.bpnl) +
                    existingBpnValidator.validate(request.bpnl)
        }.filterValues { errors -> errors.isNotEmpty() }
    }

    fun validateLegalEntityAddressUpdates(
        requests: Collection<LegalEntityPartnerUpdateRequest>
    ): Map<LegalEntityPartnerUpdateRequest, Collection<ErrorInfo<LegalEntityUpdateError>>> {

        val legalAddressRequests = requests.map { it.legalAddress }
        val regionValidator = ValidateRegionExists(legalAddressRequests, LegalEntityUpdateError.LegalAddressRegionNotFound)
        val identifiersValidator = ValidateIdentifierTypesExists(legalAddressRequests, LegalEntityUpdateError.LegalAddressIdentifierNotFound)
        val identifiersDuplicateValidator = ValidateAddressIdentifiersDuplicated(legalAddressRequests, LegalEntityUpdateError.LegalAddressDuplicateIdentifier)

        return requests.flatMap { request ->
            val legalAddress = request.legalAddress

            val validationErrors =
                regionValidator.validate(legalAddress, request) +
                        identifiersValidator.validate(legalAddress, request) +
                        identifiersDuplicateValidator.validate(legalAddress, request, request.bpnl)

            validationErrors.map { Pair(request, it) }

        }.groupBy({ it.first }, { it.second })
    }

    fun validateSiteCreates(
        requests: Collection<SitePartnerCreateRequest>,
    ): Map<SitePartnerCreateRequest, Collection<ErrorInfo<SiteCreateError>>> {

        val requestByLeError = validateSiteCreatesInternal(requests)
        val errorsByRequestAddress = validateSiteAddressCreatesInternal(requests)
        return mergeErrorMaps(requestByLeError, errorsByRequestAddress)
    }

    fun validateSiteCreatesInternal(
        requests: Collection<SitePartnerCreateRequest>,
    ): Map<SitePartnerCreateRequest, Collection<ErrorInfo<SiteCreateError>>> {


        val requestedParentBpns = requests.map { it.bpnlParent }
        val existingParentBpns = legalEntityRepository.findDistinctByBpnIn(requestedParentBpns).map { it.bpn }.toSet()

        return requests.flatMap { request ->
            val validationErrors =
                validateParentBpnExists(request.bpnlParent, request.index, existingParentBpns, SiteCreateError.LegalEntityNotFound)
            validationErrors.map { Pair(request, it) }

        }.groupBy({ it.first }, { it.second })
    }

    fun validateSiteAddressCreatesInternal(
        requests: Collection<SitePartnerCreateRequest>,
    ): Map<SitePartnerCreateRequest, Collection<ErrorInfo<SiteCreateError>>> {

        val mainAddressRequests = requests.map { it.site.mainAddress }
        val regionValidator = ValidateRegionExists(mainAddressRequests, SiteCreateError.MainAddressRegionNotFound)
        val identifiersValidator = ValidateIdentifierTypesExists(mainAddressRequests, SiteCreateError.MainAddressIdentifierNotFound)
        val identifiersDuplicateValidator = ValidateAddressIdentifiersDuplicated(mainAddressRequests, SiteCreateError.MainAddressDuplicateIdentifier)

        return requests.flatMap { request ->
            val mainAddress = request.site.mainAddress
            val validationErrors = regionValidator.validate(mainAddress, request) +
                    identifiersValidator.validate(mainAddress, request) +
                    identifiersDuplicateValidator.validate(mainAddress, request, null)

            validationErrors.map { Pair(request, it) }
        }.groupBy({ it.first }, { it.second })
    }

    fun validateSiteUpdates(
        requests: Collection<SitePartnerUpdateRequest>,
    ): Map<SitePartnerUpdateRequest, Collection<ErrorInfo<SiteUpdateError>>> {

        val requestByLeError = validateSiteUpdatesInternal(requests)
        val errorsByRequestAddress = validateSiteAddressUpdates(requests)
        return mergeErrorMaps(requestByLeError, errorsByRequestAddress)
    }

    fun validateSiteUpdatesInternal(
        requests: Collection<SitePartnerUpdateRequest>,
    ): Map<SitePartnerUpdateRequest, Collection<ErrorInfo<SiteUpdateError>>> {

        val requestedSiteBpns = requests.map { it.bpns }
        val existingSiteBpns = siteRepository.findDistinctByBpnIn(requestedSiteBpns).map { it.bpn }.toSet()
        val existingBpnValidator = ValidateUpdateBpnExists(existingSiteBpns, SiteUpdateError.SiteNotFound)

        return requests.flatMap { request ->
            val validationErrors = existingBpnValidator.validate(request.bpns)

            validationErrors.map { Pair(request, it) }

        }.groupBy({ it.first }, { it.second })
    }

    fun validateSiteAddressUpdates(
        requests: Collection<SitePartnerUpdateRequest>,
    ): Map<SitePartnerUpdateRequest, Collection<ErrorInfo<SiteUpdateError>>> {

        val mainAddressRequests = requests.map { it.site.mainAddress }
        val regionValidator = ValidateRegionExists(mainAddressRequests, SiteUpdateError.MainAddressRegionNotFound)
        val identifiersValidator = ValidateIdentifierTypesExists(mainAddressRequests, SiteUpdateError.MainAddressIdentifierNotFound)
        val identifiersDuplicateValidator = ValidateAddressIdentifiersDuplicated(mainAddressRequests, SiteUpdateError.MainAddressDuplicateIdentifier)

        return requests.flatMap { request ->
            val mainAddress = request.site.mainAddress

            val validationErrors =
                regionValidator.validate(mainAddress, request) +
                        identifiersValidator.validate(mainAddress, request) +
                        identifiersDuplicateValidator.validate(mainAddress, request, request.bpns)

            validationErrors.map { Pair(request, it) }

        }.groupBy({ it.first }, { it.second })
    }

    fun validateAddressCreates(
        requests: Collection<AddressPartnerCreateRequest>
    ): Map<AddressPartnerCreateRequest, Collection<ErrorInfo<AddressCreateError>>> {

        val addressRequests = requests.map { it.address }
        val regionValidator = ValidateRegionExists(addressRequests, AddressCreateError.RegionNotFound)
        val identifiersValidator = ValidateIdentifierTypesExists(addressRequests, AddressCreateError.IdentifierNotFound)
        val identifiersDuplicateValidator = ValidateAddressIdentifiersDuplicated(addressRequests, AddressCreateError.AddressDuplicateIdentifier)
        val parentValidator = ValidateAddressParent(requests)

        return requests.flatMap { request ->
            val address = request.address

            val validationErrors =
                regionValidator.validate(address, request) +
                        identifiersValidator.validate(address, request) +
                        identifiersDuplicateValidator.validate(address, request, bpn = null) +
                        parentValidator.validate(request.bpnParent, request)

            validationErrors.map { Pair(request, it) }

        }.groupBy({ it.first }, { it.second })
    }

    fun validateAddressUpdates(
        requests: Collection<AddressPartnerUpdateRequest>
    ): Map<AddressPartnerUpdateRequest, Collection<ErrorInfo<AddressUpdateError>>> {

        val addressRequests = requests.map { it.address }
        val requestedAddressBpns = requests.map { it.bpna }
        val existingAddressBpns = addressRepository.findDistinctByBpnIn(requestedAddressBpns).map { it.bpn }.toSet()

        val regionValidator = ValidateRegionExists(addressRequests, AddressUpdateError.RegionNotFound)
        val identifiersValidator = ValidateIdentifierTypesExists(addressRequests, AddressUpdateError.IdentifierNotFound)
        val identifiersDuplicateValidator = ValidateAddressIdentifiersDuplicated(addressRequests, AddressUpdateError.AddressDuplicateIdentifier)
        val existingBpnValidator = ValidateUpdateBpnExists(existingAddressBpns, AddressUpdateError.AddressNotFound)

        return requests.flatMap { request ->
            val address = request.address

            val validationErrors = regionValidator.validate(address, request) +
                    identifiersValidator.validate(address, request) +
                    identifiersDuplicateValidator.validate(address, request, request.bpna) +
                    existingBpnValidator.validate(request.bpna)
            validationErrors.map { Pair(request, it) }

        }.groupBy({ it.first }, { it.second })
    }

    inner class ValidateIdentifierLeTypesExists<ERROR : ErrorCode>(
        requests: Collection<IBaseLegalEntityDto>,
        private val errorCode: ERROR
    ) {

        private val existingTypes: Set<String> = metadataService.getMetadata(requests).toKeys().idTypes
        fun validate(request: IBaseLegalEntityDto, entityKey: RequestWithKey): Collection<ErrorInfo<ERROR>> {
            val requestedTypes = request.identifiers.map { it.type }
            val missingTypes = requestedTypes - existingTypes

            return missingTypes.map {
                ErrorInfo(
                    errorCode,
                    "Legal Entity Identifier Type '$it' does not exist",
                    entityKey.getRequestKey()
                )
            }
        }
    }

    inner class ValidateLegalFormExists<ERROR : ErrorCode>(
        requests: Collection<IBaseLegalEntityDto>,
        private val error: ERROR
    ) {

        val existingLegalForms: Set<String> = metadataService.getMetadata(requests).toKeys().legalForms
        fun validate(request: IBaseLegalEntityDto, entityKey: RequestWithKey): Collection<ErrorInfo<ERROR>> {
            if (request.legalForm != null) {
                if (!existingLegalForms.contains(request.legalForm)) {
                    return listOf(ErrorInfo(error, "Legal Form '${request.legalForm}' does not exist", entityKey.getRequestKey()))
                }
            }
            return emptyList()
        }
    }

    inner class ValidateIdentifierTypesExists<ERROR : ErrorCode>(
        val addresses: Collection<IBaseLogisticAddressDto>,
        private val error: ERROR
    ) {
        private val existingTypes = metadataService.getIdentifiers(addresses).map { it.technicalKey }.toSet()

        fun validate(address: IBaseLogisticAddressDto, entityKey: RequestWithKey): Collection<ErrorInfo<ERROR>> {
            val requestedTypes = address.identifiers.map { it.type }
            val missingTypes = requestedTypes - existingTypes

            return missingTypes.map {
                ErrorInfo(
                    error,
                    "Address Identifier Type '$it' does not exist",
                    entityKey.getRequestKey()
                )
            }
        }
    }

    inner class ValidateRegionExists<ERROR : ErrorCode>(
        val addresses: Collection<IBaseLogisticAddressDto>,
        private val error: ERROR
    ) {

        private val existingRegions = metadataService.getRegions(addresses).map { it.regionCode }.toSet()
        fun validate(address: IBaseLogisticAddressDto, entityKey: RequestWithKey): Collection<ErrorInfo<ERROR>> {

            val requestedTypes = listOfNotNull(
                address.physicalPostalAddress?.administrativeAreaLevel1,
                address.alternativePostalAddress?.administrativeAreaLevel1
            )
            val missingTypes = requestedTypes - existingRegions
            return missingTypes.map {
                ErrorInfo(
                    error,
                    "Address Identifier Type '$it' does not exist",
                    entityKey.getRequestKey()
                )
            }
        }
    }

    inner class ValidateUpdateBpnExists<ERROR : ErrorCode>(
        val existingBpns: Set<String>,
        private val error: ERROR
    ) {

        fun validate(bpnToUpdate: String): Collection<ErrorInfo<ERROR>> {
            return if (!existingBpns.contains(bpnToUpdate))
                listOf(ErrorInfo(error, "Business Partner with BPN '$bpnToUpdate' can't be updated as it doesn't exist", bpnToUpdate))
            else
                emptyList()
        }
    }

    private fun <ERROR : ErrorCode> validateParentBpnExists(parentBpn: String, entityKey: String?, existingParentBpns: Set<String>, error: ERROR)
            : Collection<ErrorInfo<ERROR>> {
        return if (!existingParentBpns.contains(parentBpn))
            listOf(ErrorInfo(error, "Parent with BPN '$parentBpn'not found", entityKey))
        else
            emptyList()
    }


    private inner class ValidateLegalEntityIdentifiersDuplicated<ERROR : ErrorCode>(
        requests: Collection<IBaseLegalEntityDto>,
        private val error: ERROR
    ) {

        val existingIdentifiers = getLegalEntityDuplicateIdentifierCandidates(requests)

        fun validate(legalEntity: IBaseLegalEntityDto, entityKey: RequestWithKey, bpn: String?): Collection<ErrorInfo<ERROR>> {

            return legalEntity.identifiers.mapNotNull {
                val identifierPair = IdentifierCandidateKey(type = it.type, value = it.value)
                existingIdentifiers[identifierPair]?.let { candidate ->
                    if (candidate.bpn === null || candidate.bpn != bpn)
                        ErrorInfo(error, "Duplicate Legal Entity Identifier: Value '${it.value}' of type '${it.type}'", entityKey.getRequestKey())
                    else
                        null
                }
            }
        }

        private fun getLegalEntityDuplicateIdentifierCandidates(requests: Collection<IBaseLegalEntityDto>)
                : Map<IdentifierCandidateKey, IdentifierCandidate> {
            val identifiers = requests.flatMap { it.identifiers }
            val idValues = identifiers.map { it.value }
            val duplicatesFromRequest = identifiers
                .map { IdentifierCandidateKey(it.type, it.value) }
                .findDuplicates()
                .associateWith { IdentifierCandidate(bpn = null, type = it.type, value = it.value) }
            val duplicatesFromDb = legalEntityIdentifierRepository.findByValueIn(idValues)
                .map { IdentifierCandidate(bpn = it.legalEntity.bpn, type = it.type.technicalKey, value = it.value) }
                .associateBy { IdentifierCandidateKey(it.type, it.value) }
            return duplicatesFromRequest.plus(duplicatesFromDb)
        }
    }

    private inner class ValidateAddressIdentifiersDuplicated<ERROR : ErrorCode>(
        addresses: Collection<IBaseLogisticAddressDto>,
        private val error: ERROR
    ) {

        val existingIdentifiers = getAddressDuplicateIdentifierCandidates(addresses)

        fun validate(address: IBaseLogisticAddressDto, entityKey: RequestWithKey, bpn: String?): Collection<ErrorInfo<ERROR>> {

            return address.identifiers.mapNotNull {
                val identifierPair = IdentifierCandidateKey(type = it.type, value = it.value)
                existingIdentifiers[identifierPair]?.let { candidate ->
                    if (candidate.bpn === null || candidate.bpn != bpn)
                        ErrorInfo(error, "Duplicate Address Identifier: Value '${it.value}' of type '${it.type}'", entityKey.getRequestKey())
                    else
                        null
                }
            }
        }

        private fun getAddressDuplicateIdentifierCandidates(requests: Collection<IBaseLogisticAddressDto>)
                : Map<IdentifierCandidateKey, IdentifierCandidate> {
            val identifiers = requests.flatMap { it.identifiers }
            val idValues = identifiers.map { it.value }
            val duplicatesFromRequest = identifiers
                .map { IdentifierCandidateKey(it.type, it.value) }
                .findDuplicates()
                .associateWith { IdentifierCandidate(bpn = null, type = it.type, value = it.value) }
            val duplicatesFromDb = addressIdentifierRepository.findByValueIn(idValues)
                .map { IdentifierCandidate(bpn = it.address.bpn, type = it.type.technicalKey, value = it.value) }
                .associateBy { IdentifierCandidateKey(it.type, it.value) }
            return duplicatesFromRequest.plus(duplicatesFromDb)
        }
    }

    private inner class ValidateAddressParent(requests: Collection<AddressPartnerCreateRequest>) {

        val requestsWithParentType = requests.map { Pair(it, bpnIssuingService.translateToBusinessPartnerType(it.bpnParent)) }
        val requestsByParentType = requestsWithParentType.groupBy({ it.second }, { it.first })

        val legalEntityParentBpns = requestsByParentType[BusinessPartnerType.LEGAL_ENTITY]?.map { it.bpnParent } ?: emptyList()
        val existingLegalEntities = legalEntityRepository.findDistinctByBpnIn(legalEntityParentBpns).map { it.bpn }.toSet()

        val siteParentBpns = requestsByParentType[BusinessPartnerType.SITE]?.map { it.bpnParent } ?: emptyList()
        val existingSites = siteRepository.findDistinctByBpnIn(siteParentBpns).map { it.bpn }.toSet()

        fun validate(bpnParent: String, entityKey: RequestWithKey): Collection<ErrorInfo<AddressCreateError>> {
            val type = bpnIssuingService.translateToBusinessPartnerType(bpnParent)
            return when (type) {
                BusinessPartnerType.LEGAL_ENTITY -> validateParentBpnExists(
                    bpnParent,
                    entityKey.getRequestKey(),
                    existingLegalEntities,
                    AddressCreateError.LegalEntityNotFound
                )

                BusinessPartnerType.SITE -> validateParentBpnExists(bpnParent, entityKey.getRequestKey(), existingSites, AddressCreateError.SiteNotFound)
                else -> listOf(ErrorInfo(AddressCreateError.BpnNotValid, "Parent '${bpnParent}' is not a valid BPNL/BPNS", entityKey.getRequestKey()))

            }
        }
    }

    private fun LegalEntityMetadataDto.toKeys(): LegalEntityMetadataKeys {
        return LegalEntityMetadataKeys(
            idTypes = idTypes.map { it.technicalKey }.toSet(),
            legalForms = legalForms.map { it.technicalKey }.toSet()
        )
    }

    private data class LegalEntityMetadataKeys(
        val idTypes: Set<String>,
        val legalForms: Set<String>
    )

    private data class IdentifierCandidateKey(
        val type: String,
        val value: String
    )

    data class IdentifierCandidate(
        val bpn: String?,
        val type: String,
        val value: String
    )

    data class LegalEntityBridge(
        val legalEntity: IBaseLegalEntityDto?,
        val request: RequestWithKey
    )

    fun <TError : ErrorCode, TRequest : RequestWithKey> mergeErrorMaps(
        firstMap: Map<TRequest, Collection<ErrorInfo<TError>>>,
        secondMap: Map<TRequest, Collection<ErrorInfo<TError>>>
    ): Map<TRequest, Collection<ErrorInfo<TError>>> =
        (firstMap.keys + secondMap.keys)
            .associateWith {
                (firstMap[it] ?: emptyList()) + (secondMap[it] ?: emptyList())
            }

    companion object {
        fun <TError : ErrorCode, TRequest : RequestWithKey> mergeErrorMaps(
            firstMap: Map<TRequest, Collection<ErrorInfo<TError>>>,
            secondMap: Map<TRequest, Collection<ErrorInfo<TError>>>
        ): Map<TRequest, Collection<ErrorInfo<TError>>> =

            (firstMap.keys + secondMap.keys)
                .associateWith {
                    (firstMap[it] ?: emptyList()) + (secondMap[it] ?: emptyList())
                }
    }
}