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

import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntitySearchParsed
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntitySearchRequest
import org.springframework.stereotype.Service

/**
 * Turns loose legal entity search criteria into the normalized form the search operation queries with.
 *
 * Unlike the upsert parsers this one returns its parsed value directly instead of a `ParseResult`: no search criterion
 * can be rejected — an unknown or malformed filter value matches nothing — so there is no failure to report.
 */
@Service
class LegalEntitySearchParser {

    /**
     * Normalizes the criteria by dropping blank filter values and reading BPNs case-insensitively.
     */
    fun parse(request: LegalEntitySearchRequest): LegalEntitySearchParsed =
        LegalEntitySearchParsed(
            legalEntityBpns = request.legalEntityBpns.filter { it.isNotBlank() }.map { it.uppercase() },
            legalName = request.legalName?.takeIf { it.isNotBlank() },
            isCatenaXMemberData = request.isCatenaXMemberData
        )
}
