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
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.mapper.entity.AddressEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.AddressScriptVariantParsed
import org.eclipse.tractusx.bpdm.pool.model.AddressUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.LogisticAddressParsed
import org.eclipse.tractusx.bpdm.pool.model.PendingAddressWrite
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Updates existing logistic addresses given an already-resolved target — the single address-update *operation*. It
 * optionally assigns the address to a site and applies the content change, composing both into one
 * [LogisticAddressStagedUpdateService.stageUpdate] so a membership change and a content change net a single ADDRESS UPDATE (one
 * mutation, one change detection, one changelog). Update never re-parents. Content validation and target resolution are
 * the parser's job ([org.eclipse.tractusx.bpdm.pool.service.parser.AddressUpdateParser]); callers that already hold the
 * managed target and a validated command (e.g. the address-only update operation) call [update] directly.
 * Order-preserving positional contract (see [org.eclipse.tractusx.bpdm.pool.model.ParseResult]).
 *
 * [update] is the single-call convenience that stages and immediately commits. Callers that own the *parent* changelog
 * (legal-entity / site update, passing `site = null`) instead drive [stageUpdate] and [commit] as separate phases: they
 * stage to learn whether the address changed, emit their parent changelog, and only then commit — so the parent UPDATE
 * changelog precedes the child ADDRESS UPDATE changelog. This mirrors the create side's stage/commit split.
 */
@Service
class AddressUpdateService(
    private val addressStagedUpdateService: LogisticAddressStagedUpdateService,
    private val addressEntityMapper: AddressEntityMapper
) {

    @Transactional
    fun update(parsed: List<AddressUpdateParsed>): List<UpsertResult<LogisticAddressDb>> =
        commit(stageUpdate(parsed))

    /**
     * Applies each command's optional site assignment and content change to its (already-resolved) target and
     * change-detects it, without saving or emitting a changelog — [commit] does that. Membership and content are
     * composed into one staged mutation so they net a single ADDRESS UPDATE.
     */
    fun stageUpdate(parsed: List<AddressUpdateParsed>): List<PendingAddressWrite> =
        parsed.map { entry ->
            addressStagedUpdateService.stageUpdate(entry.target) { address ->
                entry.site?.let { address.sites.add(it) }
                applyContent(address, entry.address, entry.scriptVariants)
            }
        }

    @Transactional
    fun commit(staged: List<PendingAddressWrite>): List<UpsertResult<LogisticAddressDb>> =
        addressStagedUpdateService.commit(staged)

    private fun applyContent(target: LogisticAddressDb, address: LogisticAddressParsed, scriptVariants: List<AddressScriptVariantParsed>) {
        // The sharing-member count is Pool-maintained, not part of the update payload, so carry the current value forward.
        val numberOfSharingMembers = target.confidenceCriteria.numberOfSharingMembers
        target.name = address.name
        target.physicalPostalAddress = addressEntityMapper.toPhysical(address.physicalPostalAddress)
        target.alternativePostalAddress = address.alternativePostalAddress?.let { addressEntityMapper.toAlternative(it) }
        target.confidenceCriteria = addressEntityMapper.toConfidence(address.confidenceCriteria, numberOfSharingMembers)
        target.identifiers.replace(addressEntityMapper.toIdentifiers(address.identifiers, target))
        target.states.replace(addressEntityMapper.toStates(address.states, target))
        target.scriptVariants.replace(addressEntityMapper.toScriptVariants(scriptVariants))
    }
}
