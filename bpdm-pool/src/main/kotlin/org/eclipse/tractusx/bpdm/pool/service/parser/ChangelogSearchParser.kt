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
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.error.ChangelogSearchParseError
import org.eclipse.tractusx.bpdm.pool.model.error.SearchValuesTooMany
import org.eclipse.tractusx.bpdm.pool.model.parsed.ChangelogSearchParsed
import org.eclipse.tractusx.bpdm.pool.model.request.ChangelogSearchRequest
import org.eclipse.tractusx.bpdm.pool.service.parser.bpn.BpnFilterParser
import org.springframework.stereotype.Service

/**
 * Turns loose changelog search criteria into the normalized form the search operation queries with.
 */
@Service
class ChangelogSearchParser(
    private val controllerConfigProperties: ControllerConfigProperties,
    private val bpnFilterParser: BpnFilterParser
) {

    /**
     * Normalizes the criteria by dropping blank filter values and reading BPNs case-insensitively, failing when the BPN
     * filter holds more values than the configured search request limit.
     */
    fun parse(request: ChangelogSearchRequest): ParseResult<ChangelogSearchParsed, ChangelogSearchParseError> {
        val bpns = request.bpns.orEmpty()
        if (bpns.size > controllerConfigProperties.searchRequestLimit) {
            return ParseResult.ofSingleFailure(SearchValuesTooMany(bpns.size, controllerConfigProperties.searchRequestLimit))
        }

        return ParseResult.Success(
            ChangelogSearchParsed(
                bpns = bpnFilterParser.parse(bpns).toSet(),
                businessPartnerTypes = request.businessPartnerTypes.orEmpty(),
                timestampAfter = request.timestampAfter,
                isDataSpaceParticipant = request.isDataSpaceParticipant
            )
        )
    }
}
