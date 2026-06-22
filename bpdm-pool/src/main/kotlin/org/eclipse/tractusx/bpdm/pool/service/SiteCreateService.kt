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
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.SiteContentParsed
import org.eclipse.tractusx.bpdm.pool.model.SiteCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.SiteCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.SiteCreateRequest
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.zipParseResults
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Creates sites under an existing legal entity, the site counterpart of [AdditionalAddressCreateService]. `parse` resolves
 * the legal-entity parent and validates header + main-address content (each by a single-responsibility parser, combined
 * with `zipParseResults`); `create` persists the site and its main address. Sites always attach to a persisted legal
 * entity, so no parent-injected lower layer is needed — the main address (whose parent is the still-unsaved site) is
 * delegated to the parent-injected [AddressCreateService]. Order-preserving positional contract (see [ParseResult]).
 */
@Service
class SiteCreateService(
    private val siteHeaderParser: SiteHeaderParser,
    private val legalEntityBpnParser: LegalEntityBpnParser,
    private val addressCreateService: AddressCreateService,
    private val bpnIssuingService: BpnIssuingService,
    private val siteRepository: SiteRepository,
    private val changelogService: PartnerChangelogService,
    private val siteEntityMapper: SiteEntityMapper
) {

    fun parse(requests: List<SiteCreateRequest>): List<ParseResult<SiteCreateParsed, SiteCreateParseError>> {
        val headerResults = siteHeaderParser.parse(requests.map { it.content.header })
        val legalEntityResults = legalEntityBpnParser.parse(requests.map { it.legalEntityBpn })
        val mainAddressResults = addressCreateService.parseContent(requests.map { it.content.mainAddress })

        return zipParseResults(headerResults, legalEntityResults, mainAddressResults) { header, legalEntity, mainAddress ->
            SiteCreateParsed(legalEntity, SiteContentParsed(header, mainAddress))
        }
    }

    /**
     * Returns the persisted entities (within the caller's transaction) rather than a detached response model: building
     * version-specific responses is the job of the border/application service at the edge.
     */
    @Transactional
    fun create(parsed: List<SiteCreateParsed>): List<SiteDb> {
        val bpns = bpnIssuingService.issueSiteBpns(parsed.size)
        // A new site's confidence starts with one sharing member (preserves the previous create behavior).
        val sites = parsed.zip(bpns) { entry, bpn -> siteEntityMapper.toEntity(bpn, entry, numberOfSharingMembers = 1) }

        // Emit the site changelog before the address create service emits the ADDRESS CREATE changelog, so the overall
        // changelog order stays "site, then its main address".
        changelogService.createChangelogEntries(sites.map {
            ChangelogEntryCreateRequest(it.bpn, ChangelogType.CREATE, BusinessPartnerType.SITE)
        })

        // The main address's parent is the still-unsaved site; it flushes in the right order at commit thanks to the
        // nullable back-FK and order_inserts. The address create service owns the address BPN + ADDRESS CREATE changelog.
        val mainAddresses = addressCreateService.create(parsed.zip(sites).map { (entry, site) ->
            val mainAddress = entry.content.mainAddress
            AddressCreateParsed(site.legalEntity, site, mainAddress.address, mainAddress.scriptVariants)
        })
        sites.zip(mainAddresses).forEach { (site, address) -> site.mainAddress = address }

        siteRepository.saveAll(sites)
        return sites
    }

    @Transactional
    fun parseAndCreate(requests: List<SiteCreateRequest>): List<ParseResult<SiteDb, SiteCreateParseError>> {
        val parseResults = parse(requests)
        val created = create(parseResults.filterIsInstance<ParseResult.Success<SiteCreateParsed>>().map { it.parsed }).iterator()
        return parseResults.map { result ->
            when (result) {
                is ParseResult.Success -> ParseResult.Success(created.next())
                is ParseResult.Failure -> result
            }
        }
    }
}
