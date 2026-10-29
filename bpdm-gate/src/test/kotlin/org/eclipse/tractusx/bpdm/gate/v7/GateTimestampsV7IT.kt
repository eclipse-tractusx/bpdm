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

package org.eclipse.tractusx.bpdm.gate.v7

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.model.RelationValidityPeriodDto
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationSearchRequest
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.eclipse.tractusx.bpdm.gate.service.SharingStateService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.time.LocalDate

class GateTimestampsV7IT : UnscheduledGateTestBaseV7() {
    @Autowired
    lateinit var sharingStateService: SharingStateService

    @Autowired
    lateinit var sharingStateRepository: SharingStateRepository

    @Autowired
    lateinit var transactionTemplate: TransactionTemplate

    @Test
    fun `input timestamps survive creation child-only update and no-op`() {
        val request = BusinessPartnerInputRequest(externalId = testName)
        val created = testDataClient.businessPartner.upsertInput(request)
        val readCreated = gateClient.businessParters.getBusinessPartnersInput(listOf(testName)).content.single()
        assertTimestamps(created.createdAt, created.updatedAt, readCreated.createdAt, readCreated.updatedAt)

        val update = request.copy(nameParts = listOf("Updated child record"))
        val updated = testDataClient.businessPartner.upsertInput(update)
        val readUpdated = gateClient.businessParters.getBusinessPartnersInput(listOf(testName)).content.single()
        assertTimestamps(updated.createdAt, updated.updatedAt, readUpdated.createdAt, readUpdated.updatedAt)
        assertThat(updated.createdAt).isEqualTo(created.createdAt)
        assertThat(updated.updatedAt).isAfter(created.updatedAt)

        assertThat(gateClient.businessParters.upsertBusinessPartnersInput(listOf(update)).body!!).isEmpty()
        val unchanged = gateClient.businessParters.getBusinessPartnersInput(listOf(testName)).content.single()
        assertThat(unchanged.updatedAt).isEqualTo(updated.updatedAt)
    }

    @Test
    fun `relation timestamps survive creation validity-only update and no-op`() {
        val request = testData.relation.input.request.fromSeed(testName)
        val created = testDataClient.relation.upsertRelationInputWithBusinessPartners(request)
        val search = RelationSearchRequest(externalIds = listOf(created.externalId))
        val readCreated = gateClient.relation.postSearch(search).content.single()
        assertTimestamps(created.createdAt, created.updatedAt, readCreated.createdAt, readCreated.updatedAt)

        val update = request.copy(validityPeriods = listOf(RelationValidityPeriodDto(LocalDate.of(2020, 1, 1), null)))
        val updated = testDataClient.relation.upsertRelationInput(update)
        val readUpdated = gateClient.relation.postSearch(search).content.single()
        assertTimestamps(updated.createdAt, updated.updatedAt, readUpdated.createdAt, readUpdated.updatedAt)
        assertThat(updated.createdAt).isEqualTo(created.createdAt)
        assertThat(updated.updatedAt).isAfter(created.updatedAt)

        val unchanged = testDataClient.relation.upsertRelationInput(update)
        assertThat(unchanged.updatedAt).isEqualTo(updated.updatedAt)
        assertThat(gateClient.relation.postSearch(search).content.single().updatedAt).isEqualTo(updated.updatedAt)
        assertThat(
            gateClient.relation.postSearch(search.copy(updatedAtFrom = updated.updatedAt)).content
        ).isEmpty()
    }

    @Test
    fun `sharing state timestamp is stable after persistence and usable as query cursor`() {
        testDataClient.businessPartner.upsertInput(BusinessPartnerInputRequest(externalId = testName))
        val stamped = transactionTemplate.execute {
            val state = sharingStateRepository.findByExternalIdAndTenantBpnl(testName, tenantBPNL).single()
            sharingStateService.setReady(state).updatedAt.instant
        }!!

        val read = gateClient.sharingState.getSharingStates(PaginationRequest(), listOf(testName)).content.single()
        assertThat(read.updatedAt).isEqualTo(stamped)
        assertThat(read.updatedAt.nano % 1000).isZero()
        assertThat(
            gateClient.sharingState.getSharingStates(
                PaginationRequest(), listOf(testName), updatedAfter = stamped
            ).content
        ).isEmpty()
    }

    private fun assertTimestamps(createdAt: Instant, updatedAt: Instant, readCreatedAt: Instant, readUpdatedAt: Instant) {
        assertThat(createdAt).isEqualTo(readCreatedAt)
        assertThat(updatedAt).isEqualTo(readUpdatedAt)
        assertThat(createdAt.nano % 1000).isZero()
        assertThat(updatedAt.nano % 1000).isZero()
    }
}
