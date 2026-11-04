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
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityCreateResult
import org.eclipse.tractusx.bpdm.pool.model.raw.legalentity.LegalEntityCreateRaw
import org.eclipse.tractusx.bpdm.pool.service.operation.legalentity.LegalEntityCreateService
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.LegalEntityCreateParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Creates new legal entities, each with its legal address.
 */
@Service
class LegalEntityCreateApplicationService(
    private val legalEntityCreateParser: LegalEntityCreateParser,
    private val legalEntityCreateService: LegalEntityCreateService,
    private val dbToOutputMapper: DbToOutputMapper
) {

    /**
     * Creates the legal entities of [requests] with their legal addresses and returns, per entry, either the legal entity
     * as stored or every reason that entry is rejected.
     */
    @Transactional
    fun create(requests: List<LegalEntityCreateRaw>): List<LegalEntityCreateResult> =
        parseAndExecute(requests, legalEntityCreateParser::parse, legalEntityCreateService::create).map { result ->
            when (result) {
                is ParseResult.Success -> LegalEntityCreateResult.Created(dbToOutputMapper.toRecord(result.parsed))
                is ParseResult.Failure -> LegalEntityCreateResult.Rejected(result.errors)
            }
        }
}
