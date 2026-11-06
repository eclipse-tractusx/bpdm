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

import org.eclipse.tractusx.bpdm.pool.model.raw.changelog.ChangelogType
import org.eclipse.tractusx.bpdm.pool.model.raw.relation.AddressRelationType
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.model.DbTimestamp
import org.eclipse.tractusx.bpdm.pool.model.parsed.businesspartner.UpsertResultParsed
import org.eclipse.tractusx.bpdm.pool.model.raw.businesspartner.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.AddressRelationDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationValidityPeriodDb
import org.eclipse.tractusx.bpdm.pool.model.raw.changelog.ChangelogEntryRaw
import org.eclipse.tractusx.bpdm.pool.model.parsed.relation.AddressRelationUpsertParsed
import org.eclipse.tractusx.bpdm.pool.repository.AddressRelationRepository
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.eclipse.tractusx.bpdm.pool.service.operation.changelog.ChangelogCreateService
import org.eclipse.tractusx.bpdm.pool.service.operation.legalentity.HeadquarterSyncService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * Writes succession between two addresses of one legal entity, in which the source address is replaced by the target address.
 */
@Service
class AddressRelationUpsertService(
    private val addressRelationRepository: AddressRelationRepository,
    private val addressRepository: LogisticAddressRepository,
    private val changelogCreateService: ChangelogCreateService,
    private val headquarterSyncService: HeadquarterSyncService,
    private val addressRelationTriggerService: AddressRelationTriggerService
) {

    private val logger = KotlinLogging.logger { }

    /**
     * Persists [relation] and returns the succession as stored; a succession that starts at a legal entity's legal address
     * also relocates that legal entity's headquarters to the successor.
     */
    @Transactional
    fun upsert(relation: AddressRelationUpsertParsed): AddressRelationDb {
        val upsertResult = write(relation)

        if (upsertResult.upsertType != UpsertType.NoChange) {
            val updatedAt = DbTimestamp.now()
            relation.source.updatedAt = updatedAt
            relation.target.updatedAt = updatedAt
            addressRepository.saveAll(listOf(relation.source, relation.target))
        }
        logOutcome(upsertResult.upsertType, relation)

        return upsertResult.value
    }

    private fun write(relation: AddressRelationUpsertParsed): UpsertResultParsed<AddressRelationDb> {
        val validityPeriods = relation.validityPeriods.map { RelationValidityPeriodDb(validFrom = it.validFrom, validTo = it.validTo) }
        val existingRelation = relation.existingRelation

        if (existingRelation == null) {
            val newRelation = create(relation, validityPeriods)
            handleHeadquarterSynchronization(newRelation)

            return UpsertResultParsed(newRelation, UpsertType.Created)
        }

        if (!validityPeriodsDiffer(existingRelation.validityPeriods, validityPeriods)) {
            return UpsertResultParsed(existingRelation, UpsertType.NoChange)
        }

        existingRelation.validityPeriods.clear()
        existingRelation.validityPeriods.addAll(validityPeriods)
        existingRelation.updatedAt = DbTimestamp.now()
        addressRelationRepository.saveAndFlush(existingRelation)

        handleHeadquarterSynchronization(existingRelation)

        return UpsertResultParsed(existingRelation, UpsertType.Updated)
    }

    private fun logOutcome(upsertType: UpsertType, relation: AddressRelationUpsertParsed) {
        val succession = "address succession from '${relation.source.bpn}' to '${relation.target.bpn}'"
        when (upsertType) {
            UpsertType.Created -> logger.info { "Created $succession" }
            UpsertType.Updated -> logger.info { "Updated validity periods of $succession" }
            UpsertType.NoChange -> logger.debug { "Left $succession unchanged" }
        }
    }

    private fun create(relation: AddressRelationUpsertParsed, validityPeriods: List<RelationValidityPeriodDb>): AddressRelationDb {
        val newRelation = AddressRelationDb(
            type = AddressRelationType.IsReplacedBy,
            startAddress = relation.source,
            endAddress = relation.target,
            validityPeriods = validityPeriods.toMutableList(),
            reasonCode = relation.reasonCode
        )
        addressRelationRepository.saveAndFlush(newRelation)

        changelogCreateService.record(ChangelogEntryRaw(relation.source.bpn, ChangelogType.UPDATE, BusinessPartnerType.ADDRESS))
        changelogCreateService.record(ChangelogEntryRaw(relation.target.bpn, ChangelogType.UPDATE, BusinessPartnerType.ADDRESS))

        return newRelation
    }

    // Every succession is synchronized and triggered, not only one starting at a legal address: the synchronization walks
    // forward from the legal entity's legal address, so a succession off that chain reaches nothing. Which future-dated
    // succession will be on the chain once it becomes active is not known when it is written.
    private fun handleHeadquarterSynchronization(relation: AddressRelationDb) {
        val today = LocalDate.now()

        if (relation.validityPeriods.any { it.validFrom <= today }) {
            headquarterSyncService.synchronizeHeadquarter(relation.startAddress.legalEntity!!)
        }

        addressRelationTriggerService.reschedule(relation)
    }
}
