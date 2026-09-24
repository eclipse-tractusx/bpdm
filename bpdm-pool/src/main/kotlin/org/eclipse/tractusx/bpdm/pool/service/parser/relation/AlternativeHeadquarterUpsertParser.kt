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
import org.eclipse.tractusx.bpdm.pool.model.error.AlternativeAlreadyAlternative
import org.eclipse.tractusx.bpdm.pool.model.error.AlternativeAlreadyMain
import org.eclipse.tractusx.bpdm.pool.model.error.AlternativeFlaggedUltimateOwner
import org.eclipse.tractusx.bpdm.pool.model.error.AlternativeHeadquarterUpsertParseError
import org.eclipse.tractusx.bpdm.pool.model.error.AlternativeNotFound
import org.eclipse.tractusx.bpdm.pool.model.error.AlternativeOfItself
import org.eclipse.tractusx.bpdm.pool.model.error.AlternativeOwned
import org.eclipse.tractusx.bpdm.pool.model.error.AlternativeOwns
import org.eclipse.tractusx.bpdm.pool.model.error.MainAlreadyAlternative
import org.eclipse.tractusx.bpdm.pool.model.error.MainNotFound
import org.eclipse.tractusx.bpdm.pool.model.error.RelationReasonCodeNotFound
import org.eclipse.tractusx.bpdm.pool.model.error.ReverseDesignationExists
import org.eclipse.tractusx.bpdm.pool.model.parsed.AlternativeHeadquarterUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.request.AlternativeHeadquarterUpsertRequest
import org.eclipse.tractusx.bpdm.pool.repository.ReasonCodeRepository
import org.eclipse.tractusx.bpdm.pool.repository.RelationRepository
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.LegalEntityBpnParser
import org.eclipse.tractusx.bpdm.pool.util.orFailure
import org.eclipse.tractusx.bpdm.pool.util.parsedOrRecord
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Validates and resolves one alternative headquarter designation, in which one legal entity is designated an
 * alternative of a main legal entity.
 *
 * Designations are parsed one at a time rather than as a batch: whether a legal entity is already a main or an
 * alternative is a question about the designations the entries before it left behind.
 */
@Service
class AlternativeHeadquarterUpsertParser(
    private val legalEntityBpnParser: LegalEntityBpnParser,
    private val reasonCodeRepository: ReasonCodeRepository,
    private val relationRepository: RelationRepository,
    private val relationValidityPeriodsValidator: RelationValidityPeriodsValidator,
    private val alternativeHeadquarterStateValidator: AlternativeHeadquarterStateValidator
) {

    /**
     * Resolves the two legal entities the designation names and reports every reason it cannot be written, among them
     * either of them recorded as inactive during a validity period, a designation that would break the star of
     * alternatives around one main, and an alternative that takes part in ownership.
     */
    @Transactional(readOnly = true)
    fun parse(request: AlternativeHeadquarterUpsertRequest): ParseResult<AlternativeHeadquarterUpsertParsed, AlternativeHeadquarterUpsertParseError> {
        val errors = mutableListOf<AlternativeHeadquarterUpsertParseError>()

        // Both ends resolve to the same entity type in one operation, which ResolutionParseErrors states its shared
        // errors cannot tell apart, so the roles are named here instead.
        val resolutions = legalEntityBpnParser.parse(listOf(request.alternativeBpn, request.mainBpn))
        val alternative = resolutions[0].parsedOrRecord(errors) { AlternativeNotFound(it.bpn) }
        val main = resolutions[1].parsedOrRecord(errors) { MainNotFound(it.bpn) }

        if (alternative != null && main != null && alternative.bpn == main.bpn) errors.add(AlternativeOfItself(request.alternativeBpn))

        errors += relationValidityPeriodsValidator.validate(request.validityPeriods)
        val reasonCode = request.reasonCode?.let {
            reasonCodeRepository.findByTechnicalKey(it) ?: run { errors.add(RelationReasonCodeNotFound(it)); null }
        }

        if (alternative == null || main == null || errors.isNotEmpty()) return ParseResult.Failure(errors)

        val validityPeriods = request.validityPeriods.map { RelationValidityPeriodDb(validFrom = it.validFrom, validTo = it.validTo) }
        val existingRelation = relationRepository
            .findAll(RelationRepository.byRelation(alternative, main, LegalEntityRelationType.IsAlternativeHeadquarterFor))
            .singleOrNull()

        errors += alternativeHeadquarterStateValidator.validate(alternative, main, validityPeriods)
        errors += validateAlternativeRole(alternative, main, validityPeriods, existingRelation)
        errors += validateMainRole(alternative, main, validityPeriods, existingRelation)
        errors += validateAlternativeOutsideOwnership(alternative, validityPeriods)
        errors += validateAlternativeNotFlagged(alternative)

        return AlternativeHeadquarterUpsertParsed(alternative, main, validityPeriods, reasonCode, existingRelation).orFailure(errors)
    }

    private fun validateAlternativeRole(
        alternative: LegalEntityDb,
        main: LegalEntityDb,
        validityPeriods: List<RelationValidityPeriodDb>,
        existingRelation: RelationDb?
    ): List<AlternativeHeadquarterUpsertParseError> =
        findOverlappingDesignations(alternative, validityPeriods, existingRelation).mapNotNull { designation ->
            when {
                designation.endNode.bpn == alternative.bpn && designation.startNode.bpn != main.bpn ->
                    AlternativeAlreadyMain(alternative.bpn, designation.startNode.bpn)
                designation.startNode.bpn == alternative.bpn && designation.endNode.bpn != main.bpn ->
                    AlternativeAlreadyAlternative(alternative.bpn, designation.endNode.bpn)
                else -> null
            }
        }

    private fun validateMainRole(
        alternative: LegalEntityDb,
        main: LegalEntityDb,
        validityPeriods: List<RelationValidityPeriodDb>,
        existingRelation: RelationDb?
    ): List<AlternativeHeadquarterUpsertParseError> =
        findOverlappingDesignations(main, validityPeriods, existingRelation)
            .filter { it.startNode.bpn == main.bpn }
            .map { designation ->
                when (designation.endNode.bpn) {
                    alternative.bpn -> ReverseDesignationExists(alternative.bpn, main.bpn)
                    else -> MainAlreadyAlternative(main.bpn, designation.endNode.bpn)
                }
            }

    private fun validateAlternativeOutsideOwnership(
        alternative: LegalEntityDb,
        validityPeriods: List<RelationValidityPeriodDb>
    ): List<AlternativeHeadquarterUpsertParseError> =
        relationRepository.findInSourceOrTarget(LegalEntityRelationType.IsOwnedBy, alternative)
            .filterOverlapping(validityPeriods, null)
            .map { ownership ->
                when (ownership.startNode.bpn) {
                    alternative.bpn -> AlternativeOwned(alternative.bpn, ownership.endNode.bpn)
                    else -> AlternativeOwns(alternative.bpn, ownership.startNode.bpn)
                }
            }

    private fun validateAlternativeNotFlagged(alternative: LegalEntityDb): List<AlternativeHeadquarterUpsertParseError> =
        if (alternative.ownershipUltimate) listOf(AlternativeFlaggedUltimateOwner(alternative.bpn)) else emptyList()

    private fun findOverlappingDesignations(
        legalEntity: LegalEntityDb,
        validityPeriods: List<RelationValidityPeriodDb>,
        existingRelation: RelationDb?
    ): List<RelationDb> =
        relationRepository.findInSourceOrTarget(LegalEntityRelationType.IsAlternativeHeadquarterFor, legalEntity)
            .filterOverlapping(validityPeriods, existingRelation)
}
