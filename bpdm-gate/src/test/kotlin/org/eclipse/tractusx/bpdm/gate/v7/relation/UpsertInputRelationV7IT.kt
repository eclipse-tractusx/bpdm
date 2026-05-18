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

package org.eclipse.tractusx.bpdm.gate.v7.relation

import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.api.model.RelationValidityPeriodDto
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutRequest
import org.eclipse.tractusx.bpdm.gate.v7.UnscheduledGateTestBaseV7
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withRelationType
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withSource
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withTarget
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withValidityPeriods
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.web.reactive.function.client.WebClientResponseException.BadRequest
import java.time.LocalDate

class UpsertInputRelationV7IT : UnscheduledGateTestBaseV7() {

    /**
     * GIVEN two business partner inputs
     * WHEN input manager upserts a new relation with createIfNotExist=true
     * THEN the created relation is returned with correct data for each relation type
     */
    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `upsert new input relation`(relationType: RelationType) {
        //GIVEN + WHEN
        val request = testData.relation.input.request.fromSeed(testName).withRelationType(relationType)
        val response = testDataClient.upsertRelationInputWithBusinessPartners(request)

        //THEN
        assertRepo.assertRelation(response, testData.relation.input.response.fromRequest(request))
    }

    /**
     * GIVEN an existing relation
     * WHEN input manager upserts the same external ID with different source and target (createIfNotExist=true)
     * THEN the updated relation data is returned
     */
    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `upsert existing input relation updates it`(relationType: RelationType) {
        //GIVEN
        val initial = testData.relation.input.request.fromSeed(testName).withRelationType(relationType)
        testDataClient.upsertRelationInputWithBusinessPartners(initial)

        val newSource = testDataClient.upsertBusinessPartnerInput("$testName new source")
        val newTarget = testDataClient.upsertBusinessPartnerInput("$testName new target")

        //WHEN
        val updated = initial.withSource(newSource.externalId).withTarget(newTarget.externalId)
        val response = testDataClient.upsertRelationInput(updated, createIfNotExist = true)

        //THEN
        assertRepo.assertRelation(response, testData.relation.input.response.fromRequest(updated))
    }

    /**
     * GIVEN an existing relation
     * WHEN input manager explicitly updates it with createIfNotExist=false and new source and target
     * THEN the updated relation data is returned
     */
    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `update existing input relation`(relationType: RelationType) {
        //GIVEN
        val initial = testData.relation.input.request.fromSeed(testName).withRelationType(relationType)
        testDataClient.upsertRelationInputWithBusinessPartners(initial)

        val newSource = testDataClient.upsertBusinessPartnerInput("$testName new source")
        val newTarget = testDataClient.upsertBusinessPartnerInput("$testName new target")

        //WHEN
        val updated = initial.withSource(newSource.externalId).withTarget(newTarget.externalId)
        val response = testDataClient.upsertRelationInput(updated, createIfNotExist = false)

        //THEN
        assertRepo.assertRelation(response, testData.relation.input.response.fromRequest(updated))
    }

    /**
     * GIVEN multiple business partner inputs
     * WHEN input manager upserts multiple relations in a single request
     * THEN all relations are created and returned with correct data
     */
    @Test
    fun `upsert multiple relations in one request`() {
        //GIVEN
        val source = testDataClient.upsertBusinessPartnerInput("$testName source")
        val target1 = testDataClient.upsertBusinessPartnerInput("$testName target1")
        val target2 = testDataClient.upsertBusinessPartnerInput("$testName target2")

        val entries = listOf(
            testData.relation.input.request.fromSeed("$testName rel1")
                .withRelationType(RelationType.IsManagedBy)
                .withSource(source.externalId)
                .withTarget(target1.externalId),
            testData.relation.input.request.fromSeed("$testName rel2")
                .withRelationType(RelationType.IsOwnedBy)
                .withSource(source.externalId)
                .withTarget(target2.externalId)
        )

        //WHEN
        val response = gateClient.relation.put(createIfNotExist = true, RelationPutRequest(entries))

        //THEN
        val expected = entries.map { testData.relation.input.response.fromRequest(it) }
        assertRepo.assertRelations(response.upsertedRelations, expected)
    }

    /**
     * GIVEN two business partner inputs
     * WHEN input manager upserts a relation with an open-ended validity period (null validTo)
     * THEN the relation is returned with the open-ended validity period preserved
     */
    @Test
    fun `upsert relation with open-ended validity period`() {
        //GIVEN + WHEN
        val request = testData.relation.input.request.fromSeed(testName)
            .withValidityPeriods(listOf(RelationValidityPeriodDto(validFrom = LocalDate.of(2020, 1, 1), validTo = null)))
        val response = testDataClient.upsertRelationInputWithBusinessPartners(request)

        //THEN
        assertRepo.assertRelation(response, testData.relation.input.response.fromRequest(request))
    }

    /**
     * GIVEN two business partner inputs
     * WHEN input manager upserts a relation with a bounded validity period (explicit validFrom and validTo)
     * THEN the relation is returned with the bounded validity period preserved
     */
    @Test
    fun `upsert relation with bounded validity period`() {
        //GIVEN + WHEN
        val request = testData.relation.input.request.fromSeed(testName)
            .withValidityPeriods(listOf(
                RelationValidityPeriodDto(validFrom = LocalDate.of(2020, 1, 1), validTo = LocalDate.of(2025, 12, 31))
            ))
        val response = testDataClient.upsertRelationInputWithBusinessPartners(request)

        //THEN
        assertRepo.assertRelation(response, testData.relation.input.response.fromRequest(request))
    }

