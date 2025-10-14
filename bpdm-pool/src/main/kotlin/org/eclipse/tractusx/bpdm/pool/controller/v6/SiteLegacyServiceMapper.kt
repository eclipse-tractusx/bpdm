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
import org.eclipse.tractusx.bpdm.common.util.findDuplicates
import org.eclipse.tractusx.bpdm.common.util.mergeMapsWithCollectionInValue
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.SiteCreateRequestWithLegalAddressAsMain
import org.eclipse.tractusx.bpdm.pool.api.model.request.SitePartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.SitePartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorCode
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteCreateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteUpdateError
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.SiteVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SitePartnerCreateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SitePartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SitePartnerUpdateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SiteWithMainAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.dto.AddressMetadataDto
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.repository.AddressIdentifierRepository
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.eclipse.tractusx.bpdm.pool.service.*
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService.AddressMetadataMapping
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService.Companion.createAlternativeAddress
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService.Companion.createConfidenceCriteria
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService.Companion.createPhysicalAddress
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService.Companion.createSite
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService.Companion.toAddressIdentifier
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService.Companion.toAddressState
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService.Companion.updateSite
import org.eclipse.tractusx.bpdm.pool.service.RequestValidationService.*
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SiteLegacyServiceMapper(
    private val siteRepository: SiteRepository,
    private val legalEntityRepository: LegalEntityRepository,
    private val addressService: AddressService,
    private val metadataService: MetadataService,
    private val addressIdentifierRepository: AddressIdentifierRepository,
    private val bpnIssuingService: BpnIssuingService,
    private val changelogService: PartnerChangelogService,
    private val businessPartnerEquivalenceMapper: BusinessPartnerEquivalenceMapper
) {

    private val logger = KotlinLogging.logger { }

    fun findByBpn(bpn: String): SiteWithMainAddressVerboseDto {
        logger.debug { "Executing findByBpn() with parameters $bpn " }
        val site = siteRepository.findByBpn(bpn) ?: throw BpdmNotFoundException("Site", bpn)
        return toPoolDto(site)
    }

    fun toPoolDto(entity: SiteDb): SiteWithMainAddressVerboseDto {
        return SiteWithMainAddressVerboseDto(

            site = SiteVerboseDto(
                entity.bpn,
                entity.name,
                states = entity.states.map { it.toDto() },
                bpnLegalEntity = entity.legalEntity.bpn,
                confidenceCriteria = entity.confidenceCriteria.toDto(),
                isCatenaXMemberData = entity.legalEntity.isCatenaXMemberData,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
            ),
            mainAddress = entity.mainAddress.toDto()
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

    /**
     * Search sites per page for [searchRequest] and [paginationRequest]
     */
    @Transactional
    fun searchSites(searchRequest: SiteSearchRequest, paginationRequest: PaginationRequest): PageDto<SiteWithMainAddressVerboseDto> {
        logger.debug { "Executing site search with request: $searchRequest" }
        val spec = Specification.allOf(
            SiteRepository.byBpns(searchRequest.siteBpns),
            SiteRepository.byParentBpns(searchRequest.legalEntityBpns),
            SiteRepository.byName(searchRequest.name),
            SiteRepository.byIsMember(searchRequest.isCatenaXMemberData)
        )

        val sitePage = siteRepository.findAll(spec, PageRequest.of(paginationRequest.page, paginationRequest.size))

        fetchSiteDependencies(sitePage.toSet())

        return sitePage.toDto(::toPoolDto)
    }

    private fun fetchSiteDependencies(sites: Set<SiteDb>) {
        siteRepository.joinAddresses(sites)
        siteRepository.joinStates(sites)
        val addresses = sites.flatMap { it.addresses }.toSet()
        addressService.fetchLogisticAddressDependencies(addresses)
    }

    data class SiteSearchRequest(
        val siteBpns: List<String>?,
        val legalEntityBpns: List<String>?,
        val name: String?,
        val isCatenaXMemberData: Boolean?
    )

    @Transactional
    fun createSitesWithMainAddress(requests: Collection<SitePartnerCreateRequest>): SitePartnerCreateResponseWrapper {
        logger.info { "Create ${requests.size} new sites" }

        val errorsByRequest = validateSitesToCreateFromController(requests)
        val errors = errorsByRequest.flatMap { it.value }
        val validRequests = requests.filterNot { errorsByRequest.containsKey(it) }

        val validMainAddresses = validRequests.map { it.site.mainAddress }
        val addressMetadataMap = metadataService.getMetadata(validMainAddresses).toMapping()

        val legalEntities = legalEntityRepository.findDistinctByBpnIn(validRequests.map { it.bpnlParent })
        val legalEntitiesByBpn = legalEntities.associateBy { it.bpn }

        val bpnSs = bpnIssuingService.issueSiteBpns(validRequests.size)
        val bpnAs = bpnIssuingService.issueAddressBpns(validRequests.size)

        fun createSiteWithMainAddress(bpnIndex: Int, request: SitePartnerCreateRequest) =
            createSite(request.site, bpnSs[bpnIndex], legalEntitiesByBpn[request.bpnlParent]!!)
                .apply { mainAddress = createLogisticAddress(request.site.mainAddress, bpnAs[bpnIndex], this.legalEntity, this, addressMetadataMap) }
                .let { site -> Pair(site, request) }

        val requestsBySites = validRequests
            .mapIndexed { i, request -> createSiteWithMainAddress(i, request) }
            .toMap()

        val siteResponse = createChangeLogAndSaveSiteInformation(requestsBySites).map { it.toUpsertDto(requestsBySites[it]!!.index) }

        return SitePartnerCreateResponseWrapper(siteResponse, errors)
    }

    fun SiteDb.toUpsertDto(entryId: String?): SitePartnerCreateVerboseDto {
        return SitePartnerCreateVerboseDto(
            site = toDto(),
            mainAddress = mainAddress.toDto(),
            index = entryId
        )
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

    fun validateSitesToCreateFromController(
        siteRequests: Collection<SitePartnerCreateRequest>,
    ): Map<RequestWithKey, Collection<ErrorInfo<SiteCreateError>>> {

        val siteErrorsByRequest = validateSitesToCreate(siteRequests)
        val addressErrorsByRequest = validateAddresses(siteRequests.map {
            AddressBridge(address = it.site.mainAddress, request = it, bpnA = null)
        }, siteCreateMessages)
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

    private fun <ERROR : ErrorCode> validateParentBpnExists(parentBpn: String, entityKey: String?, existingParentBpns: Set<String>, errorCode: ERROR)
            : Collection<ErrorInfo<ERROR>> {
        return if (!existingParentBpns.contains(parentBpn))
            listOf(ErrorInfo(errorCode, "Parent with BPN '$parentBpn'not found", entityKey))
        else
            emptyList()
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

    private data class IdentifierCandidateKey(
        val type: String,
        val value: String
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

    private fun createChangeLogAndSaveSiteInformation(requestsBySites: Map<SiteDb, SitePartnerCreateRequest>): Set<SiteDb> {
        val sites = requestsBySites.keys

        changelogService.createChangelogEntries(sites.map {
            ChangelogEntryCreateRequest(it.bpn, ChangelogType.CREATE, BusinessPartnerType.SITE)
        })
        changelogService.createChangelogEntries(sites.map {
            ChangelogEntryCreateRequest(it.mainAddress.bpn, ChangelogType.CREATE, BusinessPartnerType.ADDRESS)
        })

        sites.map {

            logger.info { "Site ${it.bpn} was created" }

        }

        siteRepository.saveAll(sites)
        return sites
    }

    val siteCreateMessages = ValidatorErrorCodes(
        regionNotFound = SiteCreateError.MainAddressRegionNotFound,
        identifierNotFound = SiteCreateError.MainAddressIdentifierNotFound,
        duplicateIdentifier = SiteCreateError.MainAddressDuplicateIdentifier,
        identifiersTooMany = SiteCreateError.MainAddressIdentifiersTooMany
    )

    @Transactional
    fun updateSites(requests: Collection<SitePartnerUpdateRequest>): SitePartnerUpdateResponseWrapper {
        logger.info { "Update ${requests.size} sites" }

        val errorsByRequest = validateSitesToUpdateFromController(requests)
        val errors = errorsByRequest.flatMap { it.value }
        val validRequests = requests.filterNot { errorsByRequest.containsKey(it) }

        val addressMetadataMap = metadataService.getMetadata(requests.map { it.site.mainAddress }).toMapping()

        val bpnsToFetch = validRequests.map { it.bpns }
        val sites = siteRepository.findDistinctByBpnIn(bpnsToFetch)
        val requestByBpnMap = validRequests.associateBy { it.bpns }

        val siteRequestPairs = sites.map { site -> Pair(site, requestByBpnMap[site.bpn]!!) }
        siteRequestPairs.forEach { (site, request) ->
            val siteBeforeUpdate = businessPartnerEquivalenceMapper.toEquivalenceDto(site)
            updateSite(site, request.site)
            updateLogisticAddress(site.mainAddress, request.site.mainAddress, addressMetadataMap)
            val siteAfterUpdate = businessPartnerEquivalenceMapper.toEquivalenceDto(site)

            if (siteBeforeUpdate != siteAfterUpdate) {
                logger.info { "Site ${site.bpn} was updated" }

                siteRepository.save(site)

                changelogService.createChangelogEntries(listOf(ChangelogEntryCreateRequest(site.bpn, ChangelogType.UPDATE, BusinessPartnerType.SITE)))
                changelogService.createChangelogEntries(
                    listOf(
                        ChangelogEntryCreateRequest(
                            site.mainAddress.bpn,
                            ChangelogType.UPDATE,
                            BusinessPartnerType.ADDRESS
                        )
                    )
                )
            }
        }

        val siteResponses = siteRequestPairs.map { (site, request) -> site.toUpsertDto(request.bpns) }

        return SitePartnerUpdateResponseWrapper(siteResponses, errors)
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


    val siteUpdateMessages = ValidatorErrorCodes(
        regionNotFound = SiteUpdateError.MainAddressRegionNotFound,
        identifierNotFound = SiteUpdateError.MainAddressIdentifierNotFound,
        duplicateIdentifier = SiteUpdateError.MainAddressDuplicateIdentifier,
        identifiersTooMany = SiteUpdateError.MainAddressIdentifiersTooMany
    )

    fun createSitesWithLegalAddressAsMain(requests: Collection<SiteCreateRequestWithLegalAddressAsMain>): SitePartnerCreateResponseWrapper {
        logger.info { "Create ${requests.size} new sites with legal address as site main address" }

        val legalEntities = legalEntityRepository.findDistinctByBpnIn(requests.map { it.bpnLParent })
        val legalEntitiesByBpn = legalEntities.associateBy { it.bpn }

        val bpnSs = bpnIssuingService.issueSiteBpns(requests.size)

        val createdSites = requests.zip(bpnSs).map { (siteRequest, bpnS) ->
            val legalEntityParent =
                legalEntitiesByBpn[siteRequest.bpnLParent] ?: throw BpdmValidationException("Parent ${siteRequest.bpnLParent} not found for site to create")

            if(legalEntityParent.legalAddress.site != null)
                throw BpdmValidationException("Can't create site for legal entity ${siteRequest.bpnLParent} with legal address as site main address: Legal address already belongs to site ${legalEntityParent.legalAddress.site!!.bpn}")

            createSite(siteRequest, bpnS, legalEntityParent)
                .apply { mainAddress = legalEntityParent.legalAddress }
                .apply { mainAddress.site = this }
        }

        siteRepository.saveAll(createdSites)

        changelogService.createChangelogEntries(createdSites.map {
            ChangelogEntryCreateRequest(it.bpn, ChangelogType.CREATE, BusinessPartnerType.SITE)
        })

        val siteResponse = createdSites.mapIndexed { index, site -> site.toUpsertDto(index.toString()) }

        return SitePartnerCreateResponseWrapper(siteResponse, emptyList())

    }

}