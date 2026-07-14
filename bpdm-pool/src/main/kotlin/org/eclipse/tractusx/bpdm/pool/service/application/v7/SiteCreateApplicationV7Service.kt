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

package org.eclipse.tractusx.bpdm.pool.service.application.v7

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.api.model.request.SiteCreateRequestWithLegalAddressAsMain
import org.eclipse.tractusx.bpdm.pool.api.model.request.SitePartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteCreateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.SitePartnerCreateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.model.response.SitePartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound.SiteDtoRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.SiteParseErrorMapper
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.parseAndExecute
import org.eclipse.tractusx.bpdm.pool.service.operation.SiteCreateService
import org.eclipse.tractusx.bpdm.pool.service.operation.SiteCreateWithReferencedAddressAsMainService
import org.eclipse.tractusx.bpdm.pool.service.parser.SiteCreateParser
import org.eclipse.tractusx.bpdm.pool.service.parser.SiteCreateWithLegalAddressAsMainParser
import org.eclipse.tractusx.bpdm.pool.service.toUpsertDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Application service for the V7 "create sites" operations: the boundary between the REST API and the domain. It accepts
 * the API site-create requests, translates them into the internal domain model, drives the parse/execute pipeline, and
 * maps the per-entry verdicts back into the API [SitePartnerCreateResponseWrapper]. It holds no business rules of its
 * own — validation, parent resolution and persistence all live in the collaborators it orchestrates.
 *
 * Two create variants share this boundary because both produce a site under a legal-entity parent:
 * [createSitesWithMainAddress] takes a distinct main address, while [createSitesWithLegalAddressAsMain] reuses the
 * parent's legal address as the site main address.
 *
 * `@Transactional` so parse and execute share one persistence context: the site-create parsers resolve the legal-entity
 * parent and validate content, then the site-create services persist the sites and their main addresses.
 */
@Service
class SiteCreateApplicationV7Service(
    private val siteCreateParser: SiteCreateParser,
    private val siteCreateService: SiteCreateService,
    private val siteCreateWithLegalAddressAsMainParser: SiteCreateWithLegalAddressAsMainParser,
    private val siteCreateWithReferencedAddressAsMainService: SiteCreateWithReferencedAddressAsMainService,
    private val siteDtoRequestMapper: SiteDtoRequestMapper,
    private val siteParseErrorMapper: SiteParseErrorMapper
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

    @Transactional
    fun createSitesWithLegalAddressAsMain(requests: Collection<SiteCreateRequestWithLegalAddressAsMain>): SitePartnerCreateResponseWrapper {
        logger.info { "Create ${requests.size} new sites with legal address as site main address" }

        val requestList = requests.toList()
        val createRequests = requestList.map { siteDtoRequestMapper.toCreateWithLegalAddressAsMainRequest(it) }

        val responses = mutableListOf<SitePartnerCreateVerboseDto>()
        val errors = mutableListOf<ErrorInfo<SiteCreateError>>()
        parseAndExecute(createRequests, siteCreateWithLegalAddressAsMainParser::parse, siteCreateWithReferencedAddressAsMainService::create).forEachIndexed { index, result ->
            val entityKey = index.toString()
            when (result) {
                is ParseResult.Success -> responses.add(result.parsed.toUpsertDto(entityKey))
                is ParseResult.Failure -> errors.addAll(result.errors.map { siteParseErrorMapper.toCreateErrorInfo(it, entityKey) })
            }
        }

        return SitePartnerCreateResponseWrapper(responses, errors)
    }
}
