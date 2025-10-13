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

package org.eclipse.tractusx.bpdm.pool.controller.v6

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.*
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.common.util.findDuplicates
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressCreateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressUpdateError
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.AddressPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorCode
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.AddressPartnerCreateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.AddressPartnerUpdateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.dto.AddressMetadataDto
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.repository.AddressIdentifierRepository
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.eclipse.tractusx.bpdm.pool.service.*
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService.AddressMetadataMapping
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService.Companion.createAlternativeAddress
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService.Companion.createConfidenceCriteria
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService.Companion.createPhysicalAddress
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService.Companion.toAddressIdentifier
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService.Companion.toAddressState
import org.eclipse.tractusx.bpdm.pool.service.RequestValidationService.IdentifierCandidate
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AddressLegacyServiceMapper(
    private val logisticAddressRepository: LogisticAddressRepository,
    private val legalEntityRepository: LegalEntityRepository,
    private val bpnIssuingService: BpnIssuingService,
    private val metadataService: MetadataService,
    private val changelogService: PartnerChangelogService,
    private val addressIdentifierRepository: AddressIdentifierRepository,
    private val siteRepository: SiteRepository,
    private val businessPartnerFetchService: BusinessPartnerFetchService,
    private val addressRepository: LogisticAddressRepository,
    private val businessPartnerEquivalenceMapper: BusinessPartnerEquivalenceMapper
) {

    private val logger = KotlinLogging.logger { }

    fun findByBpn(bpn: String): LogisticAddressVerboseDto {
        val address = findAddressByBpn(bpn) ?: throw BpdmNotFoundException("Address", bpn)
        return address.toDto()
    }

    fun findAddressByBpn(bpn: String): LogisticAddressDb? {
        logger.debug { "Executing findAddressByBpn() with parameters $bpn" }
        return logisticAddressRepository.findByBpn(bpn)
    }

    fun LogisticAddressDb.toDto(): LogisticAddressVerboseDto {
        return LogisticAddressVerboseDto(
            bpna = bpn,
            bpnLegalEntity = legalEntity?.bpn,
            bpnSite = site?.bpn,
            createdAt = createdAt,
            updatedAt = updatedAt,
            name = name,
            states = states.map { it.toDto() },
            identifiers = identifiers.map { it.toDto() },
            physicalPostalAddress = physicalPostalAddress.toDto(),
            alternativePostalAddress = alternativePostalAddress?.toDto(),
            confidenceCriteria = confidenceCriteria.toDto(),
            isCatenaXMemberData = legalEntity?.isCatenaXMemberData ?: site?.legalEntity?.isCatenaXMemberData ?: false,
            addressType = getAddressType(this)
        )
    }

    /**
     * Search addresses per page for [searchRequest] and [paginationRequest]
     */
    @Transactional
    fun searchAddresses(searchRequest: AddressSearchRequest, paginationRequest: PaginationRequest): PageDto<LogisticAddressVerboseDto> {

        val spec = Specification.allOf(
            LogisticAddressRepository.byBpns(searchRequest.addressBpns),
            LogisticAddressRepository.bySiteBpns(searchRequest.siteBpns),
            LogisticAddressRepository.byLegalEntityBpns(searchRequest.legalEntityBpns),
            LogisticAddressRepository.byName(searchRequest.name),
            LogisticAddressRepository.byIsMember(searchRequest.isCatenaXMemberData)
        )
        val addressPage = logisticAddressRepository.findAll(spec, paginationRequest.toPageRequest())

        return addressPage.toDto { it.toDto() }
    }

    data class AddressSearchRequest(
        val addressBpns: List<String>?,
        val siteBpns: List<String>?,
        val legalEntityBpns: List<String>?,
        val name: String?,
        val isCatenaXMemberData: Boolean?
    )

    @Transactional
    fun createAddresses(requests: Collection<AddressPartnerCreateRequest>): AddressPartnerCreateResponseWrapper {
        logger.info { "Create ${requests.size} new addresses" }

        val errorsByRequest = validateAddressesToCreateFromController(requests)
        val errors = errorsByRequest.flatMap { it.value }
        val validRequests = requests.filterNot { errorsByRequest.containsKey(it) }

        fun isLegalEntityRequest(request: AddressPartnerCreateRequest) =
            bpnIssuingService.translateToBusinessPartnerType(request.bpnParent) == BusinessPartnerType.LEGAL_ENTITY

        fun isSiteRequest(request: AddressPartnerCreateRequest) =
            bpnIssuingService.translateToBusinessPartnerType(request.bpnParent) == BusinessPartnerType.SITE

        val legalEntityRequests = validRequests.filter { isLegalEntityRequest(it) }
        val siteRequests = validRequests.filter { isSiteRequest(it) }

        val metadataMap = metadataService.getMetadata(validRequests.map { it.address }).toMapping()

        val addressResponses = createAddressesForSite(siteRequests, metadataMap)
            .plus(createAddressesForLegalEntity(legalEntityRequests, metadataMap))

        changelogService.createChangelogEntries(addressResponses.map {
            ChangelogEntryCreateRequest(it.address.bpna, ChangelogType.CREATE, BusinessPartnerType.ADDRESS)
        })

        return AddressPartnerCreateResponseWrapper(addressResponses, errors)
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

    private data class IdentifierCandidateKey(
        val type: String,
        val value: String
    )

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

    private fun <ERROR : ErrorCode> validateParentBpnExists(parentBpn: String, entityKey: String?, existingParentBpns: Set<String>, errorCode: ERROR)
            : Collection<ErrorInfo<ERROR>> {
        return if (!existingParentBpns.contains(parentBpn))
            listOf(ErrorInfo(errorCode, "Parent with BPN '$parentBpn'not found", entityKey))
        else
            emptyList()
    }

    private fun AddressMetadataDto.toMapping() =
        AddressMetadataMapping(
            idTypes = idTypes.associateBy { it.technicalKey },
            regions = regions.associateBy { it.regionCode }
        )

    private fun createAddressesForSite(
        validRequests: Collection<AddressPartnerCreateRequest>,
        metadataMap: AddressMetadataMapping
    ): List<AddressPartnerCreateVerboseDto> {

        val bpnsToFetch = validRequests.map { it.bpnParent }
        val siteParents = siteRepository.findDistinctByBpnIn(bpnsToFetch)
        val siteParentsByBpn = siteParents.associateBy { it.bpn }

        val bpnAs = bpnIssuingService.issueAddressBpns(validRequests.size)
        val addressesWithIndex = validRequests
            .mapIndexed { i, request ->
                val site = siteParentsByBpn[request.bpnParent]!!
                val address = createLogisticAddress(request.address, bpnAs[i], site.legalEntity, site, metadataMap)
                Pair(address, request.index)
            }

        logisticAddressRepository.saveAll(addressesWithIndex.map { (address, _) -> address })
        return addressesWithIndex.map { (address, index) ->
            address.toCreateResponse(index).also { logger.info { "Address ${address.bpn} was created" } }
        }
    }

    fun LogisticAddressDb.toCreateResponse(index: String?): AddressPartnerCreateVerboseDto {
        return AddressPartnerCreateVerboseDto(
            address = toDto(),
            index = index
        )
    }

    private fun createLogisticAddress(
        dto: LogisticAddressDto,
        bpn: String,
        legalEntity: LegalEntityDb,
        site: SiteDb?,
        metadataMap: AddressMetadataMapping
    ) = createLogisticAddressInternal(dto, bpn, metadataMap)
        .apply {
            this.legalEntity = legalEntity
            this.site = site
        }

    private fun createLogisticAddressInternal(
        dto: LogisticAddressDto,
        bpn: String,
        metadataMap: AddressMetadataMapping
    ): LogisticAddressDb {
        val address = LogisticAddressDb(
            bpn = bpn,
            legalEntity = null,
            site = null,
            physicalPostalAddress = createPhysicalAddress(dto.physicalPostalAddress, metadataMap.regions),
            alternativePostalAddress = dto.alternativePostalAddress?.let { createAlternativeAddress(it, metadataMap.regions) },
            name = dto.name,
            confidenceCriteria = createConfidenceCriteria(dto.confidenceCriteria)
        )

        updateLogisticAddress(address, dto, metadataMap)

        return address
    }

    private fun updateLogisticAddress(address: LogisticAddressDb, dto: LogisticAddressDto, metadataMap: AddressMetadataMapping) {
        address.name = dto.name
        address.physicalPostalAddress = createPhysicalAddress(dto.physicalPostalAddress, metadataMap.regions)
        address.alternativePostalAddress = dto.alternativePostalAddress?.let { createAlternativeAddress(it, metadataMap.regions) }

        address.identifiers.apply {
            clear()
            addAll(dto.identifiers.map { toAddressIdentifier(it, metadataMap.idTypes, address) })
        }
        address.states.apply {
            clear()
            addAll(dto.states.map { toAddressState(it, address) })
        }

        address.confidenceCriteria = createConfidenceCriteria(dto.confidenceCriteria)
    }

    private fun createAddressesForLegalEntity(
        validRequests: Collection<AddressPartnerCreateRequest>,
        metadataMap: AddressMetadataMapping
    ): Collection<AddressPartnerCreateVerboseDto> {

        val bpnLsToFetch = validRequests.map { it.bpnParent }
        val parentLegalEntities = businessPartnerFetchService.fetchByBpns(bpnLsToFetch)
        val parentLegalEntitiesByBpn = parentLegalEntities.associateBy { it.bpn }

        val bpnAs = bpnIssuingService.issueAddressBpns(validRequests.size)
        val addressesWithIndex = validRequests
            .mapIndexed { i, request ->
                val legalEntity = parentLegalEntitiesByBpn[request.bpnParent]!!
                val address = createLogisticAddress(request.address, bpnAs[i], legalEntity, null, metadataMap)
                Pair(address, request.index)
            }

        logisticAddressRepository.saveAll(addressesWithIndex.map { (address, _) -> address })
        return addressesWithIndex.map { (address, index) -> address.toCreateResponse(index) }
    }

    fun updateAddresses(requests: Collection<AddressPartnerUpdateRequest>): AddressPartnerUpdateResponseWrapper {
        logger.info { "Update ${requests.size} business partner addresses" }

        val errorsByRequest = validateAddressesToUpdateFromController(requests)
        val errors = errorsByRequest.flatMap { it.value }
        val validRequests = requests.filterNot { errorsByRequest.containsKey(it) }

        val addresses = logisticAddressRepository.findDistinctByBpnIn(validRequests.map { it.bpna })
        val metadataMap = metadataService.getMetadata(validRequests.map { it.address }).toMapping()

        val addressRequestPairs = addresses.sortedBy { it.bpn }.zip(requests.sortedBy { it.bpna })
        addressRequestPairs.forEach { (address, request) ->
            val addressBeforeUpdate = businessPartnerEquivalenceMapper.toEquivalenceDto(address)
            updateLogisticAddress(address, request.address, metadataMap)
            val addressAfterUpdate = businessPartnerEquivalenceMapper.toEquivalenceDto(address)

            if (addressBeforeUpdate != addressAfterUpdate) {
                logger.info { "Address ${address.bpn} was updated" }

                logisticAddressRepository.save(address)

                changelogService.createChangelogEntries(listOf(ChangelogEntryCreateRequest(address.bpn, ChangelogType.UPDATE, BusinessPartnerType.ADDRESS)))
            }
        }

        val addressResponses = addresses.map { it.toDto() }

        return AddressPartnerUpdateResponseWrapper(addressResponses, errors)
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


}