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

import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.model.BpnReferenceAllocation
import org.eclipse.tractusx.bpdm.pool.model.GoldenRecordUpsertResult
import org.eclipse.tractusx.bpdm.pool.model.parsed.*
import org.eclipse.tractusx.bpdm.pool.service.operation.address.AddressCreateService
import org.eclipse.tractusx.bpdm.pool.service.operation.address.AddressPayloadUpdateService
import org.eclipse.tractusx.bpdm.pool.service.operation.address.AddressUpdateService
import org.eclipse.tractusx.bpdm.pool.service.operation.legalentity.LegalEntityCreateService
import org.eclipse.tractusx.bpdm.pool.service.operation.participation.SharingMemberConfidenceService
import org.eclipse.tractusx.bpdm.pool.service.operation.legalentity.LegalEntityPayloadUpdateService
import org.eclipse.tractusx.bpdm.pool.service.operation.site.SiteCreateService
import org.eclipse.tractusx.bpdm.pool.service.operation.site.SiteCreateWithReferencedAddressAsMainService
import org.eclipse.tractusx.bpdm.pool.service.operation.site.SitePayloadUpdateService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Carries out a planned golden record upsert against the Pool.
 *
 * It writes nothing itself: each plan variant names the service that owns that entity's write, and this one only
 * decides the order and hands each step the parents the previous step produced. A request identifier that named no
 * record yet is registered here, as the record it now names comes into existence, so a later request in the same
 * batch reaches that record rather than creating a second one.
 */
