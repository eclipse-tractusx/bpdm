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

import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.inbound.SiteDtoRequestMapperV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.SiteParseErrorMapperV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.SiteResponseMapperV6
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.parseAndExecute
import org.eclipse.tractusx.bpdm.pool.service.operation.site.SitePayloadUpdateService
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteUpdateParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SitePartnerUpdateV6ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SitePartnerCreateVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.exception.SiteUpdateErrorV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.error.ErrorInfoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SitePartnerUpdateV6Request

/**
 * The REST-API boundary for the legacy v6 "update sites" operation, using the v6 request/response shapes.
 */
@Service
class SiteUpdateApplicationV6Service(
    private val siteUpdateParser: SiteUpdateParser,
    private val sitePayloadUpdateService: SitePayloadUpdateService,
    private val siteDtoRequestMapperV6: SiteDtoRequestMapperV6,
    private val siteParseErrorMapperV6: SiteParseErrorMapperV6,
    private val siteResponseMapperV6: SiteResponseMapperV6
) {

    /**
     * Applies each request to the site it addresses by BPN and returns, per request, either the updated site or the
     * errors that stopped it.
     */
    @Transactional
    fun updateSites(requests: Collection<SitePartnerUpdateV6Request>): SitePartnerUpdateV6ResponseWrapper {
        val requestList = requests.toList()
        val updateRequests = requestList.map { siteDtoRequestMapperV6.toUpdateRequest(it) }

        val responses = mutableListOf<SitePartnerCreateVerboseV6Dto>()
        val errors = mutableListOf<ErrorInfoV6<SiteUpdateErrorV6>>()
        requestList.zip(parseAndExecute(updateRequests, siteUpdateParser::parse, sitePayloadUpdateService::update)).forEach { (request, result) ->
            when (result) {
                is ParseResult.Success -> responses.add(siteResponseMapperV6.toUpsertResponse(result.parsed.value, request.bpns))
                is ParseResult.Failure -> errors.addAll(result.errors.map { siteParseErrorMapperV6.toUpdateErrorInfo(it, request.bpns) })
            }
        }

        return SitePartnerUpdateV6ResponseWrapper(responses, errors)
    }
}
