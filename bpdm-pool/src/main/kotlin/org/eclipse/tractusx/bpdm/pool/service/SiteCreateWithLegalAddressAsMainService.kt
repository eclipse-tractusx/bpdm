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
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.mapper.SiteEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.LegalAddressAlreadyMainAddress
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.SiteCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.SiteCreateWithLegalAddressAsMainParsed
import org.eclipse.tractusx.bpdm.pool.model.SiteCreateWithLegalAddressAsMainRequest
import org.eclipse.tractusx.bpdm.pool.model.combine
import org.eclipse.tractusx.bpdm.pool.model.parseAndExecute
import org.eclipse.tractusx.bpdm.pool.model.zipParseResults
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Creates sites whose main address is the parent legal entity's *existing* legal address (the V7 "site with legal
 * reference" path), the legal-address-reuse counterpart of [SiteCreateService]. `parse` resolves the legal-entity parent
 * and validates the header (each by a single-responsibility parser), and additionally rejects a legal address that
 * already backs another site ([LegalAddressAlreadyMainAddress]). `create` issues the site BPN and persists the site, then
 * reuses the legal address as its main address — so, unlike [SiteCreateService], it builds no new address, issues no
 * address BPN and emits no ADDRESS changelog. Order-preserving positional contract (see [ParseResult]).
 */
@Service
class SiteCreateWithLegalAddressAsMainService(
    private val siteHeaderParser: SiteHeaderParser,
    private val legalEntityBpnParser: LegalEntityBpnParser,
    private val bpnIssuingService: BpnIssuingService,
    private val siteRepository: SiteRepository,
    private val changelogService: PartnerChangelogService,
    private val siteEntityMapper: SiteEntityMapper
) {

    fun parse(
        requests: List<SiteCreateWithLegalAddressAsMainRequest>
    ): List<ParseResult<SiteCreateWithLegalAddressAsMainParsed, SiteCreateParseError>> {
        val headerResults = siteHeaderParser.parse(requests.map { it.header })

        // Fold the "legal address already in use" check onto the resolved parent: an already-attached legal address can't
        // back a second site. The check needs the resolved entity, so it rides on the legal-entity parse result.
        val legalEntityResults = legalEntityBpnParser.parse(requests.map { it.legalEntityBpn }).map { result ->
            val alreadyUsedErrors: List<SiteCreateParseError> = when (result) {
                is ParseResult.Success -> result.parsed.legalAddress.sites.firstOrNull()
                    ?.let { listOf(LegalAddressAlreadyMainAddress(it.bpn)) } ?: emptyList()
                is ParseResult.Failure -> emptyList()
            }
            result.combine(alreadyUsedErrors) { it }
        }

        return zipParseResults(legalEntityResults, headerResults) { legalEntity, header ->
            SiteCreateWithLegalAddressAsMainParsed(legalEntity, header)
        }
    }

    /**
     * Returns the persisted entities (within the caller's transaction) rather than a detached response model: building
     * version-specific responses is the job of the border/application service at the edge.
     */
    @Transactional
    fun create(parsed: List<SiteCreateWithLegalAddressAsMainParsed>): List<SiteDb> {
        val bpns = bpnIssuingService.issueSiteBpns(parsed.size)
        // A new site's confidence starts with one sharing member (preserves the previous create behavior).
        val sites = parsed.zip(bpns) { entry, bpn ->
            siteEntityMapper.toEntity(bpn, entry.legalEntity, entry.header, numberOfSharingMembers = 1).apply {
                // Reuse the legal entity's existing legal address as the main address instead of creating a new one.
                mainAddress = entry.legalEntity.legalAddress
                mainAddress.sites.add(this)
            }
        }

        // Only a SITE changelog: the reused legal address already exists, so no ADDRESS CREATE is emitted.
        changelogService.createChangelogEntries(sites.map {
            ChangelogEntryCreateRequest(it.bpn, ChangelogType.CREATE, BusinessPartnerType.SITE)
        })

        siteRepository.saveAll(sites)
        return sites
    }

    @Transactional
    fun parseAndCreate(
        requests: List<SiteCreateWithLegalAddressAsMainRequest>
    ): List<ParseResult<SiteDb, SiteCreateParseError>> =
        parseAndExecute(requests, ::parse, ::create)
}
