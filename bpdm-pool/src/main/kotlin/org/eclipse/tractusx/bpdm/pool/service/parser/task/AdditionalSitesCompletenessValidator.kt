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
import org.eclipse.tractusx.bpdm.pool.model.error.AdditionalSiteOmitted
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressUpsertPlan
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityUpsertPlan
import org.eclipse.tractusx.bpdm.pool.model.parsed.RecordSitePlan
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteUpsertPlan
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteMainAddressConsistencyValidator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The rule that a golden record upsert states every site its record address is the main address of.
 *
 * Writing the membership replaces the address's current sites rather than adding to them, and a site bound to the
 * address by its own main-address relation cannot be unlinked by a statement about membership. Completing the list is
 * the refinement service's job, so the Pool reports what it left out rather than filling it in.
 */
@Service
class AdditionalSitesCompletenessValidator(
    private val siteMainAddressConsistencyValidator: SiteMainAddressConsistencyValidator
) {

    /**
     * Reports every site the record address is the main address of that [recordSite] leaves out.
     */
    @Transactional(readOnly = true)
    fun validate(
        legalEntity: LegalEntityUpsertPlan?,
        recordSite: RecordSitePlan?,
        additionalAddress: AddressUpsertPlan?
    ): List<AdditionalSiteOmitted> {
        if (recordSite == null) return emptyList()
        // An address this request creates is not yet the main address of anything, so there is nothing to leave out.
        val recordAddress = existingRecordAddress(legalEntity, recordSite.site, additionalAddress) ?: return emptyList()

        // The record's own site holds the record address whether or not the request repeats it.
        val statedSites = recordSite.additionalSites.existingSites.plus(listOfNotNull(existingSite(recordSite.site)))

        return siteMainAddressConsistencyValidator.findOmittedSites(recordAddress, statedSites).map { AdditionalSiteOmitted(it.bpn) }
    }

    private fun existingLegalEntity(legalEntity: LegalEntityUpsertPlan?): LegalEntityDb? =
        when (legalEntity) {
            is LegalEntityUpsertPlan.Unchanged -> legalEntity.existingLegalEntity
            is LegalEntityUpsertPlan.Update -> legalEntity.existingLegalEntity
            is LegalEntityUpsertPlan.Create, null -> null
        }

    private fun existingSite(site: SiteUpsertPlan): SiteDb? =
        when (site) {
            is SiteUpsertPlan.Unchanged -> site.existingSite
            is SiteUpsertPlan.UpdateWithOwnMainAddress -> site.existingSite
            is SiteUpsertPlan.UpdateOnLegalAddress -> site.update.target
            is SiteUpsertPlan.CreateWithOwnMainAddress,
            is SiteUpsertPlan.CreateOnLegalAddress,
            is SiteUpsertPlan.CreateOnExistingAddress -> null
        }

    private fun existingRecordAddress(
        legalEntity: LegalEntityUpsertPlan?,
        site: SiteUpsertPlan?,
        additionalAddress: AddressUpsertPlan?
    ): LogisticAddressDb? =
        (additionalAddress as? AddressUpsertPlan.Update)?.existingAddress
            ?: existingSiteMainAddress(site)
            ?: existingLegalEntity(legalEntity)?.legalAddress

    private fun existingSiteMainAddress(site: SiteUpsertPlan?): LogisticAddressDb? =
        when (site) {
            is SiteUpsertPlan.Unchanged -> site.existingSite.mainAddress
            is SiteUpsertPlan.CreateOnExistingAddress -> site.creation.mainAddress
            is SiteUpsertPlan.UpdateWithOwnMainAddress -> site.existingSite.mainAddress
            is SiteUpsertPlan.UpdateOnLegalAddress -> site.update.target.mainAddress
            is SiteUpsertPlan.CreateWithOwnMainAddress, is SiteUpsertPlan.CreateOnLegalAddress, null -> null
        }
}
