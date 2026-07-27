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
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressCreateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.AddressPartnerCreateRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.AddressPartnerCreateResponseWrapperV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.AddressPartnerCreateVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.toCreateResponse
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.toV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.toV7
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound.AddressDtoRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.AddressParseErrorMapper
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.parseAndExecute
import org.eclipse.tractusx.bpdm.pool.model.request.AddressCreateUntypedParentRequest
import org.eclipse.tractusx.bpdm.pool.service.operation.AddressCreateService
import org.eclipse.tractusx.bpdm.pool.service.parser.UntypedParentAddressCreateParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The REST-API boundary for the legacy v6 "create additional address" operation, using the v6 request/response shapes.
 */
@Service
class AddressCreateApplicationV6Service(
    private val untypedParentAddressCreateParser: UntypedParentAddressCreateParser,
    private val addressCreateService: AddressCreateService,
    private val addressDtoRequestMapper: AddressDtoRequestMapper,
    private val addressParseErrorMapper: AddressParseErrorMapper
) {

    private val logger = KotlinLogging.logger { }

    @Transactional
    fun createAddresses(requests: Collection<AddressPartnerCreateRequestV6>): AddressPartnerCreateResponseWrapperV6 {
        logger.info { "Create ${requests.size} new addresses" }

        val requestList = requests.toList()
        val createRequests = requestList.map {
            AddressCreateUntypedParentRequest(it.bpnParent, addressDtoRequestMapper.toContentRequest(it.address.toV7(), it.scriptVariants.map { sv -> sv.toV7() }))
        }

        val responses = mutableListOf<AddressPartnerCreateVerboseDtoV6>()
        val errors = mutableListOf<ErrorInfo<AddressCreateError>>()
        requestList.zip(parseAndExecute(createRequests, untypedParentAddressCreateParser::parse, addressCreateService::create)).forEach { (request, result) ->
            when (result) {
                is ParseResult.Success -> responses.add(result.parsed.toCreateResponse(request.index))
                is ParseResult.Failure -> errors.addAll(result.errors.map { addressParseErrorMapper.toCreateErrorInfo(it, request.index) })
            }
        }

        return AddressPartnerCreateResponseWrapperV6(responses, errors.map { it.toV6() })
    }
}
