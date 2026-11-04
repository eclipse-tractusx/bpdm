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
package org.eclipse.tractusx.bpdm.pool.service.parser.site

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.SiteGetError
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.SiteGetError.SiteNotFound
import org.eclipse.tractusx.bpdm.pool.model.raw.site.SiteGetRaw
import org.springframework.stereotype.Service

/**
 * Resolves a site fetch request to the site it names.
 */
@Service
class SiteGetParser(
    private val siteBpnParser: SiteBpnParser
) {

    /**
     * Resolves the requested BPN to its site, failing when no site carries it.
     */
    fun parse(request: SiteGetRaw): ParseResult<SiteDb, SiteGetError> =
        when (val result = siteBpnParser.parse(listOf(request.bpn)).single()) {
            is ParseResult.Success -> result
            is ParseResult.Failure -> ParseResult.ofSingleFailure(SiteNotFound(request.bpn))
        }
}
