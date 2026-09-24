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


package org.eclipse.tractusx.bpdm.pool.service.operation.task

import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressSiteMembershipParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteCreateWithReferencedAddressAsMainParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.AdditionalSitesParsed
import org.eclipse.tractusx.bpdm.pool.service.operation.address.AddressUpdateService
import org.eclipse.tractusx.bpdm.pool.service.operation.site.SiteCreateWithReferencedAddressAsMainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Puts the sites a parsed golden record upsert states on the address the record shares.
 *
 * The record's own site sits on that address whether or not the request repeats it, so it is a member of what is
 * written rather than something a request can leave out.
 */
@Service
class AdditionalSiteUpsertService(
    private val siteCreateWithReferencedAddressAsMainService: SiteCreateWithReferencedAddressAsMainService,
    private val addressUpdateService: AddressUpdateService
) {

    /**
     * Creates the sites [additionalSites] states anew on [recordAddress], makes that address the main address of exactly those
     * sites and [recordSite], and reports the created sites in the order [additionalSites] states them.
     */
    @Transactional
    fun upsert(additionalSites: AdditionalSitesParsed, recordSite: SiteDb, recordAddress: LogisticAddressDb): List<SiteDb> {
        val createdSites = siteCreateWithReferencedAddressAsMainService.create(
            additionalSites.newSites.map { SiteCreateWithReferencedAddressAsMainParsed(recordAddress, it.header, mainAddressContent = null) }
        )

        val membership = (listOf(recordSite) + additionalSites.existingSites + createdSites).distinctBy { it.bpn }
        addressUpdateService.setSites(listOf(AddressSiteMembershipParsed(recordAddress, membership)))

        return createdSites
    }
}
