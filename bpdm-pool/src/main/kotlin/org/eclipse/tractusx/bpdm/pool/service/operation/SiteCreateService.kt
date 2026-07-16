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
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.SiteCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.SiteHeaderCreateParsed
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.eclipse.tractusx.bpdm.pool.service.PartnerChangelogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The single authority for creating sites under an existing legal entity together with a newly created main address:
 * issues the site BPN, builds and persists the site and its main address, and emits their CREATE changelogs. Returns
 * managed entities in the caller's transaction, not response models.
 */
@Service
class SiteCreateService(
    private val addressCreateService: AddressCreateService,
    private val siteHeaderTransientCreateService: SiteHeaderTransientCreateService,
    private val siteRepository: SiteRepository,
    private val changelogService: PartnerChangelogService
) {

    @Transactional
    fun create(parsed: List<SiteCreateParsed>): List<SiteDb> {
        val sites = siteHeaderTransientCreateService.createTransiently(parsed.map { SiteHeaderCreateParsed(it.legalEntity, it.content.header) })
        val stagedAddresses = addressCreateService.stageCreate(parsed.zip(sites).map { (entry, site) ->
            val mainAddress = entry.content.mainAddress
            AddressCreateParsed(site.legalEntity, site, mainAddress.address, mainAddress.scriptVariants)
        })

        sites.zip(stagedAddresses).forEach { (site, stagedAddress) -> site.mainAddress = stagedAddress.address }

        siteRepository.saveAll(sites)
        changelogService.createChangelogEntries(sites.map { ChangelogEntryCreateRequest(it.bpn, ChangelogType.CREATE, BusinessPartnerType.SITE) })

        addressCreateService.commit(stagedAddresses)

        return sites
    }
}
