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

package org.eclipse.tractusx.bpdm.gate.v6.inputmanager

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.api.v6.model.request.RelationPostRequest
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.RelationDto
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Instant

class RelationCreationV6IT: InputManagerV6Test() {

    /**
     * GIVEN two business partners
     * WHEN input manager posts a new relation
     * THEN input manager sees relation created response
     */
    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `create valid relation`(relationType: RelationType) {
        //GIVEN
        val input1 = testDataClient.createBusinessPartnerInput("$testName 1")
        val input2 = testDataClient.createBusinessPartnerInput("$testName 2")

        //WHEN
        val response = gateClient.relations.post(RelationPostRequest(testName, relationType, input1.externalId, input2.externalId))

        //THEN
        val expected = RelationDto(testName, relationType, input1.externalId, input2.externalId, Instant.now(), Instant.now())

        assertRepo.assertRelation(response, expected)
    }

    /**
     * GIVEN two business partners
     * WHEN input manager posts a new relation
     * THEN input manager can find relation
     */
    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `create and find valid relation`(relationType: RelationType) {
        //GIVEN
        val input1 = testDataClient.createBusinessPartnerInput("$testName 1")
        val input2 = testDataClient.createBusinessPartnerInput("$testName 2")

        //WHEN
        val createRequest = RelationPostRequest(testName, relationType, input1.externalId, input2.externalId)
        gateClient.relations.post(createRequest)

        //THEN
        val searchResponse = gateClient.relations.get()
        val expected = PageDto(1, 1, 0, 1, listOf(expectedResultFactory.buildRelationDto(createRequest)))

        assertRepo.assertRelations(searchResponse, expected)
    }



    /**
     * GIVEN business partner target
     * WHEN input manager posts a new relation with not existing source
     * THEN input manager sees 400 HTTP response
     */
    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `try create relation source not exists`(relationType: RelationType) {
        //GIVEN
        val targetInput = testDataClient.createBusinessPartnerInput("$testName 1")

        //WHEN
        val postRequest: () -> Unit = { gateClient.relations.post(RelationPostRequest(testName, relationType, "NOT EXISTS", targetInput.externalId)) }

        //THEN
        Assertions.assertThatThrownBy(postRequest).isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }

    /**
     * GIVEN business partner source
     * WHEN input manager posts a new relation with not existing target
     * THEN input manager sees 400 HTTP response
     */
    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `try create relation target not exists`(relationType: RelationType) {
        //GIVEN
        val sourceInput = testDataClient.createBusinessPartnerInput("$testName 1")

        //WHEN
        val postRequest: () -> Unit = { gateClient.relations.post(RelationPostRequest(testName, relationType, sourceInput.externalId, "NOT EXISTS")) }

        //THEN
        Assertions.assertThatThrownBy(postRequest).isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }

    /**
     * GIVEN relation
     * WHEN input manager posts a new relation with the same external ID
     * THEN input manager sees 409 HTTP response
     */
    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `try create duplicate relation`(relationType: RelationType) {
        //GIVEN
        val sourceInput = testDataClient.createBusinessPartnerInput("$testName 1")
        val targetInput = testDataClient.createBusinessPartnerInput("$testName 2")

        val givenRelation = testDataClient.createRelation(testName, sourceInput, targetInput, relationType)

        //WHEN
        val postRequest: () -> Unit
                = { gateClient.relations.post(RelationPostRequest(givenRelation.externalId, relationType, sourceInput.externalId, targetInput.externalId)) }

        //THEN
        Assertions.assertThatThrownBy(postRequest).isInstanceOf(WebClientResponseException.Conflict::class.java)
    }
}