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

package org.eclipse.tractusx.bpdm.pool.service

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.api.model.RelationType
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationValidityPeriodDb
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.repository.RelationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class RelationUpsertService(
    private val relationRepository: RelationRepository,
    private val changelogService: PartnerChangelogService
) {
    @Transactional
    fun upsertRelation(upsertRequest: UpsertRequest): UpsertResult<RelationDb>{
        val source = upsertRequest.source
        val target = upsertRequest.target
        val relationType = upsertRequest.relationType

        // Prevent self-referencing relations
        if (source == target) {
            throw BpdmValidationException("A legal entity cannot have a relation to itself (BPNL: ${source.bpn}).")
        }

        val existingRelation = relationRepository.findAll(
            RelationRepository.byRelation(
                startNode = source,
                endNode = target,
                type = relationType
            )
        ).singleOrNull()

        val upsertResult = if (existingRelation != null) {
            // Update validity periods if changed
            if (validityPeriodsDiffer(existingRelation.validityPeriods, upsertRequest.validityPeriods)) {
                existingRelation.validityPeriods.clear()
                existingRelation.validityPeriods.addAll(upsertRequest.validityPeriods)
                relationRepository.save(existingRelation)
                UpsertResult(existingRelation, UpsertType.Updated)
            } else {
                UpsertResult(existingRelation, UpsertType.NoChange)
            }
        } else {
            UpsertResult(createNewRelation(upsertRequest), UpsertType.Created)
        }

        return upsertResult
    }

    private fun createNewRelation(upsertRequest: UpsertRequest): RelationDb{
        val source = upsertRequest.source
        val target = upsertRequest.target
        val validityPeriods = upsertRequest.validityPeriods.map {
            RelationValidityPeriodDb(
                validFrom = it.validFrom,
                validTo = it.validTo
            )
        }.toMutableList()

        val newRelation = RelationDb(
            type = upsertRequest.relationType,
            startNode = source,
            endNode = target,
            validityPeriods = validityPeriods,
        )

        relationRepository.save(newRelation)

        changelogService.createChangelogEntry(ChangelogEntryCreateRequest(source.bpn, ChangelogType.UPDATE, BusinessPartnerType.LEGAL_ENTITY))
        changelogService.createChangelogEntry(ChangelogEntryCreateRequest(target.bpn, ChangelogType.UPDATE, BusinessPartnerType.LEGAL_ENTITY))

        return newRelation
    }

    private fun validityPeriodsDiffer(existingValidityPeriods: Collection<RelationValidityPeriodDb>, newValidityPeriods: Collection<RelationValidityPeriodDb>): Boolean {
        if (existingValidityPeriods.size != newValidityPeriods.size) return true
        return existingValidityPeriods.zip(newValidityPeriods).any { (e, n) ->
            e.validFrom != n.validFrom || e.validTo != n.validTo
        }
    }

    fun filterOverlappingRelations(relationToUpsert: IRelationUpsertStrategyService.UpsertRequest, relations: Collection<RelationDb>): Collection<RelationDb>{
        val relationsWithoutSelf = relations.filterNot { isTheSameRelation(relationToUpsert, it) }
        val overlappingRelations = relationsWithoutSelf.filter { hasOverlap(relationToUpsert, it) }

        return overlappingRelations
    }


    private fun isTheSameRelation(relationToUpsert: IRelationUpsertStrategyService.UpsertRequest, relation: RelationDb): Boolean{
        return relationToUpsert.source.bpn == relation.startNode.bpn && relationToUpsert.target.bpn == relation.endNode.bpn
    }

    private fun hasOverlap(relationToUpsert: IRelationUpsertStrategyService.UpsertRequest, relation: RelationDb): Boolean{
        return relationToUpsert.validityPeriods.any{ validity1 -> relation.validityPeriods.any { validity2 -> hasOverlap(validity1, validity2) } }

    }

    private fun hasOverlap(validity1: RelationValidityPeriodDb, validity2: RelationValidityPeriodDb): Boolean {
        return TimePeriod.fromUnlimited(validity1.validFrom, validity1.validTo)
            .hasOverlap(TimePeriod.fromUnlimited(validity2.validFrom, validity2.validTo))
    }

    data class UpsertRequest(
        val source: LegalEntityDb,
        val target: LegalEntityDb,
        val relationType: RelationType,
        val validityPeriods: Collection<RelationValidityPeriodDb>
    )

    data class TimePeriod(
        val validFrom: LocalDate,
        val validTo: LocalDate
    ){
        companion object{
            private val maxValidTo = LocalDate.parse("9999-01-01")

            fun fromUnlimited(validFrom: LocalDate, validTo: LocalDate?): TimePeriod{
                return TimePeriod(validFrom, validTo ?: maxValidTo)
            }
        }

        fun hasOverlap(other: TimePeriod): Boolean{
            return validFrom < other.validTo && validTo > other.validFrom
        }
    }
}