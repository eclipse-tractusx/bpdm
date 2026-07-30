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
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressUpdateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LogisticAddressVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.AddressPartnerUpdateRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.AddressPartnerUpdateResponseWrapperV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.toV6Dto
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.toV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.toV7
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound.AddressDtoRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.AddressParseErrorMapper
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.parseAndExecute
import org.eclipse.tractusx.bpdm.pool.model.request.AddressUpdateRequest
import org.eclipse.tractusx.bpdm.pool.service.operation.AddressPayloadUpdateService
import org.eclipse.tractusx.bpdm.pool.service.parser.AddressUpdateParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The REST-API boundary for the legacy v6 "update address" operation, using the v6 request/response shapes.
 */
@Service
class AddressUpdateApplicationV6Service(
    private val addressUpdateParser: AddressUpdateParser,
    private val addressPayloadUpdateService: AddressPayloadUpdateService,
    private val addressDtoRequestMapper: AddressDtoRequestMapper,
    private val addressParseErrorMapper: AddressParseErrorMapper
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
        val updateRequests = requestList.map {
            // v6 has no script variants and an update replaces the full list, so a v6 update drops the script variants
            // the address may have gained through v7 or the task path.
            AddressUpdateRequest(addressBpn = it.bpna, siteBpn = null, content = addressDtoRequestMapper.toContentRequest(it.address.toV7(), emptyList()))
        }

        val responses = mutableListOf<LogisticAddressVerboseDtoV6>()
        val errors = mutableListOf<ErrorInfo<AddressUpdateError>>()
        requestList.zip(parseAndExecute(updateRequests, addressUpdateParser::parse, addressPayloadUpdateService::update)).forEach { (request, result) ->
            when (result) {
                is ParseResult.Success -> responses.add(result.parsed.value.toV6Dto())
                is ParseResult.Failure -> errors.addAll(result.errors.map { addressParseErrorMapper.toUpdateErrorInfo(it, request.bpna) })
            }
        }

        return AddressPartnerUpdateResponseWrapperV6(responses, errors.map { it.toV6() })
    }
}
