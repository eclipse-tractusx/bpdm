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
import org.eclipse.tractusx.bpdm.pool.api.model.SiteRelationType
import org.eclipse.tractusx.bpdm.pool.entity.ReasonCodeDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationValidityPeriodDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteRelationDb
import org.eclipse.tractusx.bpdm.pool.entity.hasOverlap
import org.eclipse.tractusx.bpdm.pool.model.error.PredecessorAlreadyReplaced
import org.eclipse.tractusx.bpdm.pool.model.error.PredecessorNotFound
import org.eclipse.tractusx.bpdm.pool.model.error.SiteSuccessionParseError
import org.eclipse.tractusx.bpdm.pool.model.error.SuccessionCycle
import org.eclipse.tractusx.bpdm.pool.model.error.SuccessorInDifferentLegalEntity
import org.eclipse.tractusx.bpdm.pool.model.error.SuccessorNotFound
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteSuccessionParsed
import org.eclipse.tractusx.bpdm.pool.model.request.SuccessionUpsertRequest
import org.eclipse.tractusx.bpdm.pool.repository.SiteRelationRepository
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteBpnParser
import org.eclipse.tractusx.bpdm.pool.util.orFailure
import org.eclipse.tractusx.bpdm.pool.util.parsedOrRecord
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Validates and resolves a succession between two sites.
 */
@Service
class SiteSuccessionParser(
    private val siteBpnParser: SiteBpnParser,
    private val siteRelationRepository: SiteRelationRepository,
    private val successionStateValidator: SuccessionStateValidator
) {

    /**
     * Resolves the two sites the succession names and reports every reason it cannot be written, among them a
     * predecessor that is still in use, a successor that is not, a successor under another legal entity, a
     * predecessor already replaced by another site over the same time, and a replacement that would close a cycle.
     */
    @Transactional(readOnly = true)
    fun parse(
        request: SuccessionUpsertRequest,
        validityPeriod: RelationValidityPeriodDb,
        reasonCode: ReasonCodeDb?
    ): ParseResult<SiteSuccessionParsed, SiteSuccessionParseError> {
        val errors = mutableListOf<SiteSuccessionParseError>()

        // Both ends resolve to the same entity type in one operation, which ResolutionParseErrors states its shared
        // errors cannot tell apart, so the roles are named here instead.
        val resolutions = siteBpnParser.parse(listOf(request.predecessorBpn, request.successorBpn))
        val predecessor = resolutions[0].parsedOrRecord(errors) { PredecessorNotFound(it.bpn) }
        val successor = resolutions[1].parsedOrRecord(errors) { SuccessorNotFound(it.bpn) }

        if (predecessor == null || successor == null) return ParseResult.Failure(errors)

        val existingRelation = siteRelationRepository
            .findAll(SiteRelationRepository.byRelation(predecessor, successor, SiteRelationType.IsReplacedBy))
            .singleOrNull()

        errors += validateSameLegalEntity(predecessor, successor)
        errors += successionStateValidator.validate(predecessor, successor, validityPeriod.validFrom)
        errors += validateSingleSuccessor(predecessor, successor, validityPeriod, existingRelation)
        errors += validateNoCycle(predecessor, successor, validityPeriod, existingRelation)

        return SiteSuccessionParsed(predecessor, successor, validityPeriod, reasonCode, existingRelation).orFailure(errors)
    }

    private fun validateSameLegalEntity(predecessor: SiteDb, successor: SiteDb): List<SiteSuccessionParseError> =
        when (predecessor.legalEntity.bpn) {
            successor.legalEntity.bpn -> emptyList()
            else -> listOf(SuccessorInDifferentLegalEntity(predecessor.bpn, successor.bpn))
        }

    // Several predecessors may share one successor, so the mirror image of this check is deliberately absent: a merger
    // of multiple sites into one is a valid succession.
    private fun validateSingleSuccessor(
        predecessor: SiteDb,
        successor: SiteDb,
        validityPeriod: RelationValidityPeriodDb,
        existingRelation: SiteRelationDb?
    ): List<SiteSuccessionParseError> =
        findOverlappingSuccessions(predecessor, validityPeriod, existingRelation)
            .filter { it.endSite.bpn != successor.bpn }
            .map { PredecessorAlreadyReplaced(predecessor.bpn, it.endSite.bpn) }

    private fun validateNoCycle(
        predecessor: SiteDb,
        successor: SiteDb,
        validityPeriod: RelationValidityPeriodDb,
        existingRelation: SiteRelationDb?
    ): List<SiteSuccessionParseError> {
        val reachedBpns = collectSuccessorBpns(successor, validityPeriod, existingRelation)

        return when {
            reachedBpns.contains(predecessor.bpn) -> listOf(SuccessionCycle(predecessor.bpn, successor.bpn))
            else -> emptyList()
        }
    }

    private fun collectSuccessorBpns(
        successor: SiteDb,
        validityPeriod: RelationValidityPeriodDb,
        existingRelation: SiteRelationDb?
    ): Set<String> {
        val reachedBpns = mutableSetOf<String>()
        val processingQueue = ArrayDeque(listOf(successor))

        while (processingQueue.isNotEmpty()) {
            val reached = processingQueue.removeFirst()

            if (!reachedBpns.add(reached.bpn)) continue

            findOverlappingSuccessions(reached, validityPeriod, existingRelation)
                .forEach { processingQueue.addLast(it.endSite) }
        }

        return reachedBpns
    }

    private fun findOverlappingSuccessions(
        predecessor: SiteDb,
        validityPeriod: RelationValidityPeriodDb,
        existingRelation: SiteRelationDb?
    ): List<SiteRelationDb> =
        siteRelationRepository.findByTypeAndStartSite(SiteRelationType.IsReplacedBy, predecessor)
            .filterNot { it.id == existingRelation?.id }
            .filter { validityPeriod.hasOverlap(it.validityPeriods) }
}
