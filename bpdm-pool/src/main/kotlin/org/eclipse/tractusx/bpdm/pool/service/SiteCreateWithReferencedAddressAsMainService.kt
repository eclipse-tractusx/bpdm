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
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.SiteCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.SiteCreateWithReferencedAddressAsMainParsed
import org.eclipse.tractusx.bpdm.pool.model.SiteCreateWithReferencedAddressAsMainRequest
import org.eclipse.tractusx.bpdm.pool.model.parseAndExecute
import org.eclipse.tractusx.bpdm.pool.model.zipParseResults
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Creates sites whose main address is an *existing* address referenced by BPN (the cleaning/task path that turns an
 * additional address into a site main address), the address-reuse counterpart of [SiteCreateService]. `parse` resolves
 * the legal-entity parent, resolves the referenced address (the re-parent target), and validates the header and main
 * address content — the latter as an update of the referenced address (so it may keep its own identifiers). `create`
 * issues the site BPN and persists the site, re-parents the referenced address onto it as its main address and overwrites
 * its content — so, unlike [SiteCreateService], it builds no new address and issues no address BPN (it does emit an
 * ADDRESS changelog, preserving the previous behavior). Order-preserving positional contract (see [ParseResult]).
 */
@Service
class SiteCreateWithReferencedAddressAsMainService(
    private val siteHeaderParser: SiteHeaderParser,
    private val addressBpnParser: AddressBpnParser,
    private val addressUpdateService: AddressUpdateService,
    private val bpnIssuingService: BpnIssuingService,
    private val siteRepository: SiteRepository,
    private val changelogService: PartnerChangelogService,
    private val siteEntityMapper: SiteEntityMapper
) {

    fun parse(
        requests: List<SiteCreateWithReferencedAddressAsMainRequest>
    ): List<ParseResult<SiteCreateWithReferencedAddressAsMainParsed, SiteCreateParseError>> {
        val headerResults = siteHeaderParser.parse(requests.map { it.content.header })
        val mainAddressTargetResults = addressBpnParser.parse(requests.map { it.mainAddressBpn })

        return zipParseResults(headerResults, mainAddressTargetResults) {
            header, mainAddress -> SiteCreateWithReferencedAddressAsMainParsed(mainAddress, header)
        }
    }

    /**
     * Returns the persisted entities (within the caller's transaction) rather than a detached response model: building
     * version-specific responses is the job of the border/application service at the edge.
     */
    @Transactional
    fun create(parsed: List<SiteCreateWithReferencedAddressAsMainParsed>): List<SiteDb> {
        val bpns = bpnIssuingService.issueSiteBpns(parsed.size)
        // A new site's confidence starts with one sharing member (preserves the previous create behavior).
        val sites = parsed.zip(bpns) { entry, bpn ->
            val site = siteEntityMapper.toEntity(bpn, entry.mainAddress.legalEntity!!, entry.siteHeader, numberOfSharingMembers = 1)
            val mainAddress = entry.mainAddress

            mainAddress.sites.add(site)
            site.mainAddress = mainAddress
            site
        }

        // Mirror the previous behavior's changelog order: site first, then its (now re-parented) main address.
        changelogService.createChangelogEntries(sites.map {
            ChangelogEntryCreateRequest(it.bpn, ChangelogType.CREATE, BusinessPartnerType.SITE)
        })
        changelogService.createChangelogEntries(sites.map {
            ChangelogEntryCreateRequest(it.mainAddress.bpn, ChangelogType.UPDATE, BusinessPartnerType.ADDRESS)
        })

        siteRepository.saveAll(sites)
        return sites
    }

    @Transactional
    fun parseAndCreate(
        requests: List<SiteCreateWithReferencedAddressAsMainRequest>
    ): List<ParseResult<SiteDb, SiteCreateParseError>> =
        parseAndExecute(requests, ::parse, ::create)
}
