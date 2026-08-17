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
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LogisticAddressVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.AddressPartnerUpdateRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.AddressPartnerUpdateResponseWrapperV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.AddressUpdateErrorV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.ErrorInfoV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.inbound.AddressDtoRequestMapperV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.AddressParseErrorMapperV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.AddressResponseMapperV6
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.parseAndExecute
import org.eclipse.tractusx.bpdm.pool.service.operation.address.AddressPayloadUpdateService
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressUpdateParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The REST-API boundary for the legacy v6 "update address" operation, using the v6 request/response shapes.
 */
@Service
class AddressUpdateApplicationV6Service(
    private val addressUpdateParser: AddressUpdateParser,
    private val addressPayloadUpdateService: AddressPayloadUpdateService,
    private val addressDtoRequestMapperV6: AddressDtoRequestMapperV6,
    private val addressParseErrorMapperV6: AddressParseErrorMapperV6,
    private val addressResponseMapperV6: AddressResponseMapperV6
) {

    private val logger = KotlinLogging.logger { }

    /**
     * Applies each request to the address it addresses by BPN and returns, per request, either the updated address or
     * the errors that stopped it.
     */
    @Transactional
    fun updateAddresses(requests: Collection<AddressPartnerUpdateRequestV6>): AddressPartnerUpdateResponseWrapperV6 {
        logger.info { "Update ${requests.size} business partner addresses" }

        val requestList = requests.toList()
        val updateRequests = requestList.map { addressDtoRequestMapperV6.toUpdateRequest(it) }

        val responses = mutableListOf<LogisticAddressVerboseDtoV6>()
        val errors = mutableListOf<ErrorInfoV6<AddressUpdateErrorV6>>()
        requestList.zip(parseAndExecute(updateRequests, addressUpdateParser::parse, addressPayloadUpdateService::update)).forEach { (request, result) ->
            when (result) {
                is ParseResult.Success -> responses.add(addressResponseMapperV6.toAddress(result.parsed.value))
                is ParseResult.Failure -> errors.addAll(result.errors.map { addressParseErrorMapperV6.toUpdateErrorInfo(it, request.bpna) })
            }
        }

        return AddressPartnerUpdateResponseWrapperV6(responses, errors)
    }
}
