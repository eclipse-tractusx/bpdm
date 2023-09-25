/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.gate.controller

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerIdentifierDto
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerStateDto
import org.eclipse.tractusx.bpdm.common.dto.ClassificationDto
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.util.CommonValues
import org.eclipse.tractusx.bpdm.gate.util.DbTestHelpers
import org.eclipse.tractusx.bpdm.gate.util.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.gate.util.RequestValues
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Instant

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["bpdm.api.upsert-limit=3"]
)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class BusinessPartnerControllerIT @Autowired constructor(
    val testHelpers: DbTestHelpers,
    val gateClient: GateClient,
) {

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
    }

    @Test
    fun `insert minimal business partner`() {
        val upsertRequests = listOf(RequestValues.bpInputRequestMinimal)
        val upsertResponses = gateClient.businessParters.upsertBusinessPartnersInput(upsertRequests).body!!
        assertUpsertResponsesMatchRequests(upsertResponses, upsertRequests)

        val searchResponsePage = gateClient.businessParters.getBusinessPartnersInput(null)
        assertEquals(1, searchResponsePage.totalElements)
        testHelpers.assertRecursively(searchResponsePage.content).isEqualTo(upsertResponses)
    }

    @Test
    fun `insert three business partners`() {
        val upsertRequests = listOf(RequestValues.bpInputRequestFull, RequestValues.bpInputRequestMinimal, RequestValues.bpInputRequestChina)
        val upsertResponses = gateClient.businessParters.upsertBusinessPartnersInput(upsertRequests).body!!
        assertUpsertResponsesMatchRequests(upsertResponses, upsertRequests)

        val searchResponsePage = gateClient.businessParters.getBusinessPartnersInput(null)
        assertEquals(3, searchResponsePage.totalElements)
        testHelpers.assertRecursively(searchResponsePage.content).isEqualTo(upsertResponses)
    }

    @Test
    fun `insert and then update business partner`() {
        val insertRequests = listOf(RequestValues.bpInputRequestMinimal)
        val externalId = insertRequests.first().externalId
        val insertResponses = gateClient.businessParters.upsertBusinessPartnersInput(insertRequests).body!!
        assertUpsertResponsesMatchRequests(insertResponses, insertRequests)

        val searchResponse1Page = gateClient.businessParters.getBusinessPartnersInput(null)
        testHelpers.assertRecursively(searchResponse1Page.content).isEqualTo(insertResponses)

        val updateRequests = listOf(
            RequestValues.bpInputRequestFull.copy(externalId = externalId)
        )
        val updateResponses = gateClient.businessParters.upsertBusinessPartnersInput(updateRequests).body!!
        assertUpsertResponsesMatchRequests(updateResponses, updateRequests)

        val searchResponse2Page = gateClient.businessParters.getBusinessPartnersInput(null)
        testHelpers.assertRecursively(searchResponse2Page.content).isEqualTo(updateResponses)
    }

    @Test
    fun `insert too many business partners`() {
        // limit is 3
        val upsertRequests = listOf(
            RequestValues.bpInputRequestFull,
            RequestValues.bpInputRequestMinimal,
            RequestValues.bpInputRequestMinimal.copy(externalId = CommonValues.externalId3),
            RequestValues.bpInputRequestMinimal.copy(externalId = CommonValues.externalId4)
        )
        try {
            gateClient.businessParters.upsertBusinessPartnersInput(upsertRequests)
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.BAD_REQUEST, e.statusCode)
        }
    }

    @Test
    fun `insert duplicate business partners`() {
        val upsertRequests = listOf(
            RequestValues.bpInputRequestFull.copy(externalId = CommonValues.externalId3),
            RequestValues.bpInputRequestMinimal.copy(externalId = CommonValues.externalId3)
        )
        try {
            gateClient.businessParters.upsertBusinessPartnersInput(upsertRequests)
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.BAD_REQUEST, e.statusCode)
        }
    }

    @Test
    fun `query business partners by externalId`() {
        val upsertRequests = listOf(
            RequestValues.bpInputRequestFull.copy(externalId = CommonValues.externalId1, shortName = "1"),
            RequestValues.bpInputRequestMinimal.copy(externalId = CommonValues.externalId2, shortName = "2"),
            RequestValues.bpInputRequestMinimal.copy(externalId = CommonValues.externalId3, shortName = "3")
        )
        gateClient.businessParters.upsertBusinessPartnersInput(upsertRequests)

        val searchResponsePage = gateClient.businessParters.getBusinessPartnersInput(listOf(CommonValues.externalId1, CommonValues.externalId3))
        assertUpsertResponsesMatchRequests(searchResponsePage.content, listOf(upsertRequests[0], upsertRequests[2]))
    }

    @Test
    fun `query business partners by missing externalId`() {
        val upsertRequests = listOf(
            RequestValues.bpInputRequestFull.copy(externalId = CommonValues.externalId1, shortName = "1"),
            RequestValues.bpInputRequestMinimal.copy(externalId = CommonValues.externalId2, shortName = "2"),
            RequestValues.bpInputRequestMinimal.copy(externalId = CommonValues.externalId3, shortName = "3")
        )
        gateClient.businessParters.upsertBusinessPartnersInput(upsertRequests)

        // missing externalIds are just ignored in the response
        val searchResponsePage = gateClient.businessParters.getBusinessPartnersInput(listOf(CommonValues.externalId2, CommonValues.externalId4))
        assertUpsertResponsesMatchRequests(searchResponsePage.content, listOf(upsertRequests[1]))
    }

    @Test
    fun `query business partners using paging`() {
        val upsertRequests = listOf(
            RequestValues.bpInputRequestFull.copy(externalId = CommonValues.externalId1, shortName = "1"),
            RequestValues.bpInputRequestMinimal.copy(externalId = CommonValues.externalId2, shortName = "2"),
            RequestValues.bpInputRequestMinimal.copy(externalId = CommonValues.externalId3, shortName = "3")
        )
        gateClient.businessParters.upsertBusinessPartnersInput(upsertRequests)

        // missing externalIds are just ignored in the response
        val searchResponsePage0 = gateClient.businessParters.getBusinessPartnersInput(null, PaginationRequest(0, 2))
        assertEquals(3, searchResponsePage0.totalElements)
        assertEquals(2, searchResponsePage0.totalPages)
        assertUpsertResponsesMatchRequests(searchResponsePage0.content, listOf(upsertRequests[0], upsertRequests[1]))

        val searchResponsePage1 = gateClient.businessParters.getBusinessPartnersInput(null, PaginationRequest(1, 2))
        assertEquals(3, searchResponsePage1.totalElements)
        assertEquals(2, searchResponsePage1.totalPages)
        assertUpsertResponsesMatchRequests(searchResponsePage1.content, listOf(upsertRequests[2]))

        val searchResponsePage2 = gateClient.businessParters.getBusinessPartnersInput(null, PaginationRequest(2, 2))
        assertEquals(3, searchResponsePage2.totalElements)
        assertEquals(2, searchResponsePage2.totalPages)
        assertEquals(0, searchResponsePage2.content.size)
    }

    private fun assertUpsertResponsesMatchRequests(responses: Collection<BusinessPartnerInputDto>, requests: List<BusinessPartnerInputRequest>) {
        Assertions.assertThat(responses)
            .usingRecursiveComparison()
            .ignoringFieldsOfTypes(Instant::class.java)
            .isEqualTo(requests.map(::toExpectedResponse))
    }

    private fun toExpectedResponse(request: BusinessPartnerInputRequest): BusinessPartnerInputDto {
        // same sorting order as defined for entity
        return BusinessPartnerInputDto(
            externalId = request.externalId,
            nameParts = request.nameParts,
            shortName = request.shortName,
            identifiers = request.identifiers.toSortedSet(identifierDtoComparator),
            legalForm = request.legalForm,
            states = request.states.toSortedSet(stateDtoComparator),
            classifications = request.classifications.toSortedSet(classificationDtoComparator),
            roles = request.roles.toSortedSet(),
            postalAddress = request.postalAddress,
            isOwner = request.isOwner,
            bpnL = request.bpnL,
            bpnS = request.bpnS,
            bpnA = request.bpnA,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    val identifierDtoComparator = compareBy(
        BusinessPartnerIdentifierDto::type,
        BusinessPartnerIdentifierDto::value,
        BusinessPartnerIdentifierDto::issuingBody
    )
    val stateDtoComparator = compareBy(nullsFirst(), BusinessPartnerStateDto::validFrom)       // here null means MIN
        .thenBy(nullsLast(), BusinessPartnerStateDto::validTo)        // here null means MAX
        .thenBy(BusinessPartnerStateDto::type)
        .thenBy(BusinessPartnerStateDto::description)
    val classificationDtoComparator = compareBy(
        ClassificationDto::type,
        ClassificationDto::code,
        ClassificationDto::value
    )
}
