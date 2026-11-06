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
import org.eclipse.tractusx.bpdm.pool.entity.AddressRelationDb
import org.eclipse.tractusx.bpdm.pool.entity.AddressRelationEventTriggerDb
import org.eclipse.tractusx.bpdm.pool.entity.TriggerEventType
import org.eclipse.tractusx.bpdm.pool.repository.AddressRelationEventTriggerRepository
import org.eclipse.tractusx.bpdm.pool.service.operation.legalentity.HeadquarterSyncService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * Schedules and executes the triggers that relocate a legal entity's headquarters once an address succession starts.
 */
@Service
class AddressRelationTriggerService(
    private val triggerRepository: AddressRelationEventTriggerRepository,
    private val headquarterSyncService: HeadquarterSyncService
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Replaces the pending triggers of [relation] with one for each validity period that starts after today.
     */
    @Transactional
    fun reschedule(relation: AddressRelationDb) {
        val today = LocalDate.now()

        val pendingTriggers = triggerRepository.findByRelationAndEventType(relation, TriggerEventType.ReplacedAddress)
            .filterNot { it.isProcessed }
        triggerRepository.deleteAll(pendingTriggers)

        val futureTriggers = relation.validityPeriods
            .filter { it.validFrom > today }
            .map { AddressRelationEventTriggerDb(it.validFrom, false, TriggerEventType.ReplacedAddress, relation) }
        triggerRepository.saveAll(futureTriggers)
    }

    /**
     * Returns the earliest pending trigger due on or before [date], or null if none is due.
     */
    fun findNextDue(date: LocalDate): AddressRelationEventTriggerDb? =
        triggerRepository.findNextUnprocessed(date)

    /**
     * Relocates the headquarters of the legal entity whose address succession [trigger] belongs to, and marks the trigger
     * processed.
     */
    @Transactional
    fun execute(trigger: AddressRelationEventTriggerDb) {
        when (trigger.eventType) {
            TriggerEventType.ReplacedAddress -> synchronizeHeadquarter(trigger)
            TriggerEventType.OwnershipValidityBoundary,
            TriggerEventType.AlternativeHeadquarterValidityBoundary ->
                logger.error { "Encountered a ${trigger.eventType} trigger on address relation ${trigger.relation.id}, which only legal entity relations carry. Trigger will be deactivated." }
        }

        trigger.isProcessed = true
        trigger.updatedAt = DbTimestamp.now()
        triggerRepository.save(trigger)
        logger.debug { "Executed ${trigger.eventType} trigger of address succession from '${trigger.relation.startAddress.bpn}' to '${trigger.relation.endAddress.bpn}' due ${trigger.triggerDate}" }
    }

    private fun synchronizeHeadquarter(trigger: AddressRelationEventTriggerDb) {
        val legalEntity = trigger.relation.startAddress.legalEntity

        if (legalEntity != null) {
            headquarterSyncService.synchronizeHeadquarter(legalEntity)
        } else {
            logger.error { "Encountered an address without reference to a legal entity when processing replaced by triggers: ${trigger.relation.startAddress.bpn}. Trigger will be deactivated." }
        }
    }
}
