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

package org.eclipse.tractusx.bpdm.pool.service.parser

import org.eclipse.tractusx.bpdm.pool.config.ControllerConfigProperties
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.error.BpnRequestIdentifierSearchParseError
import org.eclipse.tractusx.bpdm.pool.model.error.SearchValuesTooMany
import org.eclipse.tractusx.bpdm.pool.model.parsed.BpnRequestIdentifierSearchParsed
import org.eclipse.tractusx.bpdm.pool.model.request.BpnRequestIdentifierSearchRequest
import org.springframework.stereotype.Service

/**
 * Validates the criteria of a BPN-by-request-identifier search.
 */
@Service
class BpnRequestIdentifierSearchParser(
    private val controllerConfigProperties: ControllerConfigProperties
) {

    /**
     * Accepts the requested identifiers as they are, failing when the request holds more of them than the configured
     * search request limit.
     */
    fun parse(request: BpnRequestIdentifierSearchRequest): ParseResult<BpnRequestIdentifierSearchParsed, BpnRequestIdentifierSearchParseError> {
        if (request.requestedIdentifiers.size > controllerConfigProperties.searchRequestLimit)
            return ParseResult.ofSingleFailure(
                SearchValuesTooMany(request.requestedIdentifiers.size, controllerConfigProperties.searchRequestLimit)
            )

        return ParseResult.Success(BpnRequestIdentifierSearchParsed(request.requestedIdentifiers))
    }
}
