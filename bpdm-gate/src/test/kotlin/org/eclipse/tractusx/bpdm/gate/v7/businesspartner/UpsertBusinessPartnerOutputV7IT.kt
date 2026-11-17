/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.bpdm.gate.v7.businesspartner

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.input.BusinessPartnerInputV7Request
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.output.BusinessPartnerOutputV7Request
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.sharingstate.SharingStateTypeV7
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.sharingstate.SharingStateV7Dto
import org.eclipse.tractusx.bpdm.gate.v7.UnscheduledGateTestBaseV7
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Instant

/** Exercises provider writes through authenticated HTTP, including atomic rejection and output provenance. */
class UpsertBusinessPartnerOutputV7IT : UnscheduledGateTestBaseV7() {
    private val process = "provider"

    @Test
    fun `write output for alternative-process partner`() {
        // GIVEN
        val input = testDataClient.businessPartner.upsertInput(testName, process)
        val request = testData.businessPartner.output.alternativeRequest(input, process)

        // WHEN
        val actual = gateClient.businessParters.upsertBusinessPartnersOutput(listOf(request))

        // THEN
        val expected = testData.businessPartner.output.fromAlternativeRequest(request)
        assertRepo.assertBusinessPartnerOutput(actual, listOf(expected))
        assertRepo.assertBusinessPartnerOutput(
            gateClient.businessParters.getBusinessPartnersOutput(listOf(input.externalId), processes = listOf(process)),
            PageDto(1, 1, 0, 1, listOf(expected))
        )
        assertRepo.assertSharingStates(
            gateClient.sharingState.getSharingStates(PaginationRequest(), listOf(input.externalId), processes = listOf(process)),
            PageDto(1, 1, 0, 1, listOf(SharingStateV7Dto(
                input.externalId, SharingStateTypeV7.Success, updatedAt = Instant.MIN, process = process
            )))
        )
    }

    @Test
    fun `rewrite alternative output`() {
        // GIVEN
        val input = testDataClient.businessPartner.upsertInput(testName, process)
        testDataClient.businessPartner.refineAlternatively(input, process)
        val request = testData.businessPartner.output.alternativeRequest(input, process, "$testName changed")

        // WHEN
        val actual = gateClient.businessParters.upsertBusinessPartnersOutput(listOf(request))

        // THEN
        assertRepo.assertBusinessPartnerOutput(actual, listOf(testData.businessPartner.output.fromAlternativeRequest(request)))
    }

    @Test
    fun `reject golden unknown and mismatching process records atomically`() {
        // GIVEN
        val alternative = testDataClient.businessPartner.upsertInput(testName, process)
        val golden = testDataClient.businessPartner.upsertInput("$testName golden")
        val valid = testData.businessPartner.output.alternativeRequest(alternative, process)
        val invalid = listOf(
            valid.copy(externalId = golden.externalId),
            valid.copy(process = "wrong"),
            valid.copy(externalId = "$testName unknown")
        )

        // WHEN / THEN
        invalid.forEach { request ->
            assertThrows<WebClientResponseException.BadRequest> {
                gateClient.businessParters.upsertBusinessPartnersOutput(listOf(valid, request))
            }
        }
        assertThat(gateClient.businessParters.getBusinessPartnersOutput(processes = listOf(process)).content).isEmpty()
        assertThat(gateClient.businessParters.getBusinessPartnersOutput().content).isEmpty()
    }

    @Test
    fun `reject site without a name instead of dropping its content`() {
        // GIVEN
        val input = testDataClient.businessPartner.upsertInput(testName, process)
        val valid = testData.businessPartner.output.alternativeRequest(input, process)
        val request = valid.copy(site = valid.site!!.copy(name = null))

        // WHEN / THEN
        assertThrows<WebClientResponseException.BadRequest> {
            gateClient.businessParters.upsertBusinessPartnersOutput(listOf(request))
        }
        assertThat(gateClient.businessParters.getBusinessPartnersOutput(processes = listOf(process)).content).isEmpty()
    }

    @Test
    fun `keep previous output under its writing process until overwritten`() {
        // GIVEN
        val input = testDataClient.businessPartner.upsertInput(testName, process)
        val previous = testDataClient.businessPartner.refineAlternatively(input, process)

        // WHEN
        gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            BusinessPartnerInputV7Request(externalId = input.externalId, process = "next")
        ))

        // THEN
        assertRepo.assertBusinessPartnerOutput(
            gateClient.businessParters.getBusinessPartnersOutput(listOf(input.externalId), processes = listOf(process)),
            PageDto(1, 1, 0, 1, listOf(previous))
        )
        assertThat(gateClient.businessParters.getBusinessPartnersOutput(processes = listOf("next")).content).isEmpty()
    }
}
