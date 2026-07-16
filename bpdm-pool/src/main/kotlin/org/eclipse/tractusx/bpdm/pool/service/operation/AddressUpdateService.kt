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

import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.*
import org.eclipse.tractusx.bpdm.pool.model.PendingAddressWrite
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerEquivalenceMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The single authority for updating logistic addresses: applies a caller's mutation, detects whether it changed the
 * address by equivalence, and persists and emits an UPDATE changelog only for those that actually changed. Mutations
 * are confined to a [LogisticAddressMutator], so no update can touch an address's identity, parent, or relations.
 *
 * [update] does this in one call. [stageUpdate] plus [commit] split it so a caller can learn whether the address
 * changed — and wire it into a not-yet-persisted parent — before committing.
 */
@Service
class AddressUpdateService(
    private val equivalenceMapper: BusinessPartnerEquivalenceMapper,
    private val logisticAddressWriteCommitService: LogisticAddressWriteCommitService
) {

    fun update(targets: List<LogisticAddressDb>, mutate: (LogisticAddressMutator) -> Unit): List<UpsertResult<LogisticAddressDb>> =
        commit(targets.map { stageUpdate(it, mutate) })

    fun stageUpdate(target: LogisticAddressDb, mutate: (LogisticAddressMutator) -> Unit): PendingAddressWrite {
        val before = equivalenceMapper.toEquivalenceDto(target)
        mutate(DefaultLogisticAddressMutator(target))
        val changed = equivalenceMapper.toEquivalenceDto(target) != before
        return PendingAddressWrite(target, if (changed) UpsertType.Updated else UpsertType.NoChange)
    }

    @Transactional
    fun commit(staged: List<PendingAddressWrite>): List<UpsertResult<LogisticAddressDb>> =
        logisticAddressWriteCommitService.commit(staged)
}

/**
 * Write-facade over a managed [LogisticAddressDb] that owns the back-reference wiring for the sub-entities it replaces,
 * keeping the entity mapper a pure translation.
 */
private class DefaultLogisticAddressMutator(private val entity: LogisticAddressDb) : LogisticAddressMutator {
    override var name: String?
        get() = entity.name
        set(value) { entity.name = value }
    override var physicalPostalAddress: PhysicalPostalAddressDb
        get() = entity.physicalPostalAddress
        set(value) { entity.physicalPostalAddress = value }
    override var alternativePostalAddress: AlternativePostalAddressDb?
        get() = entity.alternativePostalAddress
        set(value) { entity.alternativePostalAddress = value }
    override var confidenceCriteria: ConfidenceCriteriaDb
        get() = entity.confidenceCriteria
        set(value) { entity.confidenceCriteria = value }

    override fun replaceIdentifiers(identifiers: Collection<AddressIdentifierDb>) {
        identifiers.forEach { it.address = entity }
        entity.identifiers.replace(identifiers)
    }

    override fun replaceStates(states: Collection<AddressStateDb>) {
        states.forEach { it.address = entity }
        entity.states.replace(states)
    }

    override fun replaceScriptVariants(scriptVariants: Collection<LogisticAddressScriptVariantDb>) {
        entity.scriptVariants.replace(scriptVariants)
    }

    override fun assignToSite(site: SiteDb) {
        entity.sites.add(site)
    }
}
