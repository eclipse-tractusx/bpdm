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

package org.eclipse.tractusx.bpdm.pool.controller.v6

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.*
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.util.findDuplicates
import org.eclipse.tractusx.bpdm.common.util.mergeMapsWithCollectionInValue
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorCode
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityCreateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityUpdateError
import org.eclipse.tractusx.bpdm.pool.api.v6.model.*
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalEntityPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalEntityPartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityPartnerCreateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityPartnerUpdateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.dto.AddressMetadataDto
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.dto.LegalEntityMetadataDto
import org.eclipse.tractusx.bpdm.pool.entity.*
import org.eclipse.tractusx.bpdm.pool.repository.*
import org.eclipse.tractusx.bpdm.pool.service.*
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService.AddressMetadataMapping
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService.Companion.createAlternativeAddress
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService.Companion.createConfidenceCriteria
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService.Companion.createCurrentnessTimestamp
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService.Companion.createPhysicalAddress
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService.Companion.toAddressIdentifier
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService.Companion.toAddressState
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService.Companion.toLegalEntityIdentifier
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService.Companion.toLegalEntityState
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService.LegalEntityMetadataMapping
import org.eclipse.tractusx.bpdm.pool.service.RequestValidationService.*
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class LegalEntityLegacyServiceMapper(
    private val legalEntityRepository: LegalEntityRepository,
    private val identifierTypeRepository: IdentifierTypeRepository,
    private val siteRepository: SiteRepository,
    private val logisticAddressRepository: LogisticAddressRepository,
    private val legalEntityIdentifierRepository: LegalEntityIdentifierRepository,
    private val addressIdentifierRepository: AddressIdentifierRepository,
    private val addressService: AddressService,
    private val businessPartnerFetchService: BusinessPartnerFetchService,
    private val metadataService: MetadataService,
    private val changelogService: PartnerChangelogService,
    private val bpnIssuingService: BpnIssuingService,
    private val businessPartnerEquivalenceMapper: BusinessPartnerEquivalenceMapper
) {

    private val logger = KotlinLogging.logger { }

    /**
     * Search legal entities per page for [searchRequest] and [paginationRequest]
     */
    @Transactional
    fun searchLegalEntities(searchRequest: LegalEntitySearchRequest, paginationRequest: PaginationRequest): PageDto<LegalEntityWithLegalAddressVerboseDto> {
        val spec = Specification.allOf(
            LegalEntityRepository.byBpns(searchRequest.bpnLs),
            LegalEntityRepository.byLegalName(searchRequest.legalName),
            LegalEntityRepository.byIsMember(searchRequest.isCatenaXMemberData)
        )

        val legalEntityPage = legalEntityRepository.findAll(spec, PageRequest.of(paginationRequest.page, paginationRequest.size))

        return legalEntityPage.toDto(::toLegalEntityWithLegalAddress)
    }

    /**
     * Fetch a business partner by [bpn] and return as [LegalEntityWithLegalAddressVerboseDto]
     */
    fun findLegalEntityIgnoreCase(bpn: String): LegalEntityWithLegalAddressVerboseDto {
        logger.debug { "Executing findLegalEntityIgnoreCase() with parameters $bpn" }
        val legalEntity = findLegalEntityOrThrow(bpn)
        return toLegalEntityWithLegalAddress(legalEntity)
    }

    /**
     * Fetch a business partner by [identifierValue] (ignoring case) of [identifierType] and return as [LegalEntityWithLegalAddressVerboseDto]
     */
    @Transactional
    fun findLegalEntityIgnoreCase(identifierType: String, identifierValue: String): LegalEntityWithLegalAddressVerboseDto {
        logger.debug { "Executing findLegalEntityIgnoreCase() with parameters $identifierType and $identifierValue" }
        val legalEntity = findLegalEntityOrThrow(identifierType, identifierValue)
        return toLegalEntityWithLegalAddress(legalEntity)
    }

    private fun findLegalEntityOrThrow(bpn: String): LegalEntityDb {
        return legalEntityRepository.findByBpnIgnoreCase(bpn) ?: throw BpdmNotFoundException(LegalEntityDb::class.simpleName!!, bpn)
    }

    fun findLegalEntityOrThrow(identifierTypeKey: String, identifierValue: String): LegalEntityDb {
        val identifierType = findIdentifierTypeOrThrow(identifierTypeKey, IdentifierBusinessPartnerType.LEGAL_ENTITY)
        return legalEntityRepository.findByIdentifierTypeAndValueIgnoreCase(identifierType, identifierValue)
            ?: throw BpdmNotFoundException("Identifier Value", identifierValue)
    }

    private fun findIdentifierTypeOrThrow(identifierTypeKey: String, businessPartnerType: IdentifierBusinessPartnerType) =
        identifierTypeRepository.findByBusinessPartnerTypeAndTechnicalKey(businessPartnerType, identifierTypeKey)
            ?: throw BpdmNotFoundException(IdentifierTypeDb::class, "$identifierTypeKey/$businessPartnerType")


    fun toLegalEntityWithLegalAddress(entity: LegalEntityDb): LegalEntityWithLegalAddressVerboseDto {
        return LegalEntityWithLegalAddressVerboseDto(
            legalAddress = entity.legalAddress.toDto(),
            legalEntity = entity.toDto()
        )
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

    fun LegalEntityDb.toDto(): LegalEntityVerboseDto {
        return LegalEntityVerboseDto(
            bpnl = bpn,
            legalName = legalName.value,
            legalShortName = legalName.shortName,
            legalFormVerbose = legalForm?.toDto(),
            identifiers = identifiers.map { it.toDto() },
            states = states.map { it.toDto() },
            relations = startNodeRelations.plus(endNodeRelations).map { it.toDto() },
            currentness = currentness,
            confidenceCriteria = confidenceCriteria.toDto(),
            isCatenaXMemberData = isCatenaXMemberData,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    fun LegalFormDb.toDto(): LegalFormDto {
        return LegalFormDto(
            technicalKey = technicalKey,
            name = name,
            transliteratedName = transliteratedName,
            abbreviation = abbreviation,
            transliteratedAbbreviations = transliteratedAbbreviations,
            country = countryCode,
            language = languageCode,
            administrativeAreaLevel1 = administrativeArea?.regionCode,
            isActive = isActive
        )
    }

    data class LegalEntitySearchRequest(
        val bpnLs: List<String>?,
        val legalName: String?,
        val isCatenaXMemberData: Boolean?
    )

    fun findByParentBpn(bpn: String, pageIndex: Int, pageSize: Int): PageDto<SiteVerboseDto> {
        logger.debug { "Executing findByPartnerBpn() with parameters $bpn // $pageIndex // $pageSize" }
        val legalEntity = legalEntityRepository.findByBpnIgnoreCase(bpn) ?: throw BpdmNotFoundException("Business Partner", bpn)

        val page = siteRepository.findByLegalEntity(legalEntity, PageRequest.of(pageIndex, pageSize))
        fetchSiteDependencies(page.toSet())
        return page.toDto(page.content.map { it.toDto() })
    }

    private fun fetchSiteDependencies(sites: Set<SiteDb>) {
        siteRepository.joinAddresses(sites)
        siteRepository.joinStates(sites)
        val addresses = sites.flatMap { it.addresses }.toSet()
        addressService.fetchLogisticAddressDependencies(addresses)
    }

    fun SiteDb.toDto(): SiteVerboseDto {
        return SiteVerboseDto(
            bpn,
            name,
            states = states.map { it.toDto() },
            bpnLegalEntity = legalEntity.bpn,
            confidenceCriteria = confidenceCriteria.toDto(),
            isCatenaXMemberData = legalEntity.isCatenaXMemberData,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    /**
     * Find Addresses which directly belong to a Legal Entity
     */
    fun findNonSiteAddressesOfLegalEntity(bpnl: String, pageIndex: Int, pageSize: Int): PageDto<LogisticAddressVerboseDto> {
        logger.debug { "Executing findByPartnerBpn() with parameters $bpnl // $pageIndex // $pageSize" }
        val legalEntity = legalEntityRepository.findByBpnIgnoreCase(bpnl) ?:  throw BpdmNotFoundException("Business Partner", bpnl)

        val page = logisticAddressRepository.findByLegalEntityAndSiteIsNull(legalEntity, PageRequest.of(pageIndex, pageSize))
        addressService.fetchLogisticAddressDependencies(page.map { it }.toSet())
        return page.toDto(page.content.map { it.toDto() })
    }

    /**
     * Create new business partner records from [requests]
     */
    @Transactional
    fun createLegalEntities(requests: Collection<LegalEntityPartnerCreateRequest>): LegalEntityPartnerCreateResponseWrapper {
        logger.info { "Create ${requests.size} new legal entities" }

        val errorsByRequest = validateLegalEntitiesToCreateFromController(requests)
        val errors = errorsByRequest.flatMap { it.value }
        val validRequests = requests.filterNot { errorsByRequest.containsKey(it) }

        val legalEntityMetadataMap = metadataService.getMetadata(requests.map { it.legalEntity }).toMapping()
        val addressMetadataMap = metadataService.getMetadata(requests.map { it.legalAddress }).toMapping()

        val bpnLs = bpnIssuingService.issueLegalEntityBpns(validRequests.size)
        val bpnAs = bpnIssuingService.issueAddressBpns(validRequests.size)

        val requestsByLegalEntities = validRequests
            .mapIndexed { bpnIndex, request ->
                val legalEntity = createLegalEntity(request.legalEntity, bpnLs[bpnIndex], legalEntityMetadataMap)
                val legalAddress = createLogisticAddress(request.legalAddress, bpnAs[bpnIndex], legalEntity, null, addressMetadataMap)
                legalEntity.legalAddress = legalAddress
                Pair(legalEntity, request)
            }
            .toMap()

        val legalEntities = requestsByLegalEntities.keys

        changelogService.createChangelogEntries(legalEntities.map {
            ChangelogEntryCreateRequest(it.bpn, ChangelogType.CREATE, BusinessPartnerType.LEGAL_ENTITY)
        })
        changelogService.createChangelogEntries(legalEntities.map {
            ChangelogEntryCreateRequest(it.legalAddress.bpn, ChangelogType.CREATE, BusinessPartnerType.ADDRESS)
        })

        legalEntities.map {

            logger.info { logger.info { "Legal Entity ${it.bpn} was created" } }

        }
        legalEntityRepository.saveAll(legalEntities)

        val legalEntityResponse = legalEntities.map { it.toUpsertDto(requestsByLegalEntities[it]!!.index) }

        return LegalEntityPartnerCreateResponseWrapper(legalEntityResponse, errors)
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

    private data class IdentifierCandidateKey(
        val type: String,
        val value: String
    )

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

    val leCreateMessages = ValidatorErrorCodes(
        regionNotFound = LegalEntityCreateError.LegalAddressRegionNotFound,
        identifierNotFound = LegalEntityCreateError.LegalAddressIdentifierNotFound,
        duplicateIdentifier = LegalEntityCreateError.LegalAddressDuplicateIdentifier,
        identifiersTooMany = LegalEntityCreateError.LegalAddressIdentifiersTooMany
    )

    fun LegalEntityDb.toUpsertDto(entryId: String?): LegalEntityPartnerCreateVerboseDto {
        return LegalEntityPartnerCreateVerboseDto(
            legalEntity = toDto(),
            legalAddress = legalAddress.toDto(),
            index = entryId
        )
    }

    fun createLegalEntity(
        legalEntityDto: LegalEntityDto,
        bpnL: String,
        metadataMap: LegalEntityMetadataMapping
    ): LegalEntityDb {
        // it has to be validated that the legalForm exits
        val legalForm = legalEntityDto.legalForm?.let { metadataMap.legalForms[it]!! }
        val legalName = NameDb(value = legalEntityDto.legalName, shortName = legalEntityDto.legalShortName)
        val newLegalEntity = LegalEntityDb(
            bpn = bpnL,
            legalName = legalName,
            legalForm = legalForm,
            currentness = Instant.now().truncatedTo(ChronoUnit.MICROS),
            confidenceCriteria = createConfidenceCriteria(legalEntityDto.confidenceCriteria),
            isCatenaXMemberData = legalEntityDto.isCatenaXMemberData
        )
        updateLegalEntity(newLegalEntity, legalEntityDto, metadataMap)

        return newLegalEntity
    }

    fun updateLegalEntity(
        legalEntity: LegalEntityDb,
        legalEntityDto: LegalEntityDto,
        metadataMap: LegalEntityMetadataMapping
    ) {
        legalEntity.currentness = createCurrentnessTimestamp()

        legalEntity.legalName = NameDb(value = legalEntityDto.legalName, shortName = legalEntityDto.legalShortName)

        legalEntity.legalForm = legalEntityDto.legalForm?.let { metadataMap.legalForms[it]!! }

        legalEntity.identifiers.replace(legalEntityDto.identifiers.map { toLegalEntityIdentifier(it, metadataMap.idTypes, legalEntity) })
        legalEntity.states.replace(legalEntityDto.states.map { toLegalEntityState(it, legalEntity) })
        legalEntity.confidenceCriteria = createConfidenceCriteria(legalEntityDto.confidenceCriteria)
        legalEntity.isCatenaXMemberData = legalEntityDto.isCatenaXMemberData
    }

    private fun LegalEntityMetadataDto.toMapping() =
        LegalEntityMetadataMapping(
            idTypes = idTypes.associateBy { it.technicalKey },
            legalForms = legalForms.associateBy { it.technicalKey }
        )

    private fun AddressMetadataDto.toMapping() =
        AddressMetadataMapping(
            idTypes = idTypes.associateBy { it.technicalKey },
            regions = regions.associateBy { it.regionCode }
        )

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

    /**
     * Update existing records with [requests]
     */
    @Transactional
    fun updateLegalEntities(requests: Collection<LegalEntityPartnerUpdateRequest>): LegalEntityPartnerUpdateResponseWrapper {
        logger.info { "Update ${requests.size} legal entities" }

        val errorsByRequest = validateLegalEntitiesToUpdateFromController(requests)
        val errors = errorsByRequest.flatMap { it.value }
        val validRequests = requests.filterNot { errorsByRequest.containsKey(it) }

        val legalEntityMetadataMap = metadataService.getMetadata(validRequests.map { it.legalEntity }).toMapping()
        val addressMetadataMap = metadataService.getMetadata(validRequests.map { it.legalAddress }).toMapping()

        val bpnsToFetch = validRequests.map { it.bpnl }
        val legalEntities = legalEntityRepository.findDistinctByBpnIn(bpnsToFetch)
        businessPartnerFetchService.fetchDependenciesWithLegalAddress(legalEntities)
        val requestsByBpn = validRequests.associateBy { it.bpnl }

        val legalEntityRequestPairs = legalEntities.map { legalEntity -> Pair(legalEntity, requestsByBpn[legalEntity.bpn]!!) }
        legalEntityRequestPairs.forEach { (legalEntity, request) ->
            val legalEntityBeforeUpdate = businessPartnerEquivalenceMapper.toEquivalenceDto(legalEntity)
            updateLegalEntity(legalEntity, request.legalEntity, legalEntityMetadataMap)
            updateLogisticAddress(legalEntity.legalAddress, request.legalAddress, addressMetadataMap)
            val legalEntityAfterUpdate = businessPartnerEquivalenceMapper.toEquivalenceDto(legalEntity)

            if (legalEntityBeforeUpdate != legalEntityAfterUpdate) {
                logger.info { "Legal Entity ${legalEntity.bpn} was updated" }

                legalEntityRepository.save(legalEntity)

                changelogService.createChangelogEntries(
                    listOf(
                        ChangelogEntryCreateRequest(
                            legalEntity.bpn,
                            ChangelogType.UPDATE,
                            BusinessPartnerType.LEGAL_ENTITY
                        )
                    )
                )
                changelogService.createChangelogEntries(
                    listOf(
                        ChangelogEntryCreateRequest(
                            legalEntity.legalAddress.bpn,
                            ChangelogType.UPDATE,
                            BusinessPartnerType.ADDRESS
                        )
                    )
                )
            }
        }

        val legalEntityResponses = legalEntityRequestPairs.map { (legalEntity, request) -> legalEntity.toUpsertDto(request.bpnl) }

        return LegalEntityPartnerUpdateResponseWrapper(legalEntityResponses, errors)
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

    val leUpdateMessages = ValidatorErrorCodes(
        regionNotFound = LegalEntityUpdateError.LegalAddressRegionNotFound,
        identifierNotFound = LegalEntityUpdateError.LegalAddressIdentifierNotFound,
        duplicateIdentifier = LegalEntityUpdateError.LegalAddressDuplicateIdentifier,
        identifiersTooMany = LegalEntityUpdateError.LegalAddressIdentifiersTooMany
    )

}