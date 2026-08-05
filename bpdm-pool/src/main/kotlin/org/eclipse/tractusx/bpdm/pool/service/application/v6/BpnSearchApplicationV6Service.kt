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

import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.BpnRequestIdentifierSearchRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.IdentifiersSearchRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.BpnIdentifierMappingDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.BpnRequestIdentifierMappingDtoV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.toV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.toV7
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound.BpnSearchRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.BpnSearchParseErrorMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.BpnSearchResponseMapper
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.service.operation.BpnIdentifierSearchService
import org.eclipse.tractusx.bpdm.pool.service.operation.BpnRequestIdentifierSearchService
import org.eclipse.tractusx.bpdm.pool.service.parser.BpnIdentifierSearchParser
import org.eclipse.tractusx.bpdm.pool.service.parser.BpnRequestIdentifierSearchParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The REST-API boundary for the legacy v6 "search BPNs" operations, using the v6 request/response shapes.
 */
@Service
class BpnSearchApplicationV6Service(
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
    fun searchBpnsByIdentifiers(searchRequest: IdentifiersSearchRequestV6): Set<BpnIdentifierMappingDtoV6> =
        when (val criteria = bpnIdentifierSearchParser.parse(bpnSearchRequestMapper.toIdentifierSearchRequest(searchRequest.toV7()))) {
            is ParseResult.Failure -> throw bpnSearchParseErrorMapper.toIdentifierSearchException(criteria.errors)
            is ParseResult.Success -> bpnSearchResponseMapper.toIdentifierMappings(bpnIdentifierSearchService.search(criteria.parsed))
                .map { it.toV6() }
                .toSet()
        }

    /**
     * Returns the BPN that was issued for every requested request identifier.
     */
    @Transactional(readOnly = true)
    fun searchBpnsByRequestedIdentifiers(searchRequest: BpnRequestIdentifierSearchRequestV6): Set<BpnRequestIdentifierMappingDtoV6> =
        when (val criteria = bpnRequestIdentifierSearchParser.parse(bpnSearchRequestMapper.toRequestIdentifierSearchRequest(searchRequest.toV7()))) {
            is ParseResult.Failure -> throw bpnSearchParseErrorMapper.toRequestIdentifierSearchException(criteria.errors)
            is ParseResult.Success ->
                bpnSearchResponseMapper.toRequestIdentifierMappings(bpnRequestIdentifierSearchService.search(criteria.parsed))
                    .map { it.toV6() }
                    .toSet()
        }
}
