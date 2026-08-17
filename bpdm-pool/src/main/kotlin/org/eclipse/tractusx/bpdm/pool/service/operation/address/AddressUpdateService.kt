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

package org.eclipse.tractusx.bpdm.pool.service.operation.address

import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.*
import org.eclipse.tractusx.bpdm.pool.mapper.entity.AddressEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.PendingAddressWrite
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressSiteAssignmentParsed
import org.eclipse.tractusx.bpdm.pool.model.update.AddressContentUpdate
import org.eclipse.tractusx.bpdm.pool.model.update.AddressUpdate
import org.eclipse.tractusx.bpdm.pool.model.update.FieldUpdate
import org.eclipse.tractusx.bpdm.pool.model.update.ifSet
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The single authority for updating logistic addresses: applies each requested change to its address — a caller
 * describes *what* to change, this service decides *how* — detects whether the address actually changed,
 * and persists and emits an UPDATE changelog only for those that did. The change vocabulary has no field for an
 * address's identity, parent, or relations, so no update can re-identify or re-parent an address.
 */
@Service
class AddressUpdateService(
    private val addressEntityMapper: AddressEntityMapper,
    private val addressWriteCommitService: AddressWriteCommitService
) {
    /**
     * Applies the given changes and reports for each address whether it actually changed.
     */
    @Transactional
    fun update(requests: List<AddressUpdate>): List<UpsertResult<LogisticAddressDb>> =
        commit(requests.map { stageUpdate(it) })

    /**
     * Applies the given change to a single address and reports whether it actually changed.
     */
    fun update(request: AddressUpdate): UpsertResult<LogisticAddressDb> =
        update(listOf(request)).single()

    /**
     * Adds the stated site memberships to their addresses and reports for each address whether it actually changed.
     * Several memberships of one address are applied as a single write, so that address yields one result rather than
     * one per membership.
     */
    @Transactional
    fun assignToSites(assignments: List<AddressSiteAssignmentParsed>): List<UpsertResult<LogisticAddressDb>> =
        update(assignments.groupBy { it.address.bpn }.map { (_, ofOneAddress) ->
            AddressUpdate(
                ofOneAddress.first().address,
                AddressContentUpdate.NoOp.copy(assignToSites = FieldUpdate.Set(ofOneAddress.map { it.site }))
            )
        })

    /**
     * Applies one change in memory without persisting, so a caller can see whether the address changed — and wire it into
     * a not-yet-persisted parent — before handing it to [commit].
     */
    fun stageUpdate(request: AddressUpdate): PendingAddressWrite {
        val target = request.address

        // The verdict is taken before the change is applied, but the change is applied either way: a field that does
        // not count towards the verdict is still written.
        val changed = willChange(target, request.content)
        apply(target, request.content)

        return PendingAddressWrite(target, if (changed) UpsertType.Updated else UpsertType.NoChange)
    }

    /**
     * Persists the staged addresses that changed and emits their UPDATE changelog.
     */
    @Transactional
    fun commit(staged: List<PendingAddressWrite>): List<UpsertResult<LogisticAddressDb>> =
        addressWriteCommitService.commit(staged)

    /**
     * Reports whether applying the given change would leave the address different from how it stands now.
     *
     * Each field is compared as the value [apply] would write, built through the same entity mapper so the two cannot
     * drift apart. Stored entities a field points at — regions, identifier types, script codes — are compared by their
     * technical key rather than by reference: navigating to one can yield a lazy proxy while the parsed value holds the
     * initialised instance, and those two are never reference-equal.
     */
    private fun willChange(target: LogisticAddressDb, update: AddressContentUpdate): Boolean {
        update.name.ifSet { if (it != target.name) return true }
        update.physicalPostalAddress.ifSet {
            if (physicalKey(addressEntityMapper.toPhysical(it)) != physicalKey(target.physicalPostalAddress)) return true
        }
        update.alternativePostalAddress.ifSet { alternative ->
            val updated = alternative?.let { alternativeKey(addressEntityMapper.toAlternative(it)) }
            if (updated != target.alternativePostalAddress?.let { alternativeKey(it) }) return true
        }
        update.confidenceCriteria.ifSet {
            if (addressEntityMapper.toConfidence(it, target.confidenceCriteria.numberOfSharingMembers) != target.confidenceCriteria) return true
        }
        update.identifiers.ifSet {
            if (identifierKeys(addressEntityMapper.toIdentifiers(it)) != identifierKeys(target.identifiers)) return true
        }
        update.states.ifSet {
            if (stateKeys(addressEntityMapper.toStates(it)) != stateKeys(target.states)) return true
        }
        update.scriptVariants.ifSet {
            if (scriptVariantKeys(addressEntityMapper.toScriptVariants(it)) != scriptVariantKeys(target.scriptVariants)) return true
        }
        // Site membership is add-only, so assigning a site the address already belongs to changes nothing.
        update.assignToSites.ifSet { sites -> if (sites.any { site -> target.sites.none { it.bpn == site.bpn } }) return true }

        return false
    }

    private fun physicalKey(address: PhysicalPostalAddressDb): List<Any?> =
        with(address) {
            listOf(
                geographicCoordinates, country, administrativeAreaLevel1?.regionCode, administrativeAreaLevel2,
                administrativeAreaLevel3, postCode, city, districtLevel1, street?.let { streetKey(it) },
                companyPostCode, industrialZone, building, floor, door, taxJurisdictionCode
            )
        }

    private fun alternativeKey(address: AlternativePostalAddressDb): List<Any?> =
        with(address) {
            listOf(
                geographicCoordinates, country, administrativeAreaLevel1?.regionCode, postCode, city,
                deliveryServiceType, deliveryServiceNumber, deliveryServiceQualifier
            )
        }

    private fun streetKey(street: StreetDb): List<Any?> =
        with(street) {
            listOf(
                name, houseNumber, houseNumberSupplement, milestone, direction,
                namePrefix, additionalNamePrefix, nameSuffix, additionalNameSuffix
            )
        }

    private fun identifierKeys(identifiers: Collection<AddressIdentifierDb>): Set<Any?> =
        identifiers.map { it.value to it.type.technicalKey }.toSet()

    private fun stateKeys(states: Collection<AddressStateDb>): Set<Any?> =
        states.map { listOf(it.validFrom, it.validTo, it.type) }.toSet()

    private fun scriptVariantKeys(variants: Collection<LogisticAddressScriptVariantDb>): Set<Any?> =
        variants.map { listOf(it.scriptCode.technicalKey, it.name, it.physicalAddress, it.alternativeAddress) }.toSet()

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
        update.assignToSites.ifSet { target.sites.addAll(it) }
    }
}
