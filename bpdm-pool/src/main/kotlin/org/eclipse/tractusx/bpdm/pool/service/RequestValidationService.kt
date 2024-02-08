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

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.IBaseLegalEntityDto
import org.eclipse.tractusx.bpdm.common.dto.IBaseLogisticAddressDto
import org.eclipse.tractusx.bpdm.common.dto.RequestWithKey
import org.eclipse.tractusx.bpdm.common.util.findDuplicates
import org.eclipse.tractusx.bpdm.common.util.mergeMapsWithCollectionInValue
import org.eclipse.tractusx.bpdm.pool.api.model.request.*
import org.eclipse.tractusx.bpdm.pool.api.model.response.*
import org.eclipse.tractusx.bpdm.pool.dto.LegalEntityMetadataDto
import org.eclipse.tractusx.bpdm.pool.repository.*
import org.eclipse.tractusx.bpdm.pool.service.TaskStepBuildService.CleaningError.LEGAL_ENTITY_IS_NULL
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerFullDto
import org.eclipse.tractusx.orchestrator.api.model.LogisticAddressDto
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

    fun validateTasksFromOrchestrator(
        taskEntries: List<TaskStepReservationEntryDto>, requestMappings: TaskEntryBpnMapping
    ): Map<RequestWithKey, Collection<ErrorInfo<ErrorCode>>> {

        val result = mergeMapsWithCollectionInValue(
            validateLegalEntitiesFromOrchestrator(taskEntries, requestMappings),
            validateSitesFromOrchestrator(taskEntries, requestMappings),
            validateAddressesFromOrchestrator(taskEntries, requestMappings)
        )

        // remove possible duplicated entries
        return result.mapValues {
            it.value.distinct()
        }.toMap()
    }

    private fun isSiteUpdate(businessPartner: BusinessPartnerFullDto, requestMappings: TaskEntryBpnMapping) =
        businessPartner.site != null && requestMappings.hasBpnFor(businessPartner.site?.bpnSReference)

    private fun isLegalEntityUpdate(businessPartner: BusinessPartnerFullDto, requestMappings: TaskEntryBpnMapping) =
        (requestMappings.hasBpnFor(businessPartner.legalEntity?.bpnLReference))

    private fun isLegalEntityCreate(businessPartner: BusinessPartnerFullDto, requestMappings: TaskEntryBpnMapping) =
        !requestMappings.hasBpnFor(businessPartner.legalEntity?.bpnLReference)

    private fun validateLegalEntitiesFromOrchestrator(
        taskEntries: List<TaskStepReservationEntryDto>,
        requestMappings: TaskEntryBpnMapping
    ): Map<RequestWithKey, Collection<ErrorInfo<ErrorCode>>> {

        val (faultyRequestsWithLegalEntityNull, validLegalEntities) = taskEntries
            .partition { it.businessPartner.legalEntity == null || it.businessPartner.legalEntity?.bpnLReference == null }

        val legalEntitiesToCreate = validLegalEntities
            .filter {
                isLegalEntityCreate(it.businessPartner, requestMappings)
            }
        val legalEntitiesToUpdate = validLegalEntities
            .filter {
                isLegalEntityUpdate(it.businessPartner, requestMappings)
            }

        val legalEntityEmptyErrorsByRequest = faultyRequestsWithLegalEntityNull.associateWith {
            listOf(ErrorInfo(LegalEntityCreateError.LegalEntityIdentifierNotFound, LEGAL_ENTITY_IS_NULL.message, it.getRequestKey()))
        }
        val createBridges = legalEntitiesToCreate.map {
            LegalEntityBridge(legalEntity = it.businessPartner.legalEntity!!, request = it, bpnL = null)
        }
        val updateBridges = legalEntitiesToUpdate.map {
            LegalEntityBridge(
                legalEntity = it.businessPartner.legalEntity!!, request = it, bpnL = requestMappings.getBpn(it.businessPartner.legalEntity?.bpnLReference!!)!!
            )
        }

        val leCreateErrorsByRequest = validateLegalEntitiesToCreate(createBridges)
        val leUpdateErrorsByRequest = validateLegalEntitiesToUpdate(updateBridges)
        val duplicateValidationsByRequest = validateLegalEntityDuplicates(createBridges + updateBridges)
        return mergeMapsWithCollectionInValue(leCreateErrorsByRequest, leUpdateErrorsByRequest, duplicateValidationsByRequest, legalEntityEmptyErrorsByRequest)
    }

    private fun validateSitesFromOrchestrator(
        taskEntries: List<TaskStepReservationEntryDto>,
        requestMappings: TaskEntryBpnMapping
    ): Map<RequestWithKey, Collection<ErrorInfo<ErrorCode>>> {

        // sites to create are not validated, because the check for the parent legal entities is already done in the legal entity validation

        val sitesToUpdate = taskEntries
            .filter {
                isSiteUpdate(it.businessPartner, requestMappings)
            }

        return validateSitesToUpdate(sitesToUpdate.map {
            SiteUpdateBridge(request = it, bpnS = requestMappings.getBpn(it.businessPartner.site?.bpnSReference!!)!!)
        })
    }

    private fun validateAddressesFromOrchestrator(
        taskEntries: List<TaskStepReservationEntryDto>,
        requestMappings: TaskEntryBpnMapping
    ): Map<RequestWithKey, Collection<ErrorInfo<ErrorCode>>> {

        val (faultyTasksWithLegalAddressNull, validLegalEntityTasks) = taskEntries
            .partition { it.businessPartner.legalEntity?.legalAddress?.bpnAReference == null }
        val legalAddressEmptyErrorsByTask = faultyTasksWithLegalAddressNull.associateWith {
            listOf(
                ErrorInfo(
                    LegalEntityCreateError.LegalAddressIdentifierNotFound,
                    "Legal address or BpnA Reference of legal entity is Empty",
                    it.getRequestKey()
                )
            )
        }

        val (faultyTasksWithMainAddressNull, validLegalEntityAndSiteTasks) = validLegalEntityTasks
            .partition { it.businessPartner.site != null && it.businessPartner.site?.mainAddress?.bpnAReference == null }
        val mainAddressEmptyErrorsByTask = faultyTasksWithMainAddressNull.associateWith {
            listOf(
                ErrorInfo(
                    SiteCreateError.MainAddressIdentifierNotFound,
                    "Site main address or BpnA Reference is Empty",
                    it.getRequestKey()
                )
            )
        }

        val addressBridges = validLegalEntityAndSiteTasks.flatMap { getAddressBridges(it, requestMappings) }

        return mergeMapsWithCollectionInValue(
            legalAddressEmptyErrorsByTask,
            mainAddressEmptyErrorsByTask,
            validateAddresses(addressBridges, orchestratorMessages),
            validateAddressDuplicates(addressBridges)
        )
    }

    fun getAddressBridges(task: TaskStepReservationEntryDto, requestMappings: TaskEntryBpnMapping): List<AddressBridge> {

        val allAddresses: MutableList<AddressBridge> = mutableListOf()
        val businessPartner = task.businessPartner

        businessPartner.legalEntity?.let { legalEntity ->
            allAddresses.add(createAddressBridge(legalEntity.legalAddress!!, task, requestMappings))
        }
        businessPartner.site?.let { site ->
            allAddresses.add(createAddressBridge(site.mainAddress!!, task, requestMappings))
        }
        businessPartner.address?.let { address ->
            allAddresses.add(createAddressBridge(address, task, requestMappings))
        }

        return allAddresses
    }

    private fun createAddressBridge(
        address: LogisticAddressDto,
        task: TaskStepReservationEntryDto,
        requestMappings: TaskEntryBpnMapping
    ) = AddressBridge(
        address = address,
        request = task,
        bpnA = requestMappings.getBpn(address.bpnAReference)
    )

    fun validateLegalEntityDuplicates(requestBridges: List<LegalEntityBridge>): Map<RequestWithKey, Collection<ErrorInfo<ErrorCode>>> {

        val legalEntityDtos = requestBridges.map { it.legalEntity }
        val duplicatesValidator = ValidateLegalEntityIdentifiersDuplicated(legalEntityDtos, LegalEntityCreateError.LegalEntityDuplicateIdentifier)

        return requestBridges.associate {
            it.request to duplicatesValidator.validate(it.legalEntity, it.request, it.bpnL)
        }.filterValues { it.isNotEmpty() }
    }

    fun validateAddressDuplicates(requestBridges: List<AddressBridge>): Map<RequestWithKey, Collection<ErrorInfo<ErrorCode>>> {

        val addressDtos = requestBridges.map { it.address }
        val duplicatesValidator = ValidateAddressIdentifiersDuplicated(addressDtos, OrchestratorError.AddressDuplicateIdentifier)

        return requestBridges.associate {
            it.request to duplicatesValidator.validate(it.address, it.request, it.bpnA)
        }.filterValues { it.isNotEmpty() }
    }

    fun validateLegalEntitiesToCreateFromController(leCreateRequests: Collection<LegalEntityPartnerCreateRequest>): Map<RequestWithKey, Collection<ErrorInfo<LegalEntityCreateError>>> {

        val leErrorsByRequest = validateLegalEntitiesToCreate(leCreateRequests.map {
            LegalEntityBridge(legalEntity = it.legalEntity, request = it, bpnL = null)
        })
        val legalAddressBridges = leCreateRequests.map {
            AddressBridge(address = it.legalAddress, request = it, bpnA = null)
        }

        val addressErrorsByRequest = validateAddresses(legalAddressBridges, leCreateMessages)
        return mergeMapsWithCollectionInValue(leErrorsByRequest, addressErrorsByRequest)
    }

    private fun validateLegalEntitiesToCreate(
        requestBridges: List<LegalEntityBridge>
    ): Map<RequestWithKey, List<ErrorInfo<LegalEntityCreateError>>> {

        val legalEntityDtos = requestBridges.map { it.legalEntity }
        val duplicatesValidator = ValidateLegalEntityIdentifiersDuplicated(legalEntityDtos, LegalEntityCreateError.LegalEntityDuplicateIdentifier)
        val legalFormValidator = ValidateLegalFormExists(legalEntityDtos, LegalEntityCreateError.LegalFormNotFound)
        val identifierValidator = ValidateIdentifierLeTypesExists(legalEntityDtos, LegalEntityCreateError.LegalEntityIdentifierNotFound)

        return requestBridges.associate {
            val legalEntityDto = it.legalEntity
            val request = it.request

            val validationErrors =
                legalFormValidator.validate(legalEntityDto, request) +
                        identifierValidator.validate(legalEntityDto, request) +
                        duplicatesValidator.validate(legalEntityDto, request, bpn = null)
            request to validationErrors
        }.filterValues { it.isNotEmpty() }
    }

    fun validateLegalEntitiesToUpdateFromController(
        leRequests: Collection<LegalEntityPartnerUpdateRequest>
    ): Map<RequestWithKey, Collection<ErrorInfo<LegalEntityUpdateError>>> {

        val leErrorsByRequest = validateLegalEntitiesToUpdate(leRequests.map {
            LegalEntityBridge(legalEntity = it.legalEntity, request = it, bpnL = it.bpnl)
        })

        val addressBpnByLegalEntityBpnL = legalEntityRepository.findDistinctByBpnIn(leRequests.map { it.bpnl }).associate { it.bpn to it.legalAddress.bpn }
        val legalAddressBridges = leRequests.map {
            AddressBridge(address = it.legalAddress, request = it, bpnA = addressBpnByLegalEntityBpnL[it.bpnl])
        }
        val addressErrorsByRequest = validateAddresses(legalAddressBridges, leUpdateMessages)
        return mergeMapsWithCollectionInValue(leErrorsByRequest, addressErrorsByRequest)
    }

    fun validateLegalEntitiesToUpdate(
        requestBridges: Collection<LegalEntityBridge>
    ): Map<RequestWithKey, Collection<ErrorInfo<LegalEntityUpdateError>>> {

        val legalEntityDtos = requestBridges.map { it.legalEntity }
        val duplicatesValidator = ValidateLegalEntityIdentifiersDuplicated(legalEntityDtos, LegalEntityUpdateError.LegalEntityDuplicateIdentifier)
        val existingLegalEntityBpns = legalEntityRepository.findDistinctByBpnIn(requestBridges.mapNotNull { it.bpnL }).map { it.bpn }.toSet()
        val existingBpnValidator = ValidateUpdateBpnExists(existingLegalEntityBpns, LegalEntityUpdateError.LegalEntityNotFound)
        val legalFormValidator = ValidateLegalFormExists(legalEntityDtos, LegalEntityUpdateError.LegalFormNotFound)
        val identifierValidator = ValidateIdentifierLeTypesExists(legalEntityDtos, LegalEntityUpdateError.LegalEntityIdentifierNotFound)

        return requestBridges.associate { requestBridge ->
            val legalEntity = requestBridge.legalEntity

            val validationErrors =
                legalFormValidator.validate(legalEntity, requestBridge.request) +
                        identifierValidator.validate(legalEntity, requestBridge.request) +
                        duplicatesValidator.validate(legalEntity, requestBridge.request, requestBridge.bpnL) +
                        existingBpnValidator.validate(requestBridge.bpnL)
            requestBridge.request to validationErrors
        }.filterValues { it.isNotEmpty() }
    }

    fun <ERROR : ErrorCode> validateAddresses(
        addressBridges: Collection<AddressBridge>, messages: ValidatorErrorCodes<ERROR>
    ): Map<RequestWithKey, List<ErrorInfo<ERROR>>> {

        val addressDtos = addressBridges.map { it.address }
        val regionValidator = ValidateAdministrativeAreaLevel1Exists(addressDtos, messages.regionNotFound)
        val identifiersValidator = ValidateIdentifierTypesExists(addressDtos, messages.identifierNotFound)
        val identifiersDuplicateValidator = ValidateAddressIdentifiersDuplicated(addressDtos, messages.duplicateIdentifier)

        val result: MutableMap<RequestWithKey, List<ErrorInfo<ERROR>>> = mutableMapOf()
        // there could be second bridge for the same request, e.g. main address and additional address
        addressBridges.forEach { bridge ->
            val legalAddressDto = bridge.address
            val request: RequestWithKey = bridge.request
            val validationErrors =
                regionValidator.validate(legalAddressDto, request) +
                        identifiersValidator.validate(legalAddressDto, request) +
                        identifiersDuplicateValidator.validate(legalAddressDto, request, bridge.bpnA)

            if (validationErrors.isNotEmpty()) {
                val existing = result[request]
                if (existing == null) {
                    result[request] = validationErrors
                } else {
                    result[request] = existing + validationErrors
                }
            }
        }
        return result
    }

    fun validateSitesToCreateFromController(
        siteRequests: Collection<SitePartnerCreateRequest>,
    ): Map<RequestWithKey, Collection<ErrorInfo<SiteCreateError>>> {

        val siteErrorsByRequest = validateSitesToCreate(siteRequests)
        val addressErrorsByRequest = validateAddresses(siteRequests.map {
            AddressBridge(address = it.site.mainAddress, request = it, bpnA = null)
        }, siteCreateMessages)
        return mergeMapsWithCollectionInValue(siteErrorsByRequest, addressErrorsByRequest)
    }

    fun validateSitesToUpdateFromController(
        siteRequests: Collection<SitePartnerUpdateRequest>,
    ): Map<RequestWithKey, Collection<ErrorInfo<SiteUpdateError>>> {

        val siteErrorsByRequest = validateSitesToUpdate(siteRequests.map {
            SiteUpdateBridge(request = it, bpnS = it.bpns)
        })

        val addressBpnBySiteBpnS = siteRepository.findDistinctByBpnIn(siteRequests.map { it.bpns }).associate { it.bpn to it.mainAddress.bpn }
        val addressErrorsByRequest = validateAddresses(siteRequests.map {
            AddressBridge(address = it.site.mainAddress, request = it, bpnA = addressBpnBySiteBpnS[it.bpns])
        }, siteUpdateMessages)
        return mergeMapsWithCollectionInValue(siteErrorsByRequest, addressErrorsByRequest)
    }

    fun validateSitesToCreate(
        requests: Collection<SitePartnerCreateRequest>,
    ): Map<RequestWithKey, Collection<ErrorInfo<SiteCreateError>>> {

        val requestedParentBpns = requests.map { it.bpnlParent }
        val existingParentBpns = legalEntityRepository.findDistinctByBpnIn(requestedParentBpns).map { it.bpn }.toSet()

        return requests.associate { request ->
            val validationErrors =
                validateParentBpnExists(request.bpnlParent, request.getRequestKey(), existingParentBpns, SiteCreateError.LegalEntityNotFound)
            request to validationErrors
        }.filterValues { it.isNotEmpty() }
    }

    fun validateSitesToUpdate(
        requestBridges: Collection<SiteUpdateBridge>,
    ): Map<RequestWithKey, Collection<ErrorInfo<SiteUpdateError>>> {

        val requestedSiteBpns = requestBridges.map { it.bpnS }
        val existingSiteBpns = siteRepository.findDistinctByBpnIn(requestedSiteBpns).map { it.bpn }.toSet()
        val existingBpnValidator = ValidateUpdateBpnExists(existingSiteBpns, SiteUpdateError.SiteNotFound)

        return requestBridges.associate { bridge ->
            val validationErrors = existingBpnValidator.validate(bridge.bpnS)
            bridge.request to validationErrors
        }.filterValues { it.isNotEmpty() }
    }

    fun validateAddressesToCreateFromController(
        addressRequests: Collection<AddressPartnerCreateRequest>
    ): Map<AddressPartnerCreateRequest, Collection<ErrorInfo<AddressCreateError>>> {

        val addressDtos = addressRequests.map { it.address }
        val regionValidator = ValidateAdministrativeAreaLevel1Exists(addressDtos, AddressCreateError.RegionNotFound)
        val identifiersValidator = ValidateIdentifierTypesExists(addressDtos, AddressCreateError.IdentifierNotFound)
        val identifiersDuplicateValidator = ValidateAddressIdentifiersDuplicated(addressDtos, AddressCreateError.AddressDuplicateIdentifier)
        val parentValidator = ValidateAddressParent(addressRequests)

        return addressRequests.associateWith { request ->
            val address = request.address

            val validationErrors =
                regionValidator.validate(address, request) +
                        identifiersValidator.validate(address, request) +
                        identifiersDuplicateValidator.validate(address, request, bpn = null) +
                        parentValidator.validate(request.bpnParent, request)

            validationErrors
        }.filterValues { it.isNotEmpty() }
    }

    fun validateAddressesToUpdateFromController(
        addressRequests: Collection<AddressPartnerUpdateRequest>
    ): Map<AddressPartnerUpdateRequest, Collection<ErrorInfo<AddressUpdateError>>> {

        val addressDtos = addressRequests.map { it.address }
        val requestedAddressBpns = addressRequests.map { it.bpna }
        val existingAddressBpns = addressRepository.findDistinctByBpnIn(requestedAddressBpns).map { it.bpn }.toSet()

        val regionValidator = ValidateAdministrativeAreaLevel1Exists(addressDtos, AddressUpdateError.RegionNotFound)
        val identifiersValidator = ValidateIdentifierTypesExists(addressDtos, AddressUpdateError.IdentifierNotFound)
        val identifiersDuplicateValidator = ValidateAddressIdentifiersDuplicated(addressDtos, AddressUpdateError.AddressDuplicateIdentifier)
        val existingBpnValidator = ValidateUpdateBpnExists(existingAddressBpns, AddressUpdateError.AddressNotFound)

        return addressRequests.associateWith { request ->
            val address = request.address

            val validationErrors = regionValidator.validate(address, request) +
                    identifiersValidator.validate(address, request) +
                    identifiersDuplicateValidator.validate(address, request, request.bpna) +
                    existingBpnValidator.validate(request.bpna)
            validationErrors
        }.filterValues { it.isNotEmpty() }
    }

    inner class ValidateIdentifierLeTypesExists<ERROR : ErrorCode>(
        leDtos: Collection<IBaseLegalEntityDto>,
        private val errorCode: ERROR
    ) {

        private val existingTypes: Set<String> = metadataService.getMetadata(leDtos).toKeys().idTypes
        fun validate(legalEntity: IBaseLegalEntityDto, entityKey: RequestWithKey): Collection<ErrorInfo<ERROR>> {
            val requestedTypes = legalEntity.identifiers.map { it.type }
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
        leDtos: Collection<IBaseLegalEntityDto>,
        private val errorCode: ERROR
    ) {

        val existingLegalForms: Set<String> = metadataService.getMetadata(leDtos).toKeys().legalForms
        fun validate(legalEntity: IBaseLegalEntityDto, entityKey: RequestWithKey): Collection<ErrorInfo<ERROR>> {
            if (legalEntity.legalForm != null) {
                if (!existingLegalForms.contains(legalEntity.legalForm)) {
                    return listOf(ErrorInfo(errorCode, "Legal Form '${legalEntity.legalForm}' does not exist", entityKey.getRequestKey()))
                }
            }
            return emptyList()
        }
    }

    inner class ValidateIdentifierTypesExists<ERROR : ErrorCode>(
        addressDtos: Collection<IBaseLogisticAddressDto>,
        private val errorCode: ERROR
    ) {
        private val existingTypes = metadataService.getIdentifiers(addressDtos).map { it.technicalKey }.toSet()

        fun validate(addressDto: IBaseLogisticAddressDto, entityKey: RequestWithKey): Collection<ErrorInfo<ERROR>> {
            val requestedTypes = addressDto.identifiers.map { it.type }
            val missingTypes = requestedTypes - existingTypes

            return missingTypes.map {
                ErrorInfo(
                    errorCode,
                    "Address Identifier Type '$it' does not exist",
                    entityKey.getRequestKey()
                )
            }
        }
    }

    inner class ValidateAdministrativeAreaLevel1Exists<ERROR : ErrorCode>(
        addressDtos: Collection<IBaseLogisticAddressDto>,
        private val errorCode: ERROR
    ) {

        private val existingRegions = metadataService.getRegions(addressDtos).map { it.regionCode }.toSet()
        fun validate(address: IBaseLogisticAddressDto, entityKey: RequestWithKey): Collection<ErrorInfo<ERROR>> {

            val requestedAdminLevels = listOfNotNull(
                address.physicalPostalAddress?.administrativeAreaLevel1,
                address.alternativePostalAddress?.administrativeAreaLevel1
            )
            val missingAdminLevels = requestedAdminLevels - existingRegions
            return missingAdminLevels.map {
                ErrorInfo(
                    errorCode,
                    "Address administrative area level1 '$it' does not exist",
                    entityKey.getRequestKey()
                )
            }
        }
    }

    inner class ValidateUpdateBpnExists<ERROR : ErrorCode>(
        private val existingBpns: Set<String>,
        private val errorCode: ERROR
    ) {

        fun validate(bpnToUpdate: String?): Collection<ErrorInfo<ERROR>> {
            return if (bpnToUpdate != null && !existingBpns.contains(bpnToUpdate))
                listOf(ErrorInfo(errorCode, "Business Partner with BPN '$bpnToUpdate' can't be updated as it doesn't exist", bpnToUpdate))
            else
                emptyList()
        }
    }

    private fun <ERROR : ErrorCode> validateParentBpnExists(parentBpn: String, entityKey: String?, existingParentBpns: Set<String>, errorCode: ERROR)
            : Collection<ErrorInfo<ERROR>> {
        return if (!existingParentBpns.contains(parentBpn))
            listOf(ErrorInfo(errorCode, "Parent with BPN '$parentBpn'not found", entityKey))
        else
            emptyList()
    }


    private inner class ValidateLegalEntityIdentifiersDuplicated<ERROR : ErrorCode>(
        legalEntityDtos: Collection<IBaseLegalEntityDto>,
        private val errorCode: ERROR
    ) {

        val existingIdentifiers = getLegalEntityDuplicateIdentifierCandidates(legalEntityDtos)

        fun validate(legalEntity: IBaseLegalEntityDto, entityKey: RequestWithKey, bpn: String?): Collection<ErrorInfo<ERROR>> {

            return legalEntity.identifiers.mapNotNull {
                val identifierPair = IdentifierCandidateKey(type = it.type, value = it.value)
                existingIdentifiers[identifierPair]?.let { candidate ->
                    if (candidate.bpn === null || candidate.bpn != bpn)
                        ErrorInfo(errorCode, "Duplicate Legal Entity Identifier: Value '${it.value}' of type '${it.type}'", entityKey.getRequestKey())
                    else
                        null
                }
            }
        }

        private fun getLegalEntityDuplicateIdentifierCandidates(legalEntityDtos: Collection<IBaseLegalEntityDto>)
                : Map<IdentifierCandidateKey, IdentifierCandidate> {

            val identifiers = legalEntityDtos.flatMap { it.identifiers }
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
        addressDtos: Collection<IBaseLogisticAddressDto>,
        private val errorCode: ERROR
    ) {

        val existingIdentifiers = getAddressDuplicateIdentifierCandidates(addressDtos)

        fun validate(address: IBaseLogisticAddressDto, entityKey: RequestWithKey, bpn: String?): Collection<ErrorInfo<ERROR>> {

            return address.identifiers.mapNotNull {
                val identifierPair = IdentifierCandidateKey(type = it.type, value = it.value)
                existingIdentifiers[identifierPair]?.let { candidate ->
                    if (candidate.bpn === null || candidate.bpn != bpn)
                        ErrorInfo(errorCode, "Duplicate Address Identifier: Value '${it.value}' of type '${it.type}'", entityKey.getRequestKey())
                    else
                        null
                }
            }
        }

        private fun getAddressDuplicateIdentifierCandidates(addressDtos: Collection<IBaseLogisticAddressDto>)
                : Map<IdentifierCandidateKey, IdentifierCandidate> {

            val identifiers = addressDtos.flatMap { it.identifiers }
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

        val requestsByParentType = requests.groupBy { bpnIssuingService.translateToBusinessPartnerType(it.bpnParent) }
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
        val legalEntity: IBaseLegalEntityDto,
        val request: RequestWithKey,
        val bpnL: String?
    )

    data class AddressBridge(

        val request: RequestWithKey,
        val address: IBaseLogisticAddressDto,
        val bpnA: String?
    )

    data class SiteUpdateBridge(

        val request: RequestWithKey,
        val bpnS: String
    )

    data class ValidatorErrorCodes<out ERROR : ErrorCode>(

        val regionNotFound: ERROR,
        val identifierNotFound: ERROR,
        val duplicateIdentifier: ERROR
    )


    val orchestratorMessages = ValidatorErrorCodes(
        regionNotFound = OrchestratorError.AddressRegionNotFound,
        identifierNotFound = OrchestratorError.AddressIdentifierNotFound,
        duplicateIdentifier = OrchestratorError.AddressDuplicateIdentifier
    )

    val leCreateMessages = ValidatorErrorCodes(
        regionNotFound = LegalEntityCreateError.LegalAddressRegionNotFound,
        identifierNotFound = LegalEntityCreateError.LegalAddressIdentifierNotFound,
        duplicateIdentifier = LegalEntityCreateError.LegalAddressDuplicateIdentifier
    )

    val leUpdateMessages = ValidatorErrorCodes(
        regionNotFound = LegalEntityUpdateError.LegalAddressRegionNotFound,
        identifierNotFound = LegalEntityUpdateError.LegalAddressIdentifierNotFound,
        duplicateIdentifier = LegalEntityUpdateError.LegalAddressDuplicateIdentifier
    )

    val siteCreateMessages = ValidatorErrorCodes(
        regionNotFound = SiteCreateError.MainAddressRegionNotFound,
        identifierNotFound = SiteCreateError.MainAddressIdentifierNotFound,
        duplicateIdentifier = SiteCreateError.MainAddressDuplicateIdentifier
    )

    val siteUpdateMessages = ValidatorErrorCodes(
        regionNotFound = SiteUpdateError.MainAddressRegionNotFound,
        identifierNotFound = SiteUpdateError.MainAddressIdentifierNotFound,
        duplicateIdentifier = SiteUpdateError.MainAddressDuplicateIdentifier
    )

}