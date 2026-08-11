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
import org.eclipse.tractusx.bpdm.pool.api.model.SiteRelationType
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.ReasonCodeDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationTimePeriod
import org.eclipse.tractusx.bpdm.pool.entity.RelationValidityPeriodDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteRelationDb
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.repository.SiteRelationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Writes succession between two sites of one legal entity, in which the source site is replaced by the target site.
 */
@Service
class SiteRelationUpsertService(
    private val siteRelationRepository: SiteRelationRepository,
    private val changelogService: PartnerChangelogService
) {

    /**
     * Persists the succession and rejects it if the two sites belong to different legal entities, if the predecessor
     * already has a different successor in an overlapping validity period, or if it would close a replacement cycle
     * over overlapping periods.
     */
    @Transactional
    fun upsertRelation(upsertRequest: UpsertRequest): UpsertResult<SiteRelationDb> {
        validateNoSelfReference(upsertRequest)
        validateSameLegalEntity(upsertRequest)
        validateSingleSuccessor(upsertRequest)
        validateNoCycles(upsertRequest)

        val existingRelation = upsertRequest.existingRelation
            ?: return UpsertResult(createRelation(upsertRequest), UpsertType.Created)

        if (!validityPeriodsDiffer(existingRelation.validityPeriods, upsertRequest.validityPeriods)) {
            return UpsertResult(existingRelation, UpsertType.NoChange)
        }

        existingRelation.validityPeriods.clear()
        existingRelation.validityPeriods.addAll(upsertRequest.validityPeriods)
        siteRelationRepository.save(existingRelation)

        return UpsertResult(existingRelation, UpsertType.Updated)
    }

    private fun validateNoSelfReference(upsertRequest: UpsertRequest) {
        if (upsertRequest.source.bpn == upsertRequest.target.bpn)
            throw BpdmValidationException("A site cannot have a relation to itself (BPNS: ${upsertRequest.source.bpn}).")
    }

    private fun validateSameLegalEntity(upsertRequest: UpsertRequest) {
        val predecessor = upsertRequest.source
        val successor = upsertRequest.target

        if (predecessor.legalEntity.bpn != successor.legalEntity.bpn)
            throw BpdmValidationException(
                "Invalid 'IsReplacedBy' relation: The source site with BPNS '${predecessor.bpn}' and target site with BPNS " +
                        "'${successor.bpn}' do not belong to the same Legal Entity (BPNL '${predecessor.legalEntity.bpn}' and " +
                        "'${successor.legalEntity.bpn}'). Both sites must belong to the same Legal Entity to create an " +
                        "'IsReplacedBy' relation."
            )
    }

    // Several predecessors may share one successor, so the mirror image of this check is deliberately absent: a merger
    // of multiple sites into one is a valid succession.
    private fun validateSingleSuccessor(upsertRequest: UpsertRequest) {
        val predecessor = upsertRequest.source
        val successor = upsertRequest.target

        val existingSuccessions = siteRelationRepository.findByTypeAndStartSite(SiteRelationType.IsReplacedBy, predecessor)

        filterOverlappingRelations(upsertRequest, existingSuccessions).forEach { succession ->
            if (succession.endSite.bpn != successor.bpn)
                throw BpdmValidationException(
                    "Multiple successors assigned to the same site: site '${predecessor.bpn}' can't be replaced by " +
                            "'${successor.bpn}' as it is already replaced by '${succession.endSite.bpn}' in an overlapping validity period."
                )
        }
    }

    private fun validateNoCycles(upsertRequest: UpsertRequest) {
        val predecessor = upsertRequest.source
        val successor = upsertRequest.target

        if (getAllSuccessors(upsertRequest).contains(predecessor.bpn))
            throw BpdmValidationException(
                "Circular replacement detected: site '${predecessor.bpn}' is (transitively) replacing '${successor.bpn}' " +
                        "and therefore can't be replaced by '${successor.bpn}'."
            )
    }

    private fun getAllSuccessors(upsertRequest: UpsertRequest): Set<String> {
        val visitedBpns = mutableSetOf<String>()
        val successorProcessingQueue = ArrayDeque<SiteDb>()

        successorProcessingQueue.addFirst(upsertRequest.target)

        while (successorProcessingQueue.isNotEmpty()) {
            val currentSuccessor = successorProcessingQueue.removeFirst()

            if (!visitedBpns.add(currentSuccessor.bpn))
                continue

            val successionsOfCurrent = siteRelationRepository.findByTypeAndStartSite(SiteRelationType.IsReplacedBy, currentSuccessor)
            filterOverlappingRelations(upsertRequest, successionsOfCurrent)
                .forEach { successorProcessingQueue.addFirst(it.endSite) }
        }

        return visitedBpns
    }

    private fun createRelation(upsertRequest: UpsertRequest): SiteRelationDb {
        val source = upsertRequest.source
        val target = upsertRequest.target

        val newRelation = SiteRelationDb(
            type = SiteRelationType.IsReplacedBy,
            startSite = source,
            endSite = target,
            validityPeriods = upsertRequest.validityPeriods.map { RelationValidityPeriodDb(it.validFrom, it.validTo) }.toMutableList(),
            reasonCode = upsertRequest.reasonCode
        )

        siteRelationRepository.save(newRelation)

        changelogService.createChangelogEntry(ChangelogEntryCreateRequest(source.bpn, ChangelogType.UPDATE, BusinessPartnerType.SITE))
        changelogService.createChangelogEntry(ChangelogEntryCreateRequest(target.bpn, ChangelogType.UPDATE, BusinessPartnerType.SITE))

        return newRelation
    }

    private fun filterOverlappingRelations(upsertRequest: UpsertRequest, relations: Collection<SiteRelationDb>): Collection<SiteRelationDb> =
        relations
            .filterNot { relation -> upsertRequest.existingRelation?.id == relation.id }
            .filter { relation -> hasOverlap(upsertRequest.validityPeriods, relation.validityPeriods) }

    private fun hasOverlap(validityPeriods: Collection<RelationValidityPeriodDb>, otherValidityPeriods: Collection<RelationValidityPeriodDb>): Boolean =
        validityPeriods.any { period ->
            otherValidityPeriods.any { otherPeriod ->
                RelationTimePeriod.fromUnlimited(period.validFrom, period.validTo)
                    .hasOverlap(RelationTimePeriod.fromUnlimited(otherPeriod.validFrom, otherPeriod.validTo))
            }
        }

    private fun validityPeriodsDiffer(
        existingValidityPeriods: Collection<RelationValidityPeriodDb>,
        newValidityPeriods: Collection<RelationValidityPeriodDb>
    ): Boolean {
        if (existingValidityPeriods.size != newValidityPeriods.size) return true
        return existingValidityPeriods.zip(newValidityPeriods).any { (existing, new) ->
            existing.validFrom != new.validFrom || existing.validTo != new.validTo
        }
    }

    data class UpsertRequest(
        val source: SiteDb,
        val target: SiteDb,
        val validityPeriods: Collection<RelationValidityPeriodDb>,
        val existingRelation: SiteRelationDb?,
        val reasonCode: ReasonCodeDb?
    )
}
