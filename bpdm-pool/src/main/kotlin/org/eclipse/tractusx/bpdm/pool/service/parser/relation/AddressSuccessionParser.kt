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

package org.eclipse.tractusx.bpdm.pool.service.parser.relation

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.api.model.AddressRelationType
import org.eclipse.tractusx.bpdm.pool.entity.AddressRelationDb
import org.eclipse.tractusx.bpdm.pool.entity.AddressRelationKind
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.ReasonCodeDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationValidityPeriodDb
import org.eclipse.tractusx.bpdm.pool.entity.hasOverlap
import org.eclipse.tractusx.bpdm.pool.model.error.AddressSuccessionParseError
import org.eclipse.tractusx.bpdm.pool.model.error.PredecessorAlreadyReplaced
import org.eclipse.tractusx.bpdm.pool.model.error.PredecessorNotFound
import org.eclipse.tractusx.bpdm.pool.model.error.SuccessionCycle
import org.eclipse.tractusx.bpdm.pool.model.error.SuccessorInDifferentLegalEntity
import org.eclipse.tractusx.bpdm.pool.model.error.SuccessorNotFound
import org.eclipse.tractusx.bpdm.pool.model.error.SuccessorRecordedInactive
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressSuccessionParsed
import org.eclipse.tractusx.bpdm.pool.model.toActivityTimeline
import org.eclipse.tractusx.bpdm.pool.model.request.SuccessionUpsertRequest
import org.eclipse.tractusx.bpdm.pool.repository.AddressRelationRepository
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressBpnParser
import org.eclipse.tractusx.bpdm.pool.util.orFailure
import org.eclipse.tractusx.bpdm.pool.util.parsedOrRecord
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Validates and resolves a succession between two addresses.
 */
