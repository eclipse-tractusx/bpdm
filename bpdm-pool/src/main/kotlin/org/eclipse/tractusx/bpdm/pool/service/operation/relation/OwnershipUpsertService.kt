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

import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityRelationType
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.entity.TriggerEventType
import org.eclipse.tractusx.bpdm.pool.model.parsed.OwnershipUpsertParsed
import org.eclipse.tractusx.bpdm.pool.service.operation.legalentity.UltimateOwnerRecalculationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The single authority for writing an ownership between two legal entities.
 */
@Service
class OwnershipUpsertService(
    private val relationUpsertService: RelationUpsertService,
    private val ultimateOwnerRecalculationService: UltimateOwnerRecalculationService,
    private val relationValidityBoundaryTriggerService: RelationValidityBoundaryTriggerService
) {

    /**
     * Persists the ownership, brings the owned legal entity's ultimate owner in line with it and schedules a
     * recalculation for each future date on which it starts or stops holding, reporting whether it was created, whether
     * its validity changed, or whether it already stood as stated.
     */
    @Transactional
    fun upsert(parsed: OwnershipUpsertParsed): UpsertResult<RelationDb> {
        val result = relationUpsertService.upsertRelation(
            RelationUpsertService.UpsertRequest(
                source = parsed.owned,
                target = parsed.owner,
                legalEntityRelationType = LegalEntityRelationType.IsOwnedBy,
                validityPeriods = parsed.validityPeriods,
                existingRelation = parsed.existingRelation,
                reasonCode = parsed.reasonCode
            )
        )

        ultimateOwnerRecalculationService.recalculate(listOf(parsed.owned))
        relationValidityBoundaryTriggerService.reconcile(result.value, TriggerEventType.OwnershipValidityBoundary)

        return result
    }
}
