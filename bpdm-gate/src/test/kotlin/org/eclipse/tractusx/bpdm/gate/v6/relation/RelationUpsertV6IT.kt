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
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.api.v6.model.request.RelationPutEntryV6
import org.eclipse.tractusx.bpdm.gate.v6.GateUnscheduledInitialStartV6Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.web.reactive.function.client.WebClientResponseException

class RelationUpsertV6IT: GateUnscheduledInitialStartV6Test() {

    /**
     * GIVEN two business partners
     * WHEN input manager upserts new relation
     * THEN input manager sees relation created response
     */
    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `upsert new relation`(relationType: RelationType) {
        //GIVEN
        val sourceInput = testDataClient.createBusinessPartnerInput("$testName 1")
        val targetInput = testDataClient.createBusinessPartnerInput("$testName 2")

        //WHEN
        val request = RelationPutEntryV6(testName, relationType, sourceInput.externalId, targetInput.externalId)
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
    @EnumSource(RelationType::class)
    fun `update existing relation`(relationType: RelationType) {
        //GIVEN
        val givenRelation = testDataClient.createRelationWithBusinessPartners(testName, relationType)

        val newSourceInput = testDataClient.createBusinessPartnerInput("$testName 3")
        val newTargetInput = testDataClient.createBusinessPartnerInput("$testName 4")

        //WHEN
        val payload = RelationPutEntryV6(givenRelation.externalId, relationType.toNewType(), newSourceInput.externalId, newTargetInput.externalId)
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
    @EnumSource(RelationType::class)
    fun `update and find existing relation`(relationType: RelationType) {
        //GIVEN
        val givenRelation = testDataClient.createRelationWithBusinessPartners(testName, relationType)

        val newSourceInput = testDataClient.createBusinessPartnerInput("$testName 3")
        val newTargetInput = testDataClient.createBusinessPartnerInput("$testName 4")

        //WHEN
        val payload = RelationPutEntryV6(givenRelation.externalId, relationType.toNewType(), newSourceInput.externalId, newTargetInput.externalId)
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
    @EnumSource(RelationType::class)
    fun `try update relation with not existing source`(relationType: RelationType) {
        //GIVEN
        val givenRelation = testDataClient.createRelationWithBusinessPartners(testName, relationType)

        val newTargetInput = testDataClient.createBusinessPartnerInput("$testName 4")

        //WHEN
        val payload = RelationPutEntryV6(givenRelation.externalId, relationType.toNewType(), "NOT EXISTING", newTargetInput.externalId)
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
    @EnumSource(RelationType::class)
    fun `try update relation with not existing target`(relationType: RelationType) {
        //GIVEN
        val givenRelation = testDataClient.createRelationWithBusinessPartners(testName, relationType)

        val newSourceInput = testDataClient.createBusinessPartnerInput("$testName 4")

        //WHEN
        val payload = RelationPutEntryV6(givenRelation.externalId, relationType.toNewType(), newSourceInput.externalId, "NOT EXISTING")
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
    @EnumSource(RelationType::class)
    fun `try upsert new relation without allowed to create flag`(relationType: RelationType) {
        //GIVEN
        val sourceInput = testDataClient.createBusinessPartnerInput("$testName 1")
        val targetInput = testDataClient.createBusinessPartnerInput("$testName 2")

        //WHEN
        val payload = RelationPutEntryV6(testName, relationType, sourceInput.externalId, targetInput.externalId)
        val request: () -> Unit = { gateClient.relations.put(false, payload) }

        //THEN
        Assertions.assertThatThrownBy(request).isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }

    private fun RelationType.toNewType() = RelationType.entries.find { it != this }!!
}