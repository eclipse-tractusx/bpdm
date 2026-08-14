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

import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.AddressPartnerCreateRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.AddressCreateErrorV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.AddressPartnerCreateResponseWrapperV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.AddressPartnerCreateVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.ErrorInfoV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.inbound.AddressDtoRequestMapperV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.AddressParseErrorMapperV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.AddressResponseMapperV6
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.parseAndExecute
import org.eclipse.tractusx.bpdm.pool.service.operation.address.AddressCreateService
import org.eclipse.tractusx.bpdm.pool.service.parser.address.UntypedParentAddressCreateParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The REST-API boundary for the legacy v6 "create additional address" operation, using the v6 request/response shapes.
 */
@Service
class AddressCreateApplicationV6Service(
    private val untypedParentAddressCreateParser: UntypedParentAddressCreateParser,
    private val addressCreateService: AddressCreateService,
    private val addressDtoRequestMapperV6: AddressDtoRequestMapperV6,
    private val addressParseErrorMapperV6: AddressParseErrorMapperV6,
    private val addressResponseMapperV6: AddressResponseMapperV6
) {

    /**
     * Creates the requested addresses under their parent business partners and returns, per request, either the created
     * address or the errors that stopped it.
     */
    @Transactional
    fun createAddresses(requests: Collection<AddressPartnerCreateRequestV6>): AddressPartnerCreateResponseWrapperV6 {
        val requestList = requests.toList()
        val createRequests = requestList.map { addressDtoRequestMapperV6.toCreateRequest(it) }

        val responses = mutableListOf<AddressPartnerCreateVerboseDtoV6>()
        val errors = mutableListOf<ErrorInfoV6<AddressCreateErrorV6>>()
        requestList.zip(parseAndExecute(createRequests, untypedParentAddressCreateParser::parse, addressCreateService::create)).forEach { (request, result) ->
            when (result) {
                is ParseResult.Success -> responses.add(addressResponseMapperV6.toCreateResponse(result.parsed, request.index))
                is ParseResult.Failure -> errors.addAll(result.errors.map { addressParseErrorMapperV6.toCreateErrorInfo(it, request.index) })
            }
        }

        return AddressPartnerCreateResponseWrapperV6(responses, errors)
    }
}
