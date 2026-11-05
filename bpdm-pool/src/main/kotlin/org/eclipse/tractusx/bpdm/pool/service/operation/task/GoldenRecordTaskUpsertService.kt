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

package org.eclipse.tractusx.bpdm.pool.service.operation.task

import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressSiteMembershipParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.address.AddressCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.SiteCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.SiteCreateWithReferencedAddressAsMainParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.AdditionalAddressUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.AdditionalSitesParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.BpnReferenceParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.GoldenRecordTaskParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.LegalEntityUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.SiteUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.UpsertedGoldenRecordParsed
import org.eclipse.tractusx.bpdm.pool.service.operation.address.AddressCreateService
import org.eclipse.tractusx.bpdm.pool.service.operation.address.AddressPayloadUpdateService
import org.eclipse.tractusx.bpdm.pool.service.operation.address.AddressUpdateService
import org.eclipse.tractusx.bpdm.pool.service.operation.bpn.BpnRequestIdentifierMappingCreateService
import org.eclipse.tractusx.bpdm.pool.service.operation.legalentity.LegalEntityCreateService
import org.eclipse.tractusx.bpdm.pool.service.operation.legalentity.LegalEntityPayloadUpdateService
import org.eclipse.tractusx.bpdm.pool.service.operation.participation.SharingMemberConfidenceService
import org.eclipse.tractusx.bpdm.pool.service.operation.site.SiteCreateService
import org.eclipse.tractusx.bpdm.pool.service.operation.site.SiteCreateWithReferencedAddressAsMainService
import org.eclipse.tractusx.bpdm.pool.service.operation.site.SitePayloadUpdateService
import org.springframework.stereotype.Service

/**
 * Carries out a golden record task: writes its business partners, their site membership and the sharing member's
 * confidence, and maps the task's new request identifiers to the BPNs issued for them.
 *
 * A request identifier already mapped keeps its BPN, and of two business partners stating the same new request
 * identifier the one written first gets it.
 */
