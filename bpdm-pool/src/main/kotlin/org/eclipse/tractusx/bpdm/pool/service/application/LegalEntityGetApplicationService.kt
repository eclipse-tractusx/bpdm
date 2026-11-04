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
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityGetResult
import org.eclipse.tractusx.bpdm.pool.model.raw.legalentity.LegalEntityGetRaw
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.LegalEntityGetParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Fetches a single legal entity by one of its identifiers.
 */
@Service
class LegalEntityGetApplicationService(
    private val legalEntityGetParser: LegalEntityGetParser,
    private val dbToOutputMapper: DbToOutputMapper
) {

    /**
     * Returns the legal entity carrying the identifier [request] names, or the reason no legal entity could be found.
     */
    @Transactional(readOnly = true)
    fun get(request: LegalEntityGetRaw): LegalEntityGetResult =
        when (val parseResult = legalEntityGetParser.parse(request)) {
            is ParseResult.Failure -> LegalEntityGetResult.Rejected(parseResult.errors)
            is ParseResult.Success -> LegalEntityGetResult.Found(dbToOutputMapper.toRecord(parseResult.parsed))
        }
}
