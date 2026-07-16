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

import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.mapper.entity.AddressEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.AddressScriptVariantParsed
import org.eclipse.tractusx.bpdm.pool.model.AddressUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.LogisticAddressParsed
import org.eclipse.tractusx.bpdm.pool.model.PendingAddressWrite
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The single authority for applying a full parsed address-update payload — descriptive content plus an optional site
 * assignment — to an already-resolved address, composing both into one net change.
 *
 * [update] does this in one call. [stageUpdate] plus [commit] split it so a caller can learn whether the address
 * changed before committing.
 */
@Service
class AddressFullUpdateService(
    private val addressStagedUpdateService: AddressUpdateService,
    private val addressEntityMapper: AddressEntityMapper
) {

    @Transactional
    fun update(parsed: List<AddressUpdateParsed>): List<UpsertResult<LogisticAddressDb>> =
        commit(stageUpdate(parsed))

    fun stageUpdate(parsed: List<AddressUpdateParsed>): List<PendingAddressWrite> =
        parsed.map { entry ->
            addressStagedUpdateService.stageUpdate(entry.target) { address ->
                entry.site?.let { address.assignToSite(it) }
                applyContent(address, entry.address, entry.scriptVariants)
            }
        }

    @Transactional
    fun commit(staged: List<PendingAddressWrite>): List<UpsertResult<LogisticAddressDb>> =
        addressStagedUpdateService.commit(staged)

    private fun applyContent(target: LogisticAddressMutator, address: LogisticAddressParsed, scriptVariants: List<AddressScriptVariantParsed>) {
        // The sharing-member count is Pool-maintained, not part of the update payload, so carry the current value forward.
        val numberOfSharingMembers = target.confidenceCriteria.numberOfSharingMembers
        target.name = address.name
        target.physicalPostalAddress = addressEntityMapper.toPhysical(address.physicalPostalAddress)
        target.alternativePostalAddress = address.alternativePostalAddress?.let { addressEntityMapper.toAlternative(it) }
        target.confidenceCriteria = addressEntityMapper.toConfidence(address.confidenceCriteria, numberOfSharingMembers)
        target.replaceIdentifiers(addressEntityMapper.toIdentifiers(address.identifiers))
        target.replaceStates(addressEntityMapper.toStates(address.states))
        target.replaceScriptVariants(addressEntityMapper.toScriptVariants(scriptVariants))
    }
}
