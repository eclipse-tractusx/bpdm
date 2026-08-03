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
import org.eclipse.tractusx.bpdm.pool.model.error.AddressConstraintParseError
import org.eclipse.tractusx.bpdm.pool.model.request.LogisticAddressRequest
import org.eclipse.tractusx.bpdm.pool.repository.AddressIdentifierRepository
import org.springframework.stereotype.Service

/**
 * Split from the content parser because it needs each entry's owner BPN, known only to the create/update services.
 * [ownerBpns] is positional with [contents] (null for create, the address's own BPN for update); an existing identifier
 * owned by the entry's own BPN is not a duplicate.
 */
@Service
class AddressIdentifierDuplicateValidator(
    private val addressIdentifierRepository: AddressIdentifierRepository
) {

    fun validate(contents: List<LogisticAddressRequest>, ownerBpns: List<String?>): List<List<AddressConstraintParseError>> {
        require(contents.size == ownerBpns.size) { "contents and ownerBpns must be positionally aligned" }
        val candidates = buildCandidates(contents)
        return contents.mapIndexed { i, content -> duplicateErrorsFor(content, ownerBpns[i], candidates) }
    }

    private fun duplicateErrorsFor(
        content: LogisticAddressRequest,
        ownerBpn: String?,
        candidates: Map<Key, Candidate>
    ): List<AddressConstraintParseError> =
        content.identifiers.mapIndexedNotNull { index, identifier ->
            val type = identifier.type
            val value = identifier.value
            if (type == null || value == null) return@mapIndexedNotNull null

            val candidate = candidates[Key(type, value)]
            if (candidate != null && (candidate.bpn == null || candidate.bpn != ownerBpn))
                AddressConstraintParseError.DuplicateIdentifier(index, type, value)
            else
                null
        }

    private fun buildCandidates(contents: List<LogisticAddressRequest>): Map<Key, Candidate> {
        val identifiers = contents.flatMap { it.identifiers }

        val withinRequest = identifiers
            .mapNotNull { id -> if (id.type != null && id.value != null) Key(id.type, id.value) else null }
            .findDuplicates()
            .associateWith { Candidate(bpn = null, type = it.type, value = it.value) }

        val fromDb = addressIdentifierRepository.findByValueIn(identifiers.mapNotNull { it.value })
            .map { Candidate(bpn = it.address.bpn, type = it.type.technicalKey, value = it.value) }
            .associateBy { Key(it.type, it.value) }

        // DB candidates win over within-request ones for the same key (preserves existing behavior).
        return withinRequest.plus(fromDb)
    }

    private data class Key(val type: String, val value: String)
    private data class Candidate(val bpn: String?, val type: String, val value: String)
}