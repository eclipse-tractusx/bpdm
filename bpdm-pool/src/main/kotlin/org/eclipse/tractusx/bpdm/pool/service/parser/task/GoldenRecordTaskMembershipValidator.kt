/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.model.error.AddressSiteMembershipParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.AdditionalAddressUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.AdditionalSitesParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.GoldenRecordTaskError
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.LegalEntityUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.SiteUpsertParsed
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteLegalEntityConsistencyValidator
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteMainAddressConsistencyValidator
import org.springframework.stereotype.Service

/**
 * Reports a site membership a golden record task states on its record address that the address cannot take: an existing
 * site of another legal entity, or a site the address is the main address of left out.
 */
@Service
class GoldenRecordTaskMembershipValidator(
    private val siteLegalEntityConsistencyValidator: SiteLegalEntityConsistencyValidator,
    private val siteMainAddressConsistencyValidator: SiteMainAddressConsistencyValidator
) {

    /**
     * Reports every problem with the sites the task states on its record address: [additionalAddress] when the task has
     * one, or else its site's main address.
     */
    fun validate(
        legalEntity: LegalEntityUpsertParsed,
        site: SiteUpsertParsed,
        additionalAddress: AdditionalAddressUpsertParsed?,
        additionalSites: AdditionalSitesParsed
    ): List<GoldenRecordTaskError> {
        val statedSites = listOfNotNull(site.existingSite) + additionalSites.existingSites
        val recordAddress = if (additionalAddress != null) additionalAddress.existingAddress else existingMainAddress(site, legalEntity)

        if (recordAddress != null) {
            val membershipErrors: List<AddressSiteMembershipParseError> =
                statedSites.flatMap { siteLegalEntityConsistencyValidator.check(recordAddress.legalEntity, it) } +
                        siteMainAddressConsistencyValidator.check(recordAddress, statedSites)
            return membershipErrors.map { GoldenRecordTaskError.AdditionalSiteInvalid(it) }
        }

        // The record address is new, so it belongs to the task's legal entity.
        val existingLegalEntity = legalEntity.existingLegalEntity
            ?: return statedSites.map { GoldenRecordTaskError.AdditionalSiteOfOtherLegalEntity(it.bpn) }
        return statedSites
            .flatMap { siteLegalEntityConsistencyValidator.check(existingLegalEntity, it) }
            .map { GoldenRecordTaskError.AdditionalSiteInvalid(it) }
    }

    private fun existingMainAddress(site: SiteUpsertParsed, legalEntity: LegalEntityUpsertParsed): LogisticAddressDb? =
        when (site) {
            is SiteUpsertParsed.Unchanged -> site.site.mainAddress
            is SiteUpsertParsed.Update -> site.update.target.mainAddress
            is SiteUpsertParsed.CreateOnExistingAddress -> site.create.mainAddress
            is SiteUpsertParsed.CreateOnLegalAddress -> legalEntity.existingLegalEntity?.legalAddress
            is SiteUpsertParsed.CreateWithOwnMainAddress -> null
        }
}
