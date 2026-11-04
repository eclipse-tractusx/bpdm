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

package org.eclipse.tractusx.bpdm.pool.service.parser.legalentity

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.LegalEntitySiteSearchError
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.LegalEntitySiteSearchError.LegalEntityNotFound
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.SiteSearchParsed
import org.eclipse.tractusx.bpdm.pool.model.raw.site.LegalEntitySiteSearchRaw
import org.springframework.stereotype.Service

/**
 * Resolves the legal entity whose sites are being listed into the criteria the site search operation queries with.
 */
@Service
class LegalEntitySiteSearchParser(
    private val legalEntityBpnParser: LegalEntityBpnParser
) {

    /**
     * Returns criteria matching every site of the requested legal entity, failing when no legal entity carries its BPN
     * so that an unknown legal entity is distinguishable from one without sites.
     */
    fun parse(request: LegalEntitySiteSearchRaw): ParseResult<SiteSearchParsed, LegalEntitySiteSearchError> =
        when (val result = legalEntityBpnParser.parse(listOf(request.legalEntityBpn)).single()) {
            is ParseResult.Failure -> ParseResult.ofSingleFailure(LegalEntityNotFound(request.legalEntityBpn))
            is ParseResult.Success -> ParseResult.Success(
                SiteSearchParsed(
                    siteBpns = emptyList(),
                    legalEntityBpns = listOf(result.parsed.bpn),
                    name = null,
                    isDataSpaceParticipant = null
                )
            )
        }
}