    /**
     * GIVEN two business partner inputs
     * WHEN input manager upserts a relation with multiple non-overlapping validity periods
     * THEN the relation is returned with all validity periods preserved
     */
    @Test
    fun `upsert relation with multiple validity periods`() {
        //GIVEN + WHEN
        val request = testData.relation.input.request.fromSeed(testName)
            .withValidityPeriods(listOf(
                RelationValidityPeriodDto(validFrom = LocalDate.of(2010, 1, 1), validTo = LocalDate.of(2015, 12, 31)),
                RelationValidityPeriodDto(validFrom = LocalDate.of(2018, 1, 1), validTo = LocalDate.of(2022, 12, 31)),
                RelationValidityPeriodDto(validFrom = LocalDate.of(2024, 1, 1), validTo = null)
            ))
        val response = testDataClient.upsertRelationInputWithBusinessPartners(request)

        //THEN
        assertRepo.assertRelation(response, testData.relation.input.response.fromRequest(request))
    }

    /**
     * GIVEN no existing relation with the given external ID
     * WHEN input manager tries to update it with createIfNotExist=false
     * THEN a bad request error is returned
     */
    @Test
    fun `update non-existing relation fails`() {
        //GIVEN
        val request = testData.relation.input.request.fromSeed(testName)
        testDataClient.upsertBusinessPartnerInput(request.businessPartnerSourceExternalId)
        testDataClient.upsertBusinessPartnerInput(request.businessPartnerTargetExternalId)

        //WHEN + THEN
        assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            testDataClient.upsertRelationInput(request, createIfNotExist = false)
        }
    }

    /**
     * GIVEN a single business partner input
     * WHEN input manager upserts a relation where source and target reference the same business partner
     * THEN a bad request error is returned
     */
    @Test
    fun `upsert relation with source equal to target fails`() {
        //GIVEN
        val bp = testDataClient.upsertBusinessPartnerInput(testName)
        val request = testData.relation.input.request.fromSeed(testName)
            .withSource(bp.externalId)
            .withTarget(bp.externalId)

        //WHEN + THEN
        assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            testDataClient.upsertRelationInput(request)
        }
    }

    /**
     * GIVEN a business partner input as target
     * WHEN input manager upserts a relation referencing a non-existing source external ID
     * THEN a bad request error is returned
     */
    @Test
    fun `upsert relation with non-existing source fails`() {
        //GIVEN
        val target = testDataClient.upsertBusinessPartnerInput("$testName target")
        val request = testData.relation.input.request.fromSeed(testName)
            .withSource("NON_EXISTING_EXTERNAL_ID")
            .withTarget(target.externalId)

        //WHEN + THEN
        assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            testDataClient.upsertRelationInput(request)
        }
    }

    /**
     * GIVEN a business partner input as source
     * WHEN input manager upserts a relation referencing a non-existing target external ID
     * THEN a bad request error is returned
     */
    @Test
    fun `upsert relation with non-existing target fails`() {
        //GIVEN
        val source = testDataClient.upsertBusinessPartnerInput("$testName source")
        val request = testData.relation.input.request.fromSeed(testName)
            .withSource(source.externalId)
            .withTarget("NON_EXISTING_EXTERNAL_ID")

        //WHEN + THEN
        assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            testDataClient.upsertRelationInput(request)
        }
    }

    /**
     * GIVEN two business partner inputs
     * WHEN input manager upserts a relation with a validity period where validFrom equals validTo
     * THEN a bad request error is returned
     */
    @Test
    fun `upsert relation with validity period where validFrom equals validTo fails`() {
        //GIVEN
        val sameDate = LocalDate.of(2024, 6, 15)
        val request = testData.relation.input.request.fromSeed(testName)
            .withValidityPeriods(listOf(RelationValidityPeriodDto(validFrom = sameDate, validTo = sameDate)))
        testDataClient.upsertBusinessPartnerInput(request.businessPartnerSourceExternalId)
        testDataClient.upsertBusinessPartnerInput(request.businessPartnerTargetExternalId)

        //WHEN + THEN
        assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            testDataClient.upsertRelationInput(request)
        }
    }

    /**
     * GIVEN two business partner inputs
     * WHEN input manager upserts a relation with a validity period where validFrom is after validTo
     * THEN a bad request error is returned
     */
    @Test
    fun `upsert relation with validity period where validFrom is after validTo fails`() {
        //GIVEN
        val request = testData.relation.input.request.fromSeed(testName)
            .withValidityPeriods(listOf(
                RelationValidityPeriodDto(validFrom = LocalDate.of(2025, 1, 1), validTo = LocalDate.of(2020, 1, 1))
            ))
        testDataClient.upsertBusinessPartnerInput(request.businessPartnerSourceExternalId)
        testDataClient.upsertBusinessPartnerInput(request.businessPartnerTargetExternalId)

        //WHEN + THEN
        assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            testDataClient.upsertRelationInput(request)
        }
    }
}
