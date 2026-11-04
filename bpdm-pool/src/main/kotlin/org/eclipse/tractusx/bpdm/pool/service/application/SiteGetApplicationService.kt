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
import org.eclipse.tractusx.bpdm.pool.model.output.site.SiteGetResult
import org.eclipse.tractusx.bpdm.pool.model.raw.site.SiteGetRaw
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteGetParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Fetches a single site by its BPN.
 */
@Service
class SiteGetApplicationService(
    private val siteGetParser: SiteGetParser,
    private val dbToOutputMapper: DbToOutputMapper
) {

    /**
     * Returns the site carrying the BPN [request] names, or the reason no site could be found.
     */
    @Transactional(readOnly = true)
    fun get(request: SiteGetRaw): SiteGetResult =
        when (val parseResult = siteGetParser.parse(request)) {
            is ParseResult.Failure -> SiteGetResult.Rejected(parseResult.errors)
            is ParseResult.Success -> SiteGetResult.Found(dbToOutputMapper.toRecord(parseResult.parsed))
        }
}
