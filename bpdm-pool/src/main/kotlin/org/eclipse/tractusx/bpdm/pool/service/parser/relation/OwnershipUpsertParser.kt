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
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityRelationType
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationValidityPeriodDb
import org.eclipse.tractusx.bpdm.pool.entity.filterOverlapping
import org.eclipse.tractusx.bpdm.pool.model.error.GovernedByItself
import org.eclipse.tractusx.bpdm.pool.model.error.GovernedPartnerNotFound
import org.eclipse.tractusx.bpdm.pool.model.error.GoverningPartnerNotFound
import org.eclipse.tractusx.bpdm.pool.model.error.OwnedAlreadyOwned
import org.eclipse.tractusx.bpdm.pool.model.error.OwnershipCycle
import org.eclipse.tractusx.bpdm.pool.model.error.OwnershipPartnerIsAlternativeHeadquarter
import org.eclipse.tractusx.bpdm.pool.model.error.OwnershipUpsertParseError
import org.eclipse.tractusx.bpdm.pool.model.error.RelationReasonCodeNotFound
import org.eclipse.tractusx.bpdm.pool.model.parsed.OwnershipUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.request.OwnershipUpsertRequest
import org.eclipse.tractusx.bpdm.pool.repository.ReasonCodeRepository
import org.eclipse.tractusx.bpdm.pool.repository.RelationRepository
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.LegalEntityBpnParser
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.UltimateOwnerUniquenessValidator
import org.eclipse.tractusx.bpdm.pool.util.orFailure
import org.eclipse.tractusx.bpdm.pool.util.parsedOrRecord
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Validates and resolves one ownership, in which one legal entity is owned by another.
 *
 * Ownerships are parsed one at a time rather than as a batch: whether a legal entity already has an owner, and whether
 * an ownership closes a cycle, are questions about the ownership graph, so an entry has to be judged against the graph
 * the entries before it left behind.
 */
@Service
class OwnershipUpsertParser(
    private val legalEntityBpnParser: LegalEntityBpnParser,
    private val reasonCodeRepository: ReasonCodeRepository,
    private val relationRepository: RelationRepository,
    private val relationValidityPeriodsValidator: RelationValidityPeriodsValidator,
    private val governingPartnerStateValidator: GoverningPartnerStateValidator,
    private val ultimateOwnerUniquenessValidator: UltimateOwnerUniquenessValidator
) {

    /**
     * Resolves the two legal entities the ownership names and reports every reason it cannot be written, among them an
     * owner recorded as inactive during a validity period, an owned legal entity that already has another owner, and an
     * ownership that would close a cycle.
     */
    @Transactional(readOnly = true)
    fun parse(request: OwnershipUpsertRequest): ParseResult<OwnershipUpsertParsed, OwnershipUpsertParseError> {
        val errors = mutableListOf<OwnershipUpsertParseError>()

        // Both ends resolve to the same entity type in one operation, which ResolutionParseErrors states its shared
        // errors cannot tell apart, so the roles are named here instead.
        val resolutions = legalEntityBpnParser.parse(listOf(request.ownedBpn, request.ownerBpn))
        val owned = resolutions[0].parsedOrRecord(errors) { GovernedPartnerNotFound(it.bpn) }
        val owner = resolutions[1].parsedOrRecord(errors) { GoverningPartnerNotFound(it.bpn) }

        if (owned != null && owner != null && owned.bpn == owner.bpn) errors.add(GovernedByItself(request.ownedBpn))

        errors += relationValidityPeriodsValidator.validate(request.validityPeriods)
        val reasonCode = request.reasonCode?.let {
            reasonCodeRepository.findByTechnicalKey(it) ?: run { errors.add(RelationReasonCodeNotFound(it)); null }
        }

        if (owned == null || owner == null || errors.isNotEmpty()) return ParseResult.Failure(errors)

        val validityPeriods = request.validityPeriods.map { RelationValidityPeriodDb(validFrom = it.validFrom, validTo = it.validTo) }
        val existingRelation = relationRepository
            .findAll(RelationRepository.byRelation(owned, owner, LegalEntityRelationType.IsOwnedBy))
            .singleOrNull()

        errors += governingPartnerStateValidator.validate(owner, validityPeriods)
        errors += validateSingleOwner(owned, owner, validityPeriods, existingRelation)
        errors += validateNoCycle(owned, owner, validityPeriods, existingRelation)
        errors += validateNotAlternativeHeadquarter(owned, validityPeriods, existingRelation)
        errors += validateNotAlternativeHeadquarter(owner, validityPeriods, existingRelation)
        errors += ultimateOwnerUniquenessValidator.validateOwnershipEdge(owned, owner)

        return OwnershipUpsertParsed(owned, owner, validityPeriods, reasonCode, existingRelation).orFailure(errors)
    }

    private fun validateSingleOwner(
        owned: LegalEntityDb,
        owner: LegalEntityDb,
        validityPeriods: List<RelationValidityPeriodDb>,
        existingRelation: RelationDb?
    ): List<OwnershipUpsertParseError> =
        findOverlappingOwnerships(owned, validityPeriods, existingRelation)
            .filter { it.endNode.bpn != owner.bpn }
            .map { OwnedAlreadyOwned(owned.bpn, it.endNode.bpn) }

    private fun validateNoCycle(
        owned: LegalEntityDb,
        owner: LegalEntityDb,
        validityPeriods: List<RelationValidityPeriodDb>,
        existingRelation: RelationDb?
    ): List<OwnershipUpsertParseError> {
        val ancestorBpns = collectAncestorBpns(owner, validityPeriods, existingRelation)

        return when {
            ancestorBpns.contains(owned.bpn) -> listOf(OwnershipCycle(owned.bpn, owner.bpn))
            else -> emptyList()
        }
    }

    private fun validateNotAlternativeHeadquarter(
        legalEntity: LegalEntityDb,
        validityPeriods: List<RelationValidityPeriodDb>,
        existingRelation: RelationDb?
    ): List<OwnershipUpsertParseError> =
        relationRepository.findByTypeAndStartNode(LegalEntityRelationType.IsAlternativeHeadquarterFor, legalEntity)
            .filterOverlapping(validityPeriods, existingRelation)
            .take(1)
            .map { OwnershipPartnerIsAlternativeHeadquarter(legalEntity.bpn, it.endNode.bpn) }

    private fun collectAncestorBpns(
        owner: LegalEntityDb,
        validityPeriods: List<RelationValidityPeriodDb>,
        existingRelation: RelationDb?
    ): Set<String> {
        val reachedBpns = mutableSetOf<String>()
        val processingQueue = ArrayDeque(listOf(owner))

        while (processingQueue.isNotEmpty()) {
            val reached = processingQueue.removeFirst()

            if (!reachedBpns.add(reached.bpn)) continue

            findOverlappingOwnerships(reached, validityPeriods, existingRelation)
                .forEach { processingQueue.addLast(it.endNode) }
        }

        return reachedBpns
    }

    private fun findOverlappingOwnerships(
        owned: LegalEntityDb,
        validityPeriods: List<RelationValidityPeriodDb>,
        existingRelation: RelationDb?
    ): List<RelationDb> =
        relationRepository.findByTypeAndStartNode(LegalEntityRelationType.IsOwnedBy, owned)
            .filterOverlapping(validityPeriods, existingRelation)
}
