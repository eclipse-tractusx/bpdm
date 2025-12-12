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
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutRequest
import org.eclipse.tractusx.bpdm.gate.entity.RelationSharingStateDb
import org.eclipse.tractusx.bpdm.gate.repository.RelationRepository
import org.eclipse.tractusx.bpdm.gate.util.PrincipalUtil
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.gate.GateInputFactory
import org.eclipse.tractusx.bpdm.test.testdata.gate.withAddressType
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.eclipse.tractusx.bpdm.test.util.Timeframe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [
    PostgreSQLContextInitializer::class,
    KeyCloakInitializer::class,
    SelfClientAsPartnerUploaderInitializer::class
])
@ActiveProfiles("test")
class SharingStateControllerIT @Autowired constructor(
    private val testHelpers: DbTestHelpers,
    private val inputFactory: GateInputFactory,
    private val gateClient: GateClient,
    private val relationRepository: RelationRepository,
    private val principalUtil: PrincipalUtil
) {
    /**
     * Represents a time that does not matter as it should be ignored by equality comparisons
     */
    val anyTime: Instant = Instant.MIN

    var testName: String = ""

    @BeforeEach
    fun beforeEach(testInfo: TestInfo) {
        testHelpers.truncateDbTables()
        testName = testInfo.displayName
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun createSharingStateOnSharableRelation(relationType: RelationType){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"
        val relationId = "$testName R 1"

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2)
        ))

        val creationTimeframe = createRelation(relationId, relationType, legalEntityId1, legalEntityId2)

        val expected = PageDto(1, 1, 0, 1,
            listOf(RelationSharingStateDto(relationId, RelationSharingStateType.Ready, null, null, null, anyTime))
        )

        val actual = gateClient.relationSharingState.get(externalIds = listOf(relationId))

        assertIsEqual(actual, expected, creationTimeframe)
    }


    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun updateSharingStateOnChange(relationType: RelationType){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"
        val legalEntityId3 = "$testName LE 3"
        val relationId = "$testName R 1"

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2),
            createLegalEntityRequest(legalEntityId3)
        ))

        createRelation(relationId, relationType, legalEntityId1, legalEntityId2)

        val updateTimeframe = createRelation(relationId, relationType, legalEntityId1, legalEntityId3)

        val expected = PageDto(1, 1, 0, 1,
            listOf(RelationSharingStateDto(relationId, RelationSharingStateType.Ready, null, null, null, anyTime))
        )

        val actual = gateClient.relationSharingState.get(externalIds = listOf(relationId))

        assertIsEqual(actual, expected, updateTimeframe)
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun onNoChangeNoUpdate(relationType: RelationType){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"
        val legalEntityId3 = "$testName LE 3"
        val relationId = "$testName R 1"

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2),
            createLegalEntityRequest(legalEntityId3)
        ))

        val creationTimeframe = createRelation(relationId, relationType, legalEntityId1, legalEntityId2)
        createRelation(relationId, relationType, legalEntityId1, legalEntityId2)

        val expected = PageDto(1, 1, 0, 1,
            listOf(RelationSharingStateDto(relationId, RelationSharingStateType.Ready, null, null, null, anyTime))
        )

        val actual = gateClient.relationSharingState.get(externalIds = listOf(relationId))

        assertIsEqual(actual, expected, creationTimeframe)
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun filterByExternalIds(relationType: RelationType){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"
        val legalEntityId3 = "$testName LE 3"
        val legalEntityId4 = "$testName LE 4"
        val relationId1 = "$testName R 1"
        val relationId2 = "$testName R 2"

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2),
            createLegalEntityRequest(legalEntityId3),
            createLegalEntityRequest(legalEntityId4)
        ))

        val creationTimeframe = createSharableRelations(
            listOf(
                RelationContent(relationId1, relationType, legalEntityId1, legalEntityId2),
                RelationContent(relationId2, relationType, legalEntityId3, legalEntityId4),
            ),
            relationType
        )

        val expected = PageDto(1, 1, 0, 1,
            listOf(RelationSharingStateDto(relationId1, RelationSharingStateType.Ready, null, null, null, anyTime))
        )

        val actual = gateClient.relationSharingState.get(externalIds = listOf(relationId1))

        assertIsEqual(actual, expected, creationTimeframe)
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun filterBySharingStateType(relationType: RelationType){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"
        val legalEntityId3 = "$testName LE 3"
        val legalEntityId4 = "$testName LE 4"
        val relationId1 = "$testName R 1"
        val relationId2 = "$testName R 2"

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2),
            createLegalEntityRequest(legalEntityId3),
            createLegalEntityRequest(legalEntityId4)
        ))

        val creationTimeframe = createSharableRelations(
            listOf(
                RelationContent(relationId1, relationType, legalEntityId1, legalEntityId2),
                RelationContent(relationId2, relationType, legalEntityId3, legalEntityId4),
            ),
            relationType
        )

        setSharingState(relationId1, RelationSharingStateType.Pending)

        val expected = PageDto(1, 1, 0, 1,
            listOf(RelationSharingStateDto(relationId1, RelationSharingStateType.Pending, null, null,  null, anyTime))
        )

        val actual = gateClient.relationSharingState.get(sharingStateTypes = listOf(RelationSharingStateType.Pending))

        assertIsEqual(actual, expected, creationTimeframe)
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun filterByUpdatedAfter(relationType: RelationType){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"
        val legalEntityId3 = "$testName LE 3"
        val legalEntityId4 = "$testName LE 4"
        val relationId1 = "$testName R 1"
        val relationId2 = "$testName R 2"

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2),
            createLegalEntityRequest(legalEntityId3),
            createLegalEntityRequest(legalEntityId4)
        ))

        createSharableRelations(
            listOf(
                RelationContent(relationId1, relationType, legalEntityId1, legalEntityId2),
                RelationContent(relationId2, relationType, legalEntityId3, legalEntityId4),
            ),
            relationType
        )

        val updatedTimeframe = createRelation(relationId1, relationType, legalEntityId1, legalEntityId3)


        val expected = PageDto(1, 1, 0, 1,
            listOf(RelationSharingStateDto(relationId1, RelationSharingStateType.Ready, null, null, null, anyTime))
        )

        val actual = gateClient.relationSharingState.get(updatedAfter = updatedTimeframe.startTime)

        assertIsEqual(actual, expected, updatedTimeframe)
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun filterByAllFilters(relationType: RelationType){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"
        val legalEntityId3 = "$testName LE 3"
        val legalEntityId4 = "$testName LE 4"
        val legalEntityId5 = "$testName LE 5"
        val legalEntityId6 = "$testName LE 6"
        val legalEntityId7 = "$testName LE 7"
        val legalEntityId8 = "$testName LE 8"
        val relationId1 = "$testName R 1"
        val relationId2 = "$testName R 2"
        val relationId3 = "$testName R 3"
        val relationId4 = "$testName R 4"

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2),
            createLegalEntityRequest(legalEntityId3),
            createLegalEntityRequest(legalEntityId4),
            createLegalEntityRequest(legalEntityId5),
            createLegalEntityRequest(legalEntityId6),
            createLegalEntityRequest(legalEntityId7),
            createLegalEntityRequest(legalEntityId8)
        ))

        createRelation(relationId2, relationType, legalEntityId1, legalEntityId3)

        val creationTimeframe = createSharableRelations(
            listOf(
                RelationContent(relationId1, relationType, legalEntityId1, legalEntityId2),
                RelationContent(relationId3, relationType, legalEntityId5, legalEntityId6),
                RelationContent(relationId4, relationType, legalEntityId7, legalEntityId8),
            ),
            relationType
        )

        setSharingState(relationId1, RelationSharingStateType.Pending)
        setSharingState(relationId2, RelationSharingStateType.Pending)
        setSharingState(relationId4, RelationSharingStateType.Pending)

        val expected = PageDto(1, 1, 0, 1,
            listOf(RelationSharingStateDto(relationId1, RelationSharingStateType.Pending, null, null, null, anyTime)))

        val actual = gateClient.relationSharingState.get(
            externalIds = listOf(relationId1, relationId2, relationId3),
            sharingStateTypes = listOf(RelationSharingStateType.Pending),
            updatedAfter = creationTimeframe.startTime
        )

        assertIsEqual(actual, expected, creationTimeframe)
    }


    private fun createLegalEntityRequest(externalId: String) =
        inputFactory.createAllFieldsFilled(externalId).request
            .withAddressType(AddressType.LegalAddress)
            .copy(isOwnCompanyData = true)

    private fun createRelation(externalId: String, relationType: RelationType, source: String, target: String): Timeframe{
        val beforeCreation = Instant.now()
        gateClient.relation.put(
            createIfNotExist = true,
            RelationPutRequest(listOf(inputFactory.buildRelation(externalId, relationType, source, target)))
        )
        val afterCreation = Instant.now()

        return Timeframe(beforeCreation, afterCreation)
    }

    private fun createSharableRelations(requests: List<RelationContent>, relationType: RelationType): Timeframe{
        val beforeCreation = Instant.now()
        requests.forEach { createRelation(it.externalId, relationType, it.source, it.target) }
        val afterCreation = Instant.now()
        return Timeframe(beforeCreation, afterCreation)
    }

    private fun assertIsEqual(actual: PageDto<RelationSharingStateDto>, expected: PageDto<RelationSharingStateDto>, updateTimeframe: Timeframe){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(PageDto<RelationSharingStateDto>::content.name)
            .isEqualTo(expected)

        actual.content
            .sortedBy { it.externalId }
            .zip(expected.content.sortedBy { it.externalId })
            .forEach { (actualContent, expectedContent) ->
                assertIsEqual(actualContent, expectedContent, updateTimeframe)
            }
    }

    private fun assertIsEqual(actual: RelationSharingStateDto, expected: RelationSharingStateDto, updateTimeframe: Timeframe){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(RelationSharingStateDb::updatedAt.name)
            .isEqualTo(expected)

        Assertions.assertThat(actual.updatedAt).isBetween(updateTimeframe.startTime, updateTimeframe.endTime)
    }

    private fun setSharingState(externalId: String, sharingState: RelationSharingStateType){
        val relation = relationRepository.findByTenantBpnLAndExternalId(principalUtil.resolveTenantBpnl().value, externalId)!!
        relation.sharingState!!.sharingStateType = sharingState
        relationRepository.save(relation)
    }

    data class RelationContent(
        val externalId: String,
        val relationType: RelationType,
        val source: String,
        val target: String
    )

}