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
import org.eclipse.tractusx.bpdm.pool.model.BpnReferenceAllocation
import org.eclipse.tractusx.bpdm.pool.model.GoldenRecordUpsertResult
import org.eclipse.tractusx.bpdm.pool.model.parsed.GoldenRecordUpsertParsed
import org.eclipse.tractusx.bpdm.pool.service.operation.legalentity.LegalEntityAssociationFetchService
import org.eclipse.tractusx.bpdm.pool.service.operation.participation.SharingMemberConfidenceService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Carries out a planned golden record upsert against the Pool.
 *
 * It writes no partner itself: each plan variant names the services that own those writes, and this one only decides
 * the order and hands each step the parents the previous step produced. A request identifier that named no record yet
 * is registered here, as the record it now names comes into existence, so a later request in the same batch reaches
 * that record rather than creating a second one.
 */
@Service
class GoldenRecordUpsertService(
    private val legalEntityUpsertService: LegalEntityUpsertService,
    private val siteUpsertService: SiteUpsertService,
    private val additionalAddressUpsertService: AdditionalAddressUpsertService,
    private val coLocatedSiteUpsertService: CoLocatedSiteUpsertService,
    private val sharingMemberConfidenceService: SharingMemberConfidenceService,
    private val legalEntityAssociationFetchService: LegalEntityAssociationFetchService,
    private val bpnRequestIdentifierMappingCreateService: BpnRequestIdentifierMappingCreateService
) {

    /**
     * Writes what [parsed] plans and reports the records it leaves behind, loaded with what a report of them is built
     * from.
     */
    @Transactional
    fun upsert(parsed: GoldenRecordUpsertParsed, bpnReferences: BpnReferenceAllocation): GoldenRecordUpsertResult {
        val legalEntity = legalEntityUpsertService.upsert(parsed.legalEntity, bpnReferences)

        val result = when (parsed) {
            is GoldenRecordUpsertParsed.LegalEntityRecord ->
                GoldenRecordUpsertResult.LegalEntityRecord(
                    legalEntity,
                    completeWrites(parsed, legalEntity.value.legalAddress, bpnReferences)
                )

            is GoldenRecordUpsertParsed.LegalEntityAddressRecord -> {
                val address = additionalAddressUpsertService.upsert(parsed.address, legalEntity.value, site = null, bpnReferences)
                GoldenRecordUpsertResult.LegalEntityAddressRecord(
                    legalEntity,
                    address,
                    completeWrites(parsed, address.value, bpnReferences)
                )
            }

            is GoldenRecordUpsertParsed.SiteRecord -> {
                val site = siteUpsertService.upsert(parsed.site, legalEntity.value, bpnReferences)
                coLocatedSiteUpsertService.upsert(parsed.coLocatedSites, site.value, site.value.mainAddress, bpnReferences)
                GoldenRecordUpsertResult.SiteRecord(
                    legalEntity,
                    site,
                    completeWrites(parsed, site.value.mainAddress, bpnReferences)
                )
            }

            is GoldenRecordUpsertParsed.SiteAddressRecord -> {
                val site = siteUpsertService.upsert(parsed.site, legalEntity.value, bpnReferences)
                val address = additionalAddressUpsertService.upsert(parsed.address, legalEntity.value, site.value, bpnReferences)
                coLocatedSiteUpsertService.upsert(parsed.coLocatedSites, site.value, address.value, bpnReferences)
                GoldenRecordUpsertResult.SiteAddressRecord(
                    legalEntity,
                    site,
                    address,
                    completeWrites(parsed, address.value, bpnReferences)
                )
            }
        }

        legalEntityAssociationFetchService.fetch(setOf(legalEntity.value))
        return result
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
}
