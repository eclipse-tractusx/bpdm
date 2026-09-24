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

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.AddressRelationType
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.AddressRelationDb
import org.eclipse.tractusx.bpdm.pool.entity.AddressRelationEventTriggerDb
import org.eclipse.tractusx.bpdm.pool.entity.TriggerEventType
import org.eclipse.tractusx.bpdm.pool.model.ChangelogRecord
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressSuccessionParsed
import org.eclipse.tractusx.bpdm.pool.repository.AddressRelationEventTriggerRepository
import org.eclipse.tractusx.bpdm.pool.repository.AddressRelationRepository
import org.eclipse.tractusx.bpdm.pool.service.HeadquarterSyncService
import org.eclipse.tractusx.bpdm.pool.service.operation.changelog.ChangelogCreateService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * The single authority for writing a succession between two addresses of one legal entity.
 *
 * A succession that has already started relocates its legal entity's headquarters at once; one that starts later
 * leaves a trigger behind that does so on the day it starts.
 */
@Service
class AddressSuccessionUpsertService(
    private val addressRelationRepository: AddressRelationRepository,
    private val addressRelationEventTriggerRepository: AddressRelationEventTriggerRepository,
    private val headquarterSyncService: HeadquarterSyncService,
    private val changelogCreateService: ChangelogCreateService
) {

    private val logger = KotlinLogging.logger { }

    /**
     * Persists the succession, relocates the headquarters it moves, and reports whether it was created, whether its
     * validity changed, or whether it already stood as stated.
     */
    @Transactional
    fun upsert(parsed: AddressSuccessionParsed): UpsertResult<AddressRelationDb> =
        write(parsed).also { logOutcome(it.upsertType, parsed) }

    private fun write(parsed: AddressSuccessionParsed): UpsertResult<AddressRelationDb> {
        val existingRelation = parsed.existingRelation

        if (existingRelation == null) {
            val newRelation = create(parsed)
            synchronizeHeadquarter(newRelation)

            return UpsertResult(newRelation, UpsertType.Created)
        }

        if (existingRelation.validityPeriods == listOf(parsed.validityPeriod))
            return UpsertResult(existingRelation, UpsertType.NoChange)

        existingRelation.validityPeriods.clear()
        existingRelation.validityPeriods.add(parsed.validityPeriod)
        addressRelationRepository.saveAndFlush(existingRelation)

        synchronizeHeadquarter(existingRelation)

        return UpsertResult(existingRelation, UpsertType.Updated)
    }

    private fun create(parsed: AddressSuccessionParsed): AddressRelationDb {
        val newRelation = AddressRelationDb(
            type = AddressRelationType.IsReplacedBy,
            startAddress = parsed.predecessor,
            endAddress = parsed.successor,
            validityPeriods = mutableListOf(parsed.validityPeriod),
            reasonCode = parsed.reasonCode
        )

        addressRelationRepository.saveAndFlush(newRelation)

        changelogCreateService.record(ChangelogRecord(parsed.predecessor.bpn, ChangelogType.UPDATE, BusinessPartnerType.ADDRESS))
        changelogCreateService.record(ChangelogRecord(parsed.successor.bpn, ChangelogType.UPDATE, BusinessPartnerType.ADDRESS))

        return newRelation
    }

    // Every succession is synchronized and triggered, not only one starting at a legal address: the synchronization walks
    // forward from the legal entity's legal address, so a succession off that chain reaches nothing. Which future-dated
    // succession will be on the chain once it becomes active is not known when it is written.
    private fun synchronizeHeadquarter(relation: AddressRelationDb) {
        val today = LocalDate.now()

        if (relation.validityPeriods.any { it.validFrom <= today })
            headquarterSyncService.synchronizeHeadquarter(relation.startAddress.legalEntity!!)

        val unprocessedTriggers = addressRelationEventTriggerRepository
            .findByRelationAndEventType(relation, TriggerEventType.ReplacedAddress)
            .filterNot { it.isProcessed }

        addressRelationEventTriggerRepository.deleteAll(unprocessedTriggers)

        val futureTriggers = relation.validityPeriods
            .filter { it.validFrom > today }
            .map { AddressRelationEventTriggerDb(it.validFrom, false, TriggerEventType.ReplacedAddress, relation) }

        addressRelationEventTriggerRepository.saveAll(futureTriggers)
    }

    private fun logOutcome(upsertType: UpsertType, parsed: AddressSuccessionParsed) {
        val succession = "address succession from '${parsed.predecessor.bpn}' to '${parsed.successor.bpn}'"
        when (upsertType) {
            UpsertType.Created -> logger.info { "Created $succession" }
            UpsertType.Updated -> logger.info { "Updated validity of $succession" }
            UpsertType.NoChange -> logger.debug { "Left $succession unchanged" }
        }
    }
}
