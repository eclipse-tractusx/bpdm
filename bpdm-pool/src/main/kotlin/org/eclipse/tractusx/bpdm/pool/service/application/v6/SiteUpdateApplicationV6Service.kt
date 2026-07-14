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
import org.eclipse.tractusx.bpdm.pool.api.model.request.SitePartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteUpdateError
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SitePartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SitePartnerUpdateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.toUpsertDto
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound.SiteDtoRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.SiteParseErrorMapper
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.parseAndExecute
import org.eclipse.tractusx.bpdm.pool.service.operation.SiteUpdateService
import org.eclipse.tractusx.bpdm.pool.service.parser.SiteUpdateParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Application service for the v6 "update sites" operation: the boundary between the legacy v6 REST API and the domain.
 * Mirrors [org.eclipse.tractusx.bpdm.pool.service.application.v7.SiteUpdateApplicationV7Service] but maps the per-entry
 * verdicts back into the versioned `api.v6.model` response shapes.
 *
 * `@Transactional` so parse and execute share one persistence context: [SiteUpdateParser] resolves the target sites by
 * BPNS and validates content, then [SiteUpdateService] mutates their lazy collections. There is no parent to resolve on
 * update.
 */
@Service
class SiteUpdateApplicationV6Service(
    private val siteUpdateParser: SiteUpdateParser,
    private val siteUpdateService: SiteUpdateService,
    private val siteDtoRequestMapper: SiteDtoRequestMapper,
    private val siteParseErrorMapper: SiteParseErrorMapper
) {

    private val logger = KotlinLogging.logger { }

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
}
