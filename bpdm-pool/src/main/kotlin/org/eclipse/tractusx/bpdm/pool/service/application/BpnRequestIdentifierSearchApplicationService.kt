/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.service.application

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.mapper.DbToOutputMapper
import org.eclipse.tractusx.bpdm.pool.model.output.bpn.BpnRequestIdentifierSearchResult
import org.eclipse.tractusx.bpdm.pool.model.raw.bpn.BpnRequestIdentifierSearchRaw
import org.eclipse.tractusx.bpdm.pool.service.operation.bpn.BpnRequestIdentifierSearchService
import org.eclipse.tractusx.bpdm.pool.service.parser.bpn.BpnRequestIdentifierSearchParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Searches the BPNs that were issued for request identifiers.
 */
@Service
class BpnRequestIdentifierSearchApplicationService(
    private val bpnRequestIdentifierSearchParser: BpnRequestIdentifierSearchParser,
    private val bpnRequestIdentifierSearchService: BpnRequestIdentifierSearchService,
    private val dbToOutputMapper: DbToOutputMapper
) {

    /**
     * Returns the BPN that was issued for every request identifier in [request], or every reason the criteria are
     * rejected.
     */
    @Transactional(readOnly = true)
    fun search(request: BpnRequestIdentifierSearchRaw): BpnRequestIdentifierSearchResult =
        when (val parseResult = bpnRequestIdentifierSearchParser.parse(request)) {
            is ParseResult.Failure -> BpnRequestIdentifierSearchResult.Rejected(parseResult.errors)
            is ParseResult.Success -> BpnRequestIdentifierSearchResult.Found(
                bpnRequestIdentifierSearchService.search(parseResult.parsed).map(dbToOutputMapper::toRecord).toSet()
            )
        }
}
