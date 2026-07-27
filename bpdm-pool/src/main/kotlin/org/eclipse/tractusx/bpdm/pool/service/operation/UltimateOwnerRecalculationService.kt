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

package org.eclipse.tractusx.bpdm.pool.service.operation

import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.model.AddressContentUpdate
import org.eclipse.tractusx.bpdm.pool.model.FieldUpdate
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityHeaderUpdate
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityUpdate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Keeps the derived `ultimateOwnerBpnl` consistent with the `ownershipUltimate` flags and the ownership relations. It is
 * the single maintainer of that invariant, called by every writer whose change can invalidate it: a legal-entity write
 * that changes the flag, an `IsOwnedBy` relation upsert, and the trigger that fires when a relation's validity period
 * starts or ends.
 *
 * Always call it *after* the write it reacts to — it re-reads the ownership graph as it now stands. Writes go through
 * [LegalEntityUpdateService], so a value that did not actually change produces no save and no changelog entry.
 */
@Service
class UltimateOwnerRecalculationService(
    private val ultimateOwnerResolutionService: UltimateOwnerResolutionService,
    private val legalEntityUpdateService: LegalEntityUpdateService
) {

    /**
     * Re-derives the ultimate owner of [legalEntities] and of every entity they own, transitively — a flag or relation
     * change moves the ultimate owner of the whole subtree, not just the entity written.
     */
    @Transactional
    fun recalculate(legalEntities: List<LegalEntityDb>) {
        if (legalEntities.isEmpty()) return

        val updates = ultimateOwnerResolutionService.resolveForEntitiesAndDescendants(legalEntities)
            .map { (legalEntity, ultimateOwnerBpnl) ->
                LegalEntityUpdate(
                    legalEntity,
                    LegalEntityHeaderUpdate.NoOp.copy(ultimateOwnerBpnl = FieldUpdate.Set(ultimateOwnerBpnl)),
                    AddressContentUpdate.NoOp
                )
            }

        legalEntityUpdateService.update(updates)
    }
}
