/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.bpdm.gate.v7.sharingstate

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.v7.exception.BusinessPartnerSharingErrorV7
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.sharingstate.*
import org.eclipse.tractusx.bpdm.gate.api.v7.model.error.GateErrorV7Response
import org.eclipse.tractusx.bpdm.gate.v7.UnscheduledGateTestBaseV7
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Instant

/** Exercises an alternative worker's state transitions and process validation through HTTP. */
class UpdateAlternativeSharingStateV7IT : UnscheduledGateTestBaseV7() {
    private val process = "provider"

    @Test
    fun `mark alternative partner pending without a missing task error`() {
        // GIVEN
        val input = testDataClient.businessPartner.upsertInput(testName, process)

        // WHEN
        testDataClient.businessPartner.setAlternativeStateToPending(input.externalId, process)
        taskHealthCheckService.checkPendingTasks()

        // THEN
        val actual = gateClient.sharingState.getSharingStates(PaginationRequest(), listOf(input.externalId), processes = listOf(process))
        assertThat(actual.content.single().sharingProcessStarted).isNotNull()
        assertRepo.assertSharingStates(actual, PageDto(1, 1, 0, 1, listOf(SharingStateV7Dto(
            input.externalId, SharingStateTypeV7.Pending, sharingProcessStarted = actual.content.single().sharingProcessStarted,
            updatedAt = Instant.MIN, process = process
        ))))
    }

    @Test
    fun `fail alternative partner with provider message`() {
        // GIVEN
        val input = testDataClient.businessPartner.upsertInput(testName, process)

        // WHEN
        gateClient.sharingState.postSharingStateError(PostSharingStateErrorV7Request(listOf(input.externalId), process, "Cannot refine"))

        // THEN
        val actual = gateClient.sharingState.getSharingStates(PaginationRequest(), listOf(input.externalId), processes = listOf(process))
        assertRepo.assertSharingStates(actual, PageDto(1, 1, 0, 1, listOf(SharingStateV7Dto(
            input.externalId, SharingStateTypeV7.Error, BusinessPartnerSharingErrorV7.SharingProcessError, "Cannot refine",
            sharingProcessStarted = actual.content.single().sharingProcessStarted, updatedAt = Instant.MIN, process = process
        ))))
    }

    @Test
    fun `reject mismatching process and golden records for provider state changes`() {
        // GIVEN
        val alternative = testDataClient.businessPartner.upsertInput(testName, process)
        val golden = testDataClient.businessPartner.upsertInput("$testName golden")

        // WHEN / THEN
        listOf(alternative.externalId to "wrong", golden.externalId to process).forEach { (externalId, requestedProcess) ->
            assertThrows<WebClientResponseException.BadRequest> {
                gateClient.sharingState.postSharingStatePending(PostSharingStatePendingV7Request(listOf(externalId), requestedProcess))
            }
            assertThrows<WebClientResponseException.BadRequest> {
                gateClient.sharingState.postSharingStateError(PostSharingStateErrorV7Request(listOf(externalId), requestedProcess, "error"))
            }
        }
        assertThat(gateClient.sharingState.getSharingStates(PaginationRequest(), listOf(alternative.externalId), processes = listOf(process))
            .content.single().sharingStateType).isEqualTo(SharingStateTypeV7.Initial)
    }

    @Test
    fun `return separate errors for every rejected pending and error state change`() {
        // GIVEN
        val input = testDataClient.businessPartner.upsertInput(testName, process)
        val golden = testDataClient.businessPartner.upsertInput("$testName golden")
        val externalIds = listOf(input.externalId, golden.externalId, "$testName unknown")

        // WHEN
        val pendingResponse = assertThrows<WebClientResponseException.BadRequest> {
            gateClient.sharingState.postSharingStatePending(PostSharingStatePendingV7Request(externalIds, "wrong"))
        }
        val errorResponse = assertThrows<WebClientResponseException.BadRequest> {
            gateClient.sharingState.postSharingStateError(PostSharingStateErrorV7Request(externalIds, "wrong", "Cannot refine"))
        }

        // THEN
        listOf(pendingResponse, errorResponse).forEach {
            assertRepo.assertErrorPerExternalId(it.getResponseBodyAs(GateErrorV7Response::class.java)!!, externalIds)
        }
        assertThat(gateClient.sharingState.getSharingStates(PaginationRequest(), listOf(input.externalId), processes = listOf(process))
            .content.single().sharingStateType).isEqualTo(SharingStateTypeV7.Initial)
    }
}
