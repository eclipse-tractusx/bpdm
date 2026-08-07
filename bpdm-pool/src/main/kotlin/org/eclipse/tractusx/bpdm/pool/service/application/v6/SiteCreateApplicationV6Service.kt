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

package org.eclipse.tractusx.bpdm.pool.service.application.v6

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteCreateError
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.SiteCreateRequestWithLegalAddressAsMainV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.SitePartnerCreateRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SitePartnerCreateResponseWrapperV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SitePartnerCreateVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.toV6UpsertDto
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.toV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.toV7
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound.SiteDtoRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.SiteParseErrorMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.SiteResponseMapper
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.parseAndExecute
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.service.operation.SiteCreateService
import org.eclipse.tractusx.bpdm.pool.service.operation.SiteCreateWithReferencedAddressAsMainService
import org.eclipse.tractusx.bpdm.pool.service.parser.SiteCreateParser
import org.eclipse.tractusx.bpdm.pool.service.parser.SiteCreateWithLegalAddressAsMainParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The REST-API boundary for the legacy v6 "create sites" operations, using the v6 request/response shapes.
 */
@Service
class SiteCreateApplicationV6Service(
    private val siteCreateParser: SiteCreateParser,
    private val siteCreateService: SiteCreateService,
    private val siteCreateWithLegalAddressAsMainParser: SiteCreateWithLegalAddressAsMainParser,
    private val siteCreateWithReferencedAddressAsMainService: SiteCreateWithReferencedAddressAsMainService,
    private val siteDtoRequestMapper: SiteDtoRequestMapper,
    private val siteParseErrorMapper: SiteParseErrorMapper,
    private val siteResponseMapper: SiteResponseMapper,
    private val legalEntityRepository: LegalEntityRepository
) {

    private val logger = KotlinLogging.logger { }

    /**
     * Creates the requested sites, each with a main address of its own, and returns, per request, either the created
     * site or the errors that stopped it.
     */
    @Transactional
    fun createSitesWithMainAddress(requests: Collection<SitePartnerCreateRequestV6>): SitePartnerCreateResponseWrapperV6 {
        logger.info { "Create ${requests.size} new sites" }

        val requestList = requests.toList()
        val createRequests = requestList.map { siteDtoRequestMapper.toCreateRequest(it.toV7()) }

        val responses = mutableListOf<SitePartnerCreateVerboseDtoV6>()
        val errors = mutableListOf<ErrorInfo<SiteCreateError>>()
        requestList.zip(parseAndExecute(createRequests, siteCreateParser::parse, siteCreateService::create)).forEach { (request, result) ->
            when (result) {
                is ParseResult.Success -> responses.add(siteResponseMapper.toUpsertResponse(result.parsed, request.index).toV6UpsertDto())
                is ParseResult.Failure -> errors.addAll(result.errors.map { siteParseErrorMapper.toCreateErrorInfo(it, request.index) })
            }
        }

        return SitePartnerCreateResponseWrapperV6(responses, errors.map { it.toV6() })
    }

    /**
     * Creates the requested sites, each taking its parent legal entity's legal address as the site main address, and
     * returns, per request, either the created site or the errors that stopped it.
     *
     * Enforces a v6-only rule that a legal address may back at most one site: offenders are pre-filtered out as errors
     * here, because the shared parse/create path deliberately does not enforce it — v7 and the task path allow it.
     */
    @Transactional
    fun createSitesWithLegalAddressAsMain(requests: Collection<SiteCreateRequestWithLegalAddressAsMainV6>): SitePartnerCreateResponseWrapperV6 {
        logger.info { "Create ${requests.size} new sites with legal address as site main address" }

        val requestList = requests.toList()
        val legalEntitiesByBpn = legalEntityRepository.findDistinctByBpnIn(requestList.map { it.bpnLParent }).associateBy { it.bpn }

        val responses = mutableListOf<SitePartnerCreateVerboseDtoV6>()
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

        val createRequests = validRequests.map { siteDtoRequestMapper.toCreateWithLegalAddressAsMainRequest(it.toV7()) }
        parseAndExecute(createRequests, siteCreateWithLegalAddressAsMainParser::parse, siteCreateWithReferencedAddressAsMainService::create)
            .forEachIndexed { index, result ->
                val entityKey = index.toString()
                when (result) {
                    is ParseResult.Success -> responses.add(siteResponseMapper.toUpsertResponse(result.parsed, entityKey).toV6UpsertDto())
                    is ParseResult.Failure -> errors.addAll(result.errors.map { siteParseErrorMapper.toCreateErrorInfo(it, entityKey) })
                }
            }

        return SitePartnerCreateResponseWrapperV6(responses, errors.map { it.toV6() })
    }
}