@Service
class AddressSuccessionParser(
    private val addressBpnParser: AddressBpnParser,
    private val addressRelationRepository: AddressRelationRepository,
    private val successionStateValidator: SuccessionStateValidator
) {

    /**
     * Resolves the two addresses the succession names, decides whether it relocates its legal entity's headquarter,
     * and reports every reason it cannot be written, among them a predecessor that is still in use unless the
     * headquarter relocates, a successor that is not, a successor under another legal entity, a predecessor already
     * replaced by another address over the same time, and a replacement that would close a cycle.
     */
    @Transactional(readOnly = true)
    fun parse(
        request: SuccessionUpsertRequest,
        validityPeriod: RelationValidityPeriodDb,
        reasonCode: ReasonCodeDb?
    ): ParseResult<AddressSuccessionParsed, AddressSuccessionParseError> {
        val errors = mutableListOf<AddressSuccessionParseError>()

        // Both ends resolve to the same entity type in one operation, which ResolutionParseErrors states its shared
        // errors cannot tell apart, so the roles are named here instead.
        val resolutions = addressBpnParser.parse(listOf(request.predecessorBpn, request.successorBpn))
        val predecessor = resolutions[0].parsedOrRecord(errors) { PredecessorNotFound(it.bpn) }
        val successor = resolutions[1].parsedOrRecord(errors) { SuccessorNotFound(it.bpn) }

        if (predecessor == null || successor == null) return ParseResult.Failure(errors)

        val existingRelation = addressRelationRepository
            .findAll(AddressRelationRepository.byRelation(predecessor, successor, AddressRelationType.IsReplacedBy))
            .singleOrNull()
        val kind = parseKind(predecessor, existingRelation)

        errors += validateSameLegalEntity(predecessor, successor)
        errors += validateStates(kind, predecessor, successor, validityPeriod)
        errors += validateSingleSuccessor(kind, predecessor, successor, validityPeriod, existingRelation)
        errors += validateNoCycle(predecessor, successor, validityPeriod, existingRelation)

        return AddressSuccessionParsed(predecessor, successor, kind, validityPeriod, reasonCode, existingRelation).orFailure(errors)
    }

    // Once a relocation has taken effect its predecessor is no longer the legal address, so a stored relation keeps
    // its kind instead of being judged by the legal address again.
    private fun parseKind(predecessor: LogisticAddressDb, existingRelation: AddressRelationDb?): AddressRelationKind =
        when {
            existingRelation != null -> existingRelation.kind
            predecessor.legalEntity?.legalAddress?.bpn == predecessor.bpn -> AddressRelationKind.HeadquarterRelocation
            else -> AddressRelationKind.Succession
        }

    private fun validateStates(
        kind: AddressRelationKind,
        predecessor: LogisticAddressDb,
        successor: LogisticAddressDb,
        validityPeriod: RelationValidityPeriodDb
    ): List<AddressSuccessionParseError> =
        when (kind) {
            AddressRelationKind.Succession -> successionStateValidator.validate(predecessor, successor, validityPeriod.validFrom)
            AddressRelationKind.HeadquarterRelocation -> validateSuccessorInUse(successor, validityPeriod)
        }

    private fun validateSuccessorInUse(
        successor: LogisticAddressDb,
        validityPeriod: RelationValidityPeriodDb
    ): List<AddressSuccessionParseError> =
        when {
            successor.states.toActivityTimeline().isRecordedInactiveWhenReplacingFrom(validityPeriod.validFrom) ->
                listOf(SuccessorRecordedInactive(successor.bpn, validityPeriod.validFrom))
            else -> emptyList()
        }

    private fun validateSameLegalEntity(
        predecessor: LogisticAddressDb,
        successor: LogisticAddressDb
    ): List<AddressSuccessionParseError> =
        when (predecessor.legalEntity?.bpn) {
            successor.legalEntity?.bpn -> emptyList()
            else -> listOf(SuccessorInDifferentLegalEntity(predecessor.bpn, successor.bpn))
        }

    // Several predecessors may share one successor, so the mirror image of this check is deliberately absent: a merger
    // of multiple addresses into one is a valid succession. A former legal address stays in use after its relocation,
    // so a succession may still replace it; the headquarter synchronization never walks back to it.
    private fun validateSingleSuccessor(
        kind: AddressRelationKind,
        predecessor: LogisticAddressDb,
        successor: LogisticAddressDb,
        validityPeriod: RelationValidityPeriodDb,
        existingRelation: AddressRelationDb?
    ): List<AddressSuccessionParseError> =
        findOverlappingSuccessions(predecessor, validityPeriod, existingRelation)
            .filterNot { kind == AddressRelationKind.Succession && it.kind == AddressRelationKind.HeadquarterRelocation }
            .filter { it.endAddress.bpn != successor.bpn }
            .map { PredecessorAlreadyReplaced(predecessor.bpn, it.endAddress.bpn) }

    private fun validateNoCycle(
        predecessor: LogisticAddressDb,
        successor: LogisticAddressDb,
        validityPeriod: RelationValidityPeriodDb,
        existingRelation: AddressRelationDb?
    ): List<AddressSuccessionParseError> {
        val reachedBpns = collectSuccessorBpns(successor, validityPeriod, existingRelation)

        return when {
            reachedBpns.contains(predecessor.bpn) -> listOf(SuccessionCycle(predecessor.bpn, successor.bpn))
            else -> emptyList()
        }
    }

    private fun collectSuccessorBpns(
        successor: LogisticAddressDb,
        validityPeriod: RelationValidityPeriodDb,
        existingRelation: AddressRelationDb?
    ): Set<String> {
        val reachedBpns = mutableSetOf<String>()
        val processingQueue = ArrayDeque(listOf(successor))

        while (processingQueue.isNotEmpty()) {
            val reached = processingQueue.removeFirst()

            if (!reachedBpns.add(reached.bpn)) continue

            findOverlappingSuccessions(reached, validityPeriod, existingRelation)
                .forEach { processingQueue.addLast(it.endAddress) }
        }

        return reachedBpns
    }

    private fun findOverlappingSuccessions(
        predecessor: LogisticAddressDb,
        validityPeriod: RelationValidityPeriodDb,
        existingRelation: AddressRelationDb?
    ): List<AddressRelationDb> =
        addressRelationRepository.findByTypeAndStartAddress(AddressRelationType.IsReplacedBy, predecessor)
            .filterNot { it.id == existingRelation?.id }
            .filter { validityPeriod.hasOverlap(it.validityPeriods) }
}
