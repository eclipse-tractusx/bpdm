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
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntitySuccessionParsed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The single authority for writing a succession between two legal entities.
 */
@Service
class LegalEntitySuccessionUpsertService(
    private val relationUpsertService: RelationUpsertService
) {

    /**
     * Persists the succession and reports whether it was created, whether its validity changed, or whether it already
     * stood as stated.
     */
    @Transactional
    fun upsert(parsed: LegalEntitySuccessionParsed): UpsertResult<RelationDb> =
        relationUpsertService.upsertRelation(
            RelationUpsertService.UpsertRequest(
                source = parsed.predecessor,
                target = parsed.successor,
                legalEntityRelationType = LegalEntityRelationType.IsReplacedBy,
                validityPeriods = listOf(parsed.validityPeriod),
                existingRelation = parsed.existingRelation,
                reasonCode = parsed.reasonCode
            )
        )
}
