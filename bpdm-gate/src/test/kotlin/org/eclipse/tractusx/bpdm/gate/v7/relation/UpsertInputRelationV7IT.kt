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
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.error.GateErrorV7Response
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.RelationTypeV7
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.RelationValidityPeriodV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.input.RelationPutV7Request
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.input.RelationSearchV7Request
import org.eclipse.tractusx.bpdm.gate.config.ApiConfigProperties
import org.eclipse.tractusx.bpdm.gate.v7.UnscheduledGateTestBaseV7
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withRelationType
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withSource
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withTarget
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withValidityPeriods
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.reactive.function.client.WebClientResponseException.BadRequest
import java.time.LocalDate

class UpsertInputRelationV7IT : UnscheduledGateTestBaseV7() {

    @Autowired
    lateinit var apiConfigProperties: ApiConfigProperties

    /**
     * GIVEN two business partner inputs
     * WHEN input manager upserts a new relation with createIfNotExist=true
     * THEN the created relation is returned with correct data for each relation type
     */
    @ParameterizedTest
    @EnumSource(RelationTypeV7::class)
    fun `upsert new input relation`(relationType: RelationTypeV7) {
        //GIVEN + WHEN
        val request = testData.relation.input.request.fromSeed(testName).withRelationType(relationType)
        val response = testDataClient.relation.upsertRelationInputWithBusinessPartners(request)

        //THEN
        assertRepo.assertRelation(response, testData.relation.input.response.fromRequest(request))
    }

    /**
     * GIVEN an existing relation
     * WHEN input manager upserts the same external ID with different source and target (createIfNotExist=true)
     * THEN the updated relation data is returned
     */
    @ParameterizedTest
    @EnumSource(RelationTypeV7::class)
    fun `upsert existing input relation updates it`(relationType: RelationTypeV7) {
        //GIVEN
        val initial = testData.relation.input.request.fromSeed(testName).withRelationType(relationType)
        val created = testDataClient.relation.upsertRelationInputWithBusinessPartners(initial)

        val newSource = testDataClient.businessPartner.upsertInput("$testName new source")
        val newTarget = testDataClient.businessPartner.upsertInput("$testName new target")

        //WHEN
        val updated = initial.withSource(newSource.externalId).withTarget(newTarget.externalId)
        val response = testDataClient.relation.upsertRelationInput(updated, createIfNotExist = true)

        //THEN
        assertRepo.assertRelation(response, testData.relation.input.response.fromRequest(updated))
        assertRepo.assertTimestampsUpdated(response.createdAt, response.updatedAt, created.createdAt, created.updatedAt)
        val search = RelationSearchV7Request(externalIds = listOf(response.externalId))
        val read = gateClient.relation.postSearch(search)
        assertRepo.assertRelations(read, PageDto(1, 1, 0, 1, listOf(response)))
        assertRepo.assertRelations(
            gateClient.relation.postSearch(search.copy(updatedAtFrom = response.updatedAt)),
            PageDto(0, 0, 0, 0, emptyList())
        )
        val unchanged = testDataClient.relation.upsertRelationInput(updated)
        assertRepo.assertTimestampsUnchanged(unchanged.createdAt, unchanged.updatedAt, response.createdAt, response.updatedAt)
    }

