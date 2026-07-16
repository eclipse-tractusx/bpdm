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
 * Stages logistic-address updates: applies the caller's mutation to an existing address and change-detects it against
 * its before/after equivalence ([UpsertType.Updated]/[UpsertType.NoChange]). The address is not saved here — [commit]
 * (via [LogisticAddressWriteCommitService]) does that after the caller has finished wiring. Compose several changes into
 * a single [stageUpdate] (e.g. a site assignment and a content change) so they net one ADDRESS UPDATE. This is the
 * update-side counterpart of [LogisticAddressStagedCreateService]. Order-preserving positional contract (see
 * [org.eclipse.tractusx.bpdm.pool.model.ParseResult]).
 */
@Service
class LogisticAddressStagedUpdateService(
    private val equivalenceMapper: BusinessPartnerEquivalenceMapper,
    private val logisticAddressWriteCommitService: LogisticAddressWriteCommitService
) {
    /**
     * Applies [mutate] to an existing address — through a [LogisticAddressMutator] that exposes only the aspects an update
     * may change (never `bpn`, `legalEntity`, or relations) — and change-detects it against its before/after equivalence.
     * The address is not saved here — [commit] does that after the caller has finished wiring.
     */
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
 * Thin write-facade over a managed [LogisticAddressDb]; owns the back-reference wiring for the sub-entities it replaces,
 * so the entity mapper stays a pure translation. Meaningless outside [LogisticAddressStagedUpdateService.stageUpdate],
 * which is the only place it is constructed.
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
