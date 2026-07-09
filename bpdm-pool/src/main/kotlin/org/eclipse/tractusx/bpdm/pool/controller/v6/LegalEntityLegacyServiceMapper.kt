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
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityCreateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityUpdateError
import org.eclipse.tractusx.bpdm.pool.api.v6.model.*
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalEntityPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalEntityPartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.*
import org.eclipse.tractusx.bpdm.pool.entity.*
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.inbound.LegalEntityDtoRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.LegalEntityParseErrorMapper
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.parseAndExecute
import org.eclipse.tractusx.bpdm.pool.repository.IdentifierTypeRepository
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.eclipse.tractusx.bpdm.pool.service.*
import org.eclipse.tractusx.bpdm.pool.service.operation.LegalEntityCreateService
import org.eclipse.tractusx.bpdm.pool.service.operation.LegalEntityUpdateService
import org.eclipse.tractusx.bpdm.pool.service.parser.LegalEntityCreateParser
import org.eclipse.tractusx.bpdm.pool.service.parser.LegalEntityUpdateParser
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LegalEntityLegacyServiceMapper(
    private val legalEntityRepository: LegalEntityRepository,
    private val identifierTypeRepository: IdentifierTypeRepository,
    private val siteRepository: SiteRepository,
    private val logisticAddressRepository: LogisticAddressRepository,
    private val addressService: AddressService,
    private val legalEntityDtoRequestMapper: LegalEntityDtoRequestMapper,
    private val legalEntityCreateParser: LegalEntityCreateParser,
    private val legalEntityCreateService: LegalEntityCreateService,
    private val legalEntityUpdateParser: LegalEntityUpdateParser,
    private val legalEntityUpdateService: LegalEntityUpdateService,
    private val legalEntityParseErrorMapper: LegalEntityParseErrorMapper
) {

    companion object {
        // Retained for the v6 legacy tests that build the expected "too many identifiers" message; the live limit now
        // lives in org.eclipse.tractusx.bpdm.pool.util.ValidationLimits.
        const val IDENTIFIER_AMOUNT_LIMIT = 100
    }

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
            bpnSite = mainSite?.bpn,
            createdAt = createdAt,
            updatedAt = updatedAt,
            name = name,
            states = states.map { it.toDto() },
            identifiers = identifiers.map { it.toDto() },
            physicalPostalAddress = physicalPostalAddress.toDto(),
            alternativePostalAddress = alternativePostalAddress?.toDto(),
            confidenceCriteria = confidenceCriteria.toDto(),
            isCatenaXMemberData = legalEntity?.isCatenaXMemberData ?: mainSite?.legalEntity?.isCatenaXMemberData ?: false,
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

    private fun RelationDb.toDto(): RelationVerboseDto {
        return RelationVerboseDto(type, startNode.bpn, endNode.bpn)
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

        val page = logisticAddressRepository.findByLegalEntityAndSitesIsEmpty(legalEntity, PageRequest.of(pageIndex, pageSize))
        addressService.fetchLogisticAddressDependencies(page.map { it }.toSet())
        return page.toDto(page.content.map { it.toDto() })
    }

    /**
     * Create new business partner records from [requests]
     */
    @Transactional
    fun createLegalEntities(requests: Collection<LegalEntityPartnerCreateRequest>): LegalEntityPartnerCreateResponseWrapper {
        logger.info { "Create ${requests.size} new legal entities" }

        val requestList = requests.toList()
        val createRequests = requestList.map { legalEntityDtoRequestMapper.toCreateRequest(it) }

        val responses = mutableListOf<LegalEntityPartnerCreateVerboseDto>()
        val errors = mutableListOf<ErrorInfo<LegalEntityCreateError>>()
        requestList.zip(parseAndExecute(createRequests, legalEntityCreateParser::parse, legalEntityCreateService::create)).forEach { (request, result) ->
            when (result) {
                is ParseResult.Success -> responses.add(result.parsed.toUpsertDto(request.index))
                is ParseResult.Failure -> errors.addAll(result.errors.map { legalEntityParseErrorMapper.toCreateErrorInfo(it, request.index) })
            }
        }

        return LegalEntityPartnerCreateResponseWrapper(responses, errors)
    }

    fun LegalEntityDb.toUpsertDto(entryId: String?): LegalEntityPartnerCreateVerboseDto {
        return LegalEntityPartnerCreateVerboseDto(
            legalEntity = toDto(),
            legalAddress = legalAddress.toDto(),
            index = entryId
        )
    }

    /**
     * Update existing records with [requests]
     */
    @Transactional
    fun updateLegalEntities(requests: Collection<LegalEntityPartnerUpdateRequest>): LegalEntityPartnerUpdateResponseWrapper {
        logger.info { "Update ${requests.size} legal entities" }

        val requestList = requests.toList()
        val updateRequests = requestList.map { legalEntityDtoRequestMapper.toUpdateRequest(it) }

        val responses = mutableListOf<LegalEntityPartnerCreateVerboseDto>()
        val errors = mutableListOf<ErrorInfo<LegalEntityUpdateError>>()
        requestList.zip(parseAndExecute(updateRequests, legalEntityUpdateParser::parse, legalEntityUpdateService::update)).forEach { (request, result) ->
            when (result) {
                is ParseResult.Success -> responses.add(result.parsed.value.toUpsertDto(request.bpnl))
                is ParseResult.Failure -> errors.addAll(result.errors.map { legalEntityParseErrorMapper.toUpdateErrorInfo(it, request.bpnl) })
            }
        }

        return LegalEntityPartnerUpdateResponseWrapper(responses, errors)
    }
}
