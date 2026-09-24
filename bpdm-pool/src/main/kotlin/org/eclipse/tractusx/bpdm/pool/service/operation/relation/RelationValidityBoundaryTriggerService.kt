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

package org.eclipse.tractusx.bpdm.pool.service.operation.relation

import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityRelationEventTriggerDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.entity.TriggerEventType
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRelationEventTriggerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * The single authority for scheduling the event triggers that fire on the future dates a relation starts or stops
 * holding.
 */
@Service
class RelationValidityBoundaryTriggerService(
    private val legalEntityRelationEventTriggerRepository: LegalEntityRelationEventTriggerRepository
) {

    /**
     * Schedules a trigger of [eventType] for every future date on which the relation starts or stops holding, and drops
     * the pending ones of that type whose date it no longer has.
     */
    @Transactional
    fun reconcile(relation: RelationDb, eventType: TriggerEventType) {
        val today = LocalDate.now()

        val validFromDates = relation.validityPeriods
            .map { it.validFrom }
            .filter { it > today }

        val expiryDates = relation.validityPeriods
            .mapNotNull { it.validTo }
            .filter { it > today }

        val desiredTriggerDates = (validFromDates + expiryDates).toSet()

        val existingUnprocessedTriggers = legalEntityRelationEventTriggerRepository
            .findByRelationAndEventType(relation, eventType)
            .filterNot { it.isProcessed }
        val existingTriggerDates = existingUnprocessedTriggers.map { it.triggerDate }.toSet()

        // Only the difference is applied, because Hibernate flushes inserts before deletes within a transaction: deleting
        // and reinserting an unchanged trigger date would collide on the (relation_id, event_type, trigger_date) unique
        // constraint.
        val triggersToDelete = existingUnprocessedTriggers.filterNot { it.triggerDate in desiredTriggerDates }
        val triggerDatesToCreate = desiredTriggerDates - existingTriggerDates

        legalEntityRelationEventTriggerRepository.deleteAll(triggersToDelete)
        legalEntityRelationEventTriggerRepository.saveAll(
            triggerDatesToCreate.map { LegalEntityRelationEventTriggerDb(it, false, eventType, relation) }
        )
    }
}
