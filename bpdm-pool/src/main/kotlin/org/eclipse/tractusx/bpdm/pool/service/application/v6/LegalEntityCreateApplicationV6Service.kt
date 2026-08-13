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
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalEntityPartnerCreateRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.ErrorInfoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityCreateErrorV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityPartnerCreateResponseWrapperV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityPartnerCreateVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.inbound.LegalEntityDtoRequestMapperV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.LegalEntityParseErrorMapperV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.LegalEntityResponseMapperV6
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.parseAndExecute
import org.eclipse.tractusx.bpdm.pool.service.operation.LegalEntityCreateService
import org.eclipse.tractusx.bpdm.pool.service.parser.LegalEntityCreateParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The REST-API boundary for the legacy v6 "create legal entities" operation, using the v6 request/response shapes.
 */
@Service
class LegalEntityCreateApplicationV6Service(
    private val legalEntityCreateParser: LegalEntityCreateParser,
    private val legalEntityCreateService: LegalEntityCreateService,
    private val legalEntityDtoRequestMapperV6: LegalEntityDtoRequestMapperV6,
    private val legalEntityParseErrorMapperV6: LegalEntityParseErrorMapperV6,
    private val legalEntityResponseMapperV6: LegalEntityResponseMapperV6
) {

    private val logger = KotlinLogging.logger { }

    /**
     * Creates the requested legal entities with their legal addresses and returns, per request, either the created legal
     * entity or the errors that stopped it.
     */
    @Transactional
    fun createLegalEntities(requests: Collection<LegalEntityPartnerCreateRequestV6>): LegalEntityPartnerCreateResponseWrapperV6 {
        logger.info { "Create ${requests.size} new legal entities" }

        val requestList = requests.toList()
        val createRequests = requestList.map { legalEntityDtoRequestMapperV6.toCreateRequest(it) }

        val responses = mutableListOf<LegalEntityPartnerCreateVerboseDtoV6>()
        val errors = mutableListOf<ErrorInfoV6<LegalEntityCreateErrorV6>>()
        requestList.zip(parseAndExecute(createRequests, legalEntityCreateParser::parse, legalEntityCreateService::create)).forEach { (request, result) ->
            when (result) {
                is ParseResult.Success -> responses.add(legalEntityResponseMapperV6.toUpsertResponse(result.parsed, request.index))
                is ParseResult.Failure -> errors.addAll(result.errors.map { legalEntityParseErrorMapperV6.toCreateErrorInfo(it, request.index) })
            }
        }

        return LegalEntityPartnerCreateResponseWrapperV6(responses, errors)
    }
}
