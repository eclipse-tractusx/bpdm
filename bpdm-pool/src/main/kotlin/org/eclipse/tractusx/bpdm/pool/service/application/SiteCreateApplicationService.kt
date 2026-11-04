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
import org.eclipse.tractusx.bpdm.common.model.parseAndExecute
import org.eclipse.tractusx.bpdm.pool.mapper.DbToOutputMapper
import org.eclipse.tractusx.bpdm.pool.model.output.site.SiteCreateResult
import org.eclipse.tractusx.bpdm.pool.model.raw.site.SiteCreateRaw
import org.eclipse.tractusx.bpdm.pool.service.operation.site.SiteCreateService
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteCreateParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Creates sites under existing legal entities, each with a main address of its own.
 */
@Service
class SiteCreateApplicationService(
    private val siteCreateParser: SiteCreateParser,
    private val siteCreateService: SiteCreateService,
    private val dbToOutputMapper: DbToOutputMapper
) {

    /**
     * Creates the sites of [requests] with their main addresses and returns, per entry, either the site as stored or
     * every reason that entry is rejected.
     */
    @Transactional
    fun create(requests: List<SiteCreateRaw>): List<SiteCreateResult> =
        parseAndExecute(requests, siteCreateParser::parse, siteCreateService::create).map { result ->
            when (result) {
                is ParseResult.Success -> SiteCreateResult.Created(dbToOutputMapper.toRecord(result.parsed))
                is ParseResult.Failure -> SiteCreateResult.Rejected(result.errors)
            }
        }
}
