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
import org.eclipse.tractusx.bpdm.common.model.DbTimestamp
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityRelationEventTriggerDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.entity.TriggerEventType
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRelationEventTriggerRepository
import org.eclipse.tractusx.bpdm.pool.service.operation.legalentity.UltimateOwnerRecalculationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * Schedules and executes the triggers that recalculate ultimate owners once a legal entity relation's validity period
 * starts or ends.
 */
@Service
class LegalEntityRelationTriggerService(
    private val triggerRepository: LegalEntityRelationEventTriggerRepository,
    private val ultimateOwnerRecalculationService: UltimateOwnerRecalculationService
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Brings the pending [eventType] triggers of [relation] in line with its validity periods: one for each start and for
     * each day after an end that lies after today.
     */
    @Transactional
    fun reschedule(relation: RelationDb, eventType: TriggerEventType) {
        val today = LocalDate.now()
        val startDates = relation.validityPeriods.map { it.validFrom }.filter { it > today }
        val expiryDates = relation.validityPeriods.mapNotNull { it.validTo }.map { it.plusDays(1) }.filter { it > today }
        val desiredTriggerDates = (startDates + expiryDates).toSet()

        val pendingTriggers = triggerRepository.findByRelationAndEventType(relation, eventType).filterNot { it.isProcessed }
        val scheduledTriggerDates = pendingTriggers.map { it.triggerDate }.toSet()

        // Only the difference is applied: Hibernate flushes inserts before deletes, so deleting and reinserting a trigger with an
        // unchanged date would collide on the (relation_id, event_type, trigger_date) unique constraint.
        triggerRepository.deleteAll(pendingTriggers.filterNot { it.triggerDate in desiredTriggerDates })
        triggerRepository.saveAll((desiredTriggerDates - scheduledTriggerDates).map { LegalEntityRelationEventTriggerDb(it, false, eventType, relation) })
    }

    /**
     * Returns the earliest pending trigger due on or before [date], or null if none is due.
     */
    fun findNextDue(date: LocalDate): LegalEntityRelationEventTriggerDb? =
        triggerRepository.findNextUnprocessed(date)

    /**
     * Recalculates the ultimate owners of the source legal entity of the relation [trigger] belongs to and of the legal
     * entities below it, and marks the trigger processed.
     */
    @Transactional
    fun execute(trigger: LegalEntityRelationEventTriggerDb) {
        when (trigger.eventType) {
            TriggerEventType.OwnershipValidityBoundary,
            TriggerEventType.AlternativeHeadquarterValidityBoundary ->
                ultimateOwnerRecalculationService.recalculate(listOf(trigger.relation.startNode))

            TriggerEventType.ReplacedAddress ->
                logger.error { "Encountered a ${trigger.eventType} trigger on legal entity relation ${trigger.relation.id}, which only address relations carry. Trigger will be deactivated." }
        }

        trigger.isProcessed = true
        trigger.updatedAt = DbTimestamp.now()
        triggerRepository.save(trigger)
        logger.debug { "Executed ${trigger.eventType} trigger of ${trigger.relation.type} relation from legal entity '${trigger.relation.startNode.bpn}' to '${trigger.relation.endNode.bpn}' due ${trigger.triggerDate}" }
    }
}
