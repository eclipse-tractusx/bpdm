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
import org.eclipse.tractusx.bpdm.pool.model.GoldenRecordUpsertResult
import org.eclipse.tractusx.bpdm.pool.model.parsed.BpnReferenceParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.GoldenRecordUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.AdditionalSitesPlan
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteUpsertPlan
import org.eclipse.tractusx.bpdm.pool.service.operation.legalentity.LegalEntityAssociationFetchService
import org.eclipse.tractusx.bpdm.pool.service.operation.participation.SharingMemberConfidenceService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Carries out a planned golden record upsert against the Pool.
 *
 * It writes no partner itself: each plan variant names the services that own those writes, and this one only decides
 * the order and hands each step the parents the previous step produced. A request identifier the plan left pending is
 * registered here, as the record it names comes into existence, so that a later task naming it reaches that record
 * rather than creating a second one.
 */
@Service
class GoldenRecordUpsertService(
    private val legalEntityUpsertService: LegalEntityUpsertService,
    private val siteUpsertService: SiteUpsertService,
    private val additionalAddressUpsertService: AdditionalAddressUpsertService,
    private val additionalSiteUpsertService: AdditionalSiteUpsertService,
    private val sharingMemberConfidenceService: SharingMemberConfidenceService,
    private val legalEntityAssociationFetchService: LegalEntityAssociationFetchService,
    private val bpnRequestIdentifierMappingCreateService: BpnRequestIdentifierMappingCreateService
) {

    /**
     * Writes what [parsed] plans and reports the records it leaves behind, loaded with what a report of them is built
     * from.
     */
    @Transactional
    fun upsert(parsed: GoldenRecordUpsertParsed): GoldenRecordUpsertResult {
        val issued = mutableMapOf<String, String>()

        val legalEntity = legalEntityUpsertService.upsert(parsed.legalEntity)
        issued.issue(parsed.legalEntity.reference, legalEntity.value.bpn)
        issued.issue(parsed.legalEntity.legalAddressReference, legalEntity.value.legalAddress.bpn)

        val records = when (parsed) {
            is GoldenRecordUpsertParsed.LegalEntityRecord ->
                GoldenRecordUpsertResult.LegalEntityRecord(
                    legalEntity,
                    confidenceUpdates(parsed, legalEntity.value.legalAddress)
                )

            is GoldenRecordUpsertParsed.LegalEntityAddressRecord -> {
                val address = additionalAddressUpsertService.upsert(parsed.address, legalEntity.value, site = null)
                issued.issue(parsed.address.reference, address.value.bpn)
                GoldenRecordUpsertResult.LegalEntityAddressRecord(
                    legalEntity,
                    address,
                    confidenceUpdates(parsed, address.value)
                )
            }

            is GoldenRecordUpsertParsed.SiteRecord -> {
                val site = siteUpsertService.upsert(parsed.site, legalEntity.value)
                issued.issueSite(parsed.site, site.value.bpn, site.value.mainAddress.bpn)
                val additionalSites = additionalSiteUpsertService.upsert(parsed.additionalSites, site.value, site.value.mainAddress)
                issued.issueAdditionalSites(parsed.additionalSites, additionalSites)
                GoldenRecordUpsertResult.SiteRecord(
                    legalEntity,
                    site,
                    confidenceUpdates(parsed, site.value.mainAddress)
                )
            }

            is GoldenRecordUpsertParsed.SiteAddressRecord -> {
                val site = siteUpsertService.upsert(parsed.site, legalEntity.value)
                issued.issueSite(parsed.site, site.value.bpn, site.value.mainAddress.bpn)
                val address = additionalAddressUpsertService.upsert(parsed.address, legalEntity.value, site.value)
                issued.issue(parsed.address.reference, address.value.bpn)
                val additionalSites = additionalSiteUpsertService.upsert(parsed.additionalSites, site.value, address.value)
                issued.issueAdditionalSites(parsed.additionalSites, additionalSites)
                GoldenRecordUpsertResult.SiteAddressRecord(
                    legalEntity,
                    site,
                    address,
                    confidenceUpdates(parsed, address.value)
                )
            }
        }

        bpnRequestIdentifierMappingCreateService.create(issued)
        legalEntityAssociationFetchService.fetch(setOf(legalEntity.value))

        return records
    }

    private fun confidenceUpdates(
        parsed: GoldenRecordUpsertParsed,
        recordAddress: LogisticAddressDb
    ): SharingMemberConfidenceService.Result =
        // The record now shares the address it is about, which is what the count of sharing members per golden
        // record is taken from, so it is recounted once every write of this request has landed.
        sharingMemberConfidenceService.updateAddress(parsed.sharingMemberRecordId, recordAddress.bpn)

    private fun MutableMap<String, String>.issue(reference: BpnReferenceParsed, bpn: String) {
        // A request identifier is answered by the first record it reaches, so a second write under the same
        // identifier leaves the answer it already has standing.
        if (reference is BpnReferenceParsed.Pending) putIfAbsent(reference.requestIdentifier, bpn)
    }

    private fun MutableMap<String, String>.issueSite(plan: SiteUpsertPlan, siteBpn: String, mainAddressBpn: String) {
        issue(plan.reference, siteBpn)
        // A site whose main address is the legal address states no address of its own: that address is already
        // answering to the legal entity's legal address reference.
        mainAddressReference(plan)?.let { issue(it, mainAddressBpn) }
    }

    private fun MutableMap<String, String>.issueAdditionalSites(plan: AdditionalSitesPlan, createdSites: List<SiteDb>) {
        plan.newSites.zip(createdSites).forEach { (planned, created) -> issue(planned.reference, created.bpn) }
    }

    private fun mainAddressReference(plan: SiteUpsertPlan): BpnReferenceParsed? =
        when (plan) {
            is SiteUpsertPlan.CreateWithOwnMainAddress -> plan.mainAddressReference
            is SiteUpsertPlan.CreateOnExistingAddress -> plan.mainAddressReference
            is SiteUpsertPlan.UpdateWithOwnMainAddress -> plan.mainAddressReference
            is SiteUpsertPlan.CreateOnLegalAddress, is SiteUpsertPlan.UpdateOnLegalAddress, is SiteUpsertPlan.Unchanged -> null
        }
}
