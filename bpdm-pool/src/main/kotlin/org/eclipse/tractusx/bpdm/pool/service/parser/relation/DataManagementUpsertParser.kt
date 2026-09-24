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
import org.eclipse.tractusx.bpdm.pool.model.error.DataManagementUpsertParseError
import org.eclipse.tractusx.bpdm.pool.model.error.GovernedByItself
import org.eclipse.tractusx.bpdm.pool.model.error.GovernedPartnerNotFound
import org.eclipse.tractusx.bpdm.pool.model.error.GoverningPartnerNotFound
import org.eclipse.tractusx.bpdm.pool.model.error.ManagedAlreadyManaged
import org.eclipse.tractusx.bpdm.pool.model.error.ManagedIsManager
import org.eclipse.tractusx.bpdm.pool.model.error.ManagerIsManaged
import org.eclipse.tractusx.bpdm.pool.model.error.ManagerNotDataSpaceParticipant
import org.eclipse.tractusx.bpdm.pool.model.error.PastValidityPeriodAdded
import org.eclipse.tractusx.bpdm.pool.model.error.PastValidityPeriodEndMoved
import org.eclipse.tractusx.bpdm.pool.model.error.PastValidityPeriodRemoved
import org.eclipse.tractusx.bpdm.pool.model.error.RelationReasonCodeNotFound
import org.eclipse.tractusx.bpdm.pool.model.parsed.DataManagementUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.request.DataManagementUpsertRequest
import org.eclipse.tractusx.bpdm.pool.repository.ReasonCodeRepository
import org.eclipse.tractusx.bpdm.pool.repository.RelationRepository
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.LegalEntityBpnParser
import org.eclipse.tractusx.bpdm.pool.util.orFailure
import org.eclipse.tractusx.bpdm.pool.util.parsedOrRecord
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * Validates and resolves one data management relation, in which one legal entity maintains another's data.
 *
 * Data management relations are parsed one at a time rather than as a batch: whether a legal entity already has a
 * manager, and whether management would chain, are questions about the relations already written, so an entry has to
 * be judged against what the entries before it left behind.
 */
@Service
class DataManagementUpsertParser(
    private val legalEntityBpnParser: LegalEntityBpnParser,
    private val reasonCodeRepository: ReasonCodeRepository,
    private val relationRepository: RelationRepository,
    private val relationValidityPeriodsValidator: RelationValidityPeriodsValidator,
    private val governingPartnerStateValidator: GoverningPartnerStateValidator
) {

    /**
     * Resolves the two legal entities the relation names and reports every reason it cannot be written, among them a
     * manager recorded as inactive during a validity period, a manager that is no data space participant, a managed
     * legal entity that already has a manager, and a rewrite of validity that already lies in the past.
     */
    @Transactional(readOnly = true)
    fun parse(request: DataManagementUpsertRequest): ParseResult<DataManagementUpsertParsed, DataManagementUpsertParseError> {
        val errors = mutableListOf<DataManagementUpsertParseError>()

        // Both ends resolve to the same entity type in one operation, which ResolutionParseErrors states its shared
        // errors cannot tell apart, so the roles are named here instead.
        val resolutions = legalEntityBpnParser.parse(listOf(request.managedBpn, request.managerBpn))
        val managed = resolutions[0].parsedOrRecord(errors) { GovernedPartnerNotFound(it.bpn) }
        val manager = resolutions[1].parsedOrRecord(errors) { GoverningPartnerNotFound(it.bpn) }

        if (managed != null && manager != null && managed.bpn == manager.bpn) errors.add(GovernedByItself(request.managedBpn))

        errors += relationValidityPeriodsValidator.validate(request.validityPeriods)
        val reasonCode = request.reasonCode?.let {
            reasonCodeRepository.findByTechnicalKey(it) ?: run { errors.add(RelationReasonCodeNotFound(it)); null }
        }

        if (managed == null || manager == null || errors.isNotEmpty()) return ParseResult.Failure(errors)

        val validityPeriods = request.validityPeriods.map { RelationValidityPeriodDb(validFrom = it.validFrom, validTo = it.validTo) }
        val existingRelation = relationRepository
            .findAll(RelationRepository.byRelation(managed, manager, LegalEntityRelationType.IsManagedBy))
            .singleOrNull()

        errors += governingPartnerStateValidator.validate(manager, validityPeriods)
        if (!manager.isDataSpaceParticipant) errors.add(ManagerNotDataSpaceParticipant(manager.bpn))
        errors += validateSingleManager(managed, validityPeriods, existingRelation)
        errors += validateNoChain(managed, manager, validityPeriods, existingRelation)
        errors += validateNoPastChanges(existingRelation?.validityPeriods ?: emptyList(), validityPeriods)

        return DataManagementUpsertParsed(managed, manager, validityPeriods, reasonCode, existingRelation).orFailure(errors)
    }

    private fun validateSingleManager(
        managed: LegalEntityDb,
        validityPeriods: List<RelationValidityPeriodDb>,
        existingRelation: RelationDb?
    ): List<DataManagementUpsertParseError> {
        val existingManagerBpns = relationRepository.findByTypeAndStartNode(LegalEntityRelationType.IsManagedBy, managed)
            .filterOverlapping(validityPeriods, existingRelation)
            .map { it.endNode.bpn }

        return when {
            existingManagerBpns.isEmpty() -> emptyList()
            else -> listOf(ManagedAlreadyManaged(managed.bpn, existingManagerBpns))
        }
    }

    private fun validateNoChain(
        managed: LegalEntityDb,
        manager: LegalEntityDb,
        validityPeriods: List<RelationValidityPeriodDb>,
        existingRelation: RelationDb?
    ): List<DataManagementUpsertParseError> {
        val managedIsManager = relationRepository.findInSourceOrTarget(LegalEntityRelationType.IsManagedBy, managed)
            .filterOverlapping(validityPeriods, existingRelation)
            .any { it.endNode.bpn == managed.bpn }
        val managerIsManaged = relationRepository.findInSourceOrTarget(LegalEntityRelationType.IsManagedBy, manager)
            .filterOverlapping(validityPeriods, existingRelation)
            .any { it.startNode.bpn == manager.bpn }

        return listOfNotNull(
            ManagedIsManager(managed.bpn).takeIf { managedIsManager },
            ManagerIsManaged(manager.bpn).takeIf { managerIsManaged }
        )
    }

    private fun validateNoPastChanges(
        existingValidityPeriods: Collection<RelationValidityPeriodDb>,
        validityPeriods: Collection<RelationValidityPeriodDb>
    ): List<DataManagementUpsertParseError> {
        val today = LocalDate.now()
        val existingPeriodsByStart = existingValidityPeriods.associateBy { it.validFrom }
        val statedStarts = validityPeriods.map { it.validFrom }.toSet()

        val statedPeriodErrors = validityPeriods
            .filter { it.validFrom < today }
            .mapNotNull { period ->
                val existingPeriod = existingPeriodsByStart[period.validFrom]
                when {
                    existingPeriod == null -> PastValidityPeriodAdded(period.validFrom)
                    endsInPast(period, today) && existingPeriod.validTo != period.validTo -> PastValidityPeriodEndMoved(period.validFrom)
                    else -> null
                }
            }

        val removedPeriodErrors = existingValidityPeriods
            .filter { it.validFrom < today && it.validFrom !in statedStarts }
            .map { PastValidityPeriodRemoved(it.validFrom) }

        return statedPeriodErrors + removedPeriodErrors
    }

    private fun endsInPast(period: RelationValidityPeriodDb, today: LocalDate): Boolean =
        period.validTo?.let { it < today } ?: false
}
