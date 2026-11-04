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
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityUpdateResult
import org.eclipse.tractusx.bpdm.pool.model.raw.legalentity.LegalEntityUpdateRaw
import org.eclipse.tractusx.bpdm.pool.service.operation.legalentity.LegalEntityPayloadUpdateService
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.LegalEntityUpdateParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Updates existing legal entities, each together with its legal address.
 */
@Service
class LegalEntityUpdateApplicationService(
    private val legalEntityUpdateParser: LegalEntityUpdateParser,
    private val legalEntityPayloadUpdateService: LegalEntityPayloadUpdateService,
    private val dbToOutputMapper: DbToOutputMapper
) {

    /**
     * Applies each entry of [requests] to the legal entity it addresses by BPN and returns, per entry, either the legal
     * entity as stored or every reason that entry is rejected.
     */
    @Transactional
    fun update(requests: List<LegalEntityUpdateRaw>): List<LegalEntityUpdateResult> =
        parseAndExecute(requests, legalEntityUpdateParser::parse, legalEntityPayloadUpdateService::update).map { result ->
            when (result) {
                is ParseResult.Success -> LegalEntityUpdateResult.Updated(dbToOutputMapper.toRecord(result.parsed.value))
                is ParseResult.Failure -> LegalEntityUpdateResult.Rejected(result.errors)
            }
        }
}
