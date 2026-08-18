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
import org.eclipse.tractusx.bpdm.pool.api.model.AddressRelationType
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.*
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.model.ChangelogRecord
import org.eclipse.tractusx.bpdm.pool.repository.AddressRelationEventTriggerRepository
import org.eclipse.tractusx.bpdm.pool.repository.AddressRelationRepository
import org.eclipse.tractusx.bpdm.pool.service.operation.changelog.ChangelogCreateService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * Writes succession between two addresses of one legal entity, in which the source address is replaced by the target address.
 */
@Service
class AddressRelationUpsertService(
    private val addressRelationRepository: AddressRelationRepository,
    private val changelogCreateService: ChangelogCreateService,
    private val headquarterSyncService: HeadquarterSyncService,
    private val addressRelationEventTriggerRepository: AddressRelationEventTriggerRepository
): IAddressRelationUpsertStratergyService {

    /**
     * Persists the succession and rejects it if the two addresses belong to different legal entities, if the predecessor
     * already has a different successor in an overlapping validity period, or if it would close a replacement cycle over
     * overlapping periods. A succession that starts at a legal entity's legal address also relocates that legal entity's
     * headquarters to the successor.
     */
    @Transactional
    override fun upsertRelation(upsertRequest: IAddressRelationUpsertStratergyService.UpsertRequest): UpsertResult<AddressRelationDb> {
        validateNoSelfReference(upsertRequest)
        validateSameLegalEntity(upsertRequest)

        val existingRelation = findExistingRelation(upsertRequest)

        validateSingleSuccessor(upsertRequest, existingRelation)
        validateNoCycles(upsertRequest, existingRelation)

        if (existingRelation == null) {
            val newRelation = createRelation(upsertRequest)
            handleHeadquarterSynchronization(newRelation)

            return UpsertResult(newRelation, UpsertType.Created)
        }

        if (!validityPeriodsDiffer(existingRelation.validityPeriods, upsertRequest.validityPeriods)) {
            return UpsertResult(existingRelation, UpsertType.NoChange)
        }

        existingRelation.validityPeriods.clear()
        existingRelation.validityPeriods.addAll(upsertRequest.validityPeriods)
        addressRelationRepository.saveAndFlush(existingRelation)

        handleHeadquarterSynchronization(existingRelation)

        return UpsertResult(existingRelation, UpsertType.Updated)
    }

    private fun findExistingRelation(upsertRequest: IAddressRelationUpsertStratergyService.UpsertRequest): AddressRelationDb? =
        addressRelationRepository.findAll(
            AddressRelationRepository.byRelation(
                startAddress = upsertRequest.source,
                endAddress = upsertRequest.target,
                type = AddressRelationType.IsReplacedBy
            )
        ).singleOrNull()

    private fun validateNoSelfReference(upsertRequest: IAddressRelationUpsertStratergyService.UpsertRequest) {
        if (upsertRequest.source.bpn == upsertRequest.target.bpn)
            throw BpdmValidationException("An address cannot have a relation to itself (BPNA: ${upsertRequest.source.bpn}).")
    }

    private fun validateSameLegalEntity(upsertRequest: IAddressRelationUpsertStratergyService.UpsertRequest) {
        val source = upsertRequest.source
        val target = upsertRequest.target

        if (source.legalEntity!!.bpn != target.legalEntity!!.bpn) {
            throw BpdmValidationException("Invalid 'IsReplacedBy' relation: The source address with BPNA '${source.bpn}' and target address with BPNA '${target.bpn}' do not belong to the same Legal Entity (BPNL '${source.legalEntity!!.bpn}' and '${target.legalEntity!!.bpn}'). "
                    + "Both addresses must belong to the same Legal Entity to create an 'IsReplacedBy' relation.")
        }
    }

    // Several predecessors may share one successor, so the mirror image of this check is deliberately absent: a merger
    // of multiple addresses into one is a valid succession.
    private fun validateSingleSuccessor(
        upsertRequest: IAddressRelationUpsertStratergyService.UpsertRequest,
        existingRelation: AddressRelationDb?
    ) {
        val predecessor = upsertRequest.source
        val successor = upsertRequest.target

        val existingSuccessions = addressRelationRepository.findByTypeAndStartAddress(AddressRelationType.IsReplacedBy, predecessor)

        filterOverlappingRelations(upsertRequest, existingRelation, existingSuccessions).forEach { succession ->
            if (succession.endAddress.bpn != successor.bpn)
                throw BpdmValidationException(
                    "Multiple successors assigned to the same address: address '${predecessor.bpn}' can't be replaced by " +
                            "'${successor.bpn}' as it is already replaced by '${succession.endAddress.bpn}' in an overlapping validity period."
                )
        }
    }

    private fun validateNoCycles(
        upsertRequest: IAddressRelationUpsertStratergyService.UpsertRequest,
        existingRelation: AddressRelationDb?
    ) {
        val predecessor = upsertRequest.source
        val successor = upsertRequest.target

        if (getAllSuccessors(upsertRequest, existingRelation).contains(predecessor.bpn))
            throw BpdmValidationException(
                "Circular replacement detected: address '${predecessor.bpn}' is (transitively) replacing '${successor.bpn}' " +
                        "and therefore can't be replaced by '${successor.bpn}'."
            )
    }

    private fun getAllSuccessors(
        upsertRequest: IAddressRelationUpsertStratergyService.UpsertRequest,
        existingRelation: AddressRelationDb?
    ): Set<String> {
        val visitedBpns = mutableSetOf<String>()
        val successorProcessingQueue = ArrayDeque<LogisticAddressDb>()

        successorProcessingQueue.addFirst(upsertRequest.target)

        while (successorProcessingQueue.isNotEmpty()) {
            val currentSuccessor = successorProcessingQueue.removeFirst()

            if (!visitedBpns.add(currentSuccessor.bpn))
                continue

            val successionsOfCurrent = addressRelationRepository.findByTypeAndStartAddress(AddressRelationType.IsReplacedBy, currentSuccessor)
            filterOverlappingRelations(upsertRequest, existingRelation, successionsOfCurrent)
                .forEach { successorProcessingQueue.addFirst(it.endAddress) }
        }

        return visitedBpns
    }

    private fun filterOverlappingRelations(
        upsertRequest: IAddressRelationUpsertStratergyService.UpsertRequest,
        existingRelation: AddressRelationDb?,
        relations: Collection<AddressRelationDb>
    ): Collection<AddressRelationDb> =
        relations
            .filterNot { relation -> existingRelation?.id == relation.id }
            .filter { relation -> hasOverlap(upsertRequest.validityPeriods, relation.validityPeriods) }

    private fun hasOverlap(validityPeriods: Collection<RelationValidityPeriodDb>, otherValidityPeriods: Collection<RelationValidityPeriodDb>): Boolean =
        validityPeriods.any { period ->
            otherValidityPeriods.any { otherPeriod ->
                RelationTimePeriod.fromUnlimited(period.validFrom, period.validTo)
                    .hasOverlap(RelationTimePeriod.fromUnlimited(otherPeriod.validFrom, otherPeriod.validTo))
            }
        }

    private fun createRelation(upsertRequest: IAddressRelationUpsertStratergyService.UpsertRequest): AddressRelationDb {
        val source = upsertRequest.source
        val target = upsertRequest.target
        val validityPeriods = upsertRequest.validityPeriods.map {
            RelationValidityPeriodDb(
                validFrom = it.validFrom,
                validTo = it.validTo
            )
        }.toMutableList()

        val newRelation = AddressRelationDb(
            type = AddressRelationType.IsReplacedBy,
            startAddress = source,
            endAddress = target,
            validityPeriods = validityPeriods,
            reasonCode = upsertRequest.reasonCode
        )

        addressRelationRepository.saveAndFlush(newRelation)

        changelogCreateService.record(ChangelogRecord(source.bpn, ChangelogType.UPDATE, BusinessPartnerType.ADDRESS))
        changelogCreateService.record(ChangelogRecord(target.bpn, ChangelogType.UPDATE, BusinessPartnerType.ADDRESS))

        return newRelation
    }

    // Every succession is synchronized and triggered, not only one starting at a legal address: the synchronization walks
    // forward from the legal entity's legal address, so a succession off that chain reaches nothing. Which future-dated
    // succession will be on the chain once it becomes active is not known when it is written.
    private fun handleHeadquarterSynchronization(relation: AddressRelationDb){
        val today = LocalDate.now()

        if(relation.validityPeriods.any { it.validFrom <= today}){
            headquarterSyncService.synchronizeHeadquarter(relation.startAddress.legalEntity!!)
        }

         val existingUnprocessedTriggers = addressRelationEventTriggerRepository.findByRelationAndEventType(relation, TriggerEventType.ReplacedAddress)
            .filterNot { it.isProcessed }

        addressRelationEventTriggerRepository.deleteAll(existingUnprocessedTriggers)


        val futureEventTriggers = relation.validityPeriods
            .filter { it.validFrom > today }
            .map { AddressRelationEventTriggerDb(it.validFrom, false, TriggerEventType.ReplacedAddress, relation) }

        addressRelationEventTriggerRepository.saveAll(futureEventTriggers)
    }

    private fun validityPeriodsDiffer(existingValidityPeriods: Collection<RelationValidityPeriodDb>, newValidityPeriods: Collection<RelationValidityPeriodDb>): Boolean {
        if (existingValidityPeriods.size != newValidityPeriods.size) return true
        return existingValidityPeriods.zip(newValidityPeriods).any { (e, n) ->
            e.validFrom != n.validFrom || e.validTo != n.validTo
        }
    }
}