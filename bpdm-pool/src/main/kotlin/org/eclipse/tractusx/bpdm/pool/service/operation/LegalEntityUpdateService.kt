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
import org.eclipse.tractusx.bpdm.pool.model.AddressUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityContentParsed
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityUpdateParsed
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityIdentifierRepository
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerEquivalenceMapper
import org.eclipse.tractusx.bpdm.pool.service.PartnerChangelogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Updates legal entities — the composite legal-entity-update *operation*. It consumes a [LegalEntityUpdateParsed] command
 * (target resolved, header + legal-address content validated by
 * [org.eclipse.tractusx.bpdm.pool.service.parser.LegalEntityUpdateParser]), applies and change-detects the header, and
 * delegates the legal-address change to [AddressUpdateService], netting a single UPDATE when either side changed. The
 * legal address is staged (not yet committed) so the parent LEGAL_ENTITY changelog is emitted before the child ADDRESS
 * changelog. Order-preserving positional contract (see [org.eclipse.tractusx.bpdm.pool.model.ParseResult]).
 */
@Service
class LegalEntityUpdateService(
    private val addressUpdateService: AddressUpdateService,
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
                it.content.legalAddress.address,
                it.content.legalAddress.scriptVariants
            )
        }
        val stagedLegalAddresses = addressUpdateService.stageUpdate(legalAddressRequests)

        val overallResults = headerResults.zip(stagedLegalAddresses){ headerResult, stagedLegalAddress ->
            val changed = headerResult.upsertType == UpsertType.Updated || stagedLegalAddress.upsertType == UpsertType.Updated
            UpsertResult(headerResult.value, if (changed) UpsertType.Updated else UpsertType.NoChange)
        }

        // Emit the parent LEGAL_ENTITY changelog before committing the legal address so the parent UPDATE precedes the
        // child ADDRESS UPDATE. Staging above yielded the address change flag without emitting its changelog yet.
        changelogService.createChangelogEntries(
            overallResults
                .filter { it.upsertType == UpsertType.Updated }
                .map { ChangelogEntryCreateRequest(it.value.bpn, ChangelogType.UPDATE, BusinessPartnerType.LEGAL_ENTITY) }
        )

        addressUpdateService.commit(stagedLegalAddresses)

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
        // The sharing-member count is Pool-maintained, not part of the update payload, so carry the current value forward.
        target.confidenceCriteria = legalEntityEntityMapper.toConfidence(header.confidenceCriteria, target.confidenceCriteria.numberOfSharingMembers)
        target.isCatenaXMemberData = header.isParticipantData
        target.identifiers.replace(legalEntityEntityMapper.toIdentifiers(header.identifiers, target))
        target.states.replace(legalEntityEntityMapper.toStates(header.states, target))
        target.scriptVariants.replace(legalEntityEntityMapper.toScriptVariants(header.scriptVariants))
        // currentness is refreshed on every update; it is excluded from the equivalence diff, so it never by itself marks
        // the aggregate as changed (matches the previous update behavior).
        target.currentness = Instant.now().truncatedTo(ChronoUnit.MICROS)
    }
}