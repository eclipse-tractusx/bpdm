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
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityRecord
import org.eclipse.tractusx.bpdm.pool.model.raw.legalentity.LegalEntitySearchRaw
import org.eclipse.tractusx.bpdm.pool.service.operation.legalentity.LegalEntitySearchService
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.LegalEntitySearchParser
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Searches legal entities.
 */
@Service
class LegalEntitySearchApplicationService(
    private val legalEntitySearchParser: LegalEntitySearchParser,
    private val legalEntitySearchService: LegalEntitySearchService,
    private val dbToOutputMapper: DbToOutputMapper
) {

    /**
     * Returns the requested page of legal entities matching [request].
     */
    @Transactional(readOnly = true)
    fun search(request: LegalEntitySearchRaw, pageable: Pageable): Page<LegalEntityRecord> =
        search(request, pageable, isDataSpaceParticipant = null)

    /**
     * Returns the requested page of data space participant legal entities matching [request].
     */
    @Transactional(readOnly = true)
    fun searchMembers(request: LegalEntitySearchRaw, pageable: Pageable): Page<LegalEntityRecord> =
        search(request, pageable, isDataSpaceParticipant = true)

    private fun search(request: LegalEntitySearchRaw, pageable: Pageable, isDataSpaceParticipant: Boolean?): Page<LegalEntityRecord> =
        legalEntitySearchService.search(legalEntitySearchParser.parse(request, isDataSpaceParticipant), pageable)
            .map(dbToOutputMapper::toRecord)
}