    /**
     * GIVEN an existing relation
     * WHEN input manager explicitly updates it with createIfNotExist=false and new source and target
     * THEN the updated relation data is returned
     */
    @ParameterizedTest
    @EnumSource(RelationTypeV7::class)
    fun `update existing input relation`(relationType: RelationTypeV7) {
        //GIVEN
        val initial = testData.relation.input.request.fromSeed(testName).withRelationType(relationType)
        testDataClient.relation.upsertRelationInputWithBusinessPartners(initial)

        val newSource = testDataClient.businessPartner.upsertInput("$testName new source")
        val newTarget = testDataClient.businessPartner.upsertInput("$testName new target")

        //WHEN
        val updated = initial.withSource(newSource.externalId).withTarget(newTarget.externalId)
        val response = testDataClient.relation.upsertRelationInput(updated, createIfNotExist = false)

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
        val source = testDataClient.businessPartner.upsertInput("$testName source")
        val target1 = testDataClient.businessPartner.upsertInput("$testName target1")
        val target2 = testDataClient.businessPartner.upsertInput("$testName target2")

        val entries = listOf(
            testData.relation.input.request.fromSeed("$testName rel1")
                .withRelationType(RelationTypeV7.IsManagedBy)
                .withSource(source.externalId)
                .withTarget(target1.externalId),
            testData.relation.input.request.fromSeed("$testName rel2")
                .withRelationType(RelationTypeV7.IsOwnedBy)
                .withSource(source.externalId)
                .withTarget(target2.externalId)
        )

        //WHEN
        val response = gateClient.relation.put(createIfNotExist = true, RelationPutV7Request(entries))

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
            .withValidityPeriods(listOf(RelationValidityPeriodV7Dto(validFrom = LocalDate.of(2020, 1, 1), validTo = null)))
        val response = testDataClient.relation.upsertRelationInputWithBusinessPartners(request)

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
                RelationValidityPeriodV7Dto(validFrom = LocalDate.of(2020, 1, 1), validTo = LocalDate.of(2025, 12, 31))
            ))
        val response = testDataClient.relation.upsertRelationInputWithBusinessPartners(request)

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
                RelationValidityPeriodV7Dto(validFrom = LocalDate.of(2010, 1, 1), validTo = LocalDate.of(2015, 12, 31)),
                RelationValidityPeriodV7Dto(validFrom = LocalDate.of(2018, 1, 1), validTo = LocalDate.of(2022, 12, 31)),
                RelationValidityPeriodV7Dto(validFrom = LocalDate.of(2024, 1, 1), validTo = null)
            ))
        val response = testDataClient.relation.upsertRelationInputWithBusinessPartners(request)

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
        testDataClient.businessPartner.upsertInput(request.businessPartnerSourceExternalId)
        testDataClient.businessPartner.upsertInput(request.businessPartnerTargetExternalId)

        //WHEN + THEN
        assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            testDataClient.relation.upsertRelationInput(request, createIfNotExist = false)
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
        val bp = testDataClient.businessPartner.upsertInput(testName)
        val request = testData.relation.input.request.fromSeed(testName)
            .withSource(bp.externalId)
            .withTarget(bp.externalId)

        //WHEN + THEN
        assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            testDataClient.relation.upsertRelationInput(request)
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
        val target = testDataClient.businessPartner.upsertInput("$testName target")
        val request = testData.relation.input.request.fromSeed(testName)
            .withSource("NON_EXISTING_EXTERNAL_ID")
            .withTarget(target.externalId)

        //WHEN + THEN
        assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            testDataClient.relation.upsertRelationInput(request)
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
        val source = testDataClient.businessPartner.upsertInput("$testName source")
        val request = testData.relation.input.request.fromSeed(testName)
            .withSource(source.externalId)
            .withTarget("NON_EXISTING_EXTERNAL_ID")

        //WHEN + THEN
        assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            testDataClient.relation.upsertRelationInput(request)
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
            .withValidityPeriods(listOf(RelationValidityPeriodV7Dto(validFrom = sameDate, validTo = sameDate)))
        testDataClient.businessPartner.upsertInput(request.businessPartnerSourceExternalId)
        testDataClient.businessPartner.upsertInput(request.businessPartnerTargetExternalId)

        //WHEN + THEN
        assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            testDataClient.relation.upsertRelationInput(request)
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
                RelationValidityPeriodV7Dto(validFrom = LocalDate.of(2025, 1, 1), validTo = LocalDate.of(2020, 1, 1))
            ))
        testDataClient.businessPartner.upsertInput(request.businessPartnerSourceExternalId)
        testDataClient.businessPartner.upsertInput(request.businessPartnerTargetExternalId)

        //WHEN + THEN
        assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            testDataClient.relation.upsertRelationInput(request)
        }
    }

    /**
     * GIVEN a business partner input
     * WHEN input manager upserts a relation to an unknown target and a relation from that business partner to itself
     * THEN input manager sees one error for each of the relations
     */
    @Test
    fun `try upsert several invalid relations`() {
        //GIVEN
        val businessPartner = testDataClient.businessPartner.upsertInput(testName)
        val unknownTarget = testData.relation.input.request.fromSeed("$testName unknown target")
            .withSource(businessPartner.externalId)
            .withTarget("NON_EXISTING_EXTERNAL_ID")
        val selfRelation = testData.relation.input.request.fromSeed("$testName self relation")
            .withSource(businessPartner.externalId)
            .withTarget(businessPartner.externalId)

        //WHEN
        val response = assertThrows<BadRequest> {
            gateClient.relation.put(createIfNotExist = true, RelationPutV7Request(listOf(unknownTarget, selfRelation)))
        }

        //THEN
        assertRepo.assertErrorPerExternalId(
            response.getResponseBodyAs(GateErrorV7Response::class.java)!!,
            listOf(unknownTarget.externalId, selfRelation.externalId)
        )
    }

    /**
     * GIVEN two business partner inputs
     * WHEN input manager upserts two relations with the same external ID in one request
     * THEN a bad request error is returned
     */
    @Test
    fun `try upsert relations with duplicate external IDs`() {
        //GIVEN
        val request = testData.relation.input.request.fromSeed(testName)
        testDataClient.businessPartner.upsertInput(request.businessPartnerSourceExternalId)
        testDataClient.businessPartner.upsertInput(request.businessPartnerTargetExternalId)

        //WHEN + THEN
        assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(createIfNotExist = true, RelationPutV7Request(listOf(request, request)))
        }
    }

    /**
     * GIVEN the configured upsert limit
     * WHEN input manager upserts one relation more than the limit allows in one request
     * THEN a bad request error is returned
     */
    @Test
    fun `try upsert more relations than the upsert limit allows`() {
        //GIVEN
        val entries = (0..apiConfigProperties.upsertLimit).map { testData.relation.input.request.fromSeed("$testName $it") }

        //WHEN + THEN
        assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(createIfNotExist = true, RelationPutV7Request(entries))
        }
    }

    /**
     * GIVEN two business partner inputs
     * WHEN input manager upserts a valid relation together with a relation from an unknown source
     * THEN neither relation exists
     */
    @Test
    fun `upsert no relation when any relation is invalid`() {
        //GIVEN
        val valid = testData.relation.input.request.fromSeed("$testName valid")
        testDataClient.businessPartner.upsertInput(valid.businessPartnerSourceExternalId)
        testDataClient.businessPartner.upsertInput(valid.businessPartnerTargetExternalId)
        val invalid = testData.relation.input.request.fromSeed("$testName invalid")
            .withSource("NON_EXISTING_EXTERNAL_ID")
            .withTarget(valid.businessPartnerTargetExternalId)

        //WHEN
        assertThrows<BadRequest> {
            gateClient.relation.put(createIfNotExist = true, RelationPutV7Request(listOf(valid, invalid)))
        }

        //THEN
        val actualResponse = gateClient.relation.postSearch(RelationSearchV7Request(externalIds = listOf(valid.externalId, invalid.externalId)))
        assertRepo.assertRelations(actualResponse, PageDto(0, 0, 0, 0, emptyList()))
    }
}
