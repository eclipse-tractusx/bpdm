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

package org.eclipse.tractusx.bpdm.gate.v6.relation

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.relation.RelationTypeV6
import org.eclipse.tractusx.bpdm.gate.api.v6.model.relation.RelationValidityPeriodV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.relation.input.RelationPutV6Entry
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.RelationTypeV7
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.RelationValidityPeriodV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.input.RelationPutV7Entry
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.input.RelationPutV7Request
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.input.RelationSearchV7Request
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.input.RelationV7Dto
import org.eclipse.tractusx.bpdm.gate.v6.UnscheduledGateV6TestBase
import org.eclipse.tractusx.bpdm.gate.v7.util.GateTestClientProviderV7
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.GateAssertRepositoryV7
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Instant
import java.time.LocalDate

class RelationUpsertV6IT: UnscheduledGateV6TestBase() {

    @Autowired
    lateinit var testClientProviderV7: GateTestClientProviderV7
    @Autowired
    lateinit var assertRepoV7: GateAssertRepositoryV7

    /**
     * GIVEN two business partners
     * WHEN input manager upserts new relation
     * THEN input manager sees relation created response
     */
    @ParameterizedTest
    @EnumSource(RelationTypeV6::class)
    fun `upsert new relation`(relationType: RelationTypeV6) {
        //GIVEN
        val sourceInput = testDataClient.createBusinessPartnerInput("$testName 1")
        val targetInput = testDataClient.createBusinessPartnerInput("$testName 2")

        //WHEN
        val request = RelationPutV6Entry(testName, relationType, sourceInput.externalId, targetInput.externalId)
        val response = gateClient.relations.put(true, request)

        //THEN
        val expected = expectedResultFactory.buildRelationDto(request)

        assertRepo.assertRelation(response, expected)
    }

    /**
     * GIVEN relation between two business partners and two other business partners
     * WHEN input manager updates relation with new business partners
     * THEN input manager sees relation updated response
     */
    @ParameterizedTest
    @EnumSource(RelationTypeV6::class)
    fun `update existing relation`(relationType: RelationTypeV6) {
        //GIVEN
        val givenRelation = testDataClient.createRelationWithBusinessPartners(testName, relationType)

        val newSourceInput = testDataClient.createBusinessPartnerInput("$testName 3")
        val newTargetInput = testDataClient.createBusinessPartnerInput("$testName 4")

        //WHEN
        val payload = RelationPutV6Entry(givenRelation.externalId, relationType.toNewType(), newSourceInput.externalId, newTargetInput.externalId)
        val response = gateClient.relations.put(false, payload)

        //THEN
        val expected = expectedResultFactory.buildRelationDto(payload)

        assertRepo.assertRelation(response, expected)
    }

    /**
     * GIVEN relation between two business partners and two other business partners
     * WHEN input manager updates relation with new business partners
     * THEN input manager can find updated relation
     */
    @ParameterizedTest
    @EnumSource(RelationTypeV6::class)
    fun `update and find existing relation`(relationType: RelationTypeV6) {
        //GIVEN
        val givenRelation = testDataClient.createRelationWithBusinessPartners(testName, relationType)

        val newSourceInput = testDataClient.createBusinessPartnerInput("$testName 3")
        val newTargetInput = testDataClient.createBusinessPartnerInput("$testName 4")

        //WHEN
        val payload = RelationPutV6Entry(givenRelation.externalId, relationType.toNewType(), newSourceInput.externalId, newTargetInput.externalId)
        gateClient.relations.put(false, payload)

        //THEN
        val searchResponse = gateClient.relations.get()
        val expected = PageDto(1, 1, 0, 1, listOf(expectedResultFactory.buildRelationDto(payload)))

        assertRepo.assertRelations(searchResponse, expected)
    }

