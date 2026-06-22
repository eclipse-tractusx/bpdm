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

package org.eclipse.tractusx.bpdm.pool.service

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.mapper.LegalEntityEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityContentParsed
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityUpdateParseError
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityUpdateRequest
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.combine
import org.eclipse.tractusx.bpdm.pool.model.zipParseResults
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Updates existing legal entities. `parse` resolves the target legal entity and validates header + identifier-uniqueness +
 * legal-address content; `update` mutates the managed legal entity and its legal address, gating the save and BOTH
 * changelog entries on a whole-aggregate change (preserving the previous update behavior). The legal address is mutated
 * via [AddressUpdateService.applyTo] — which deliberately does not save or emit its own changelog — so the legal entity
 * keeps aggregate-level gating. Order-preserving positional contract (see [ParseResult]).
 */
@Service
class LegalEntityUpdateService(
    private val legalEntityBpnParser: LegalEntityBpnParser,
    private val legalEntityHeaderParser: LegalEntityHeaderParser,
    private val duplicateValidator: LegalEntityIdentifierDuplicateValidator,
    private val addressUpdateService: AddressUpdateService,
    private val legalEntityRepository: LegalEntityRepository,
    private val changelogService: PartnerChangelogService,
    private val equivalenceMapper: BusinessPartnerEquivalenceMapper,
    private val legalEntityEntityMapper: LegalEntityEntityMapper
) {

    fun parse(requests: List<LegalEntityUpdateRequest>): List<ParseResult<LegalEntityUpdateParsed, LegalEntityUpdateParseError>> {
        val targetResults = legalEntityBpnParser.parse(requests.map { it.legalEntityBpn })

        val headers = requests.map { it.content.header }
        val headerResults = legalEntityHeaderParser.parse(headers)
        // An identifier already owned by the target legal entity itself is not a duplicate; its BPN comes from the target.
        val ownerBpns = targetResults.map { (it as? ParseResult.Success)?.parsed?.bpn }
        val duplicateErrors = duplicateValidator.validate(headers, ownerBpns)
        val mergedHeaderResults = headerResults.zip(duplicateErrors) { result, extra -> result.combine(extra) { it } }

        // The legal address's duplicate check is scoped to its owning address; its BPN comes from the resolved target.
        val legalAddressOwnerBpns = targetResults.map { (it as? ParseResult.Success)?.parsed?.legalAddress?.bpn }
        val legalAddressResults = addressUpdateService.parseContent(requests.map { it.content.legalAddress }, legalAddressOwnerBpns)

        return zipParseResults(mergedHeaderResults, targetResults, legalAddressResults) { header, target, legalAddress ->
            LegalEntityUpdateParsed(target, LegalEntityContentParsed(header, legalAddress))
        }
    }

    /**
     * Returns the updated entities (within the caller's transaction) rather than a detached response model: building
     * version-specific responses is the job of the border/application service at the edge.
     */
    @Transactional
    fun update(parsed: List<LegalEntityUpdateParsed>): List<UpsertResult<LegalEntityDb>> =
        parsed.map { update(it) }

    private fun update(parsed: LegalEntityUpdateParsed): UpsertResult<LegalEntityDb> {
        val target = parsed.target

        val before = equivalenceMapper.toEquivalenceDto(target)
        applyTo(target, parsed.content)
        val after = equivalenceMapper.toEquivalenceDto(target)

        val upsertType = if (before != after) {
            legalEntityRepository.save(target)
            changelogService.createChangelogEntries(listOf(ChangelogEntryCreateRequest(target.bpn, ChangelogType.UPDATE, BusinessPartnerType.LEGAL_ENTITY)))
            changelogService.createChangelogEntries(listOf(ChangelogEntryCreateRequest(target.legalAddress.bpn, ChangelogType.UPDATE, BusinessPartnerType.ADDRESS)))
            UpsertType.Updated
        } else {
            UpsertType.NoChange
        }

        return UpsertResult(target, upsertType)
    }

    private fun applyTo(target: LegalEntityDb, content: LegalEntityContentParsed) {
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

        val legalAddress = content.legalAddress
        addressUpdateService.applyTo(target.legalAddress, legalAddress.address, legalAddress.scriptVariants, target.legalAddress.confidenceCriteria.numberOfSharingMembers)
    }

    @Transactional
    fun parseAndUpdate(requests: List<LegalEntityUpdateRequest>): List<ParseResult<UpsertResult<LegalEntityDb>, LegalEntityUpdateParseError>> {
        val parseResults = parse(requests)
        val updated = update(parseResults.filterIsInstance<ParseResult.Success<LegalEntityUpdateParsed>>().map { it.parsed }).iterator()
        return parseResults.map { result ->
            when (result) {
                is ParseResult.Success -> ParseResult.Success(updated.next())
                is ParseResult.Failure -> result
            }
        }
    }
}
