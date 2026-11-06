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

package org.eclipse.tractusx.bpdm.pool.service.operation.relation

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.model.DbTimestamp
import org.eclipse.tractusx.bpdm.pool.api.v7.model.changelog.ChangelogTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.model.parsed.businesspartner.UpsertResultParsed
import org.eclipse.tractusx.bpdm.pool.model.raw.businesspartner.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationValidityPeriodDb
import org.eclipse.tractusx.bpdm.pool.entity.TriggerEventType
import org.eclipse.tractusx.bpdm.pool.model.raw.changelog.ChangelogEntryRaw
import org.eclipse.tractusx.bpdm.pool.model.parsed.participation.DataSpaceParticipantUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.relation.LegalEntityRelationUpsertParsed
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.RelationRepository
import org.eclipse.tractusx.bpdm.pool.service.operation.changelog.ChangelogCreateService
import org.eclipse.tractusx.bpdm.pool.service.operation.legalentity.UltimateOwnerRecalculationService
import org.eclipse.tractusx.bpdm.pool.service.operation.participation.DataSpaceParticipantUpdateService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Writes relations between two legal entities and keeps the state that depends on them current.
 */
@Service
class LegalEntityRelationUpsertService(
    private val relationRepository: RelationRepository,
    private val legalEntityRepository: LegalEntityRepository,
    private val changelogCreateService: ChangelogCreateService,
    private val ultimateOwnerRecalculationService: UltimateOwnerRecalculationService,
    private val legalEntityRelationTriggerService: LegalEntityRelationTriggerService,
    private val dataSpaceParticipantUpdateService: DataSpaceParticipantUpdateService
) {

    private val logger = KotlinLogging.logger { }

    /**
     * Persists [relation] and returns it as stored; an ownership or alternative headquarter relation also recalculates
     * the ultimate owners and reschedules its validity boundary triggers, a management relation makes the managed legal
     * entity a data space participant.
     */
    @Transactional
    fun upsert(relation: LegalEntityRelationUpsertParsed): RelationDb {
        val upsertResult = write(relation)
        val written = upsertResult.value

        if (upsertResult.upsertType != UpsertType.NoChange) {
            val updatedAt = DbTimestamp.now()
            relation.source.updatedAt = updatedAt
            relation.target.updatedAt = updatedAt
            legalEntityRepository.saveAll(listOf(relation.source, relation.target))
        }
        logOutcome(upsertResult.upsertType, relation)

        when (relation.type) {
            LegalEntityRelationTypeV7.IsOwnedBy -> {
                ultimateOwnerRecalculationService.recalculate(listOf(relation.source))
                legalEntityRelationTriggerService.reschedule(written, TriggerEventType.OwnershipValidityBoundary)
            }
            LegalEntityRelationTypeV7.IsAlternativeHeadquarterFor -> {
                ultimateOwnerRecalculationService.recalculate(listOf(relation.source))
                legalEntityRelationTriggerService.reschedule(written, TriggerEventType.AlternativeHeadquarterValidityBoundary)
            }
            LegalEntityRelationTypeV7.IsManagedBy -> makeParticipant(relation.source)
            LegalEntityRelationTypeV7.IsReplacedBy -> Unit
        }

        return written
    }

    private fun write(relation: LegalEntityRelationUpsertParsed): UpsertResultParsed<RelationDb> {
        val validityPeriods = relation.validityPeriods.map { RelationValidityPeriodDb(validFrom = it.validFrom, validTo = it.validTo) }
        val existingRelation = relation.existingRelation
            ?: return UpsertResultParsed(create(relation, validityPeriods), UpsertType.Created)

        if (!validityPeriodsDiffer(existingRelation.validityPeriods, validityPeriods)) {
            return UpsertResultParsed(existingRelation, UpsertType.NoChange)
        }

        existingRelation.validityPeriods.clear()
        existingRelation.validityPeriods.addAll(validityPeriods)
        existingRelation.updatedAt = DbTimestamp.now()
        relationRepository.save(existingRelation)

        return UpsertResultParsed(existingRelation, UpsertType.Updated)
    }

    private fun logOutcome(upsertType: UpsertType, relation: LegalEntityRelationUpsertParsed) {
        val description = "${relation.type} relation from legal entity '${relation.source.bpn}' to '${relation.target.bpn}'"
        when (upsertType) {
            UpsertType.Created -> logger.info { "Created $description" }
            UpsertType.Updated -> logger.info { "Updated validity periods of $description" }
            UpsertType.NoChange -> logger.debug { "Left $description unchanged" }
        }
    }

    private fun create(relation: LegalEntityRelationUpsertParsed, validityPeriods: List<RelationValidityPeriodDb>): RelationDb {
        val newRelation = RelationDb(
            type = relation.type,
            startNode = relation.source,
            endNode = relation.target,
            validityPeriods = validityPeriods.toMutableList(),
            reasonCode = relation.reasonCode
        )
        relationRepository.save(newRelation)

        changelogCreateService.record(ChangelogEntryRaw(relation.source.bpn, ChangelogTypeV7.UPDATE, BusinessPartnerType.LEGAL_ENTITY))
        changelogCreateService.record(ChangelogEntryRaw(relation.target.bpn, ChangelogTypeV7.UPDATE, BusinessPartnerType.LEGAL_ENTITY))

        return newRelation
    }

    private fun makeParticipant(legalEntity: LegalEntityDb) {
        if (!legalEntity.isDataSpaceParticipant)
            dataSpaceParticipantUpdateService.update(listOf(DataSpaceParticipantUpdateParsed(legalEntity, isDataSpaceParticipant = true)))
    }
}
