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
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.mapper.entity.SiteEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.SiteHeaderCreateParsed
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.eclipse.tractusx.bpdm.pool.service.BpnIssuingService
import org.eclipse.tractusx.bpdm.pool.service.PartnerChangelogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Creates the site *header* — the shared piece the site-create paths ([SiteCreateService],
 * [SiteCreateWithReferencedAddressAsMainService]) have in common: issue the
 * site BPN, map the header to an entity, and emit the SITE CREATE changelog. It deliberately does **not** create,
 * resolve, or attach a main address, and it does **not** persist the site.
 *
 * The returned [org.eclipse.tractusx.bpdm.pool.entity.SiteDb] is therefore incomplete — its `mainAddress` is unset, and a site cannot be flushed without its
 * non-null `main_address` FK. The caller must attach a main address and `save` the site before the transaction commits.
 * That half-built hand-off is a symptom of the site↔main-address model being a mutually-mandatory cycle; it is accepted
 * here as a bridge rather than duplicating the header logic across the three paths.
 *
 * The changelog is emitted here so a caller that additionally emits an ADDRESS changelog (for a newly created or
 * re-parented main address) does so *after*, preserving the "site, then its main address" order. Honours the
 * order-preserving positional list contract (see [org.eclipse.tractusx.bpdm.pool.model.ParseResult]).
 */
@Service
class SiteHeaderCreateService(
    private val bpnIssuingService: BpnIssuingService,
    private val changelogService: PartnerChangelogService,
    private val siteEntityMapper: SiteEntityMapper
) {

    /**
     * Builds the site headers under their (already-resolved) parents and emits the SITE CREATE changelog. Returns the
     * built-but-unsaved entities with `mainAddress` unset — the caller attaches the main address and persists (see the
     * class doc). Runs within the caller's transaction.
     */
    @Transactional
    fun create(parsed: List<SiteHeaderCreateParsed>): List<SiteDb> {
        val bpns = bpnIssuingService.issueSiteBpns(parsed.size)
        // A new site's confidence starts with one sharing member (preserves the previous create behavior).
        val sites = parsed.zip(bpns) { entry, bpn ->
            siteEntityMapper.toEntity(bpn, entry.legalEntity, entry.header, numberOfSharingMembers = 1)
        }

        changelogService.createChangelogEntries(sites.map {
            ChangelogEntryCreateRequest(it.bpn, ChangelogType.CREATE, BusinessPartnerType.SITE)
        })

        return sites
    }
}