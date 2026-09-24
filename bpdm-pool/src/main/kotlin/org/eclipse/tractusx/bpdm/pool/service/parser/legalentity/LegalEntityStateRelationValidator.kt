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
package org.eclipse.tractusx.bpdm.pool.service.parser.legalentity

import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityRelationType
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationValidityPeriodDb
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityUpdateContentWrite
import org.eclipse.tractusx.bpdm.pool.model.PartnerActivityTimeline
import org.eclipse.tractusx.bpdm.pool.model.error.AlternativeHeadquarterRecordedInactive
import org.eclipse.tractusx.bpdm.pool.model.error.LegalEntityStateRelationParseError
import org.eclipse.tractusx.bpdm.pool.model.error.MainHeadquarterRecordedInactive
import org.eclipse.tractusx.bpdm.pool.model.error.ManagerRecordedInactive
import org.eclipse.tractusx.bpdm.pool.model.error.OwnerRecordedInactive
import org.eclipse.tractusx.bpdm.pool.model.error.ReplacedLegalEntityRecordedActive
import org.eclipse.tractusx.bpdm.pool.model.error.ReplacingLegalEntityRecordedInactive
import org.eclipse.tractusx.bpdm.pool.model.toActivityTimeline
import org.eclipse.tractusx.bpdm.pool.repository.RelationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Checks the states a legal entity update states against the relations the legal entity already takes part in.
 *
 * Only the side a relation depends on is judged, so an owned or managed legal entity may go out of use while the
 * relation holds.
 */
@Service
class LegalEntityStateRelationValidator(
    private val relationRepository: RelationRepository
) {

    /**
     * Reports, per entry of a legal entity update batch, each relation and validity period the states it states would
     * contradict.
     */
    @Transactional(readOnly = true)
    fun validate(writes: List<LegalEntityUpdateContentWrite>): List<List<LegalEntityStateRelationParseError>> {
        if (writes.isEmpty()) return emptyList()

        val relations = relationRepository.findByStartNodeInOrEndNodeIn(writes.map { it.target })

        return writes.map { write ->
            val bpn = write.target.bpn
            val activity = write.content.header.states.toActivityTimeline()
            relations
                .filter { it.startNode.bpn == bpn || it.endNode.bpn == bpn }
                .flatMap { relation -> relation.validityPeriods.sortedBy { it.validFrom }.flatMap { validate(bpn, activity, relation, it) } }
        }
    }

    private fun validate(
        bpn: String,
        activity: PartnerActivityTimeline,
        relation: RelationDb,
        validityPeriod: RelationValidityPeriodDb
    ): List<LegalEntityStateRelationParseError> {
        val isSource = relation.startNode.bpn == bpn
        val isTarget = relation.endNode.bpn == bpn
        val validFrom = validityPeriod.validFrom
        val validTo = validityPeriod.validTo

        return when (relation.type) {
            LegalEntityRelationType.IsReplacedBy -> listOfNotNull(
                if (isSource && activity.isRecordedActiveOnceReplacedFrom(validFrom))
                    ReplacedLegalEntityRecordedActive(bpn, relation.endNode.bpn, validFrom) else null,
                if (isTarget && activity.isRecordedInactiveWhenReplacingFrom(validFrom))
                    ReplacingLegalEntityRecordedInactive(bpn, relation.startNode.bpn, validFrom) else null
            )

            LegalEntityRelationType.IsOwnedBy -> listOfNotNull(
                if (isTarget && activity.isRecordedInactiveDuring(validityPeriod))
                    OwnerRecordedInactive(bpn, relation.startNode.bpn, validFrom, validTo) else null
            )

            LegalEntityRelationType.IsManagedBy -> listOfNotNull(
                if (isTarget && activity.isRecordedInactiveDuring(validityPeriod))
                    ManagerRecordedInactive(bpn, relation.startNode.bpn, validFrom, validTo) else null
            )

            LegalEntityRelationType.IsAlternativeHeadquarterFor -> listOfNotNull(
                if (isSource && activity.isRecordedInactiveDuring(validityPeriod))
                    AlternativeHeadquarterRecordedInactive(bpn, relation.endNode.bpn, validFrom, validTo) else null,
                if (isTarget && activity.isRecordedInactiveDuring(validityPeriod))
                    MainHeadquarterRecordedInactive(bpn, relation.startNode.bpn, validFrom, validTo) else null
            )
        }
    }
}
