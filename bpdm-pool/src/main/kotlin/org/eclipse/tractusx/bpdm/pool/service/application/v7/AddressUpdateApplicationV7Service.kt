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
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressPartnerUpdateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressPartnerUpdateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressUpdateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound.AddressDtoRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.AddressParseErrorMapper
import org.eclipse.tractusx.bpdm.pool.model.request.AddressUpdateRequest
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.parseAndExecute
import org.eclipse.tractusx.bpdm.pool.service.operation.AddressFullUpdateService
import org.eclipse.tractusx.bpdm.pool.service.parser.AddressUpdateParser
import org.eclipse.tractusx.bpdm.pool.service.toUpdateDto
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
 * Application service for the V7 "update address" operation: the boundary between the REST API and the domain. It
 * accepts the API [org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerUpdateRequest]s, translates them into the loose [org.eclipse.tractusx.bpdm.pool.model.request.AddressUpdateRequest] domain model,
 * drives the parse/execute pipeline, and maps the per-entry verdicts back into the API
 * [org.eclipse.tractusx.bpdm.pool.api.model.response.AddressPartnerUpdateResponseWrapper]. It holds no business rules of its own — validation, target resolution and
 * persistence all live in the collaborators it orchestrates.
 *
 * `@Transactional` so parse and execute share one persistence context: [org.eclipse.tractusx.bpdm.pool.service.parser.AddressUpdateParser] resolves the target
 * entities (reporting "address not found") and validates content, then [org.eclipse.tractusx.bpdm.pool.service.operation.AddressFullUpdateService] mutates their lazy
 * collections — instead of relying on Open-Session-in-View. There is no parent to resolve on update.
 */
@Service
class AddressUpdateApplicationV7Service(
    private val addressUpdateParser: AddressUpdateParser,
    private val addressFullUpdateService: AddressFullUpdateService,
    private val addressDtoRequestMapper: AddressDtoRequestMapper,
    private val addressParseErrorMapper: AddressParseErrorMapper
) {

    private val logger = KotlinLogging.logger { }

    @Transactional
    fun updateAddresses(requests: Collection<AddressPartnerUpdateRequest>): AddressPartnerUpdateResponseWrapper {
        logger.info { "Update ${requests.size} business partner addresses" }

        val requestList = requests.toList()
        val updateRequests = requestList.map {
            AddressUpdateRequest(addressBpn = it.bpna, siteBpn = null, content = addressDtoRequestMapper.toContentRequest(it.address, it.scriptVariants))
        }

        val responses = mutableListOf<AddressPartnerUpdateVerboseDto>()
        val errors = mutableListOf<ErrorInfo<AddressUpdateError>>()
        requestList.zip(parseAndExecute(updateRequests, addressUpdateParser::parse, addressFullUpdateService::update)).forEach { (request, result) ->
            when (result) {
                is ParseResult.Success -> responses.add(result.parsed.value.toUpdateDto())
                is ParseResult.Failure -> errors.addAll(result.errors.map { addressParseErrorMapper.toUpdateErrorInfo(it, request.bpna) })
            }
        }

        return AddressPartnerUpdateResponseWrapper(responses, errors)
    }
}