@Service
class GoldenRecordUpsertService(
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
     * Writes what [parsed] plans and reports the records it leaves behind.
     */
    @Transactional
    fun upsert(parsed: GoldenRecordUpsertParsed, bpnReferences: BpnReferenceAllocation): GoldenRecordUpsertResult {
        val legalEntity = writeLegalEntity(parsed.legalEntity, bpnReferences)

        return when (parsed) {
            is GoldenRecordUpsertParsed.LegalEntityRecord ->
                GoldenRecordUpsertResult.LegalEntityRecord(
                    legalEntity,
                    completeWrites(parsed, legalEntity.legalAddress, bpnReferences)
                )

            is GoldenRecordUpsertParsed.LegalEntityAddressRecord -> {
                val address = writeAdditionalAddress(parsed.address, legalEntity, site = null, bpnReferences)
                GoldenRecordUpsertResult.LegalEntityAddressRecord(
                    legalEntity,
                    address,
                    completeWrites(parsed, address, bpnReferences)
                )
            }

            is GoldenRecordUpsertParsed.SiteRecord -> {
                val site = writeSite(parsed.site, legalEntity, bpnReferences)
                writeMembership(parsed.coLocatedSites, site, site.mainAddress, bpnReferences)
                GoldenRecordUpsertResult.SiteRecord(
                    legalEntity,
                    site,
                    completeWrites(parsed, site.mainAddress, bpnReferences)
                )
            }

            is GoldenRecordUpsertParsed.SiteAddressRecord -> {
                val site = writeSite(parsed.site, legalEntity, bpnReferences)
                val address = writeAdditionalAddress(parsed.address, legalEntity, site, bpnReferences)
                writeMembership(parsed.coLocatedSites, site, address, bpnReferences)
                GoldenRecordUpsertResult.SiteAddressRecord(
                    legalEntity,
                    site,
                    address,
                    completeWrites(parsed, address, bpnReferences)
                )
            }
        }
    }

    private fun completeWrites(
        parsed: GoldenRecordUpsertParsed,
        recordAddress: LogisticAddressDb,
        bpnReferences: BpnReferenceAllocation
    ): SharingMemberConfidenceService.Result {
        // The record now shares the address it is about, which is what the count of sharing members per golden
        // record is taken from, so it is recounted once every write of this request has landed.
        val confidenceUpdates = sharingMemberConfidenceService.updateAddress(parsed.sharingMemberRecordId, recordAddress.bpn)

        bpnRequestIdentifierMappingCreateService.create(bpnReferences.drainAllocated())

        return confidenceUpdates
    }

    private fun writeLegalEntity(plan: LegalEntityUpsertPlan, bpnReferences: BpnReferenceAllocation): LegalEntityDb {
        val legalEntity = when (plan) {
            is LegalEntityUpsertPlan.Unchanged -> plan.target
            is LegalEntityUpsertPlan.Create ->
                legalEntityCreateService.create(listOf(LegalEntityCreateParsed(plan.content))).single()
            is LegalEntityUpsertPlan.Update ->
                legalEntityPayloadUpdateService.update(listOf(LegalEntityUpdateParsed(plan.target, plan.content))).single().value
        }

        bpnReferences.allocate(plan.reference, legalEntity.bpn)
        bpnReferences.allocate(plan.legalAddressReference, legalEntity.legalAddress.bpn)
        return legalEntity
    }

    private fun writeSite(plan: SiteUpsertPlan, legalEntity: LegalEntityDb, bpnReferences: BpnReferenceAllocation): SiteDb {
        val site = when (plan) {
            is SiteUpsertPlan.Unchanged -> plan.target
            is SiteUpsertPlan.CreateWithOwnMainAddress ->
                siteCreateService.create(listOf(SiteCreateParsed(legalEntity, plan.content))).single()
            is SiteUpsertPlan.CreateOnLegalAddress ->
                siteCreateWithReferencedAddressAsMainService.create(
                    listOf(SiteCreateWithReferencedAddressAsMainParsed(legalEntity.legalAddress, plan.header, mainAddressContent = null))
                ).single()
            is SiteUpsertPlan.CreateOnExistingAddress ->
                siteCreateWithReferencedAddressAsMainService.create(listOf(plan.parsed)).single()
            is SiteUpsertPlan.UpdateWithOwnMainAddress ->
                sitePayloadUpdateService.update(listOf(SiteUpdateParsed(plan.target, plan.content))).single().value
            is SiteUpsertPlan.UpdateOnLegalAddress ->
                sitePayloadUpdateService.updateHeaders(listOf(plan.parsed)).single().value
        }

        bpnReferences.allocate(plan.reference, site.bpn)
        mainAddressReference(plan)?.let { bpnReferences.allocate(it, site.mainAddress.bpn) }
        return site
    }

    // A site whose main address is the legal address states no address of its own, so there is nothing to register:
    // that address answers to the legal entity's legal address reference.
    private fun mainAddressReference(plan: SiteUpsertPlan): BpnReferenceParsed? =
        when (plan) {
            is SiteUpsertPlan.CreateWithOwnMainAddress -> plan.mainAddressReference
            is SiteUpsertPlan.CreateOnExistingAddress -> plan.mainAddressReference
            is SiteUpsertPlan.UpdateWithOwnMainAddress -> plan.mainAddressReference
            is SiteUpsertPlan.CreateOnLegalAddress, is SiteUpsertPlan.UpdateOnLegalAddress, is SiteUpsertPlan.Unchanged -> null
        }

    private fun writeAdditionalAddress(
        plan: AddressUpsertPlan,
        legalEntity: LegalEntityDb,
        site: SiteDb?,
        bpnReferences: BpnReferenceAllocation
    ): LogisticAddressDb {
        val address = when (plan) {
            is AddressUpsertPlan.Create ->
                addressCreateService.create(listOf(AddressCreateParsed(legalEntity, site, plan.content))).single()
            is AddressUpsertPlan.Update ->
                // Site membership is stated once for the whole record, by the membership plan, so this update leaves it alone.
                addressPayloadUpdateService.update(listOf(AddressUpdateParsed(plan.target, sites = null, address = plan.content)))
                    .single().value
        }

        bpnReferences.allocate(plan.reference, address.bpn)
        return address
    }

    private fun writeMembership(
        plan: SiteMembershipPlan,
        recordSite: SiteDb,
        recordAddress: LogisticAddressDb,
        bpnReferences: BpnReferenceAllocation
    ) {
        val createdSites = siteCreateWithReferencedAddressAsMainService.create(
            plan.newSites.map { SiteCreateWithReferencedAddressAsMainParsed(recordAddress, it.header, mainAddressContent = null) }
        )
        plan.newSites.zip(createdSites).forEach { (planned, created) -> bpnReferences.allocate(planned.reference, created.bpn) }

        val membership = (listOf(recordSite) + plan.existingSites + createdSites).distinctBy { it.bpn }
        addressUpdateService.setSites(listOf(AddressSiteMembershipParsed(recordAddress, membership)))
    }
}
