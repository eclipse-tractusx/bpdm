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

package org.eclipse.tractusx.bpdm.gate.v7.sharingstate

import org.eclipse.tractusx.bpdm.gate.v7.UnscheduledGateTestBaseV7
import org.eclipse.tractusx.orchestrator.api.model.SharingMemberRecordUpdateRequest
import org.junit.jupiter.api.Test

class SyncGoldenRecordCountedV7IT : UnscheduledGateTestBaseV7() {

    @Test
    fun `do not sync a partner created for an alternative process`() {
        // GIVEN
        val input = testDataClient.businessPartner.upsertInput(testName, "provider")
        testDataClient.businessPartner.refineAlternatively(input, "provider")

        // WHEN / THEN
        assertRepo.assertSharingMemberRecordUpdates(
            testDataClient.businessPartner.goldenRecordCountedSyncedToOrchestrator(), emptyList()
        )
    }

    @Test
    fun `moving a counted partner to an alternative process removes its count and promotes the next partner`() {
        // GIVEN
        val golden = poolMockDataFactory.mockAdditionalAddressOfSiteSearchResult(testName)
        val first = testDataClient.businessPartner.upsertInput("$testName first")
        val firstTask = testDataClient.businessPartner.setStateToSuccess(first.externalId, first.externalId, golden)
        val second = testDataClient.businessPartner.upsertInput("$testName second")
        val secondTask = testDataClient.businessPartner.setStateToSuccess(second.externalId, second.externalId, golden)
        testDataClient.businessPartner.goldenRecordCountedSyncedToOrchestrator()

        // WHEN
        testDataClient.businessPartner.upsertInput(
            org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.input.BusinessPartnerInputV7Request(
                externalId = first.externalId, process = "provider"
            )
        )
        val sent = testDataClient.businessPartner.goldenRecordCountedSyncedToOrchestrator()

        // THEN
        assertRepo.assertSharingMemberRecordUpdates(sent, listOf(
            SharingMemberRecordUpdateRequest(firstTask.recordId, false),
            SharingMemberRecordUpdateRequest(secondTask.recordId, true)
        ))
        assertRepo.assertSharingMemberRecordUpdates(testDataClient.businessPartner.goldenRecordCountedSyncedToOrchestrator(), emptyList())
    }

    /**
     * GIVEN successfully shared business partner
     * WHEN the Gate synchronizes its golden record counts
     * THEN the Orchestrator learns that the record of that business partner counts towards its golden record
     */
    @Test
    fun `sync golden record count of shared business partner`() {
        //GIVEN
        val input = testDataClient.businessPartner.upsertInput(testName)
        val task = testDataClient.businessPartner.setStateToSuccess(input.externalId)

        //WHEN
        val sentUpdates = testDataClient.businessPartner.goldenRecordCountedSyncedToOrchestrator()

        //THEN
        assertRepo.assertSharingMemberRecordUpdates(sentUpdates, listOf(SharingMemberRecordUpdateRequest(task.recordId, true)))
    }

    /**
     * GIVEN successfully shared business partners, more than the synchronization covers in one page
     * WHEN the Gate synchronizes its golden record counts
     * THEN the Orchestrator learns that the record of each of those business partners counts towards its golden record
     */
    @Test
    fun `sync golden record counts of shared business partners across pages`() {
        //GIVEN
        val tasks = listOf("1 $testName", "2 $testName", "3 $testName").map { externalId ->
            testDataClient.businessPartner.upsertInput(externalId)
            testDataClient.businessPartner.setStateToSuccess(externalId)
        }

        //WHEN
        val sentUpdates = testDataClient.businessPartner.goldenRecordCountedSyncedToOrchestrator()

        //THEN
        assertRepo.assertSharingMemberRecordUpdates(sentUpdates, tasks.map { SharingMemberRecordUpdateRequest(it.recordId, true) })
    }

    /**
     * GIVEN successfully shared business partner whose golden record count the Orchestrator already received
     * WHEN the Gate synchronizes its golden record counts again
     * THEN the Orchestrator is not sent that golden record count again
     */
    @Test
    fun `sync golden record count only once`() {
        //GIVEN
        val input = testDataClient.businessPartner.upsertInput(testName)
        testDataClient.businessPartner.setStateToSuccess(input.externalId)
        testDataClient.businessPartner.goldenRecordCountedSyncedToOrchestrator()

        //WHEN
        val sentUpdates = testDataClient.businessPartner.goldenRecordCountedSyncedToOrchestrator()

        //THEN
        assertRepo.assertSharingMemberRecordUpdates(sentUpdates, emptyList())
    }
}
