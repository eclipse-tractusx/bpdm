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
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressCreateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressPartnerCreateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound.AddressDtoRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.AddressParseErrorMapper
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateUntypedParentRequest
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.parseAndExecute
import org.eclipse.tractusx.bpdm.pool.service.operation.AddressCreateService
import org.eclipse.tractusx.bpdm.pool.service.parser.UntypedParentAddressCreateParser
import org.eclipse.tractusx.bpdm.pool.service.toCreateResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
/**
 * Application service for the V7 "create additional address" operation: the boundary between the REST API and the
 * domain. It accepts the API [org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerCreateRequest]s, translates them into the loose
 * [org.eclipse.tractusx.bpdm.pool.model.AddressCreateUntypedParentRequest] domain model, drives the parse/execute pipeline, and maps the per-entry verdicts
 * back into the API [org.eclipse.tractusx.bpdm.pool.api.model.response.AddressPartnerCreateResponseWrapper]. It holds no business rules of its own — validation, parent
 * resolution and persistence all live in the collaborators it orchestrates.
 *
 * `@Transactional` so parse and execute share one persistence context: [org.eclipse.tractusx.bpdm.pool.service.parser.UntypedParentAddressCreateParser] resolves the
 * single `bpnParent` into the explicit (legalEntity, site) parents — reporting the precise
 * `BpnNotValid`/`SiteNotFound`/`LegalEntityNotFound` parent errors and validating content — then [org.eclipse.tractusx.bpdm.pool.service.operation.AddressCreateService]
 * persists the addresses.
 */
@Service
class AddressCreateApplicationV7Service(
    private val untypedParentAddressCreateParser: UntypedParentAddressCreateParser,
    private val addressCreateService: AddressCreateService,
    private val addressDtoRequestMapper: AddressDtoRequestMapper,
    private val addressParseErrorMapper: AddressParseErrorMapper
) {

    private val logger = KotlinLogging.logger { }

    @Transactional
    fun createAddresses(requests: Collection<AddressPartnerCreateRequest>): AddressPartnerCreateResponseWrapper {
        logger.info { "Create ${requests.size} new addresses" }

        val requestList = requests.toList()
        val createRequests = requestList.map {
            AddressCreateUntypedParentRequest(it.bpnParent, addressDtoRequestMapper.toContentRequest(it.address, it.scriptVariants))
        }

        val responses = mutableListOf<AddressPartnerCreateVerboseDto>()
        val errors = mutableListOf<ErrorInfo<AddressCreateError>>()
        requestList.zip(parseAndExecute(createRequests, untypedParentAddressCreateParser::parse, addressCreateService::create)).forEach { (request, result) ->
            when (result) {
                is ParseResult.Success -> responses.add(result.parsed.toCreateResponse(request.index))
                is ParseResult.Failure -> errors.addAll(result.errors.map { addressParseErrorMapper.toCreateErrorInfo(it, request.index) })
            }
        }

        return AddressPartnerCreateResponseWrapper(responses, errors)
    }
}