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
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.mapper.entity.AddressEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.update.AddressContentUpdate
import org.eclipse.tractusx.bpdm.pool.model.update.AddressUpdate
import org.eclipse.tractusx.bpdm.pool.model.PendingAddressWrite
import org.eclipse.tractusx.bpdm.pool.model.update.ifSet
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerEquivalenceMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The single authority for updating logistic addresses: applies each [AddressUpdate]'s change to its address (how a
 * change is applied is this service's responsibility — a caller describes *what* to change, not *how*), detects whether
 * the address actually changed by equivalence, and persists and emits an UPDATE changelog only for those that changed.
 * The change vocabulary is confined to [AddressContentUpdate], which has no field for the address's identity, parent, or
 * relations, so no update can re-identify or re-parent an address.
 *
 * [update] does this in one call. [stageUpdate] plus [commit] split it so a caller can learn whether the address
 * changed — and wire it into a not-yet-persisted parent — before committing.
 */
@Service
class AddressUpdateService(
    private val equivalenceMapper: BusinessPartnerEquivalenceMapper,
    private val addressEntityMapper: AddressEntityMapper,
    private val logisticAddressWriteCommitService: LogisticAddressWriteCommitService
) {
    @Transactional
    fun update(requests: List<AddressUpdate>): List<UpsertResult<LogisticAddressDb>> =
        commit(requests.map { stageUpdate(it) })

    fun update(request: AddressUpdate): UpsertResult<LogisticAddressDb> =
        update(listOf(request)).single()

    fun stageUpdate(request: AddressUpdate): PendingAddressWrite {
        val target = request.address

        // A change that sets nothing cannot make the address differ, so skip the equivalence snapshots — they would
        // otherwise force the address's identifiers, states and script variants to load for nothing. This is the common
        // case for parents whose write only touches derived fields.
        if (request.content == AddressContentUpdate.NoOp) return PendingAddressWrite(target, UpsertType.NoChange)

        val before = equivalenceMapper.toEquivalenceDto(target)
        apply(target, request.content)
        val changed = equivalenceMapper.toEquivalenceDto(target) != before
        return PendingAddressWrite(target, if (changed) UpsertType.Updated else UpsertType.NoChange)
    }

    @Transactional
    fun commit(staged: List<PendingAddressWrite>): List<UpsertResult<LogisticAddressDb>> =
        logisticAddressWriteCommitService.commit(staged)

    private fun apply(target: LogisticAddressDb, update: AddressContentUpdate) {
        // The sharing-member count is Pool-maintained, not part of the update payload, so carry the current value forward.
        val numberOfSharingMembers = target.confidenceCriteria.numberOfSharingMembers
        update.name.ifSet { target.name = it }
        update.physicalPostalAddress.ifSet { target.physicalPostalAddress = addressEntityMapper.toPhysical(it) }
        update.alternativePostalAddress.ifSet { target.alternativePostalAddress = it?.let { alt -> addressEntityMapper.toAlternative(alt) } }
        update.confidenceCriteria.ifSet { target.confidenceCriteria = addressEntityMapper.toConfidence(it, numberOfSharingMembers) }
        update.identifiers.ifSet { target.identifiers.replace(addressEntityMapper.toIdentifiers(it).onEach { id -> id.address = target }) }
        update.states.ifSet { target.states.replace(addressEntityMapper.toStates(it).onEach { state -> state.address = target }) }
        update.scriptVariants.ifSet { target.scriptVariants.replace(addressEntityMapper.toScriptVariants(it)) }
        // Site membership is add-only; assigning is idempotent and never removes.
        update.assignToSite.ifSet { target.sites.add(it) }
    }
}
