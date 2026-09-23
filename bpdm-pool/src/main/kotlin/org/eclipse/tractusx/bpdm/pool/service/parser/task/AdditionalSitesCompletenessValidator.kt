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
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.RecordAddressSitesParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteUpsertParsed
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
     * Reports every site the record address is the main address of that [recordAddressSites] leaves out.
     */
    @Transactional(readOnly = true)
    fun validate(
        legalEntity: LegalEntityUpsertParsed?,
        recordAddressSites: RecordAddressSitesParsed?,
        additionalAddress: AddressUpsertParsed?
    ): List<AdditionalSiteOmitted> {
        if (recordAddressSites == null) return emptyList()
        // An address this request creates is not yet the main address of anything, so there is nothing to leave out.
        val recordAddress = existingRecordAddress(legalEntity, recordAddressSites.recordSite, additionalAddress) ?: return emptyList()

        // The record's own site holds the record address whether or not the request repeats it.
        val statedSites = recordAddressSites.additionalSites.existingSites
            .plus(listOfNotNull(existingSite(recordAddressSites.recordSite)))

        return siteMainAddressConsistencyValidator.findOmittedSites(recordAddress, statedSites).map { AdditionalSiteOmitted(it.bpn) }
    }

    private fun existingLegalEntity(legalEntity: LegalEntityUpsertParsed?): LegalEntityDb? =
        when (legalEntity) {
            is LegalEntityUpsertParsed.Unchanged -> legalEntity.existingLegalEntity
            is LegalEntityUpsertParsed.Update -> legalEntity.update.target
            is LegalEntityUpsertParsed.Create, null -> null
        }

    private fun existingSite(site: SiteUpsertParsed): SiteDb? =
        when (site) {
            is SiteUpsertParsed.Unchanged -> site.existingSite
            is SiteUpsertParsed.UpdateWithOwnMainAddress -> site.existingSite
            is SiteUpsertParsed.UpdateOnLegalAddress -> site.existingSite
            is SiteUpsertParsed.CreateWithOwnMainAddress,
            is SiteUpsertParsed.CreateOnLegalAddress,
            is SiteUpsertParsed.CreateOnExistingAddress -> null
        }

    private fun existingRecordAddress(
        legalEntity: LegalEntityUpsertParsed?,
        site: SiteUpsertParsed?,
        additionalAddress: AddressUpsertParsed?
    ): LogisticAddressDb? =
        (additionalAddress as? AddressUpsertParsed.Update)?.existingAddress
            ?: existingSiteMainAddress(site)
            ?: existingLegalEntity(legalEntity)?.legalAddress

    private fun existingSiteMainAddress(site: SiteUpsertParsed?): LogisticAddressDb? =
        when (site) {
            is SiteUpsertParsed.Unchanged -> site.existingSite.mainAddress
            is SiteUpsertParsed.CreateOnExistingAddress -> site.existingMainAddress
            is SiteUpsertParsed.UpdateWithOwnMainAddress -> site.existingSite.mainAddress
            is SiteUpsertParsed.UpdateOnLegalAddress -> site.existingSite.mainAddress
            is SiteUpsertParsed.CreateWithOwnMainAddress, is SiteUpsertParsed.CreateOnLegalAddress, null -> null
        }
}