@Service
class GoldenRecordTaskUpsertService(
    private val legalEntityCreateService: LegalEntityCreateService,
    private val legalEntityPayloadUpdateService: LegalEntityPayloadUpdateService,
    private val siteCreateService: SiteCreateService,
    private val siteCreateWithReferencedAddressAsMainService: SiteCreateWithReferencedAddressAsMainService,
    private val sitePayloadUpdateService: SitePayloadUpdateService,
    private val addressCreateService: AddressCreateService,
    private val addressPayloadUpdateService: AddressPayloadUpdateService,
    private val addressUpdateService: AddressUpdateService,
    private val sharingMemberConfidenceService: SharingMemberConfidenceService,
    private val bpnRequestIdentifierMappingCreateService: BpnRequestIdentifierMappingCreateService
) {

    /**
     * Writes what [task] decided and returns its business partners as they stand afterwards.
     */
    fun upsert(task: GoldenRecordTaskParsed): UpsertedGoldenRecordParsed {
        val issuedBpns = IssuedBpns()

        val legalEntity = upsertLegalEntity(task.legalEntity, issuedBpns)
        val site = task.site?.let { upsertSite(it, legalEntity, issuedBpns) }
        val additionalAddress = task.additionalAddress?.let { upsertAdditionalAddress(it, legalEntity, site, issuedBpns) }

        // The address the task is about: its additional address, or else its site's main address, or else its legal address.
        val recordAddress = additionalAddress ?: site?.mainAddress ?: legalEntity.legalAddress
        val additionalSites = if (site != null && task.additionalSites != null)
            setAddressSites(recordAddress, site, task.additionalSites, issuedBpns)
        else
            emptyList()

        sharingMemberConfidenceService.updateAddress(task.sharingMemberRecordId, recordAddress.bpn)
        bpnRequestIdentifierMappingCreateService.create(issuedBpns.byRequestIdentifier)

        return UpsertedGoldenRecordParsed(legalEntity, site, additionalAddress, additionalSites)
    }

    private fun upsertLegalEntity(legalEntity: LegalEntityUpsertParsed, issuedBpns: IssuedBpns): LegalEntityDb {
        val written = when (legalEntity) {
            is LegalEntityUpsertParsed.Unchanged -> return legalEntity.legalEntity
            is LegalEntityUpsertParsed.Create -> legalEntityCreateService.create(listOf(legalEntity.create)).single()
            is LegalEntityUpsertParsed.Update -> legalEntityPayloadUpdateService.update(listOf(legalEntity.update)).single().value
        }

        issuedBpns.record(legalEntity.reference, written.bpn)
        issuedBpns.record(legalEntity.legalAddressReference, written.legalAddress.bpn)
        return written
    }

    private fun upsertSite(site: SiteUpsertParsed, legalEntity: LegalEntityDb, issuedBpns: IssuedBpns): SiteDb {
        val written = when (site) {
            is SiteUpsertParsed.Unchanged -> return site.site
            is SiteUpsertParsed.CreateWithOwnMainAddress ->
                siteCreateService.create(listOf(SiteCreateParsed(legalEntity, site.content))).single()
                    .also { issuedBpns.record(site.mainAddressReference, it.mainAddress.bpn) }
            is SiteUpsertParsed.CreateOnLegalAddress ->
                siteCreateWithReferencedAddressAsMainService
                    .create(listOf(SiteCreateWithReferencedAddressAsMainParsed(legalEntity.legalAddress, site.header, mainAddressContent = null))).single()
            is SiteUpsertParsed.CreateOnExistingAddress ->
                siteCreateWithReferencedAddressAsMainService.create(listOf(site.create)).single()
                    .also { issuedBpns.record(site.mainAddressReference, it.mainAddress.bpn) }
            is SiteUpsertParsed.Update ->
                sitePayloadUpdateService.update(listOf(site.update)).single().value
                    .also { issuedBpns.record(site.mainAddressReference, it.mainAddress.bpn) }
        }

        issuedBpns.record(site.reference, written.bpn)
        return written
    }

    private fun upsertAdditionalAddress(
        additionalAddress: AdditionalAddressUpsertParsed,
        legalEntity: LegalEntityDb,
        site: SiteDb?,
        issuedBpns: IssuedBpns
    ): LogisticAddressDb {
        val written = when (additionalAddress) {
            is AdditionalAddressUpsertParsed.Create ->
                addressCreateService.create(listOf(AddressCreateParsed(legalEntity, site, additionalAddress.content))).single()
            is AdditionalAddressUpsertParsed.Update ->
                addressPayloadUpdateService.update(listOf(additionalAddress.update)).single().value
        }

        issuedBpns.record(additionalAddress.reference, written.bpn)
        return written
    }

    private fun setAddressSites(
        recordAddress: LogisticAddressDb,
        recordSite: SiteDb,
        additionalSites: AdditionalSitesParsed,
        issuedBpns: IssuedBpns
    ): List<SiteDb> {
        val createdSites = siteCreateWithReferencedAddressAsMainService.create(
            additionalSites.newSites.map { SiteCreateWithReferencedAddressAsMainParsed(recordAddress, it.header, mainAddressContent = null) }
        )
        additionalSites.newSites.zip(createdSites).forEach { (newSite, createdSite) -> issuedBpns.record(newSite.reference, createdSite.bpn) }

        val completeMembership = (listOf(recordSite) + additionalSites.existingSites + createdSites).distinctBy { it.bpn }
        addressUpdateService.setSites(listOf(AddressSiteMembershipParsed(recordAddress, completeMembership)))

        return recordAddress.sites.filterNot { it.bpn == recordSite.bpn }.sortedBy { it.createdAt }
    }

    private class IssuedBpns {
        val byRequestIdentifier = linkedMapOf<String, String>()

        fun record(reference: BpnReferenceParsed, bpn: String) {
            if (reference is BpnReferenceParsed.Pending) byRequestIdentifier.putIfAbsent(reference.requestIdentifier, bpn)
        }
    }
}
