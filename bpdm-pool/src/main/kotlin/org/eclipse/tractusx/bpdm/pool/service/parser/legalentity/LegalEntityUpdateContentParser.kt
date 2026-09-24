/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.service.parser.legalentity

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.combine
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityContentWrite
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityUpdateContentWrite
import org.eclipse.tractusx.bpdm.pool.model.error.LegalEntityUpdateContentParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityUpdateParsed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Validates the content one legal entity update states, together with the ownership and relation rules that hold only
 * once the legal entity exists.
 */
@Service
class LegalEntityUpdateContentParser(
    private val legalEntityContentParser: LegalEntityContentParser,
    private val ownershipValidator: LegalEntityOwnershipValidator,
    private val stateRelationValidator: LegalEntityStateRelationValidator
) {

    /**
     * Validates each update and reports either the resolved target with its validated content or every problem found in
     * that entry.
     */
    @Transactional(readOnly = true)
    fun parse(writes: List<LegalEntityUpdateContentWrite>): List<ParseResult<LegalEntityUpdateParsed, LegalEntityUpdateContentParseError>> {
        val contentWrites = writes.map { LegalEntityContentWrite(it.content, it.target) }
        val contentResults = legalEntityContentParser.parse(contentWrites)
        val ownershipViolations = ownershipValidator.validate(contentWrites.map { it.headerWrite })
        val stateRelationViolations = stateRelationValidator.validate(writes)

        return writes.indices.map { index ->
            contentResults[index].combine(ownershipViolations[index] + stateRelationViolations[index]) { content ->
                LegalEntityUpdateParsed(writes[index].target, content)
            }
        }
    }
}
