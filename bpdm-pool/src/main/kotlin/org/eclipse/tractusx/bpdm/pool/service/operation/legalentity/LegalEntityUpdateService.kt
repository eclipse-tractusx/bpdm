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

package org.eclipse.tractusx.bpdm.pool.service.operation.legalentity

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.*
import org.eclipse.tractusx.bpdm.pool.mapper.entity.LegalEntityEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.ChangelogRecord
import org.eclipse.tractusx.bpdm.pool.model.update.*
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.service.operation.changelog.ChangelogCreateService
import org.eclipse.tractusx.bpdm.pool.service.operation.address.AddressUpdateService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The single authority for writing an existing legal entity, treated as a whole aggregate. A caller describes the change
 * as data — a header change, including the derived ultimate-owner BPNL, plus a change for the legal address, either of
 * which may leave its side alone — and this service, not the caller, decides how each field is applied. It detects
 * whether the aggregate changed, persists it, and emits exactly one LEGAL_ENTITY changelog for those that did; every
 * writer reuses it, so none can forget to log.
 */
@Service
class LegalEntityUpdateService(
    private val addressUpdateService: AddressUpdateService,
    private val legalEntityEntityMapper: LegalEntityEntityMapper,
    private val repository: LegalEntityRepository,
    private val changelogCreateService: ChangelogCreateService
) {

    /**
     * Applies the given changes and reports for each legal entity whether the aggregate — its header or its legal
     * address — actually changed.
     */
    @Transactional
    fun update(requests: List<LegalEntityUpdate>): List<UpsertResult<LegalEntityDb>> {
        val headerUpdates = requests.map { updateHeader(it) }
        val legalAddressUpdates = addressUpdateService.update(requests.map { AddressUpdate(it.legalEntity.legalAddress, it.legalAddress) })

        val legalEntityChangeResults = headerUpdates.zip(legalAddressUpdates) { headerResult, legalAddressResult ->
            val hasChanged = headerResult.upsertType != UpsertType.NoChange || legalAddressResult.upsertType != UpsertType.NoChange
            UpsertResult(headerResult.value, if (hasChanged) UpsertType.Updated else UpsertType.NoChange)
        }

        val updatedLegalEntities = legalEntityChangeResults.filter { it.upsertType == UpsertType.Updated }

        repository.saveAll(updatedLegalEntities.map { it.value })
        changelogCreateService.record(updatedLegalEntities.map { ChangelogRecord(it.value.bpn, ChangelogType.UPDATE, BusinessPartnerType.LEGAL_ENTITY) })

        return legalEntityChangeResults
    }

    private fun updateHeader(request: LegalEntityUpdate): UpsertResult<LegalEntityDb> {
        // The verdict is taken before the change is applied, but the change is applied either way: `currentness` does
        // not count towards the verdict yet must still be restamped.
        val changed = willChange(request.legalEntity, request.header)
        applyHeader(request.legalEntity, request.header)

        return UpsertResult(request.legalEntity, if (changed) UpsertType.Updated else UpsertType.NoChange)
    }

    /**
     * Reports whether applying the given change would leave the legal entity's own fields different from how they stand
     * now; the legal address answers for itself.
     *
     * Each field is compared as the value [applyHeader] would write, built through the same entity mapper so the two
     * cannot drift apart. Stored entities a field points at — legal forms, identifier types, script codes — are compared
     * by their technical key rather than by reference, because navigating to one can yield a lazy proxy while the parsed
     * value holds the initialised instance.
     *
     * `currentness` is deliberately absent: the Pool restamps it on every task resolution, so counting it would emit a
     * changelog for every touch and keep the golden record process rediscovering its own writes.
     */
    private fun willChange(target: LegalEntityDb, header: LegalEntityHeaderUpdate): Boolean {
        header.legalName.ifSet { if (it != target.legalName.value) return true }
        header.legalShortName.ifSet { if (it != target.legalName.shortName) return true }
        header.legalForm.ifSet { if (it?.technicalKey != target.legalForm?.technicalKey) return true }
        header.confidenceCriteria.ifSet {
            if (legalEntityEntityMapper.toConfidence(it, target.confidenceCriteria.numberOfSharingMembers) != target.confidenceCriteria) return true
        }
        header.isDataSpaceParticipant.ifSet { if (it != target.isDataSpaceParticipant) return true }
        header.ownershipUltimate.ifSet { if (it != target.ownershipUltimate) return true }
        header.ultimateOwnerBpnl.ifSet { if (it != target.ultimateOwnerBpnl) return true }
        header.identifiers.ifSet {
            if (identifierKeys(legalEntityEntityMapper.toIdentifiers(it, target)) != identifierKeys(target.identifiers)) return true
        }
        header.states.ifSet {
            if (stateKeys(legalEntityEntityMapper.toStates(it, target)) != stateKeys(target.states)) return true
        }
        header.scriptVariants.ifSet {
            if (scriptVariantKeys(legalEntityEntityMapper.toScriptVariants(it)) != scriptVariantKeys(target.scriptVariants)) return true
        }

        return false
    }

    private fun identifierKeys(identifiers: Collection<LegalEntityIdentifierDb>): Set<Any?> =
        identifiers.map { listOf(it.value, it.type.technicalKey, it.issuingBody) }.toSet()

    private fun stateKeys(states: Collection<LegalEntityStateDb>): Set<Any?> =
        states.map { listOf(it.validFrom, it.validTo, it.type) }.toSet()

    private fun scriptVariantKeys(variants: Collection<LegalEntityScriptVariantDb>): Set<Any?> =
        variants.map { listOf(it.scriptCode.technicalKey, it.legalName, it.shortName) }.toSet()

    private fun applyHeader(target: LegalEntityDb, header: LegalEntityHeaderUpdate) {
        // The sharing-member count is Pool-maintained, not part of the update payload, so carry the current value forward.
        val numberOfSharingMembers = target.confidenceCriteria.numberOfSharingMembers
        applyLegalName(target, header)
        header.legalForm.ifSet { target.legalForm = it }
        header.confidenceCriteria.ifSet { target.confidenceCriteria = legalEntityEntityMapper.toConfidence(it, numberOfSharingMembers) }
        header.isDataSpaceParticipant.ifSet { target.isDataSpaceParticipant = it }
        header.ownershipUltimate.ifSet { target.ownershipUltimate = it }
        header.ultimateOwnerBpnl.ifSet { target.ultimateOwnerBpnl = it }
        header.currentness.ifSet { target.currentness = it }
        header.identifiers.ifSet { target.identifiers.replace(legalEntityEntityMapper.toIdentifiers(it, target)) }
        header.states.ifSet { target.states.replace(legalEntityEntityMapper.toStates(it, target)) }
        header.scriptVariants.ifSet { target.scriptVariants.replace(legalEntityEntityMapper.toScriptVariants(it)) }
    }

    // Legal name and short name are separate update fields but one embedded entity value, so setting either rebuilds the
    // pair and carries the untouched half forward.
    private fun applyLegalName(target: LegalEntityDb, header: LegalEntityHeaderUpdate) {
        if (header.legalName is FieldUpdate.NoOp && header.legalShortName is FieldUpdate.NoOp) return

        target.legalName = NameDb(
            value = header.legalName.orKeep(target.legalName.value),
            shortName = header.legalShortName.orKeep(target.legalName.shortName)
        )
    }
}
