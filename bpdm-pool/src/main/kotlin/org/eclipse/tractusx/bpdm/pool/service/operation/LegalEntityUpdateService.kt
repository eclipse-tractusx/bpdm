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

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.NameDb
import org.eclipse.tractusx.bpdm.pool.mapper.entity.LegalEntityEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.update.*
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerEquivalenceMapper
import org.eclipse.tractusx.bpdm.pool.service.PartnerChangelogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The single authority for writing an existing legal entity, treated as a whole aggregate. A caller describes the change
 * as data — a header change, including the derived ultimate-owner BPNL, plus a change for the legal address, either of
 * which may leave its side alone — and this service, not the caller, decides how each field is applied. It detects
 * whether the aggregate changed, persists it, and emits exactly one LEGAL_ENTITY changelog for those that did; every
 * writer reuses it, so none can forget to log.
 *
 * The parent LEGAL_ENTITY changelog is emitted before the legal address is committed, so the parent entry always precedes
 * the child.
 */
@Service
class LegalEntityUpdateService(
    private val equivalenceMapper: BusinessPartnerEquivalenceMapper,
    private val addressUpdateService: AddressUpdateService,
    private val legalEntityEntityMapper: LegalEntityEntityMapper,
    private val repository: LegalEntityRepository,
    private val changelogService: PartnerChangelogService
) {

    /**
     * Applies the given changes and reports for each legal entity whether the aggregate — its header or its legal
     * address — actually changed.
     */
    @Transactional
    fun update(requests: List<LegalEntityUpdate>): List<UpsertResult<LegalEntityDb>> {
        val headerUpdates = requests.map { updateHeader(it) }
        val stagedLegalAddressUpdates = requests.map { addressUpdateService.stageUpdate(AddressUpdate(it.legalEntity.legalAddress, it.legalAddress)) }

        val legalEntityChangeResults = headerUpdates.zip(stagedLegalAddressUpdates) { headerResult, legalAddressResult ->
            val hasChanged = headerResult.upsertType != UpsertType.NoChange || legalAddressResult.upsertType != UpsertType.NoChange
            UpsertResult(headerResult.value, if (hasChanged) UpsertType.Updated else UpsertType.NoChange)
        }

        val updatedLegalEntities = legalEntityChangeResults.filter { it.upsertType == UpsertType.Updated }

        repository.saveAll(updatedLegalEntities.map { it.value })
        changelogService.createChangelogEntries(updatedLegalEntities.map { ChangelogEntryCreateRequest(it.value.bpn, ChangelogType.UPDATE, BusinessPartnerType.LEGAL_ENTITY) })

        addressUpdateService.commit(stagedLegalAddressUpdates)

        return legalEntityChangeResults
    }

    private fun updateHeader(request: LegalEntityUpdate): UpsertResult<LegalEntityDb> {
        val before = equivalenceMapper.toEquivalenceDto(request.legalEntity)
        applyHeader(request.legalEntity, request.header)
        val hasChanged = equivalenceMapper.toEquivalenceDto(request.legalEntity) != before

        return UpsertResult(request.legalEntity, if (hasChanged) UpsertType.Updated else UpsertType.NoChange)
    }

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
