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
import org.eclipse.tractusx.bpdm.pool.model.ChangelogRecord
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.mapper.entity.AddressUpdateMapper
import org.eclipse.tractusx.bpdm.pool.model.update.AddressContentUpdate
import org.eclipse.tractusx.bpdm.pool.model.update.AddressUpdate
import org.eclipse.tractusx.bpdm.pool.model.update.FieldUpdate
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteCreateWithReferencedAddressAsMainParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteHeaderCreateParsed
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The single authority for creating a site whose main address is an *existing* address referenced by BPN, re-parenting
 * that address onto the new site (the cleaning/task path that promotes an additional address to a site main address).
 * Unlike an ordinary site create it builds no new address and issues no address BPN, but it does emit an ADDRESS
 * changelog for the re-parented address.
 *
 * A parse that states the referenced address's content also applies it: a site arriving with its own view of that
 * address is the current golden record for it. A parse without content leaves the address as it stands. Several sites
 * may be created on one address in a single call, in which case at most one of them may state that address's content.
 */
@Service
class SiteCreateWithReferencedAddressAsMainService(
    private val addressUpdateService: AddressUpdateService,
    private val siteHeaderTransientCreateService: SiteHeaderTransientCreateService,
    private val addressUpdateMapper: AddressUpdateMapper,
    private val siteRepository: SiteRepository,
    private val changelogCreateService: ChangelogCreateService
) {

    /**
     * Creates the given sites on their referenced main addresses and returns the persisted entities.
     */
    @Transactional
    fun create(parsed: List<SiteCreateWithReferencedAddressAsMainParsed>): List<SiteDb> {

        val sites = siteHeaderTransientCreateService.createTransiently(parsed.map { SiteHeaderCreateParsed(it.mainAddress.legalEntity!!, it.siteHeader) })

        // Several sites may name the same main address, so the address is staged once for all of them: staging it per
        // site would write and log that one address several times over.
        val stagedAddressUpdates = parsed.zip(sites)
            .groupBy { (entry, _) -> entry.mainAddress.bpn }
            .mapValues { (_, ofOneAddress) ->
                addressUpdateService.stageUpdate(AddressUpdate(ofOneAddress.first().first.mainAddress, mainAddressUpdate(ofOneAddress)))
            }
        parsed.zip(sites).forEach { (entry, site) -> site.mainAddress = stagedAddressUpdates.getValue(entry.mainAddress.bpn).address }

        siteRepository.saveAll(sites)
        changelogCreateService.record(sites.map { ChangelogRecord(it.bpn, ChangelogType.CREATE, BusinessPartnerType.SITE) })

        addressUpdateService.commit(stagedAddressUpdates.values.toList())

        return sites
    }

    private fun mainAddressUpdate(ofOneAddress: List<Pair<SiteCreateWithReferencedAddressAsMainParsed, SiteDb>>): AddressContentUpdate {
        val sites = ofOneAddress.map { (_, site) -> site }
        val statedContent = ofOneAddress.mapNotNull { (entry, _) -> entry.mainAddressContent }.singleOrNull()

        return statedContent
            ?.let { addressUpdateMapper.toFullUpdate(it, assignToSites = sites) }
            ?: AddressContentUpdate.NoOp.copy(assignToSites = FieldUpdate.Set(sites))
    }
}
