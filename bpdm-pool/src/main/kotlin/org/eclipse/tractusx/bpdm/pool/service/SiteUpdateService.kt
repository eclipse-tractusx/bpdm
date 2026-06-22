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
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.mapper.SiteEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.SiteContentParsed
import org.eclipse.tractusx.bpdm.pool.model.SiteUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.SiteUpdateParseError
import org.eclipse.tractusx.bpdm.pool.model.SiteUpdateRequest
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.zipParseResults
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Updates existing sites, the site counterpart of [AdditionalAddressUpdateService]. `parse` resolves the target site and
 * validates header + main-address content; `update` mutates the managed site and its main address, gating the save and
 * BOTH changelog entries on a whole-aggregate change (preserving the previous update behavior). The main address is
 * mutated via [AddressUpdateService.applyTo] — which deliberately does not save or emit its own changelog — so the site
 * keeps aggregate-level gating. Order-preserving positional contract (see [ParseResult]).
 */
@Service
class SiteUpdateService(
    private val siteHeaderParser: SiteHeaderParser,
    private val siteBpnParser: SiteBpnParser,
    private val addressUpdateService: AddressUpdateService,
    private val siteRepository: SiteRepository,
    private val changelogService: PartnerChangelogService,
    private val equivalenceMapper: BusinessPartnerEquivalenceMapper,
    private val siteEntityMapper: SiteEntityMapper
) {

    fun parse(requests: List<SiteUpdateRequest>): List<ParseResult<SiteUpdateParsed, SiteUpdateParseError>> {
        val targetResults = siteBpnParser.parseRequired(requests.map { it.siteBpn })
        val headerResults = siteHeaderParser.parse(requests.map { it.content.header })
        // The main address's duplicate check is scoped to its owning address; its BPN comes from the resolved target.
        val ownerBpns = targetResults.map { (it as? ParseResult.Success)?.parsed?.mainAddress?.bpn }
        val mainAddressResults = addressUpdateService.parseContent(requests.map { it.content.mainAddress }, ownerBpns)

        return zipParseResults(headerResults, targetResults, mainAddressResults) { header, target, mainAddress ->
            SiteUpdateParsed(target, SiteContentParsed(header, mainAddress))
        }
    }

    /**
     * Returns the updated entities (within the caller's transaction) rather than a detached response model: building
     * version-specific responses is the job of the border/application service at the edge.
     */
    @Transactional
    fun update(parsed: List<SiteUpdateParsed>): List<UpsertResult<SiteDb>> =
        parsed.map { update(it) }

    private fun update(parsed: SiteUpdateParsed): UpsertResult<SiteDb> {
        val target = parsed.target

        val before = equivalenceMapper.toEquivalenceDto(target)
        applyTo(target, parsed.content)
        val after = equivalenceMapper.toEquivalenceDto(target)

        val upsertType = if (before != after) {
            siteRepository.save(target)
            changelogService.createChangelogEntries(listOf(ChangelogEntryCreateRequest(target.bpn, ChangelogType.UPDATE, BusinessPartnerType.SITE)))
            changelogService.createChangelogEntries(listOf(ChangelogEntryCreateRequest(target.mainAddress.bpn, ChangelogType.UPDATE, BusinessPartnerType.ADDRESS)))
            UpsertType.Updated
        } else {
            UpsertType.NoChange
        }

        return UpsertResult(target, upsertType)
    }

    private fun applyTo(target: SiteDb, content: SiteContentParsed) {
        val header = content.header
        target.name = header.name
        // The sharing-member count is Pool-maintained, not part of the update payload, so carry the current value forward.
        target.confidenceCriteria = siteEntityMapper.toConfidence(header.confidenceCriteria, target.confidenceCriteria.numberOfSharingMembers)
        target.states.replace(siteEntityMapper.toStates(header.states, target))
        target.scriptVariants.replace(siteEntityMapper.toScriptVariants(header.scriptVariants))

        val mainAddress = content.mainAddress
        addressUpdateService.applyTo(target.mainAddress, mainAddress.address, mainAddress.scriptVariants, target.mainAddress.confidenceCriteria.numberOfSharingMembers)
    }

    @Transactional
    fun parseAndUpdate(requests: List<SiteUpdateRequest>): List<ParseResult<UpsertResult<SiteDb>, SiteUpdateParseError>> {
        val parseResults = parse(requests)
        val updated = update(parseResults.filterIsInstance<ParseResult.Success<SiteUpdateParsed>>().map { it.parsed }).iterator()
        return parseResults.map { result ->
            when (result) {
                is ParseResult.Success -> ParseResult.Success(updated.next())
                is ParseResult.Failure -> result
            }
        }
    }
}
