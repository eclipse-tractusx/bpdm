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
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressUpsertPlan
import org.eclipse.tractusx.bpdm.pool.service.operation.address.AddressCreateService
import org.eclipse.tractusx.bpdm.pool.service.operation.address.AddressPayloadUpdateService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Carries out the address a planned golden record upsert states beside the legal address and any site main address.
 */
@Service
class AdditionalAddressUpsertService(
    private val addressCreateService: AddressCreateService,
    private val addressPayloadUpdateService: AddressPayloadUpdateService
) {

    /**
     * Writes what [plan] states under [legalEntity] and [site], reporting the address it leaves behind and whether that write
     * changed it.
     */
    @Transactional
    fun upsert(
        plan: AddressUpsertPlan,
        legalEntity: LegalEntityDb,
        site: SiteDb?
    ): UpsertResult<LogisticAddressDb> =
        when (plan) {
            is AddressUpsertPlan.Create ->
                UpsertResult(addressCreateService.create(listOf(AddressCreateParsed(legalEntity, site, plan.content))).single(), UpsertType.Created)
            is AddressUpsertPlan.Update ->
                // Site membership is stated once for the whole record, by the membership plan, so this update leaves it alone.
                addressPayloadUpdateService.update(listOf(AddressUpdateParsed(plan.existingAddress, sites = null, address = plan.content)))
                    .single()
        }
}
