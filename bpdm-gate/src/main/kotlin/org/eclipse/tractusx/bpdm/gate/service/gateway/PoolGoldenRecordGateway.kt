/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.bpdm.gate.service.gateway

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.mapper.pool.inbound.PoolGoldenRecordRequestMapper
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.*
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.ChangelogSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntitySearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.SiteSearchRequest
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Reads golden records and their changes from the Pool.
 */
@Component
class PoolGoldenRecordGateway(
    private val poolClient: PoolApiClient,
    private val requestMapper: PoolGoldenRecordRequestMapper
) {

    /**
     * Returns the oldest page of golden record changes the Pool recorded after [after], together with the current golden
     * records of the changed BPNs and of every site their addresses belong to.
     */
    fun fetchChangesAfter(after: Instant, pageSize: Int): GoldenRecordUpdatePageRequest {
        val changes = fetchChanges(after, pageSize)
        return GoldenRecordUpdatePageRequest(changes, fetchGoldenRecords(changes.changed))
    }

    private fun fetchChanges(after: Instant, pageSize: Int): PoolChangePageRequest {
        val entries = poolClient.changelogs.getChangelogEntries(ChangelogSearchRequest(timestampAfter = after), PaginationRequest(0, pageSize))
        val changedBpnsByType = entries.content.groupBy({ it.businessPartnerType }, { it.bpn })
        return PoolChangePageRequest(
            changed = GoldenRecordBpns(
                legalEntities = changedBpnsByType[BusinessPartnerType.LEGAL_ENTITY].orEmpty().toSet(),
                sites = changedBpnsByType[BusinessPartnerType.SITE].orEmpty().toSet(),
                addresses = changedBpnsByType[BusinessPartnerType.ADDRESS].orEmpty().toSet()
            ),
            changedUntil = entries.content.lastOrNull()?.timestamp,
            hasMore = entries.totalPages > 1
        )
    }

    /**
     * Returns the current golden records of [bpns] together with every site their addresses belong to.
     */
    fun fetchGoldenRecords(bpns: GoldenRecordBpns): PoolGoldenRecordsRequest {
        val legalEntities = fetchLegalEntities(bpns.legalEntities)
        val addresses = fetchAddresses(bpns.addresses)
        val sitesOfAddresses = addresses.flatMap { listOfNotNull(it.siteBpn) + it.additionalSiteBpns }
        val sites = fetchSites(bpns.sites + sitesOfAddresses)

        return PoolGoldenRecordsRequest(
            requested = bpns,
            legalEntities = legalEntities.associateBy { it.bpn },
            sites = sites.associateBy { it.bpn },
            addresses = addresses.associateBy { it.bpn }
        )
    }

    /**
     * Returns those of [bpns] the Pool knows.
     */
    fun fetchExistingBpns(bpns: GoldenRecordBpns): GoldenRecordBpns =
        GoldenRecordBpns(
            legalEntities = fetchLegalEntities(bpns.legalEntities).map { it.bpn }.toSet(),
            sites = fetchSites(bpns.sites).map { it.bpn }.toSet(),
            addresses = fetchAddresses(bpns.addresses).map { it.bpn }.toSet()
        )

    private fun fetchLegalEntities(bpns: Set<String>): List<LegalEntityGoldenRecordRequest> =
        bpns.chunked(MAX_PAGE_SIZE) { chunk ->
            poolClient.legalEntities.getLegalEntities(LegalEntitySearchRequest(bpnLs = chunk), PaginationRequest(0, chunk.size)).content
        }.flatten().map(requestMapper::toRequest)

    private fun fetchSites(bpns: Set<String>): List<SiteGoldenRecordRequest> =
        bpns.chunked(MAX_PAGE_SIZE) { chunk ->
            poolClient.sites.getSites(SiteSearchRequest(siteBpns = chunk), PaginationRequest(0, chunk.size)).content
        }.flatten().map { requestMapper.toRequest(it.site) }

    private fun fetchAddresses(bpns: Set<String>): List<AddressGoldenRecordRequest> =
        bpns.chunked(MAX_PAGE_SIZE) { chunk ->
            poolClient.addresses.getAddresses(AddressSearchRequest(addressBpns = chunk), PaginationRequest(0, chunk.size)).content
        }.flatten().map(requestMapper::toRequest)

    companion object {
        private const val MAX_PAGE_SIZE = 100
    }
}
