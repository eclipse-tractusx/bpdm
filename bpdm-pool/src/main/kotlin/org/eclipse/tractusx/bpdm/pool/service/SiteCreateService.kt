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
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.SiteContentParsed
import org.eclipse.tractusx.bpdm.pool.model.SiteCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.SiteCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.SiteCreateRequest
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.SiteHeaderCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.parseAndExecute
import org.eclipse.tractusx.bpdm.pool.model.zipParseResults
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
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
    private val legalEntityBpnParser: LegalEntityBpnParser,
    private val addressCreateService: AddressCreateService,
    private val siteHeaderCreateService: SiteHeaderCreateService,
    private val siteRepository: SiteRepository
) {

    fun parse(requests: List<SiteCreateRequest>): List<ParseResult<SiteCreateParsed, SiteCreateParseError>> {
        val headerResults = siteHeaderCreateService.parse(requests.map { it.content.header })
        val legalEntityResults = legalEntityBpnParser.parse(requests.map { it.legalEntityBpn })
        val mainAddressResults = addressCreateService.parse(requests.map { it.content.mainAddress })

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
        val sites = siteHeaderCreateService.create(parsed.map { SiteHeaderCreateParsed(it.legalEntity, it.content.header) })

        val mainAddresses = addressCreateService.create(parsed.zip(sites).map { (entry, site) ->
            val mainAddress = entry.content.mainAddress
            AddressCreateParsed(site.legalEntity, site, mainAddress.address, mainAddress.scriptVariants)
        })

        // The sites are already persistent (saved by siteHeaderCreateService); setting mainAddress mutates the managed
        // entities, so dirty tracking flushes the main_address FK at commit — same as the other site-create paths.
        sites.zip(mainAddresses).forEach { (site, address) -> site.mainAddress = address }

        siteRepository.saveAll(sites)

        return sites
    }

    @Transactional
    fun parseAndCreate(requests: List<SiteCreateRequest>): List<ParseResult<SiteDb, SiteCreateParseError>> {
        return parseAndExecute(requests, ::parse, ::create)
    }
}
