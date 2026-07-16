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
import org.eclipse.tractusx.bpdm.pool.mapper.entity.LegalEntityEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityContentParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityUpdateParsed
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerEquivalenceMapper
import org.eclipse.tractusx.bpdm.pool.service.PartnerChangelogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * The single authority for updating legal entities together with their legal address: applies and change-detects the
 * header, applies the legal-address change, and reports one UPDATE when either side actually changed. The parent
 * LEGAL_ENTITY changelog is emitted before the child ADDRESS changelog.
 */
@Service
class LegalEntityUpdateService(
    private val addressFullUpdateService: AddressFullUpdateService,
    private val legalEntityEntityMapper: LegalEntityEntityMapper,
    private val repository: LegalEntityRepository,
    private val equivalenceMapper: BusinessPartnerEquivalenceMapper,
    private val changelogService: PartnerChangelogService
) {

    @Transactional
    fun update(parsed: List<LegalEntityUpdateParsed>): List<UpsertResult<LegalEntityDb>>{
        val headerResults = parsed.map { updateHeader(it) }

        val legalAddressRequests = parsed.map {
            AddressUpdateParsed(
                it.target.legalAddress,
                null,
                it.content.legalAddress
            )
        }
        val stagedLegalAddresses = addressFullUpdateService.stageUpdate(legalAddressRequests)

        val overallResults = headerResults.zip(stagedLegalAddresses){ headerResult, stagedLegalAddress ->
            val changed = headerResult.upsertType == UpsertType.Updated || stagedLegalAddress.upsertType == UpsertType.Updated
            UpsertResult(headerResult.value, if (changed) UpsertType.Updated else UpsertType.NoChange)
        }

        // Emit the parent changelog before committing the staged address so the parent UPDATE precedes the child.
        changelogService.createChangelogEntries(
            overallResults
                .filter { it.upsertType == UpsertType.Updated }
                .map { ChangelogEntryCreateRequest(it.value.bpn, ChangelogType.UPDATE, BusinessPartnerType.LEGAL_ENTITY) }
        )

        addressFullUpdateService.commit(stagedLegalAddresses)

        return overallResults
    }

    private fun updateHeader(request: LegalEntityUpdateParsed): UpsertResult<LegalEntityDb> {
        val before = equivalenceMapper.toEquivalenceDto(request.target)
        doUpdateEntity(request.target, request.content)
        val after = equivalenceMapper.toEquivalenceDto(request.target)

        val changed = before != after

        if(changed) repository.save(request.target)

        return UpsertResult(request.target, if (changed) UpsertType.Updated else UpsertType.NoChange)
    }


    private fun doUpdateEntity(target: LegalEntityDb, content: LegalEntityContentParsed) {
        val header = content.header
        target.legalName = legalEntityEntityMapper.toLegalName(header)
        target.legalForm = header.legalForm
        // Sharing-member count is Pool-maintained and absent from the payload, so carry it forward.
        target.confidenceCriteria = legalEntityEntityMapper.toConfidence(header.confidenceCriteria, target.confidenceCriteria.numberOfSharingMembers)
        target.isCatenaXMemberData = header.isParticipantData
        target.identifiers.replace(legalEntityEntityMapper.toIdentifiers(header.identifiers, target))
        target.states.replace(legalEntityEntityMapper.toStates(header.states, target))
        target.scriptVariants.replace(legalEntityEntityMapper.toScriptVariants(header.scriptVariants))
        // currentness refreshes every update but is excluded from the equivalence diff, so it never by itself marks a change.
        target.currentness = Instant.now().truncatedTo(ChronoUnit.MICROS)
    }
}