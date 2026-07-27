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
import org.eclipse.tractusx.bpdm.pool.model.AddressContentUpdate
import org.eclipse.tractusx.bpdm.pool.model.AddressUpdate
import org.eclipse.tractusx.bpdm.pool.model.FieldUpdate
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteCreateWithReferencedAddressAsMainParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteHeaderCreateParsed
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.eclipse.tractusx.bpdm.pool.service.PartnerChangelogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The single authority for creating a site whose main address is an *existing* address referenced by BPN, re-parenting
 * that address onto the new site (the cleaning/task path that promotes an additional address to a site main address).
 * Unlike an ordinary site create it builds no new address and issues no address BPN, but it does emit an ADDRESS
 * changelog for the re-parented address.
 */
@Service
class SiteCreateWithReferencedAddressAsMainService(
    private val addressUpdateService: AddressUpdateService,
    private val siteHeaderTransientCreateService: SiteHeaderTransientCreateService,
    private val siteRepository: SiteRepository,
    private val changelogService: PartnerChangelogService
) {

    @Transactional
    fun create(parsed: List<SiteCreateWithReferencedAddressAsMainParsed>): List<SiteDb> {

        val sites = siteHeaderTransientCreateService.createTransiently(parsed.map { SiteHeaderCreateParsed(it.mainAddress.legalEntity!!, it.siteHeader) })

        val stagedAddressUpdates = parsed.zip(sites).map { (entry, site) ->
            addressUpdateService.stageUpdate(
                AddressUpdate(entry.mainAddress, AddressContentUpdate.NoOp.copy(assignToSite = FieldUpdate.Set(site)))
            )
        }
        sites.zip(stagedAddressUpdates).forEach { (site, stagedAddressUpdate) -> site.mainAddress = stagedAddressUpdate.address }

        siteRepository.saveAll(sites)
        changelogService.createChangelogEntries(sites.map { ChangelogEntryCreateRequest(it.bpn, ChangelogType.CREATE, BusinessPartnerType.SITE) })

        addressUpdateService.commit(stagedAddressUpdates)

        return sites
    }
}
