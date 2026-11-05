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
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.BusinessPartnerSearchResult
import org.eclipse.tractusx.bpdm.pool.model.raw.businesspartner.BusinessPartnerSearchRaw
import org.eclipse.tractusx.bpdm.pool.service.operation.businesspartner.BusinessPartnerSearchService
import org.eclipse.tractusx.bpdm.pool.service.parser.businesspartner.BusinessPartnerSearchParser
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Searches legal entities, sites and additional addresses together, each represented by one of its addresses.
 */
@Service
class BusinessPartnerSearchApplicationService(
    private val businessPartnerSearchParser: BusinessPartnerSearchParser,
    private val businessPartnerSearchService: BusinessPartnerSearchService,
    private val dbToOutputMapper: DbToOutputMapper
) {

    /**
     * Returns the requested page of business partners matching [request], or the reasons the search was rejected.
     */
    @Transactional(readOnly = true)
    fun search(request: BusinessPartnerSearchRaw, pageable: Pageable): BusinessPartnerSearchResult =
        when (val parseResult = businessPartnerSearchParser.parse(request)) {
            is ParseResult.Failure -> BusinessPartnerSearchResult.Rejected(parseResult.errors)
            is ParseResult.Success -> BusinessPartnerSearchResult.Found(
                businessPartnerSearchService.search(parseResult.parsed, pageable).map(dbToOutputMapper::toBusinessPartnerSearchRecord)
            )
        }
}
