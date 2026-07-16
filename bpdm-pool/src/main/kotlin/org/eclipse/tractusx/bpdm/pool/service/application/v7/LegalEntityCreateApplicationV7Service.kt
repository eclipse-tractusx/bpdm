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
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityCreateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityPartnerCreateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound.LegalEntityDtoRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.LegalEntityParseErrorMapper
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.parseAndExecute
import org.eclipse.tractusx.bpdm.pool.service.operation.LegalEntityCreateService
import org.eclipse.tractusx.bpdm.pool.service.parser.LegalEntityCreateParser
import org.eclipse.tractusx.bpdm.pool.service.toUpsertDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Application service for the V7 "create legal entities" operation: the boundary between the REST API and the domain. It
 * accepts the API [LegalEntityPartnerCreateRequest]s, translates them into the internal
 * [org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityCreateRequest] domain model, drives the parse/execute pipeline, and
 * maps the per-entry verdicts back into the API [LegalEntityPartnerCreateResponseWrapper]. It holds no business rules of
 * its own — validation and persistence live in the collaborators it orchestrates.
 *
 * `@Transactional` so parse and execute share one persistence context: a legal entity is the top of the hierarchy so
 * there is no parent to resolve — [LegalEntityCreateParser] validates the header, identifier uniqueness and legal-address
 * content, then [LegalEntityCreateService] persists the legal entities and their legal addresses.
 */
@Service
class LegalEntityCreateApplicationV7Service(
    private val legalEntityCreateParser: LegalEntityCreateParser,
    private val legalEntityCreateService: LegalEntityCreateService,
    private val legalEntityDtoRequestMapper: LegalEntityDtoRequestMapper,
    private val legalEntityParseErrorMapper: LegalEntityParseErrorMapper
) {

    private val logger = KotlinLogging.logger { }

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
}
