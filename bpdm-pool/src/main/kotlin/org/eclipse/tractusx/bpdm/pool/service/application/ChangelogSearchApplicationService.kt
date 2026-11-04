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
import org.eclipse.tractusx.bpdm.pool.model.output.changelog.ChangelogSearchResult
import org.eclipse.tractusx.bpdm.pool.model.raw.changelog.ChangelogSearchRaw
import org.eclipse.tractusx.bpdm.pool.service.operation.changelog.ChangelogSearchService
import org.eclipse.tractusx.bpdm.pool.service.parser.changelog.ChangelogSearchParser
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Searches the changelog of business partners.
 */
@Service
class ChangelogSearchApplicationService(
    private val changelogSearchParser: ChangelogSearchParser,
    private val changelogSearchService: ChangelogSearchService,
    private val dbToOutputMapper: DbToOutputMapper
) {

    /**
     * Returns the requested page of changelog entries matching [request], or every reason the criteria are rejected.
     */
    @Transactional(readOnly = true)
    fun search(request: ChangelogSearchRaw, pageable: Pageable): ChangelogSearchResult =
        search(request, pageable, isDataSpaceParticipant = null)

    /**
     * Returns the requested page of changelog entries of data space participants matching [request], or every reason
     * the criteria are rejected.
     */
    @Transactional(readOnly = true)
    fun searchMembers(request: ChangelogSearchRaw, pageable: Pageable): ChangelogSearchResult =
        search(request, pageable, isDataSpaceParticipant = true)

    private fun search(request: ChangelogSearchRaw, pageable: Pageable, isDataSpaceParticipant: Boolean?): ChangelogSearchResult =
        when (val parseResult = changelogSearchParser.parse(request, isDataSpaceParticipant)) {
            is ParseResult.Failure -> ChangelogSearchResult.Rejected(parseResult.errors)
            is ParseResult.Success -> ChangelogSearchResult.Found(
                changelogSearchService.search(parseResult.parsed, pageable).map(dbToOutputMapper::toRecord)
            )
        }
}
