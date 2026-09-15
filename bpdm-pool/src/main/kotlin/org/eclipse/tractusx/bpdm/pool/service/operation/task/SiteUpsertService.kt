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
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.model.BpnReferenceAllocation
import org.eclipse.tractusx.bpdm.pool.model.parsed.BpnReferenceParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteCreateWithReferencedAddressAsMainParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteUpsertPlan
import org.eclipse.tractusx.bpdm.pool.service.operation.site.SiteCreateService
import org.eclipse.tractusx.bpdm.pool.service.operation.site.SiteCreateWithReferencedAddressAsMainService
import org.eclipse.tractusx.bpdm.pool.service.operation.site.SitePayloadUpdateService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Carries out the site a planned golden record upsert states.
 *
 * Which main address the site ends up on is the plan variant's decision, and only a variant that states an address of
 * its own leaves a reference for that address to be registered against.
 */
@Service
class SiteUpsertService(
    private val siteCreateService: SiteCreateService,
    private val siteCreateWithReferencedAddressAsMainService: SiteCreateWithReferencedAddressAsMainService,
    private val sitePayloadUpdateService: SitePayloadUpdateService
) {

    /**
     * Writes what [plan] states under [legalEntity] and reports the site it leaves behind, and whether that write changed it.
     */
    @Transactional
    fun upsert(plan: SiteUpsertPlan, legalEntity: LegalEntityDb, bpnReferences: BpnReferenceAllocation): UpsertResult<SiteDb> {
        val result = when (plan) {
            is SiteUpsertPlan.Unchanged -> UpsertResult(plan.target, UpsertType.NoChange)
            is SiteUpsertPlan.CreateWithOwnMainAddress ->
                UpsertResult(siteCreateService.create(listOf(SiteCreateParsed(legalEntity, plan.content))).single(), UpsertType.Created)
            is SiteUpsertPlan.CreateOnLegalAddress ->
                UpsertResult(
                    siteCreateWithReferencedAddressAsMainService.create(
                        listOf(SiteCreateWithReferencedAddressAsMainParsed(legalEntity.legalAddress, plan.header, mainAddressContent = null))
                    ).single(),
                    UpsertType.Created
                )
            is SiteUpsertPlan.CreateOnExistingAddress ->
                UpsertResult(siteCreateWithReferencedAddressAsMainService.create(listOf(plan.parsed)).single(), UpsertType.Created)
            is SiteUpsertPlan.UpdateWithOwnMainAddress ->
                sitePayloadUpdateService.update(listOf(SiteUpdateParsed(plan.target, plan.content))).single()
            is SiteUpsertPlan.UpdateOnLegalAddress ->
                sitePayloadUpdateService.updateHeaders(listOf(plan.parsed)).single()
        }

        bpnReferences.allocate(plan.reference, result.value.bpn)
        mainAddressReference(plan)?.let { bpnReferences.allocate(it, result.value.mainAddress.bpn) }
        return result
    }

    // A site whose main address is the legal address states no address of its own, so there is nothing to register:
    // that address answers to the legal entity's legal address reference.
    private fun mainAddressReference(plan: SiteUpsertPlan): BpnReferenceParsed? =
        when (plan) {
            is SiteUpsertPlan.CreateWithOwnMainAddress -> plan.mainAddressReference
            is SiteUpsertPlan.CreateOnExistingAddress -> plan.mainAddressReference
            is SiteUpsertPlan.UpdateWithOwnMainAddress -> plan.mainAddressReference
            is SiteUpsertPlan.CreateOnLegalAddress, is SiteUpsertPlan.UpdateOnLegalAddress, is SiteUpsertPlan.Unchanged -> null
        }
}
