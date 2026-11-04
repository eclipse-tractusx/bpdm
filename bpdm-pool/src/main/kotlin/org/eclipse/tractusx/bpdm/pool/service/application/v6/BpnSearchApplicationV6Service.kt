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

import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.inbound.BpnSearchRequestMapperV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.BpnSearchResponseMapperV6
import org.eclipse.tractusx.bpdm.pool.mapper.shared.outbound.BpnSearchParseErrorMapper
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.service.operation.bpn.BpnRequestIdentifierSearchService
import org.eclipse.tractusx.bpdm.pool.service.parser.bpn.BpnRequestIdentifierSearchParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.eclipse.tractusx.bpdm.pool.api.v6.model.bpn.BpnRequestIdentifierMappingV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.bpn.BpnRequestIdentifierSearchV6Request

/**
 * The REST-API boundary for the legacy v6 "search BPNs" operations, using the v6 request/response shapes.
 */
@Service
class BpnSearchApplicationV6Service(
    private val bpnRequestIdentifierSearchParser: BpnRequestIdentifierSearchParser,
    private val bpnRequestIdentifierSearchService: BpnRequestIdentifierSearchService,
    private val bpnSearchRequestMapperV6: BpnSearchRequestMapperV6,
    private val bpnSearchResponseMapperV6: BpnSearchResponseMapperV6,
    private val bpnSearchParseErrorMapper: BpnSearchParseErrorMapper
) {

    /**
     * Returns the BPN that was issued for every requested request identifier.
     */
    @Transactional(readOnly = true)
    fun searchBpnsByRequestedIdentifiers(searchRequest: BpnRequestIdentifierSearchV6Request): Set<BpnRequestIdentifierMappingV6Dto> =
        when (val criteria = bpnRequestIdentifierSearchParser.parse(bpnSearchRequestMapperV6.toRequestIdentifierSearchRequest(searchRequest))) {
            is ParseResult.Failure -> throw bpnSearchParseErrorMapper.toRequestIdentifierSearchException(criteria.errors)
            is ParseResult.Success ->
                bpnSearchResponseMapperV6.toRequestIdentifierMappings(bpnRequestIdentifierSearchService.search(criteria.parsed))
                    .map { it }
                    .toSet()
        }
}
