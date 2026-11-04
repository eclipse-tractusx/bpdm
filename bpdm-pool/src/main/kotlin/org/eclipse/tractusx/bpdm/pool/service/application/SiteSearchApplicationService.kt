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

import org.eclipse.tractusx.bpdm.pool.mapper.DbToOutputMapper
import org.eclipse.tractusx.bpdm.pool.model.output.site.SiteRecord
import org.eclipse.tractusx.bpdm.pool.model.raw.site.SiteSearchRaw
import org.eclipse.tractusx.bpdm.pool.service.operation.site.SiteSearchService
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteSearchParser
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Searches sites.
 */
@Service
class SiteSearchApplicationService(
    private val siteSearchParser: SiteSearchParser,
    private val siteSearchService: SiteSearchService,
    private val dbToOutputMapper: DbToOutputMapper
) {

    /**
     * Returns the requested page of sites matching [request].
     */
    @Transactional(readOnly = true)
    fun search(request: SiteSearchRaw, pageable: Pageable): Page<SiteRecord> =
        search(request, pageable, isDataSpaceParticipant = null)

    /**
     * Returns the requested page of data space participant sites matching [request].
     */
    @Transactional(readOnly = true)
    fun searchMembers(request: SiteSearchRaw, pageable: Pageable): Page<SiteRecord> =
        search(request, pageable, isDataSpaceParticipant = true)

    private fun search(request: SiteSearchRaw, pageable: Pageable, isDataSpaceParticipant: Boolean?): Page<SiteRecord> =
        siteSearchService.search(siteSearchParser.parse(request, isDataSpaceParticipant), pageable)
            .map(dbToOutputMapper::toRecord)
}
