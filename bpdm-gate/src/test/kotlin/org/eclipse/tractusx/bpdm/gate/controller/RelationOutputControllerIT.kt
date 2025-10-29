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

package org.eclipse.tractusx.bpdm.gate.controller

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.RelationOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.SharableRelationType
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationOutputSearchRequest
import org.eclipse.tractusx.bpdm.gate.entity.RelationDb
import org.eclipse.tractusx.bpdm.gate.entity.RelationOutputDb
import org.eclipse.tractusx.bpdm.gate.entity.RelationSharingStateDb
import org.eclipse.tractusx.bpdm.gate.entity.RelationValidityPeriodDb
import org.eclipse.tractusx.bpdm.gate.repository.RelationRepository
import org.eclipse.tractusx.bpdm.gate.util.GateTestValues
import org.eclipse.tractusx.bpdm.gate.util.PrincipalUtil
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [
    PostgreSQLContextInitializer::class,
    KeyCloakInitializer::class,
    SelfClientAsPartnerUploaderInitializer::class
])
@ActiveProfiles("test")
class RelationOutputControllerIT @Autowired constructor(
    private val gateClient: GateClient,
    private val testHelpers: DbTestHelpers,
    private val relationRepository: RelationRepository,
    private val principalUtil: PrincipalUtil
){
    var testName: String = ""

    /**
     * Represents a time that does not matter as it should be ignored by equality comparisons
     */
    val anyTime: Instant = OffsetDateTime.of(2025, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC).toInstant()


    @BeforeEach
    fun beforeEach(testInfo: TestInfo) {
        testHelpers.truncateDbTables()
        testName = testInfo.displayName
    }

    @Test
    fun getRelationsViaExternalId(){
        val bpnL1 = "$testName LE 1"
        val bpnL2 = "$testName LE 2"
        val bpnL3 = "$testName LE 3"
        val bpnL4 = "$testName LE 4"
        val bpnL5 = "$testName LE 5"
        val bpnL6 = "$testName LE 6"
        val relationId1 = "$testName Relation1"
        val relationId2 = "$testName Relation2"
        val relationId3 = "$testName Relation3"
        val relationId4 = "$testName Relation4"
        val relation1UpdateTime = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val relation2UpdateTime = relation1UpdateTime.plusSeconds(1)
        val relation3UpdateTime = relation2UpdateTime.plusSeconds(2)
        val relation4UpdateTime = relation2UpdateTime.plusSeconds(3)

        createOutputRelation(relationId1, SharableRelationType.IsAlternativeHeadquarterFor, bpnL1, bpnL2, relation1UpdateTime)
        createOutputRelation(relationId2, SharableRelationType.IsAlternativeHeadquarterFor, bpnL1, bpnL3, relation2UpdateTime)
        createOutputRelation(relationId3, SharableRelationType.IsAlternativeHeadquarterFor, bpnL1, bpnL4, relation3UpdateTime)
        createOutputRelation(relationId4, SharableRelationType.IsManagedBy, bpnL5, bpnL6, relation4UpdateTime)

        val expected = PageDto<RelationOutputDto>(2, 1, 0, 2, listOf(
            RelationOutputDto(relationId1, SharableRelationType.IsAlternativeHeadquarterFor, bpnL1, bpnL2, GateTestValues.alwaysActiveRelationValidity, relation1UpdateTime),
            RelationOutputDto(relationId4, SharableRelationType.IsManagedBy, bpnL5, bpnL6, GateTestValues.alwaysActiveRelationValidity, relation4UpdateTime)
        ))
        val actual = gateClient.relationOutput.postSearch(RelationOutputSearchRequest(externalIds = listOf(relationId1, relationId4)))

        Assertions.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getRelationsViaRelationType(){
        val bpnL1 = "$testName LE 1"
        val bpnL2 = "$testName LE 2"
        val bpnL3 = "$testName LE 3"
        val bpnL4 = "$testName LE 4"
        val bpnL5 = "$testName LE 5"
        val bpnL6 = "$testName LE 6"
        val relationId1 = "$testName Relation1"
        val relationId2 = "$testName Relation2"
        val relationId3 = "$testName Relation3"
        val relationId4 = "$testName Relation4"
        val relation1UpdateTime = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val relation2UpdateTime = relation1UpdateTime.plusSeconds(1)
        val relation3UpdateTime = relation2UpdateTime.plusSeconds(2)
        val relation4UpdateTime = relation2UpdateTime.plusSeconds(3)

        createOutputRelation(relationId1, SharableRelationType.IsAlternativeHeadquarterFor, bpnL1, bpnL2, relation1UpdateTime)
        createOutputRelation(relationId2, SharableRelationType.IsAlternativeHeadquarterFor, bpnL1, bpnL3, relation2UpdateTime)
        createOutputRelation(relationId3, SharableRelationType.IsAlternativeHeadquarterFor, bpnL1, bpnL4, relation3UpdateTime)
        createOutputRelation(relationId4, SharableRelationType.IsManagedBy, bpnL5, bpnL6, relation4UpdateTime)

        val expectedAlternativeHeadquarterRelation = PageDto<RelationOutputDto>(3, 1, 0, 3, listOf(
            RelationOutputDto(relationId1, SharableRelationType.IsAlternativeHeadquarterFor, bpnL1, bpnL2, GateTestValues.alwaysActiveRelationValidity, relation1UpdateTime),
            RelationOutputDto(relationId2, SharableRelationType.IsAlternativeHeadquarterFor, bpnL1, bpnL3, GateTestValues.alwaysActiveRelationValidity, relation2UpdateTime),
            RelationOutputDto(relationId3, SharableRelationType.IsAlternativeHeadquarterFor, bpnL1, bpnL4, GateTestValues.alwaysActiveRelationValidity, relation3UpdateTime)
        ))
        val actualAlternativeHeadquarterRelation = gateClient.relationOutput.postSearch(RelationOutputSearchRequest(relationType = SharableRelationType.IsAlternativeHeadquarterFor))

        Assertions.assertThat(actualAlternativeHeadquarterRelation).isEqualTo(expectedAlternativeHeadquarterRelation)

        val expectedManagedRelation = PageDto<RelationOutputDto>(1, 1, 0, 1, listOf(
            RelationOutputDto(relationId4, SharableRelationType.IsManagedBy, bpnL5, bpnL6, GateTestValues.alwaysActiveRelationValidity, relation4UpdateTime)
        ))

        val actualManagedRelation = gateClient.relationOutput.postSearch(RelationOutputSearchRequest(relationType = SharableRelationType.IsManagedBy))

        Assertions.assertThat(actualManagedRelation).isEqualTo(expectedManagedRelation)
    }

    @Test
    fun getRelationsViaSource(){
        val bpnL1 = "$testName LE 1"
        val bpnL2 = "$testName LE 2"
        val bpnL3 = "$testName LE 3"
        val bpnL4 = "$testName LE 4"
        val bpnL5 = "$testName LE 5"
        val bpnL6 = "$testName LE 6"
        val relationId1 = "$testName Relation1"
        val relationId2 = "$testName Relation2"
        val relationId3 = "$testName Relation3"
        val relationId4 = "$testName Relation4"
        val relation1UpdateTime = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val relation2UpdateTime = relation1UpdateTime.plusSeconds(1)
        val relation3UpdateTime = relation2UpdateTime.plusSeconds(2)
        val relation4UpdateTime = relation2UpdateTime.plusSeconds(3)

        createOutputRelation(relationId1, SharableRelationType.IsAlternativeHeadquarterFor, bpnL1, bpnL2, relation1UpdateTime)
        createOutputRelation(relationId2, SharableRelationType.IsAlternativeHeadquarterFor, bpnL2, bpnL3, relation2UpdateTime)
        createOutputRelation(relationId3, SharableRelationType.IsAlternativeHeadquarterFor, bpnL2, bpnL4, relation3UpdateTime)
        createOutputRelation(relationId4, SharableRelationType.IsManagedBy, bpnL5, bpnL6, relation4UpdateTime)

        val expected = PageDto<RelationOutputDto>(2, 1, 0, 2, listOf(
            RelationOutputDto(relationId1, SharableRelationType.IsAlternativeHeadquarterFor, bpnL1, bpnL2, GateTestValues.alwaysActiveRelationValidity, relation1UpdateTime),
            RelationOutputDto(relationId4, SharableRelationType.IsManagedBy, bpnL5, bpnL6, GateTestValues.alwaysActiveRelationValidity, relation4UpdateTime)
        ))
        val actual = gateClient.relationOutput.postSearch(RelationOutputSearchRequest(sourceBpnLs = listOf(bpnL1, bpnL5)))

        Assertions.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getRelationsViaTarget(){
        val bpnL1 = "$testName LE 1"
        val bpnL2 = "$testName LE 2"
        val bpnL3 = "$testName LE 3"
        val bpnL4 = "$testName LE 4"
        val bpnL5 = "$testName LE 5"
        val bpnL6 = "$testName LE 6"
        val relationId1 = "$testName Relation1"
        val relationId2 = "$testName Relation2"
        val relationId3 = "$testName Relation3"
        val relationId4 = "$testName Relation4"
        val relation1UpdateTime = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val relation2UpdateTime = relation1UpdateTime.plusSeconds(1)
        val relation3UpdateTime = relation2UpdateTime.plusSeconds(2)
        val relation4UpdateTime = relation2UpdateTime.plusSeconds(3)

        createOutputRelation(relationId1, SharableRelationType.IsAlternativeHeadquarterFor, bpnL1, bpnL2, relation1UpdateTime)
        createOutputRelation(relationId2, SharableRelationType.IsAlternativeHeadquarterFor, bpnL1, bpnL3, relation2UpdateTime)
        createOutputRelation(relationId3, SharableRelationType.IsAlternativeHeadquarterFor, bpnL1, bpnL4, relation3UpdateTime)
        createOutputRelation(relationId4, SharableRelationType.IsManagedBy, bpnL5, bpnL6, relation4UpdateTime)

        val expected = PageDto<RelationOutputDto>(2, 1, 0, 2, listOf(
            RelationOutputDto(relationId1, SharableRelationType.IsAlternativeHeadquarterFor, bpnL1, bpnL2, GateTestValues.alwaysActiveRelationValidity, relation1UpdateTime),
            RelationOutputDto(relationId4, SharableRelationType.IsManagedBy, bpnL5, bpnL6, GateTestValues.alwaysActiveRelationValidity, relation4UpdateTime)
        ))
        val actual = gateClient.relationOutput.postSearch(RelationOutputSearchRequest(targetBpnLs = listOf(bpnL2, bpnL6)))

        Assertions.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getRelationsViaUpdatedAfter(){
        val bpnL1 = "$testName LE 1"
        val bpnL2 = "$testName LE 2"
        val bpnL3 = "$testName LE 3"
        val bpnL4 = "$testName LE 4"
        val bpnL5 = "$testName LE 5"
        val bpnL6 = "$testName LE 6"
        val relationId1 = "$testName Relation1"
        val relationId2 = "$testName Relation2"
        val relationId3 = "$testName Relation3"
        val relationId4 = "$testName Relation4"
        val relation1UpdateTime = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val relation2UpdateTime = relation1UpdateTime.plusSeconds(1)
        val relation3UpdateTime = relation2UpdateTime.plusSeconds(2)
        val relation4UpdateTime = relation2UpdateTime.plusSeconds(3)

        createOutputRelation(relationId1, SharableRelationType.IsAlternativeHeadquarterFor, bpnL1, bpnL2, relation1UpdateTime)
        createOutputRelation(relationId2, SharableRelationType.IsAlternativeHeadquarterFor, bpnL1, bpnL3, relation2UpdateTime)
        createOutputRelation(relationId3, SharableRelationType.IsAlternativeHeadquarterFor, bpnL1, bpnL4, relation3UpdateTime)
        createOutputRelation(relationId4, SharableRelationType.IsManagedBy, bpnL5, bpnL6, relation4UpdateTime)

        val expected = PageDto<RelationOutputDto>(2, 1, 0, 2, listOf(
            RelationOutputDto(relationId3, SharableRelationType.IsAlternativeHeadquarterFor, bpnL1, bpnL4, GateTestValues.alwaysActiveRelationValidity, relation3UpdateTime),
            RelationOutputDto(relationId4, SharableRelationType.IsManagedBy, bpnL5, bpnL6, GateTestValues.alwaysActiveRelationValidity, relation4UpdateTime)
        ))
        val actual = gateClient.relationOutput.postSearch(RelationOutputSearchRequest(updatedAtFrom = relation2UpdateTime))

        Assertions.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getRelationsViaAllFilters(){
        val bpnL1 = "$testName LE 1"
        val bpnL2 = "$testName LE 2"
        val bpnL3 = "$testName LE 3"
        val bpnL4 = "$testName LE 4"
        val bpnL5 = "$testName LE 5"
        val bpnL6 = "$testName LE 6"
        val bpnL7 = "$testName LE 7"
        val bpnL8 = "$testName LE 8"
        val bpnL9 = "$testName LE 9"
        val bpnL10 = "$testName LE 10"
        val bpnL11 = "$testName LE 11"
        val bpnL12 = "$testName LE 12"
        val relationId1 = "$testName Relation1"
        val relationId2 = "$testName Relation2"
        val relationId3 = "$testName Relation3"
        val relationId4 = "$testName Relation4"
        val relationId5 = "$testName Relation5"
        val relationId6 = "$testName Relation6"
        val relation1UpdateTime = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val relation2UpdateTime = relation1UpdateTime.plusSeconds(1)
        val relation3UpdateTime = relation2UpdateTime.plusSeconds(2)
        val relation4UpdateTime = relation3UpdateTime.plusSeconds(2)
        val relation5UpdateTime = relation4UpdateTime.plusSeconds(2)
        val relation6UpdateTime = relation5UpdateTime.plusSeconds(2)

        createOutputRelation(relationId1, SharableRelationType.IsAlternativeHeadquarterFor, bpnL1, bpnL2, relation1UpdateTime)
        createOutputRelation(relationId2, SharableRelationType.IsAlternativeHeadquarterFor, bpnL3, bpnL4, relation2UpdateTime)
        createOutputRelation(relationId3, SharableRelationType.IsAlternativeHeadquarterFor, bpnL5, bpnL6, relation3UpdateTime)
        createOutputRelation(relationId4, SharableRelationType.IsManagedBy, bpnL7, bpnL8, relation4UpdateTime)
        createOutputRelation(relationId5, SharableRelationType.IsManagedBy, bpnL9, bpnL10, relation5UpdateTime)
        createOutputRelation(relationId6, SharableRelationType.IsManagedBy, bpnL11, bpnL12, relation6UpdateTime)

        val expectedAlternativeHeadquarterRelation = PageDto<RelationOutputDto>(1, 1, 0, 1, listOf(
            RelationOutputDto(relationId2, SharableRelationType.IsAlternativeHeadquarterFor, bpnL3, bpnL4, GateTestValues.alwaysActiveRelationValidity, relation2UpdateTime)
        ))
        val actualAlternativeHeadquarterRelation = gateClient.relationOutput.postSearch(RelationOutputSearchRequest(
            externalIds = listOf(relationId1, relationId2, relationId3),
            relationType = SharableRelationType.IsAlternativeHeadquarterFor,
            sourceBpnLs = listOf(bpnL1, bpnL3),
            targetBpnLs = listOf(bpnL2, bpnL4, bpnL6),
            updatedAtFrom = relation1UpdateTime
        ))

        Assertions.assertThat(actualAlternativeHeadquarterRelation).isEqualTo(expectedAlternativeHeadquarterRelation)

        val expectedManagedRelation = PageDto<RelationOutputDto>(2, 1, 0, 2, listOf(
            RelationOutputDto(relationId4, SharableRelationType.IsManagedBy, bpnL7, bpnL8, GateTestValues.alwaysActiveRelationValidity, relation4UpdateTime),
            RelationOutputDto(relationId5, SharableRelationType.IsManagedBy, bpnL9, bpnL10, GateTestValues.alwaysActiveRelationValidity, relation5UpdateTime)
        ))
        val actualManagedRelation = gateClient.relationOutput.postSearch(RelationOutputSearchRequest(
            externalIds = listOf(relationId4, relationId5),
            relationType = SharableRelationType.IsManagedBy,
            sourceBpnLs = listOf(bpnL7, bpnL9),
            targetBpnLs = listOf(bpnL8, bpnL10, bpnL12),
            updatedAtFrom = relation1UpdateTime
        ))

        Assertions.assertThat(actualManagedRelation).isEqualTo(expectedManagedRelation)
    }

    private fun createOutputRelation(externalId: String, relationType: SharableRelationType, source: String, target: String, updatedAt: Instant){
        val output = RelationOutputDb(
            relationType = relationType,
            sourceBpnL = source,
            targetBpnL = target,
            updatedAt = updatedAt,
            validityPeriods = GateTestValues.alwaysActiveRelationValidity.map {
                    RelationValidityPeriodDb(
                        validFrom = it.validFrom,
                        validTo = it.validTo
                    )
                }.toMutableList()
        )
        val successSharingState = RelationSharingStateDb(RelationSharingStateType.Success, null, null, anyTime, null, null, false)
        val relation = RelationDb(externalId, principalUtil.resolveTenantBpnl().value, successSharingState, output)
        relationRepository.save(relation)
    }

}