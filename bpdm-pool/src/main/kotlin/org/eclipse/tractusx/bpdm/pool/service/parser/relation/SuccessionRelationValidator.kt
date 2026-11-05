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

package org.eclipse.tractusx.bpdm.pool.service.parser.relation

import org.eclipse.tractusx.bpdm.common.model.BaseEntityWithExplicitTimestamps
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.entity.AddressRelationDb
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationValidityPeriodDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteRelationDb
import org.eclipse.tractusx.bpdm.pool.model.parsed.relation.RelationSubject
import org.eclipse.tractusx.bpdm.pool.model.parsed.relation.RelationUpsertError
import org.eclipse.tractusx.bpdm.pool.model.raw.relation.RelationValidityPeriodRaw
import org.eclipse.tractusx.bpdm.pool.repository.AddressRelationRepository
import org.eclipse.tractusx.bpdm.pool.repository.RelationRepository
import org.eclipse.tractusx.bpdm.pool.repository.SiteRelationRepository
import org.springframework.stereotype.Service

/**
 * Checks that a succession gives its predecessor a single successor at a time and closes no replacement cycle; several
 * predecessors may share one successor, as a merger is a valid succession.
 */
@Service
class SuccessionRelationValidator(
    private val relationRepository: RelationRepository,
    private val siteRelationRepository: SiteRelationRepository,
    private val addressRelationRepository: AddressRelationRepository
) {

    /**
     * Reports why legal entity [predecessor] cannot be replaced by [successor] during [validityPeriods], where
     * [existingRelation] is the succession being rewritten.
     */
    fun validate(
        predecessor: LegalEntityDb,
        successor: LegalEntityDb,
        validityPeriods: List<RelationValidityPeriodRaw>,
        existingRelation: RelationDb?
    ): List<RelationUpsertError> =
        successionViolations(
            subject = RelationSubject.LegalEntity,
            predecessor = predecessor,
            successor = successor,
            bpnOf = { it.bpn },
            successionsOf = { relationRepository.findByTypeAndStartNode(LegalEntityRelationTypeV7.IsReplacedBy, it) },
            successorOf = { it.endNode },
            validityPeriodsOf = { it.validityPeriods },
            validityPeriods = validityPeriods,
            existingRelation = existingRelation
        )

    /**
     * Reports why site [predecessor] cannot be replaced by [successor] during [validityPeriods], where [existingRelation]
     * is the succession being rewritten; both sites must belong to the same legal entity.
     */
    fun validate(
        predecessor: SiteDb,
        successor: SiteDb,
        validityPeriods: List<RelationValidityPeriodRaw>,
        existingRelation: SiteRelationDb?
    ): List<RelationUpsertError> {
        val legalEntityMismatch = RelationUpsertError.SuccessionAcrossLegalEntities(
            RelationSubject.Site, predecessor.bpn, successor.bpn, predecessor.legalEntity.bpn, successor.legalEntity.bpn
        ).takeIf { predecessor.legalEntity.bpn != successor.legalEntity.bpn }

        return listOfNotNull(legalEntityMismatch) + successionViolations(
            subject = RelationSubject.Site,
            predecessor = predecessor,
            successor = successor,
            bpnOf = { it.bpn },
            successionsOf = { siteRelationRepository.findByTypeAndStartSite(SiteRelationTypeV7.IsReplacedBy, it) },
            successorOf = { it.endSite },
            validityPeriodsOf = { it.validityPeriods },
            validityPeriods = validityPeriods,
            existingRelation = existingRelation
        )
    }

    /**
     * Reports why address [predecessor] cannot be replaced by [successor] during [validityPeriods], where
     * [existingRelation] is the succession being rewritten; both addresses must belong to the same legal entity.
     */
    fun validate(
        predecessor: LogisticAddressDb,
        successor: LogisticAddressDb,
        validityPeriods: List<RelationValidityPeriodRaw>,
        existingRelation: AddressRelationDb?
    ): List<RelationUpsertError> {
        val predecessorLegalEntityBpn = predecessor.legalEntity!!.bpn
        val successorLegalEntityBpn = successor.legalEntity!!.bpn
        val legalEntityMismatch = RelationUpsertError.SuccessionAcrossLegalEntities(
            RelationSubject.Address, predecessor.bpn, successor.bpn, predecessorLegalEntityBpn, successorLegalEntityBpn
        ).takeIf { predecessorLegalEntityBpn != successorLegalEntityBpn }

        return listOfNotNull(legalEntityMismatch) + successionViolations(
            subject = RelationSubject.Address,
            predecessor = predecessor,
            successor = successor,
            bpnOf = { it.bpn },
            successionsOf = { addressRelationRepository.findByTypeAndStartAddress(AddressRelationTypeV7.IsReplacedBy, it) },
            successorOf = { it.endAddress },
            validityPeriodsOf = { it.validityPeriods },
            validityPeriods = validityPeriods,
            existingRelation = existingRelation
        )
    }

    private fun <N, R : BaseEntityWithExplicitTimestamps> successionViolations(
        subject: RelationSubject,
        predecessor: N,
        successor: N,
        bpnOf: (N) -> String,
        successionsOf: (N) -> Collection<R>,
        successorOf: (R) -> N,
        validityPeriodsOf: (R) -> Collection<RelationValidityPeriodDb>,
        validityPeriods: List<RelationValidityPeriodRaw>,
        existingRelation: R?
    ): List<RelationUpsertError> {
        val successionsDuringPeriods = { node: N -> overlappingRelations(successionsOf(node), existingRelation, validityPeriods, validityPeriodsOf) }
        val predecessorBpn = bpnOf(predecessor)
        val successorBpn = bpnOf(successor)

        val multipleSuccessors = successionsDuringPeriods(predecessor)
            .map { bpnOf(successorOf(it)) }
            .firstOrNull { it != successorBpn }
            ?.let { RelationUpsertError.MultipleSuccessors(subject, predecessorBpn, successorBpn, it) }

        val transitiveSuccessorBpns = mutableSetOf<String>()
        val successorQueue = ArrayDeque(listOf(successor))
        while (successorQueue.isNotEmpty()) {
            val currentSuccessor = successorQueue.removeFirst()
            if (!transitiveSuccessorBpns.add(bpnOf(currentSuccessor))) continue

            successionsDuringPeriods(currentSuccessor).forEach { successorQueue.addFirst(successorOf(it)) }
        }
        val cycle = RelationUpsertError.SuccessionCycle(subject, predecessorBpn, successorBpn).takeIf { predecessorBpn in transitiveSuccessorBpns }

        return listOfNotNull(multipleSuccessors, cycle)
    }
}
