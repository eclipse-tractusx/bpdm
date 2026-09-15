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

package org.eclipse.tractusx.bpdm.pool.service.parser.task

import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.model.error.MembershipSiteNotInLegalEntity
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressUpsertPlan
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityUpsertPlan
import org.eclipse.tractusx.bpdm.pool.model.parsed.RecordSitePlan
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteUpsertPlan
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteLegalEntityConsistencyValidator
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteMainAddressConsistencyValidator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Completes and checks the sites that will sit on the address a golden record upsert is about.
 *
 * Writing the membership replaces the address's current sites rather than adding to them, so a site bound to the
 * address by its own main-address relation is kept whether or not the request states it. A site of another legal
 * entity is refused instead, as the address belongs to the request's own legal entity.
 */
@Service
class RecordAddressSitesParser(
    private val siteMainAddressConsistencyValidator: SiteMainAddressConsistencyValidator,
    private val siteLegalEntityConsistencyValidator: SiteLegalEntityConsistencyValidator
) {

    /**
     * Reports the record's site with its stated membership extended by every site already bound to the record address.
     */
    @Transactional(readOnly = true)
    fun withBoundSites(
        legalEntity: LegalEntityUpsertPlan?,
        recordSite: RecordSitePlan?,
        additionalAddress: AddressUpsertPlan?
    ): RecordSitePlan? {
        if (recordSite == null) return null
        val recordAddress = recordAddressTarget(legalEntity, recordSite.site, additionalAddress) ?: return recordSite

        // The record's own site holds the record address whether or not the request repeats it.
        val statedSites = recordSite.coLocatedSites.existingSites.plus(listOfNotNull(siteTarget(recordSite.site)))
        val boundSites = siteMainAddressConsistencyValidator.omittedSites(recordAddress, statedSites)

        return recordSite.copy(
            coLocatedSites = recordSite.coLocatedSites.copy(
                existingSites = recordSite.coLocatedSites.existingSites + boundSites
            )
        )
    }

    /**
     * Reports each site the request states as sitting on its record address that belongs to another legal entity.
     */
    @Transactional(readOnly = true)
    fun validate(legalEntity: LegalEntityUpsertPlan?, recordSite: RecordSitePlan?): List<MembershipSiteNotInLegalEntity> {
        val statedSites = recordSite?.coLocatedSites?.existingSites ?: return emptyList()
        val legalEntityTarget = legalEntityTarget(legalEntity)
            // A legal entity this request creates has no sites yet, so every site already persisted is another's.
            ?: return statedSites.map { MembershipSiteNotInLegalEntity(it.bpn, null) }

        return statedSites
            .flatMap { siteLegalEntityConsistencyValidator.check(legalEntityTarget, it) }
            .map { MembershipSiteNotInLegalEntity(it.siteBpn, it.legalEntityBpn) }
    }

    private fun legalEntityTarget(plan: LegalEntityUpsertPlan?): LegalEntityDb? =
        when (plan) {
            is LegalEntityUpsertPlan.Unchanged -> plan.target
            is LegalEntityUpsertPlan.Update -> plan.target
            is LegalEntityUpsertPlan.Create, null -> null
        }

    private fun siteTarget(site: SiteUpsertPlan): SiteDb? =
        when (site) {
            is SiteUpsertPlan.Unchanged -> site.target
            is SiteUpsertPlan.UpdateWithOwnMainAddress -> site.target
            is SiteUpsertPlan.UpdateOnLegalAddress -> site.parsed.target
            is SiteUpsertPlan.CreateWithOwnMainAddress,
            is SiteUpsertPlan.CreateOnLegalAddress,
            is SiteUpsertPlan.CreateOnExistingAddress -> null
        }

    private fun recordAddressTarget(
        legalEntity: LegalEntityUpsertPlan?,
        site: SiteUpsertPlan?,
        additionalAddress: AddressUpsertPlan?
    ): LogisticAddressDb? =
        (additionalAddress as? AddressUpsertPlan.Update)?.target
            ?: siteMainAddressTarget(site)
            ?: legalEntityTarget(legalEntity)?.legalAddress

    private fun siteMainAddressTarget(site: SiteUpsertPlan?): LogisticAddressDb? =
        when (site) {
            is SiteUpsertPlan.Unchanged -> site.target.mainAddress
            is SiteUpsertPlan.CreateOnExistingAddress -> site.parsed.mainAddress
            is SiteUpsertPlan.UpdateWithOwnMainAddress -> site.target.mainAddress
            is SiteUpsertPlan.UpdateOnLegalAddress -> site.parsed.target.mainAddress
            is SiteUpsertPlan.CreateWithOwnMainAddress, is SiteUpsertPlan.CreateOnLegalAddress, null -> null
        }
}
