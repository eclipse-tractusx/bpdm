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

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressInvariantVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound.AddressSearchRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.AddressResponseMapper
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.service.operation.address.AddressSearchService
import org.eclipse.tractusx.bpdm.pool.service.parser.AddressSearchParser
import org.eclipse.tractusx.bpdm.pool.service.parser.LegalEntityAddressSearchParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressSearchRequest as AddressSearchRequestDto

/**
 * The REST-API boundary for the V7 "search addresses" operation.
 */
@Service
class AddressSearchApplicationV7Service(
    private val addressSearchParser: AddressSearchParser,
    private val legalEntityAddressSearchParser: LegalEntityAddressSearchParser,
    private val addressSearchService: AddressSearchService,
    private val addressSearchRequestMapper: AddressSearchRequestMapper,
    private val addressResponseMapper: AddressResponseMapper
) {

    /**
     * Returns the requested page of addresses matching the given criteria.
     */
    @Transactional(readOnly = true)
    fun searchAddresses(searchRequest: AddressSearchRequestDto, paginationRequest: PaginationRequest): PageDto<LogisticAddressVerboseDto> =
        search(searchRequest, paginationRequest, isDataSpaceParticipant = null).toPageDto { addressResponseMapper.toAddress(it) }

    /**
     * Returns the requested page of addresses matching the given criteria, restricted to those of Catena-X members and
     * without their script variants.
     */
    @Transactional(readOnly = true)
    fun searchMemberAddresses(
        searchRequest: AddressSearchRequestDto,
        paginationRequest: PaginationRequest
    ): PageDto<LogisticAddressInvariantVerboseDto> =
        search(searchRequest, paginationRequest, isDataSpaceParticipant = true).toPageDto { addressResponseMapper.toInvariantAddress(it) }

    /**
     * Returns the requested page of addresses that belong to the given legal entity directly instead of through one of
     * its sites, and fails with a not-found error when no legal entity carries that BPN.
     */
    @Transactional(readOnly = true)
    fun searchLegalEntityAddresses(bpnl: String, paginationRequest: PaginationRequest): PageDto<LogisticAddressVerboseDto> {
        val criteria = when (val result = legalEntityAddressSearchParser.parse(addressSearchRequestMapper.toDirectAddressesRequest(bpnl))) {
            is ParseResult.Success -> result.parsed
            is ParseResult.Failure -> throw BpdmNotFoundException("Business Partner", bpnl)
        }

        return addressSearchService.search(criteria, paginationRequest.toPageRequest()).toPageDto { addressResponseMapper.toAddress(it) }
    }

    private fun search(searchRequest: AddressSearchRequestDto, paginationRequest: PaginationRequest, isDataSpaceParticipant: Boolean?) =
        addressSearchService.search(
            addressSearchParser.parse(addressSearchRequestMapper.toSearchRequest(searchRequest, isDataSpaceParticipant)),
            paginationRequest.toPageRequest()
        )
}
