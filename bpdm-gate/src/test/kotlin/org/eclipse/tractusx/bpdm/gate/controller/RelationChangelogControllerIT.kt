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
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.api.model.request.ChangelogSearchRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutEntry
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.ChangelogGateDto
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.gate.GateInputFactory
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [
    PostgreSQLContextInitializer::class,
    KeyCloakInitializer::class,
    SelfClientAsPartnerUploaderInitializer::class
])
@ActiveProfiles("test")
class RelationChangelogControllerIT @Autowired constructor(
    private val gateClient: GateClient,
    private val testHelpers: DbTestHelpers,
    private val gateInputFactory: GateInputFactory
) {

    private var testName: String = ""

    /**
     * Represents a time that does not matter as it should be ignored by equality comparisons
     */
    private val anyTime: Instant = OffsetDateTime.of(2025, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC).toInstant()


    @BeforeEach
    fun beforeEach(testInfo: TestInfo) {
        testHelpers.truncateDbTables()
        testName = testInfo.displayName
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun changelogCreated(relationType: RelationType){
        givenRelations(RelationPutEntry("${testName}_rel", relationType, "${testName}_source", "${testName}_target"))

        val expectedContent = listOf(
            ChangelogGateDto("${testName}_rel", anyTime, ChangelogType.CREATE)
        )
        val response = gateClient.relationChangelog.getInputChangelog(PaginationRequest(), ChangelogSearchRequest(null, null))

        assertChangelogsEqual(response.content, expectedContent)
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun changelogUpdated(relationType: RelationType){
        val relationExternalId = "${testName}_rel"
        val sourceExternalId = "${testName}_source"
        val targetExternalId = "${testName}_target"
        givenRelations(RelationPutEntry(relationExternalId, relationType, sourceExternalId, targetExternalId))
        gateClient.relation.put(false, RelationPutRequest(listOf(RelationPutEntry(relationExternalId, relationType, targetExternalId, sourceExternalId))))

        val expectedContent = listOf(
            ChangelogGateDto("${testName}_rel", anyTime, ChangelogType.CREATE),
            ChangelogGateDto("${testName}_rel", anyTime, ChangelogType.UPDATE)
        )
        val response = gateClient.relationChangelog.getInputChangelog(PaginationRequest(), ChangelogSearchRequest(null, null))

        assertChangelogsEqual(response.content, expectedContent)
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun filterByExternalId(relationType: RelationType){
        val externalId1 = "${testName}_rel1"
        val externalId2 = "${testName}_rel2"

        givenRelations(
            RelationPutEntry(externalId1, relationType, "${externalId1}_source", "${externalId1}_target"),
            RelationPutEntry(externalId2, relationType, "${externalId2}_source", "${externalId2}_target"),
            )

        val expectedContent = listOf(
            ChangelogGateDto(externalId1, anyTime, ChangelogType.CREATE)
        )
        val response = gateClient.relationChangelog.getInputChangelog(PaginationRequest(), ChangelogSearchRequest(externalIds = setOf(externalId1)))

        assertChangelogsEqual(response.content, expectedContent)
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun filterByTimestamp(relationType: RelationType){
        val externalId1 = "${testName}_rel1"
        val externalId2 = "${testName}_rel2"

        givenRelations(RelationPutEntry(externalId1, relationType, "${externalId1}_source", "${externalId1}_target"))

        val timeAfterFirstCreate = Instant.now()

        givenRelations(RelationPutEntry(externalId2, relationType, "${externalId2}_source", "${externalId2}_target"))

        val expectedContent = listOf(
            ChangelogGateDto(externalId2, anyTime, ChangelogType.CREATE)
        )
        val response = gateClient.relationChangelog.getInputChangelog(PaginationRequest(), ChangelogSearchRequest(timestampAfter = timeAfterFirstCreate))

        assertChangelogsEqual(response.content, expectedContent)
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun filterByAll(relationType: RelationType){
        val externalId1 = "${testName}_rel1"
        val externalId2 = "${testName}_rel2"
        val externalId3 = "${testName}_rel3"

        givenRelations(RelationPutEntry(externalId1, relationType, "${externalId1}_source", "${externalId1}_target"))

        val timeAfterFirstCreate = Instant.now()

        givenRelations(
            RelationPutEntry(externalId2, relationType, "${externalId2}_source", "${externalId2}_target"),
            RelationPutEntry(externalId3, relationType, "${externalId3}_source", "${externalId3}_target"),
        )

        val expectedContent = listOf(
            ChangelogGateDto(externalId2, anyTime, ChangelogType.CREATE)
        )
        val response = gateClient.relationChangelog.getInputChangelog(PaginationRequest(), ChangelogSearchRequest(timestampAfter = timeAfterFirstCreate, externalIds = setOf(externalId1, externalId2)))

        assertChangelogsEqual(response.content, expectedContent)
    }


    private fun givenRelations(vararg relations: RelationPutEntry){
        val sources = relations.map {  gateInputFactory.createFullValid(it.businessPartnerSourceExternalId) }
        val targets = relations.map {  gateInputFactory.createFullValid(it.businessPartnerTargetExternalId) }

        gateClient.businessParters.upsertBusinessPartnersInput(sources + targets)
        gateClient.relation.put(true, RelationPutRequest(relations.toList()))
    }


    private fun assertChangelogsEqual(actualList: Collection<ChangelogGateDto>, expectedList: Collection<ChangelogGateDto>){
        Assertions.assertThat(actualList)
            .usingRecursiveComparison()
            .ignoringFields(ChangelogGateDto::timestamp.name)
            .isEqualTo(expectedList)

    }

}