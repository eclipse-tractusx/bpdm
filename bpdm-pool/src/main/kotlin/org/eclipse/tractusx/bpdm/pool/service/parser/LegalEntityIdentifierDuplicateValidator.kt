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
import org.eclipse.tractusx.bpdm.pool.model.error.LegalEntityContentParseError
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityHeaderRequest
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityIdentifierRepository
import org.springframework.stereotype.Service

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
/**
 * Identity-aware uniqueness check for legal-entity identifiers, the counterpart of [AddressIdentifierDuplicateValidator].
 * Kept out of the header parser because it needs each entry's owner BPN — which only the create/update services know.
 * Batched: the candidate map (within-request duplicates + existing DB identifiers) is built once for the whole input,
 * then each entry is checked against it.
 *
 * [ownerBpns] is positional with [headers] (null for create, the legal entity's own BPN for update); an existing
 * identifier owned by the entry's own BPN is not a duplicate.
 */
@Service
class LegalEntityIdentifierDuplicateValidator(
    private val legalEntityIdentifierRepository: LegalEntityIdentifierRepository
) {

    fun validate(headers: List<LegalEntityHeaderRequest>, ownerBpns: List<String?>): List<List<LegalEntityContentParseError>> {
        require(headers.size == ownerBpns.size) { "headers and ownerBpns must be positionally aligned" }
        val candidates = buildCandidates(headers)
        return headers.mapIndexed { i, header -> duplicateErrorsFor(header, ownerBpns[i], candidates) }
    }

    private fun duplicateErrorsFor(
        header: LegalEntityHeaderRequest,
        ownerBpn: String?,
        candidates: Map<Key, Candidate>
    ): List<LegalEntityContentParseError> =
        header.identifiers.mapIndexedNotNull { index, identifier ->
            val type = identifier.type
            val value = identifier.value
            if (type == null || value == null) return@mapIndexedNotNull null

            val candidate = candidates[Key(type, value)]
            if (candidate != null && (candidate.bpn == null || candidate.bpn != ownerBpn))
                LegalEntityContentParseError.DuplicateIdentifier(index, type, value)
            else
                null
        }

    private fun buildCandidates(headers: List<LegalEntityHeaderRequest>): Map<Key, Candidate> {
        val identifiers = headers.flatMap { it.identifiers }

        val withinRequest = identifiers
            .mapNotNull { id -> if (id.type != null && id.value != null) Key(id.type, id.value) else null }
            .findDuplicates()
            .associateWith { Candidate(bpn = null, type = it.type, value = it.value) }

        val fromDb = legalEntityIdentifierRepository.findByValueIn(identifiers.mapNotNull { it.value })
            .map { Candidate(bpn = it.legalEntity.bpn, type = it.type.technicalKey, value = it.value) }
            .associateBy { Key(it.type, it.value) }

        // DB candidates win over within-request ones for the same key (preserves existing behavior).
        return withinRequest.plus(fromDb)
    }

    private data class Key(val type: String, val value: String)
    private data class Candidate(val bpn: String?, val type: String, val value: String)
}