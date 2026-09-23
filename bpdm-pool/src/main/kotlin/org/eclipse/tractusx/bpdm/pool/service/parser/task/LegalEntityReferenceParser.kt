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


package org.eclipse.tractusx.bpdm.pool.service.parser.task

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.model.error.LegalEntityNotFound
import org.eclipse.tractusx.bpdm.pool.model.parsed.BpnReferenceParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.ResolvedReference
import org.eclipse.tractusx.bpdm.pool.model.request.BpnReferenceRequest
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.LegalEntityBpnParser
import org.springframework.stereotype.Service

/**
 * Turns a legal entity's stated BPN reference into the legal entity it names.
 */
@Service
class LegalEntityReferenceParser(
    private val bpnReferenceParser: BpnReferenceParser,
    private val legalEntityBpnParser: LegalEntityBpnParser
) {

    /**
     * Reports the reference together with the legal entity it names, or a rejection where it names none that exists.
     */
    fun parse(
        reference: BpnReferenceRequest
    ): ParseResult<ResolvedReference<LegalEntityDb>, LegalEntityNotFound> {
        val parsed = bpnReferenceParser.parse(reference)
        val bpn = (parsed as? BpnReferenceParsed.Existing)?.bpn
            ?: return ParseResult.Success(ResolvedReference(parsed, existingRecord = null))

        return when (val result = legalEntityBpnParser.parse(listOf(bpn)).single()) {
            is ParseResult.Success -> ParseResult.Success(ResolvedReference(parsed, result.parsed))
            is ParseResult.Failure -> ParseResult.ofSingleFailure(LegalEntityNotFound(bpn))
        }
    }
}
