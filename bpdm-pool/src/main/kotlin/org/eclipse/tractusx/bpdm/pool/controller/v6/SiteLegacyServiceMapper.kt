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
import org.eclipse.tractusx.bpdm.pool.api.model.request.SiteCreateRequestWithLegalAddressAsMain
import org.eclipse.tractusx.bpdm.pool.api.model.request.SitePartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.SitePartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteCreateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteUpdateError
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.SiteVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SitePartnerCreateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SitePartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SitePartnerUpdateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SiteWithMainAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound.SiteDtoRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.SiteParseErrorMapper
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.parseAndExecute
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.eclipse.tractusx.bpdm.pool.service.*
import org.eclipse.tractusx.bpdm.pool.service.operation.SiteCreateService
import org.eclipse.tractusx.bpdm.pool.service.operation.SiteCreateWithReferencedAddressAsMainService
import org.eclipse.tractusx.bpdm.pool.service.operation.SiteUpdateService
import org.eclipse.tractusx.bpdm.pool.service.parser.SiteCreateParser
import org.eclipse.tractusx.bpdm.pool.service.parser.SiteCreateWithLegalAddressAsMainParser
import org.eclipse.tractusx.bpdm.pool.service.parser.SiteUpdateParser
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SiteLegacyServiceMapper(
    private val siteRepository: SiteRepository,
    private val legalEntityRepository: LegalEntityRepository,
    private val addressService: AddressService,
    private val siteDtoRequestMapper: SiteDtoRequestMapper,
    private val siteParseErrorMapper: SiteParseErrorMapper,
    private val siteCreateParser: SiteCreateParser,
    private val siteCreateService: SiteCreateService,
    private val siteCreateWithLegalAddressAsMainParser: SiteCreateWithLegalAddressAsMainParser,
    private val siteCreateWithReferencedAddressAsMainService: SiteCreateWithReferencedAddressAsMainService,
    private val siteUpdateParser: SiteUpdateParser,
    private val siteUpdateService: SiteUpdateService
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

        val requestList = requests.toList()
        val createRequests = requestList.map { siteDtoRequestMapper.toCreateRequest(it) }

        val responses = mutableListOf<SitePartnerCreateVerboseDto>()
        val errors = mutableListOf<ErrorInfo<SiteCreateError>>()
        requestList.zip(parseAndExecute(createRequests, siteCreateParser::parse, siteCreateService::create)).forEach { (request, result) ->
            when (result) {
                is ParseResult.Success -> responses.add(result.parsed.toUpsertDto(request.index))
                is ParseResult.Failure -> errors.addAll(result.errors.map { siteParseErrorMapper.toCreateErrorInfo(it, request.index) })
            }
        }

        return SitePartnerCreateResponseWrapper(responses, errors)
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

    @Transactional
    fun updateSites(requests: Collection<SitePartnerUpdateRequest>): SitePartnerUpdateResponseWrapper {
        logger.info { "Update ${requests.size} sites" }

        val requestList = requests.toList()
        val updateRequests = requestList.map { siteDtoRequestMapper.toUpdateRequest(it) }

        val responses = mutableListOf<SitePartnerCreateVerboseDto>()
        val errors = mutableListOf<ErrorInfo<SiteUpdateError>>()
        requestList.zip(parseAndExecute(updateRequests, siteUpdateParser::parse, siteUpdateService::update)).forEach { (request, result) ->
            when (result) {
                is ParseResult.Success -> responses.add(result.parsed.value.toUpsertDto(request.bpns))
                is ParseResult.Failure -> errors.addAll(result.errors.map { siteParseErrorMapper.toUpdateErrorInfo(it, request.bpns) })
            }
        }

        return SitePartnerUpdateResponseWrapper(responses, errors)
    }

    /**
     * Creates sites whose main address is the parent legal entity's own legal address.
     *
     * The duplicate guard (a legal address may back at most one site) is a v6-only rule kept here deliberately: the shared
     * [SiteCreateWithLegalAddressAsMainParser] intentionally does not enforce it (v7 and the cleaning/task path allow it),
     * so this method pre-filters offenders and only delegates the survivors to the shared parse/create path.
     */
    @Transactional
    fun createSitesWithLegalAddressAsMain(requests: Collection<SiteCreateRequestWithLegalAddressAsMain>): SitePartnerCreateResponseWrapper {
        logger.info { "Create ${requests.size} new sites with legal address as site main address" }

        val requestList = requests.toList()
        val legalEntitiesByBpn = legalEntityRepository.findDistinctByBpnIn(requestList.map { it.bpnLParent }).associateBy { it.bpn }

        val responses = mutableListOf<SitePartnerCreateVerboseDto>()
        val errors = mutableListOf<ErrorInfo<SiteCreateError>>()

        val validRequests = requestList.filter { request ->
            val legalEntity = legalEntitiesByBpn[request.bpnLParent]
            when {
                legalEntity == null -> {
                    errors.add(
                        ErrorInfo(
                            SiteCreateError.LegalEntityNotFound,
                            "Parent ${request.bpnLParent} not found for site to create",
                            request.bpnLParent
                        )
                    )
                    false
                }

                legalEntity.legalAddress.sites.isNotEmpty() -> {
                    errors.add(
                        ErrorInfo(
                            SiteCreateError.MainAddressDuplicateIdentifier,
                            "Can't create site for legal entity ${request.bpnLParent} with legal address as site main address: " +
                                    "Legal address already belongs to site ${legalEntity.legalAddress.sites.first().bpn}",
                            request.name
                        )
                    )
                    false
                }

                else -> true
            }
        }

        val createRequests = validRequests.map { siteDtoRequestMapper.toCreateWithLegalAddressAsMainRequest(it) }
        parseAndExecute(createRequests, siteCreateWithLegalAddressAsMainParser::parse, siteCreateWithReferencedAddressAsMainService::create)
            .forEachIndexed { index, result ->
                val entityKey = index.toString()
                when (result) {
                    is ParseResult.Success -> responses.add(result.parsed.toUpsertDto(entityKey))
                    is ParseResult.Failure -> errors.addAll(result.errors.map { siteParseErrorMapper.toCreateErrorInfo(it, entityKey) })
                }
            }

        return SitePartnerCreateResponseWrapper(responses, errors)
    }
}
