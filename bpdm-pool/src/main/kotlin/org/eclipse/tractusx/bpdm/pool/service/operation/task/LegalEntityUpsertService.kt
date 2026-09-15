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


package org.eclipse.tractusx.bpdm.pool.service.operation.task

import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.model.BpnReferenceAllocation
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityUpsertPlan
import org.eclipse.tractusx.bpdm.pool.service.operation.legalentity.LegalEntityCreateService
import org.eclipse.tractusx.bpdm.pool.service.operation.legalentity.LegalEntityPayloadUpdateService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Carries out the legal entity a planned golden record upsert states.
 *
 * The record the plan settles on answers to both references the plan carries, whether this write created it or found
 * it unchanged, so the legal address reference is registered here rather than by whoever writes an address.
 */
@Service
class LegalEntityUpsertService(
    private val legalEntityCreateService: LegalEntityCreateService,
    private val legalEntityPayloadUpdateService: LegalEntityPayloadUpdateService
) {

    /**
     * Writes what [plan] states and reports the legal entity it leaves behind, and whether that write changed it.
     */
    @Transactional
    fun upsert(plan: LegalEntityUpsertPlan, bpnReferences: BpnReferenceAllocation): UpsertResult<LegalEntityDb> {
        val result = when (plan) {
            is LegalEntityUpsertPlan.Unchanged -> UpsertResult(plan.target, UpsertType.NoChange)
            is LegalEntityUpsertPlan.Create ->
                UpsertResult(legalEntityCreateService.create(listOf(LegalEntityCreateParsed(plan.content))).single(), UpsertType.Created)
            is LegalEntityUpsertPlan.Update ->
                legalEntityPayloadUpdateService.update(listOf(LegalEntityUpdateParsed(plan.target, plan.content))).single()
        }

        bpnReferences.allocate(plan.reference, result.value.bpn)
        bpnReferences.allocate(plan.legalAddressReference, result.value.legalAddress.bpn)
        return result
    }
}
