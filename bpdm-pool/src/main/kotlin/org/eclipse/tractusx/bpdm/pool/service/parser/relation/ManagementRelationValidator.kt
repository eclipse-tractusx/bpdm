/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationTimePeriod
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.RelationUpsertError
import org.eclipse.tractusx.bpdm.pool.model.raw.relation.RelationValidityPeriodRaw
import org.eclipse.tractusx.bpdm.pool.repository.RelationRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * Checks that a management relation has a data space participant as manager, gives the managed legal entity a single
 * manager without forming a chain, and leaves the past unchanged.
 */
@Service
class ManagementRelationValidator(
    private val relationRepository: RelationRepository
) {

    /**
     * Reports why [managed] cannot be managed by [manager] during [validityPeriods], where [existingRelation] is the
     * relation being rewritten.
     */
    fun validate(
        managed: LegalEntityDb,
        manager: LegalEntityDb,
        validityPeriods: List<RelationValidityPeriodRaw>,
        existingRelation: RelationDb?
    ): List<RelationUpsertError> {
        val relationsDuringPeriods = { relations: Collection<RelationDb> ->
            overlappingRelations(relations, existingRelation, validityPeriods) { it.validityPeriods }
        }

        return listOfNotNull(
            RelationUpsertError.ManagerNotParticipant(manager.bpn).takeIf { !manager.isDataSpaceParticipant },
            multipleManagers(managed, relationsDuringPeriods),
            managementChain(managed, manager, relationsDuringPeriods),
            historicChange(existingRelation, validityPeriods)
        )
    }

    private fun multipleManagers(
        managed: LegalEntityDb,
        relationsDuringPeriods: (Collection<RelationDb>) -> List<RelationDb>
    ): RelationUpsertError? =
        relationsDuringPeriods(relationRepository.findByTypeAndStartNode(LegalEntityRelationTypeV7.IsManagedBy, managed))
            .takeIf { it.isNotEmpty() }
            ?.let { managements -> RelationUpsertError.MultipleManagers(managed.bpn, managements.map { it.endNode.bpn }) }

    private fun managementChain(
        managed: LegalEntityDb,
        manager: LegalEntityDb,
        relationsDuringPeriods: (Collection<RelationDb>) -> List<RelationDb>
    ): RelationUpsertError? {
        val managedRelations = relationsDuringPeriods(relationRepository.findInSourceOrTarget(LegalEntityRelationTypeV7.IsManagedBy, managed))
        val managerRelations = relationsDuringPeriods(relationRepository.findInSourceOrTarget(LegalEntityRelationTypeV7.IsManagedBy, manager))

        return when {
            managedRelations.any { it.endNode.bpn == managed.bpn } -> RelationUpsertError.ManagedIsManager(managed.bpn)
            managerRelations.any { it.startNode.bpn == manager.bpn } -> RelationUpsertError.ManagerIsManaged(manager.bpn)
            else -> null
        }
    }

    private fun historicChange(existingRelation: RelationDb?, validityPeriods: List<RelationValidityPeriodRaw>): RelationUpsertError? {
        val today = LocalDate.now()
        val existingTimePeriods = existingRelation?.validityPeriods.orEmpty().map { RelationTimePeriod.fromUnlimited(it.validFrom, it.validTo) }
        val newTimePeriods = validityPeriods.map { RelationTimePeriod.fromUnlimited(it.validFrom, it.validTo) }

        val changedPastPeriod = newTimePeriods
            .filter { it.validFrom < today }
            .firstNotNullOfOrNull { newTimePeriod ->
                val matchingExistingTimePeriod = existingTimePeriods.find { it.validFrom == newTimePeriod.validFrom }
                when {
                    matchingExistingTimePeriod == null -> RelationUpsertError.PastValidityPeriodAdded(newTimePeriod.validFrom)
                    newTimePeriod.validTo < today && matchingExistingTimePeriod.validTo != newTimePeriod.validTo ->
                        RelationUpsertError.PastValidityPeriodLimited(matchingExistingTimePeriod.validFrom)
                    else -> null
                }
            }
        if (changedPastPeriod != null) return changedPastPeriod

        return existingTimePeriods
            .filter { it.validFrom < today }
            .firstOrNull { existingTimePeriod -> newTimePeriods.none { it.validFrom == existingTimePeriod.validFrom } }
            ?.let { RelationUpsertError.PastValidityPeriodDeleted(it.validFrom) }
    }
}
