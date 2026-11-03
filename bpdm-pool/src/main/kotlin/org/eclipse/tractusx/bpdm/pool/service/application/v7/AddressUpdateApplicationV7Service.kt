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

import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound.AddressDtoRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.AddressParseErrorMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.AddressResponseMapper
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.parseAndExecute
import org.eclipse.tractusx.bpdm.pool.model.request.AddressUpdateRequest
import org.eclipse.tractusx.bpdm.pool.service.operation.address.AddressPayloadUpdateService
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressUpdateParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerUpdateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerUpdateV7ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v7.model.error.ErrorInfoV7
import org.eclipse.tractusx.bpdm.pool.api.v7.exception.AddressUpdateErrorV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerUpdateV7Request

/**
 * The REST-API boundary for the V7 "update address" operation.
 */
@Service
class AddressUpdateApplicationV7Service(
    private val addressUpdateParser: AddressUpdateParser,
    private val addressPayloadUpdateService: AddressPayloadUpdateService,
    private val addressDtoRequestMapper: AddressDtoRequestMapper,
    private val addressParseErrorMapper: AddressParseErrorMapper,
    private val addressResponseMapper: AddressResponseMapper
) {

    /**
     * Applies each request to the address it addresses by BPN and returns, per request, either the updated address or
     * the errors that stopped it.
     */
    @Transactional
    fun updateAddresses(requests: Collection<AddressPartnerUpdateV7Request>): AddressPartnerUpdateV7ResponseWrapper {
        val requestList = requests.toList()
        val updateRequests = requestList.map {
            AddressUpdateRequest(addressBpn = it.bpna, siteBpns = it.bpnSites, content = addressDtoRequestMapper.toContentRequest(it.address, it.scriptVariants))
        }

        val responses = mutableListOf<AddressPartnerUpdateVerboseV7Dto>()
        val errors = mutableListOf<ErrorInfoV7<AddressUpdateErrorV7>>()
        requestList.zip(parseAndExecute(updateRequests, addressUpdateParser::parse, addressPayloadUpdateService::update)).forEach { (request, result) ->
            when (result) {
                is ParseResult.Success -> responses.add(addressResponseMapper.toUpdateResponse(result.parsed.value))
                is ParseResult.Failure -> errors.addAll(result.errors.map { addressParseErrorMapper.toUpdateErrorInfo(it, request.bpna) })
            }
        }

        return AddressPartnerUpdateV7ResponseWrapper(responses, errors)
    }
}