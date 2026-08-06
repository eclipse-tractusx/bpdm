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

package org.eclipse.tractusx.bpdm.pool.service.parser

import org.eclipse.tractusx.bpdm.common.util.findDuplicates
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.combine
import org.eclipse.tractusx.bpdm.pool.model.error.DataSpaceParticipantUpdateParseError
import org.eclipse.tractusx.bpdm.pool.model.error.DataSpaceParticipantUpdateParseError.DuplicateParticipantEntry
import org.eclipse.tractusx.bpdm.pool.model.parsed.DataSpaceParticipantUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.request.DataSpaceParticipantUpdateRequest
import org.springframework.stereotype.Service

/**
 * Validates data space participation update requests: the legal entity each one addresses, and that no two entries of
 * one request address the same one.
 */
@Service
class DataSpaceParticipantUpdateParser(
    private val legalEntityBpnParser: LegalEntityBpnParser
) {

    /**
     * Validates each request and reports either the resolved legal entity with the participation to set on it, or every
     * problem found in that entry.
     */
    fun parse(requests: List<DataSpaceParticipantUpdateRequest>): List<ParseResult<DataSpaceParticipantUpdateParsed, DataSpaceParticipantUpdateParseError>> {
        val targetResults = legalEntityBpnParser.parse(requests.map { it.legalEntityBpn })
        val duplicateErrors = duplicateErrors(requests)

        return requests.indices.map { index ->
            val requested = requests[index].isDataSpaceParticipant
            targetResults[index].combine(duplicateErrors[index]) { target -> DataSpaceParticipantUpdateParsed(target, requested) }
        }
    }

    private fun duplicateErrors(requests: List<DataSpaceParticipantUpdateRequest>): List<List<DataSpaceParticipantUpdateParseError>> {
        val duplicatedBpns = requests.map { it.legalEntityBpn }.findDuplicates()

        return requests.map { request ->
            if (request.legalEntityBpn in duplicatedBpns) listOf(DuplicateParticipantEntry(request.legalEntityBpn)) else emptyList()
        }
    }
}
