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

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.entity.AddressRelationEventTriggerDb
import org.eclipse.tractusx.bpdm.pool.entity.TriggerEventType
import org.eclipse.tractusx.bpdm.pool.repository.AddressRelationEventTriggerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class AddressRelationTriggerExecutionService(
    private val headquarterSyncService: HeadquarterSyncService,
    private val addressRelationEventTriggerRepository: AddressRelationEventTriggerRepository
): IsBatchProcessService {
    private val logger = KotlinLogging.logger { }

    @Transactional
    override fun executeNextBatch(): Boolean {
        val nextUnprocessedTrigger = addressRelationEventTriggerRepository.findNextUnprocessed(LocalDate.now())
            ?: return false

        when (nextUnprocessedTrigger.eventType) {
            TriggerEventType.ReplacedAddress -> executeHeadquarterRelocationSync(nextUnprocessedTrigger)
        }

        nextUnprocessedTrigger.isProcessed = true
        addressRelationEventTriggerRepository.save(nextUnprocessedTrigger)

        return true
    }

    private fun executeHeadquarterRelocationSync(trigger: AddressRelationEventTriggerDb) {
        val legalEntity = trigger.relation.startAddress.legalEntity

        if(legalEntity != null){
            headquarterSyncService.synchronizeHeadquarter(legalEntity)
        }else{
            logger.error { "Encountered an address without reference to a legal entity when processing replaced by triggers: ${trigger.relation.startAddress}. Trigger will be deactivated." }
        }

    }
}