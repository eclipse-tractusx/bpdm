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
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.pool.v7

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntitySearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.SiteSearchRequest
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class BusinessPartnerTimestampsV7IT : UnscheduledPoolTestBaseV7() {
    @Test
    fun `legal entity creation and update timestamps survive subsequent reads`() {
        val created = testDataClient.createLegalEntity(testName)
        val searched = poolClient.legalEntities.postLegalEntitySearch(
            LegalEntitySearchRequest(bpnLs = listOf(created.header.bpnl)), PaginationRequest()
        ).content.single()

        assertTimestamps(created.header.createdAt, created.header.updatedAt, searched.header.createdAt, searched.header.updatedAt)
        assertTimestamps(created.legalAddress.createdAt, created.legalAddress.updatedAt, searched.legalAddress.createdAt, searched.legalAddress.updatedAt)

        val request = requestFactory.buildLegalEntityUpdateRequest("Updated $testName", created.header.bpnl)
        val updated = poolClient.legalEntities.updateBusinessPartners(listOf(request)).entities.single().legalEntity
        val read = poolClient.legalEntities.getLegalEntity(created.header.bpnl)

        assertTimestamps(updated.header.createdAt, updated.header.updatedAt, read.header.createdAt, read.header.updatedAt)
        assertTimestamps(updated.legalAddress.createdAt, updated.legalAddress.updatedAt, read.legalAddress.createdAt, read.legalAddress.updatedAt)
        assertThat(updated.header.createdAt).isEqualTo(created.header.createdAt)
        assertThat(updated.header.updatedAt).isAfter(created.header.updatedAt)
        assertThat(updated.legalAddress.createdAt).isEqualTo(created.legalAddress.createdAt)
        assertThat(updated.legalAddress.updatedAt).isAfter(created.legalAddress.updatedAt)
    }

    @Test
    fun `site creation and update timestamps survive subsequent reads`() {
        val legalEntity = testDataClient.createParticipantLegalEntity(testName)
        val created = testDataClient.createSite(legalEntity, testName)
        val searched = poolClient.sites.postSiteSearch(
            SiteSearchRequest(siteBpns = listOf(created.site.bpns)), PaginationRequest()
        ).content.single()

        assertTimestamps(created.site.createdAt, created.site.updatedAt, searched.site.createdAt, searched.site.updatedAt)
        assertTimestamps(created.mainAddress.createdAt, created.mainAddress.updatedAt, searched.mainAddress.createdAt, searched.mainAddress.updatedAt)

        val request = requestFactory.createSiteUpdateRequest("Updated $testName", created)
        val updated = poolClient.sites.updateSite(listOf(request)).entities.single()
        val read = poolClient.sites.getSite(created.site.bpns)

        assertTimestamps(updated.site.createdAt, updated.site.updatedAt, read.site.createdAt, read.site.updatedAt)
        assertTimestamps(updated.mainAddress.createdAt, updated.mainAddress.updatedAt, read.mainAddress.createdAt, read.mainAddress.updatedAt)
        assertThat(updated.site.createdAt).isEqualTo(created.site.createdAt)
        assertThat(updated.site.updatedAt).isAfter(created.site.updatedAt)
        assertThat(updated.mainAddress.createdAt).isEqualTo(created.mainAddress.createdAt)
        assertThat(updated.mainAddress.updatedAt).isAfter(created.mainAddress.updatedAt)
    }

    @Test
    fun `address creation and update timestamps survive subsequent reads`() {
        val legalEntity = testDataClient.createParticipantLegalEntity(testName)
        val request = requestFactory.buildAdditionalAddressCreateRequest(testName, legalEntity)
        val created = poolClient.addresses.createAddresses(listOf(request)).entities.single().address
        val searched = poolClient.addresses.searchAddresses(
            AddressSearchRequest(addressBpns = listOf(created.bpna)), PaginationRequest()
        ).content.single().address

        assertTimestamps(created.createdAt, created.updatedAt, searched.createdAt, searched.updatedAt)

        val updateRequest = requestFactory.buildAddressUpdateRequest("Updated $testName", created.bpna)
        val updated = poolClient.addresses.updateAddresses(listOf(updateRequest)).entities.single().address
        val read = poolClient.addresses.getAddress(created.bpna).address

        assertTimestamps(updated.createdAt, updated.updatedAt, read.createdAt, read.updatedAt)
        assertThat(updated.createdAt).isEqualTo(created.createdAt)
        assertThat(updated.updatedAt).isAfter(created.updatedAt)
    }

    @Test
    fun `changing only legal entity identifiers updates parent timestamp and no-op preserves it`() {
        val original = requestFactory.buildLegalEntity(testName)
        val request = original.copy(
            header = original.header.copy(
                states = original.header.states.map {
                    it.copy(
                        validFrom = it.validFrom?.truncatedTo(ChronoUnit.MICROS),
                        validTo = it.validTo?.truncatedTo(ChronoUnit.MICROS)
                    )
                },
                confidenceCriteria = original.header.confidenceCriteria.let {
                    it.copy(
                        lastConfidenceCheckAt = it.lastConfidenceCheckAt.truncatedTo(ChronoUnit.MICROS),
                        nextConfidenceCheckAt = it.nextConfidenceCheckAt.truncatedTo(ChronoUnit.MICROS)
                    )
                }
            ),
            legalAddress = original.legalAddress.copy(
                states = original.legalAddress.states.map {
                    it.copy(
                        validFrom = it.validFrom?.truncatedTo(ChronoUnit.MICROS),
                        validTo = it.validTo?.truncatedTo(ChronoUnit.MICROS)
                    )
                },
                confidenceCriteria = original.legalAddress.confidenceCriteria.let {
                    it.copy(
                        lastConfidenceCheckAt = it.lastConfidenceCheckAt.truncatedTo(ChronoUnit.MICROS),
                        nextConfidenceCheckAt = it.nextConfidenceCheckAt.truncatedTo(ChronoUnit.MICROS)
                    )
                }
            )
        )
        val created = testDataClient.createLegalEntity(request)
        val content = request.copy(
            header = request.header.copy(
                identifiers = listOf(requestFactory.buildLegalEntityIdentifier("Updated $testName"))
            )
        )
        val updateRequest = LegalEntityPartnerUpdateRequest(created.header.bpnl, content)
        val updated = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest)).entities.single().legalEntity
        val read = poolClient.legalEntities.getLegalEntity(created.header.bpnl)

        assertThat(updated.header.updatedAt).isAfter(created.header.updatedAt)
        assertThat(updated.header.updatedAt).isEqualTo(read.header.updatedAt)
        assertThat(updated.legalAddress.updatedAt).isEqualTo(created.legalAddress.updatedAt)

        val unchanged = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest)).entities.single().legalEntity
        assertThat(unchanged.header.updatedAt).isEqualTo(updated.header.updatedAt)
        assertThat(poolClient.legalEntities.getLegalEntity(created.header.bpnl).header.updatedAt).isEqualTo(updated.header.updatedAt)
    }

    private fun assertTimestamps(createdAt: Instant, updatedAt: Instant, readCreatedAt: Instant, readUpdatedAt: Instant) {
        assertThat(createdAt).isEqualTo(readCreatedAt)
        assertThat(updatedAt).isEqualTo(readUpdatedAt)
        assertThat(createdAt.nano % 1000).isZero()
        assertThat(updatedAt.nano % 1000).isZero()
    }
}
