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
import org.eclipse.tractusx.bpdm.pool.api.model.request.SiteCreateRequestWithLegalAddressAsMain
import org.eclipse.tractusx.bpdm.pool.api.model.request.SitePartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteCreateError
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SitePartnerCreateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SitePartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.toUpsertDto
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound.SiteDtoRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.SiteParseErrorMapper
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
 * Application service for the v6 "create sites" operations: the boundary between the legacy v6 REST API and the domain.
 * Mirrors [org.eclipse.tractusx.bpdm.pool.service.application.v7.SiteCreateApplicationV7Service] but maps the per-entry
 * verdicts back into the versioned `api.v6.model` response shapes.
 *
 * `@Transactional` so parse and execute share one persistence context: the site-create parsers resolve the legal-entity
 * parent and validate content, then the site-create services persist the sites and their main addresses.
 */
@Service
class SiteCreateApplicationV6Service(
    private val siteCreateParser: SiteCreateParser,
    private val siteCreateService: SiteCreateService,
    private val siteCreateWithLegalAddressAsMainParser: SiteCreateWithLegalAddressAsMainParser,
    private val siteCreateWithReferencedAddressAsMainService: SiteCreateWithReferencedAddressAsMainService,
    private val siteDtoRequestMapper: SiteDtoRequestMapper,
    private val siteParseErrorMapper: SiteParseErrorMapper,
    private val legalEntityRepository: LegalEntityRepository
) {

    private val logger = KotlinLogging.logger { }

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