    /**
     * GIVEN relation between two business partners and another business partner
     * WHEN input manager updates relation with not existing source
     * THEN input manager sees 400 HTTP error response
     */
    @ParameterizedTest
    @EnumSource(RelationTypeV6::class)
    fun `try update relation with not existing source`(relationType: RelationTypeV6) {
        //GIVEN
        val givenRelation = testDataClient.createRelationWithBusinessPartners(testName, relationType)

        val newTargetInput = testDataClient.createBusinessPartnerInput("$testName 4")

        //WHEN
        val payload = RelationPutV6Entry(givenRelation.externalId, relationType.toNewType(), "NOT EXISTING", newTargetInput.externalId)
        val request: () -> Unit = { gateClient.relations.put(false, payload) }

        //THEN
        Assertions.assertThatThrownBy(request).isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }

    /**
     * GIVEN relation between two business partners and another business partner
     * WHEN input manager updates relation with not existing target
     * THEN input manager sees 400 HTTP error response
     */
    @ParameterizedTest
    @EnumSource(RelationTypeV6::class)
    fun `try update relation with not existing target`(relationType: RelationTypeV6) {
        //GIVEN
        val givenRelation = testDataClient.createRelationWithBusinessPartners(testName, relationType)

        val newSourceInput = testDataClient.createBusinessPartnerInput("$testName 4")

        //WHEN
        val payload = RelationPutV6Entry(givenRelation.externalId, relationType.toNewType(), newSourceInput.externalId, "NOT EXISTING")
        val request: () -> Unit = { gateClient.relations.put(false, payload) }

        //THEN
        Assertions.assertThatThrownBy(request).isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }

    /**
     * GIVEN two business partners
     * WHEN input manager tries to upsert new relation without the flag allowing creating a new relation
     * THEN input manager sees 400 HTTP error response
     */
    @ParameterizedTest
    @EnumSource(RelationTypeV6::class)
    fun `try upsert new relation without allowed to create flag`(relationType: RelationTypeV6) {
        //GIVEN
        val sourceInput = testDataClient.createBusinessPartnerInput("$testName 1")
        val targetInput = testDataClient.createBusinessPartnerInput("$testName 2")

        //WHEN
        val payload = RelationPutV6Entry(testName, relationType, sourceInput.externalId, targetInput.externalId)
        val request: () -> Unit = { gateClient.relations.put(false, payload) }

        //THEN
        Assertions.assertThatThrownBy(request).isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }

    /**
     * GIVEN relation with a reason code between two business partners
     * WHEN input manager updates the relation with a validity period
     * THEN input manager sees the relation with that validity period and without reason code in the current API
     */
    @Test
    fun `update relation with validity period`() {
        //GIVEN
        val gateClientV7 = testClientProviderV7.createClient(KeyCloakInitializer.CLIENT_ID_OPERATOR)
        val sourceInput = testDataClient.createBusinessPartnerInput("$testName 1")
        val targetInput = testDataClient.createBusinessPartnerInput("$testName 2")
        val givenRelation = RelationPutV7Entry(testName, RelationTypeV7.IsManagedBy, sourceInput.externalId, targetInput.externalId, reasonCode = "$testName reason")
        gateClientV7.relation.put(true, RelationPutV7Request(listOf(givenRelation)))

        //WHEN
        val validityPeriods = listOf(RelationValidityPeriodV6Dto(validFrom = LocalDate.of(2024, 1, 1), validTo = null))
        gateClient.relations.put(false, RelationPutV6Entry(testName, RelationTypeV6.IsManagedBy, sourceInput.externalId, targetInput.externalId, validityPeriods))

        //THEN
        val actual = gateClientV7.relation.postSearch(RelationSearchV7Request(externalIds = listOf(testName))).content.single()
        val expectedValidityPeriods = listOf(RelationValidityPeriodV7Dto(validFrom = LocalDate.of(2024, 1, 1), validTo = null))
        val expected = RelationV7Dto(testName, RelationTypeV7.IsManagedBy, sourceInput.externalId, targetInput.externalId, expectedValidityPeriods, null, Instant.now(), Instant.now())

        assertRepoV7.assertRelation(actual, expected)
    }

    private fun RelationTypeV6.toNewType() = RelationTypeV6.entries.find { it != this }!!
}