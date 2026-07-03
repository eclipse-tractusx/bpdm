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
 * [org.eclipse.tractusx.bpdm.pool.service.parser.LegalEntityUpdateParser]), applies the header change under before/after
 * change detection, and delegates the legal-address change to [AddressUpdateService], netting a single UPDATE when either
 * side changed. Order-preserving positional contract (see [org.eclipse.tractusx.bpdm.pool.model.ParseResult]).
 */
@Service
class LegalEntityUpdateService(
    private val addressUpdateService: AddressUpdateService,
    private val legalEntityRepository: LegalEntityRepository,
    private val changelogService: PartnerChangelogService,
    private val equivalenceMapper: BusinessPartnerEquivalenceMapper,
    private val legalEntityEntityMapper: LegalEntityEntityMapper
) {

    @Transactional
    fun update(parsed: List<LegalEntityUpdateParsed>): List<UpsertResult<LegalEntityDb>>{
        val headerResults = parsed.map { update(it) }

        val legalAddressRequests = parsed.map {
            AddressUpdateParsed(
                it.target.legalAddress,
                null,
                it.content.legalAddress.address,
                it.content.legalAddress.scriptVariants
            )
        }
        val legalAddressResults = addressUpdateService.update(legalAddressRequests)

        return headerResults.zip(legalAddressResults){ headerResult, legalAddressResult ->
            val changed = headerResult.upsertType == UpsertType.Updated || legalAddressResult.upsertType == UpsertType.Updated
            UpsertResult(headerResult.value, if (changed) UpsertType.Updated else UpsertType.NoChange)
        }
    }

    private fun update(parsed: LegalEntityUpdateParsed): UpsertResult<LegalEntityDb> {
        val target = parsed.target

        val before = equivalenceMapper.toEquivalenceDto(target)
        doUpdateEntity(target, parsed.content)
        val after = equivalenceMapper.toEquivalenceDto(target)

        val upsertType = if (before != after) {
            legalEntityRepository.save(target)
            changelogService.createChangelogEntries(listOf(ChangelogEntryCreateRequest(target.bpn, ChangelogType.UPDATE, BusinessPartnerType.LEGAL_ENTITY)))
            UpsertType.Updated
        } else {
            UpsertType.NoChange
        }

        return UpsertResult(target, upsertType)
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