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
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteUpdateError
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.SitePartnerUpdateRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SitePartnerCreateVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SitePartnerUpdateResponseWrapperV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.toUpsertDto
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.toV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.toV7
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound.SiteDtoRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.SiteParseErrorMapper
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.parseAndExecute
import org.eclipse.tractusx.bpdm.pool.service.operation.SitePayloadUpdateService
import org.eclipse.tractusx.bpdm.pool.service.parser.SiteUpdateParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The REST-API boundary for the legacy v6 "update sites" operation, using the v6 request/response shapes.
 */
@Service
class SiteUpdateApplicationV6Service(
    private val siteUpdateParser: SiteUpdateParser,
    private val sitePayloadUpdateService: SitePayloadUpdateService,
    private val siteDtoRequestMapper: SiteDtoRequestMapper,
    private val siteParseErrorMapper: SiteParseErrorMapper
) {

    private val logger = KotlinLogging.logger { }

    @Transactional
    fun updateSites(requests: Collection<SitePartnerUpdateRequestV6>): SitePartnerUpdateResponseWrapperV6 {
        logger.info { "Update ${requests.size} sites" }

        val requestList = requests.toList()
        val updateRequests = requestList.map { siteDtoRequestMapper.toUpdateRequest(it.toV7()) }

        val responses = mutableListOf<SitePartnerCreateVerboseDtoV6>()
        val errors = mutableListOf<ErrorInfo<SiteUpdateError>>()
        requestList.zip(parseAndExecute(updateRequests, siteUpdateParser::parse, sitePayloadUpdateService::update)).forEach { (request, result) ->
            when (result) {
                is ParseResult.Success -> responses.add(result.parsed.value.toUpsertDto(request.bpns))
                is ParseResult.Failure -> errors.addAll(result.errors.map { siteParseErrorMapper.toUpdateErrorInfo(it, request.bpns) })
            }
        }

        return SitePartnerUpdateResponseWrapperV6(responses, errors.map { it.toV6() })
    }
}
