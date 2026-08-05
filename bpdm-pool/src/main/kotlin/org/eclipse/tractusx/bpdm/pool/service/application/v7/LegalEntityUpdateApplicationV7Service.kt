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
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityPartnerUpdateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityUpdateError
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound.LegalEntityDtoRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.LegalEntityParseErrorMapper
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.parseAndExecute
import org.eclipse.tractusx.bpdm.pool.service.operation.LegalEntityPayloadUpdateService
import org.eclipse.tractusx.bpdm.pool.service.parser.LegalEntityUpdateParser
import org.eclipse.tractusx.bpdm.pool.service.toUpsertDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The REST-API boundary for the V7 "update legal entities" operation.
 */
@Service
class LegalEntityUpdateApplicationV7Service(
    private val legalEntityUpdateParser: LegalEntityUpdateParser,
    private val legalEntityPayloadUpdateService: LegalEntityPayloadUpdateService,
    private val legalEntityDtoRequestMapper: LegalEntityDtoRequestMapper,
    private val legalEntityParseErrorMapper: LegalEntityParseErrorMapper
) {

    private val logger = KotlinLogging.logger { }

    /**
     * Applies each request to the legal entity it addresses by BPN and returns, per request, either the updated legal
     * entity or the errors that stopped it.
     */
    @Transactional
    fun updateLegalEntities(requests: Collection<LegalEntityPartnerUpdateRequest>): LegalEntityPartnerUpdateResponseWrapper {
        logger.info { "Update ${requests.size} legal entities" }

        val requestList = requests.toList()
        val updateRequests = requestList.map { legalEntityDtoRequestMapper.toUpdateRequest(it) }

        val responses = mutableListOf<LegalEntityPartnerCreateVerboseDto>()
        val errors = mutableListOf<ErrorInfo<LegalEntityUpdateError>>()
        requestList.zip(parseAndExecute(updateRequests, legalEntityUpdateParser::parse, legalEntityPayloadUpdateService::update)).forEach { (request, result) ->
            when (result) {
                is ParseResult.Success -> responses.add(result.parsed.value.toUpsertDto(request.bpnl))
                is ParseResult.Failure -> errors.addAll(result.errors.map { legalEntityParseErrorMapper.toUpdateErrorInfo(it, request.bpnl) })
            }
        }

        return LegalEntityPartnerUpdateResponseWrapper(responses, errors)
    }
}
