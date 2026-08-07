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

import org.eclipse.tractusx.bpdm.pool.api.model.request.IdentifiersSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.BpnIdentifierMappingDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.BpnRequestIdentifierMappingDto
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound.BpnSearchRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.BpnSearchResponseMapper
import org.eclipse.tractusx.bpdm.pool.mapper.shared.outbound.BpnSearchParseErrorMapper
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.service.operation.BpnIdentifierSearchService
import org.eclipse.tractusx.bpdm.pool.service.operation.BpnRequestIdentifierSearchService
import org.eclipse.tractusx.bpdm.pool.service.parser.BpnIdentifierSearchParser
import org.eclipse.tractusx.bpdm.pool.service.parser.BpnRequestIdentifierSearchParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.eclipse.tractusx.bpdm.pool.api.model.request.BpnRequestIdentifierSearchRequest as BpnRequestIdentifierSearchRequestDto

/**
 * The REST-API boundary for the V7 "search BPNs" operations.
 */
@Service
class BpnSearchApplicationV7Service(
    private val bpnIdentifierSearchParser: BpnIdentifierSearchParser,
    private val bpnRequestIdentifierSearchParser: BpnRequestIdentifierSearchParser,
    private val bpnIdentifierSearchService: BpnIdentifierSearchService,
    private val bpnRequestIdentifierSearchService: BpnRequestIdentifierSearchService,
    private val bpnSearchRequestMapper: BpnSearchRequestMapper,
    private val bpnSearchResponseMapper: BpnSearchResponseMapper,
    private val bpnSearchParseErrorMapper: BpnSearchParseErrorMapper
) {

    /**
     * Returns the BPN of every business partner carrying one of the requested identifiers.
     */
    @Transactional(readOnly = true)
    fun searchBpnsByIdentifiers(searchRequest: IdentifiersSearchRequest): Set<BpnIdentifierMappingDto> =
        when (val criteria = bpnIdentifierSearchParser.parse(bpnSearchRequestMapper.toIdentifierSearchRequest(searchRequest))) {
            is ParseResult.Failure -> throw bpnSearchParseErrorMapper.toIdentifierSearchException(criteria.errors)
            is ParseResult.Success -> bpnSearchResponseMapper.toIdentifierMappings(bpnIdentifierSearchService.search(criteria.parsed))
        }

    /**
     * Returns the BPN that was issued for every requested request identifier.
     */
    @Transactional(readOnly = true)
    fun searchBpnsByRequestedIdentifiers(searchRequest: BpnRequestIdentifierSearchRequestDto): Set<BpnRequestIdentifierMappingDto> =
        when (val criteria = bpnRequestIdentifierSearchParser.parse(bpnSearchRequestMapper.toRequestIdentifierSearchRequest(searchRequest))) {
            is ParseResult.Failure -> throw bpnSearchParseErrorMapper.toRequestIdentifierSearchException(criteria.errors)
            is ParseResult.Success ->
                bpnSearchResponseMapper.toRequestIdentifierMappings(bpnRequestIdentifierSearchService.search(criteria.parsed))
        }
}
