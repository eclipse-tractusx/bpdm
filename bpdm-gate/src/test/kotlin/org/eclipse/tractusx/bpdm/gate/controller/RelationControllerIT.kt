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
import org.eclipse.tractusx.bpdm.gate.api.model.RelationDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutEntry
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationSearchRequest
import org.eclipse.tractusx.bpdm.gate.util.GateTestValues
import org.eclipse.tractusx.bpdm.gate.util.MockAndAssertUtils
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
import org.springframework.web.reactive.function.client.WebClientResponseException.BadRequest
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [
    PostgreSQLContextInitializer::class,
    KeyCloakInitializer::class,
    SelfClientAsPartnerUploaderInitializer::class
])
@ActiveProfiles("test")
class RelationControllerIT @Autowired constructor(
    private val gateClient: GateClient,
    private val testHelpers: DbTestHelpers,
    private val inputFactory: GateInputFactory,
    private val mockAndAssertUtils: MockAndAssertUtils
){
    var testName: String = ""

    @BeforeEach
    fun beforeEach(testInfo: TestInfo) {
        testHelpers.truncateDbTables()
        testName = testInfo.displayName
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun putUpdateRelation(relationType: RelationType){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"
        val legalEntityId3 = "$testName LE 3"
        val legalEntityId4 = "$testName LE 4"
        val relationId = "$testName Relation"

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2),
            createLegalEntityRequest(legalEntityId3),
            createLegalEntityRequest(legalEntityId4)
        ))

        val beforeCreation = Instant.now()
        gateClient.relation.put(
            createIfNotExist = true,
            singleUpserRequest(
                externalId = relationId,
                relationType = relationType,
                businessPartnerSourceExternalId = legalEntityId1,
                businessPartnerTargetExternalId = legalEntityId2
            )
        )
        val afterCreation = Instant.now()

        val beforeUpdate = Instant.now()
        val response = gateClient.relation.put(
            createIfNotExist = false,
            singleUpserRequest(
                externalId = relationId,
                relationType = relationType,
                businessPartnerSourceExternalId = legalEntityId3,
                businessPartnerTargetExternalId = legalEntityId4
            )
        )
        val afterUpdate = Instant.now()

        val expectation = RelationDto(
            externalId = relationId,
            relationType = relationType,
            businessPartnerSourceExternalId = legalEntityId3,
            businessPartnerTargetExternalId = legalEntityId4,
            validityPeriods = GateTestValues.alwaysActiveRelationValidity,
            updatedAt = Instant.now(),
            createdAt = Instant.now(),
        )

        mockAndAssertUtils.assertRelation(
            actual = response,
            expectation = expectation,
            updateTimeframe = Timeframe(beforeUpdate, afterUpdate),
            createTimeframe = Timeframe(beforeCreation, afterCreation),
            ignoreExternalId = false)
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun putUpdateRelationWithLegalAndSiteMainAddress(relationType: RelationType){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"
        val legalEntityId3 = "$testName LE 3"
        val legalEntityId4 = "$testName LE 4"
        val relationId = "$testName Relation"

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            createLegalEntityRequest(legalEntityId1).withAddressType(AddressType.LegalAndSiteMainAddress),
            createLegalEntityRequest(legalEntityId2).withAddressType(AddressType.LegalAndSiteMainAddress),
            createLegalEntityRequest(legalEntityId3).withAddressType(AddressType.LegalAndSiteMainAddress),
            createLegalEntityRequest(legalEntityId4).withAddressType(AddressType.LegalAndSiteMainAddress)
        ))

        val beforeCreation = Instant.now()
        gateClient.relation.put(
            createIfNotExist = true,
            singleUpserRequest(
                externalId = relationId,
                relationType = relationType,
                businessPartnerSourceExternalId = legalEntityId1,
                businessPartnerTargetExternalId = legalEntityId2
            )
        )
        val afterCreation = Instant.now()

        val beforeUpdate = Instant.now()
        val response = gateClient.relation.put(
            createIfNotExist = false,
            singleUpserRequest(
                externalId = relationId,
                relationType = relationType,
                businessPartnerSourceExternalId = legalEntityId3,
                businessPartnerTargetExternalId = legalEntityId4
            )
        )
        val afterUpdate = Instant.now()

        val expectation = RelationDto(
            externalId = relationId,
            relationType = relationType,
            businessPartnerSourceExternalId = legalEntityId3,
            businessPartnerTargetExternalId = legalEntityId4,
            validityPeriods = GateTestValues.alwaysActiveRelationValidity,
            updatedAt = Instant.now(),
            createdAt = Instant.now()
        )

        mockAndAssertUtils.assertRelation(
            actual = response,
            expectation = expectation,
            updateTimeframe = Timeframe(beforeUpdate, afterUpdate),
            createTimeframe = Timeframe(beforeCreation, afterCreation),
            ignoreExternalId = false)
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun putUpdateRelationWithTenant(relationType: RelationType){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"
        val legalEntityId3 = "$testName LE 3"
        val legalEntityId4 = "$testName LE 4"
        val relationId = "$testName Relation"

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2),
            createLegalEntityRequest(legalEntityId3),
            createLegalEntityRequest(legalEntityId4)
        ))

        val beforeCreation = Instant.now()
        gateClient.relation.put(
            createIfNotExist = true,
            singleUpserRequest(
                externalId = relationId,
                relationType = relationType,
                businessPartnerSourceExternalId = legalEntityId1,
                businessPartnerTargetExternalId = legalEntityId2
            )
        )
        val afterCreation = Instant.now()

        val beforeUpdate = Instant.now()
        val response = gateClient.relation.put(
            createIfNotExist = false,
            singleUpserRequest(
                externalId = relationId,
                relationType = relationType,
                businessPartnerSourceExternalId = legalEntityId3,
                businessPartnerTargetExternalId = legalEntityId4
            )
        )
        val afterUpdate = Instant.now()

        val expectation = RelationDto(
            externalId = relationId,
            relationType = relationType,
            businessPartnerSourceExternalId = legalEntityId3,
            businessPartnerTargetExternalId = legalEntityId4,
            validityPeriods = GateTestValues.alwaysActiveRelationValidity,
            updatedAt = Instant.now(),
            createdAt = Instant.now()
        )

        mockAndAssertUtils.assertRelation(
            actual = response,
            expectation = expectation,
            updateTimeframe = Timeframe(beforeUpdate, afterUpdate),
            createTimeframe = Timeframe(beforeCreation, afterCreation),
            ignoreExternalId = false)
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun putUpdateRelationWithMultipleSources(relationType: RelationType){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"
        val legalEntityId3 = "$testName LE 3"
        val legalEntityId4 = "$testName LE 4"
        val relationId1 = "$testName Relation 1"
        val relationId2 = "$testName Relation 2"

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2),
            createLegalEntityRequest(legalEntityId3),
            createLegalEntityRequest(legalEntityId4)
        ))

        val beforeCreation = Instant.now()
        gateClient.relation.put(
            createIfNotExist = true,
            singleUpserRequest(
                externalId = relationId1,
                relationType = relationType,
                businessPartnerSourceExternalId = legalEntityId2,
                businessPartnerTargetExternalId = legalEntityId1
            )
        )
        gateClient.relation.put(
            createIfNotExist = true,
            singleUpserRequest(
                externalId = relationId2,
                relationType = relationType,
                businessPartnerSourceExternalId = legalEntityId3,
                businessPartnerTargetExternalId = legalEntityId1
            )
        )
        val afterCreation = Instant.now()

        val beforeUpdate = Instant.now()
        val response = gateClient.relation.put(
            createIfNotExist = false,
            singleUpserRequest(
                externalId = relationId2,
                relationType = relationType,
                businessPartnerSourceExternalId = legalEntityId4,
                businessPartnerTargetExternalId = legalEntityId1
            )
        )
        val afterUpdate = Instant.now()

        val expectation = RelationDto(
            externalId = relationId2,
            relationType = relationType,
            businessPartnerSourceExternalId = legalEntityId4,
            businessPartnerTargetExternalId = legalEntityId1,
            validityPeriods = GateTestValues.alwaysActiveRelationValidity,
            updatedAt = Instant.now(),
            createdAt = Instant.now()
        )

        mockAndAssertUtils.assertRelation(
            actual = response,
            expectation = expectation,
            updateTimeframe = Timeframe(beforeUpdate, afterUpdate),
            createTimeframe = Timeframe(beforeCreation, afterCreation),
            ignoreExternalId = false)
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun putUpdateRelationIdNotExist(relationType: RelationType){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2),
        )
        )

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = false,
                singleUpserRequest(
                    externalId = testName,
                    relationType = relationType,
                    businessPartnerSourceExternalId = legalEntityId1,
                    businessPartnerTargetExternalId = legalEntityId1
                )
            )
        }
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun putUpdateRelationWithSourceEqualsTarget(relationType: RelationType){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"
        val legalEntityId3 = "$testName LE 3"
        val legalEntityId4 = "$testName LE 4"
        val relationId = "$testName Relation"

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2),
            createLegalEntityRequest(legalEntityId3),
            createLegalEntityRequest(legalEntityId4)
        ))

        gateClient.relation.put(
            createIfNotExist = true,
            singleUpserRequest(
                externalId = relationId,
                relationType = relationType,
                businessPartnerSourceExternalId = legalEntityId1,
                businessPartnerTargetExternalId = legalEntityId2
            )
        )

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = false,
                singleUpserRequest(
                    externalId = relationId,
                    relationType = relationType,
                    businessPartnerSourceExternalId = legalEntityId3,
                    businessPartnerTargetExternalId = legalEntityId3
                )
            )
        }
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun putUpdateRelationWithSourceNotExist(relationType: RelationType){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"
        val legalEntityId3 = "$testName LE 3"
        val legalEntityId4 = "$testName LE 4"
        val relationId = "$testName Relation"

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2),
            createLegalEntityRequest(legalEntityId3),
            createLegalEntityRequest(legalEntityId4)
        ))

        gateClient.relation.put(
            createIfNotExist = true,
            singleUpserRequest(
                externalId = relationId,
                relationType = relationType,
                businessPartnerSourceExternalId = legalEntityId1,
                businessPartnerTargetExternalId = legalEntityId2
            )
        )

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = false,
                singleUpserRequest(
                    externalId = relationId,
                    relationType = relationType,
                    businessPartnerSourceExternalId = "NOT EXISTS",
                    businessPartnerTargetExternalId = legalEntityId3
                )
            )
        }
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun putUpdateRelationWithTargetNotExist(relationType: RelationType){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"
        val legalEntityId3 = "$testName LE 3"
        val legalEntityId4 = "$testName LE 4"
        val relationId = "$testName Relation"

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2),
            createLegalEntityRequest(legalEntityId3),
            createLegalEntityRequest(legalEntityId4)
        ))

        gateClient.relation.put(
            createIfNotExist = true,
            singleUpserRequest(
                externalId = relationId,
                relationType = relationType,
                businessPartnerSourceExternalId = legalEntityId1,
                businessPartnerTargetExternalId = legalEntityId2
            )
        )

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = false,
                singleUpserRequest(
                    externalId = relationId,
                    relationType = relationType,
                    businessPartnerSourceExternalId = legalEntityId3,
                    businessPartnerTargetExternalId = "NOT EXISTS"
                )
            )
        }
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun putUpsertRelation(relationType: RelationType){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"
        val relationId = "$testName Relation"

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2)
        ))

        val beforePost = Instant.now()
        val response = gateClient.relation.put(
            createIfNotExist = true,
            singleUpserRequest(
                externalId = relationId,
                relationType = relationType,
                businessPartnerSourceExternalId = legalEntityId1,
                businessPartnerTargetExternalId = legalEntityId2
            )
        )
        val afterPost = Instant.now()

        val expectation = RelationDto(
            externalId = relationId,
            relationType = relationType,
            businessPartnerSourceExternalId = legalEntityId1,
            businessPartnerTargetExternalId = legalEntityId2,
            validityPeriods = GateTestValues.alwaysActiveRelationValidity,
            updatedAt = Instant.now(),
            createdAt = Instant.now()
        )

        mockAndAssertUtils.assertRelation(response, expectation, Timeframe(beforePost, afterPost), ignoreExternalId = false)
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun putUpsertRelationWithTenant(relationType: RelationType){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"
        val relationId = "$testName Relation"

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2)
        ))

        val beforePost = Instant.now()
        val response = gateClient.relation.put(
            createIfNotExist = true,
            singleUpserRequest(
                externalId = relationId,
                relationType = relationType,
                businessPartnerSourceExternalId = legalEntityId1,
                businessPartnerTargetExternalId = legalEntityId2
            )
        )
        val afterPost = Instant.now()

        val expectation = RelationDto(
            externalId = relationId,
            relationType = relationType,
            businessPartnerSourceExternalId = legalEntityId1,
            businessPartnerTargetExternalId = legalEntityId2,
            validityPeriods = GateTestValues.alwaysActiveRelationValidity,
            updatedAt = Instant.now(),
            createdAt = Instant.now()
        )

        mockAndAssertUtils.assertRelation(response, expectation, Timeframe(beforePost, afterPost), ignoreExternalId =  false)
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun putUpsertRelationWithMultipleSources(relationType: RelationType){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"
        val legalEntityId3 = "$testName LE 3"
        val relationId1 = "$testName Relation 1"
        val relationId2 = "$testName Relation 2"

        gateClient.businessParters.upsertBusinessPartnersInput( listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2),
            createLegalEntityRequest(legalEntityId3)
        ))
        gateClient.relation.put(
            createIfNotExist = true,
            singleUpserRequest(
                externalId = relationId1,
                relationType = relationType,
                businessPartnerSourceExternalId = legalEntityId2,
                businessPartnerTargetExternalId = legalEntityId1
            )
        )

        val beforePost = Instant.now()
        val response = gateClient.relation.put(
            createIfNotExist = true,
            singleUpserRequest(
                externalId = relationId2,
                relationType = relationType,
                businessPartnerSourceExternalId = legalEntityId3,
                businessPartnerTargetExternalId = legalEntityId1
            )
        )
        val afterPost = Instant.now()

        val expectation = RelationDto(
            externalId = relationId2,
            relationType = relationType,
            businessPartnerSourceExternalId = legalEntityId3,
            businessPartnerTargetExternalId = legalEntityId1,
            validityPeriods = GateTestValues.alwaysActiveRelationValidity,
            updatedAt = Instant.now(),
            createdAt = Instant.now()
        )

        mockAndAssertUtils.assertRelation(response, expectation, Timeframe(beforePost, afterPost), ignoreExternalId = false)
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun putUpsertRelationWithSameId(relationType: RelationType){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"
        val legalEntityId3 = "$testName LE 3"
        val legalEntityId4 = "$testName LE 4"

        val legalEntities = listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2),
            createLegalEntityRequest(legalEntityId3),
            createLegalEntityRequest(legalEntityId4)
        )
        gateClient.businessParters.upsertBusinessPartnersInput(legalEntities)
        val beforeCreate = Instant.now()
        gateClient.relation.put(
            createIfNotExist = true,
            singleUpserRequest(
                externalId = testName,
                relationType = relationType,
                businessPartnerSourceExternalId = legalEntityId1,
                businessPartnerTargetExternalId = legalEntityId2
            )
        )
        val afterCreate = Instant.now()

        val beforePut = Instant.now()
        val response = gateClient.relation.put(
            createIfNotExist = true,
            singleUpserRequest(
                externalId = testName,
                relationType = relationType,
                businessPartnerSourceExternalId = legalEntityId3,
                businessPartnerTargetExternalId = legalEntityId4
            )
        )
        val afterPut = Instant.now()

        val expectation = RelationDto(
            externalId = testName,
            relationType = relationType,
            businessPartnerSourceExternalId = legalEntityId3,
            businessPartnerTargetExternalId = legalEntityId4,
            validityPeriods = GateTestValues.alwaysActiveRelationValidity,
            updatedAt = Instant.now(),
            createdAt = Instant.now()
        )

        mockAndAssertUtils.assertRelation(
            actual = response,
            expectation = expectation,
            updateTimeframe = Timeframe(beforePut, afterPut),
            createTimeframe = Timeframe(beforeCreate, afterCreate),
            ignoreExternalId = false
        )
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun putUpsertRelationWithSourceEqualsTarget(relationType: RelationType){
        val legalEntityId1 = "$testName LE 1"
        gateClient.businessParters.upsertBusinessPartnersInput(
            listOf(
                createLegalEntityRequest(legalEntityId1)
            )
        )

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = true,
                singleUpserRequest(
                    externalId = testName,
                    relationType = relationType,
                    businessPartnerSourceExternalId = legalEntityId1,
                    businessPartnerTargetExternalId = legalEntityId1
                )
            )
        }
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun putUpsertRelationWithSourceNotExist(relationType: RelationType){
        val legalEntityId1 = "$testName LE 1"
        gateClient.businessParters.upsertBusinessPartnersInput(
            listOf(
                createLegalEntityRequest(legalEntityId1)
            )
        )

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = true,
                singleUpserRequest(
                    externalId = testName,
                    relationType = relationType,
                    businessPartnerSourceExternalId = "NOT EXIST",
                    businessPartnerTargetExternalId = legalEntityId1
                )
            )
        }
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun putUpsertRelationWithTargetNotExist(relationType: RelationType){
        val legalEntityId1 = "$testName LE 1"
        gateClient.businessParters.upsertBusinessPartnersInput(
            listOf(
                createLegalEntityRequest(legalEntityId1)
            )
        )

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = true,
                singleUpserRequest(
                    externalId = testName,
                    relationType = relationType,
                    businessPartnerSourceExternalId = legalEntityId1,
                    businessPartnerTargetExternalId = "NOT EXIST"
                )
            )
        }
    }


    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun getRelations(relationType: RelationType){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"
        val legalEntityId3 = "$testName LE 3"
        val legalEntityId4 = "$testName LE 4"
        val relationId1 = "$testName Relation1"
        val relationId2 = "$testName Relation2"
        val relationId3 = "$testName Relation3"

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2),
            createLegalEntityRequest(legalEntityId3),
            createLegalEntityRequest(legalEntityId4)
        ))

        val beforeCreation = Instant.now()
        val postResponse =  gateClient.relation.put(
            createIfNotExist = true,
            RelationPutRequest(
                relations = listOf(
                    RelationPutEntry(
                        externalId = relationId1,
                        relationType = relationType,
                        businessPartnerSourceExternalId = legalEntityId2,
                        businessPartnerTargetExternalId = legalEntityId1
                    ),
                    RelationPutEntry(
                        externalId = relationId2,
                        relationType = relationType,
                        businessPartnerSourceExternalId = legalEntityId3,
                        businessPartnerTargetExternalId = legalEntityId1
                    ),
                    RelationPutEntry(
                        externalId = relationId3,
                        relationType = relationType,
                        businessPartnerSourceExternalId = legalEntityId4,
                        businessPartnerTargetExternalId = legalEntityId1
                    )
                )
            )
        )

        val afterCreation = Instant.now()


        val response = gateClient.relation.postSearch()

        val expectation = PageDto(3, 1, 0, 3, postResponse.upsertedRelations)

        mockAndAssertUtils.assertRelationPage(response, expectation, Timeframe(beforeCreation, afterCreation))
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun getRelationsViaExternalId(relationType: RelationType){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"
        val legalEntityId3 = "$testName LE 3"
        val legalEntityId4 = "$testName LE 4"
        val relationId1 = "$testName Relation1"
        val relationId2 = "$testName Relation2"
        val relationId3 = "$testName Relation3"

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2),
            createLegalEntityRequest(legalEntityId3),
            createLegalEntityRequest(legalEntityId4)
        ))

        val beforeCreation = Instant.now()
        val postResponse = gateClient.relation.put(
            createIfNotExist = true,
            RelationPutRequest(
                relations = listOf(
                    RelationPutEntry(
                        externalId = relationId1,
                        relationType = relationType,
                        businessPartnerSourceExternalId = legalEntityId2,
                        businessPartnerTargetExternalId = legalEntityId1
                    ),
                    RelationPutEntry(
                        externalId = relationId2,
                        relationType = relationType,
                        businessPartnerSourceExternalId = legalEntityId3,
                        businessPartnerTargetExternalId = legalEntityId1
                    ),
                    RelationPutEntry(
                        externalId = relationId3,
                        relationType = relationType,
                        businessPartnerSourceExternalId = legalEntityId4,
                        businessPartnerTargetExternalId = legalEntityId1
                    )
                )
            )
        )

        val afterCreation = Instant.now()

        val response = gateClient.relation.postSearch(RelationSearchRequest(externalIds = listOf(relationId1, relationId2)))

        val expectedRelations = postResponse.upsertedRelations.filter { it.externalId in listOf(relationId1, relationId2) }
        val expectation = PageDto(2, 1, 0, 2, expectedRelations )

        mockAndAssertUtils.assertRelationPage(response, expectation, Timeframe(beforeCreation, afterCreation))
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun getRelationsViaRelationType(relationType: RelationType){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"
        val legalEntityId3 = "$testName LE 3"
        val legalEntityId4 = "$testName LE 4"
        val relationId1 = "$testName Relation1"
        val relationId2 = "$testName Relation2"
        val relationId3 = "$testName Relation3"

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2),
            createLegalEntityRequest(legalEntityId3),
            createLegalEntityRequest(legalEntityId4)
        ))

        val beforeCreation = Instant.now()
        val postResponse = gateClient.relation.put(
            createIfNotExist = true,
            RelationPutRequest(
                relations = listOf(
                    RelationPutEntry(
                        externalId = relationId1,
                        relationType = relationType,
                        businessPartnerSourceExternalId = legalEntityId2,
                        businessPartnerTargetExternalId = legalEntityId1
                    ),
                    RelationPutEntry(
                        externalId = relationId2,
                        relationType = relationType,
                        businessPartnerSourceExternalId = legalEntityId3,
                        businessPartnerTargetExternalId = legalEntityId1
                    ),
                    RelationPutEntry(
                        externalId = relationId3,
                        relationType = relationType,
                        businessPartnerSourceExternalId = legalEntityId4,
                        businessPartnerTargetExternalId = legalEntityId1
                    )
                )
            )
        )

        val afterCreation = Instant.now()

        val response = gateClient.relation.postSearch(RelationSearchRequest(relationType = relationType))

        val expectation = PageDto(3, 1, 0, 3, postResponse.upsertedRelations)

        mockAndAssertUtils.assertRelationPage(response, expectation, Timeframe(beforeCreation, afterCreation))
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun getRelationsViaTarget(relationType: RelationType){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"
        val legalEntityId3 = "$testName LE 3"
        val legalEntityId4 = "$testName LE 4"
        val legalEntityId5 = "$testName LE 5"
        val legalEntityId6 = "$testName LE 6"
        val relationId1 = "$testName Relation1"
        val relationId2 = "$testName Relation2"
        val relationId3 = "$testName Relation3"
        val relationId4 = "$testName Relation4"

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2),
            createLegalEntityRequest(legalEntityId3),
            createLegalEntityRequest(legalEntityId4),
            createLegalEntityRequest(legalEntityId5),
            createLegalEntityRequest(legalEntityId6),
        ))

        val beforeCreation = Instant.now()
        val postResponse = gateClient.relation.put(
            createIfNotExist = true,
            RelationPutRequest(
                relations = listOf(
                    RelationPutEntry(
                        externalId = relationId1,
                        relationType = relationType,
                        businessPartnerSourceExternalId = legalEntityId2,
                        businessPartnerTargetExternalId = legalEntityId1
                    ),
                    RelationPutEntry(
                        externalId = relationId2,
                        relationType = relationType,
                        businessPartnerSourceExternalId = legalEntityId3,
                        businessPartnerTargetExternalId = legalEntityId1
                    ),
                    RelationPutEntry(
                        externalId = relationId3,
                        relationType = relationType,
                        businessPartnerSourceExternalId = legalEntityId4,
                        businessPartnerTargetExternalId = legalEntityId1
                    ),
                    RelationPutEntry(
                        externalId = relationId4,
                        relationType = relationType,
                        businessPartnerSourceExternalId = legalEntityId5,
                        businessPartnerTargetExternalId = legalEntityId6
                    )
                )
            )
        )
        val afterCreation = Instant.now()

        val response = gateClient.relation.postSearch(RelationSearchRequest(businessPartnerTargetExternalIds = listOf(legalEntityId1)))

        val expectedRelations = postResponse.upsertedRelations.filter { it.externalId in listOf(relationId1, relationId2, relationId3) }
        val expectation = PageDto(3, 1, 0, 3, expectedRelations )

        mockAndAssertUtils.assertRelationPage(response, expectation, Timeframe(beforeCreation, afterCreation))
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun getRelationsViaSource(relationType: RelationType){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"
        val legalEntityId3 = "$testName LE 3"
        val legalEntityId4 = "$testName LE 4"
        val relationId1 = "$testName Relation1"
        val relationId2 = "$testName Relation2"
        val relationId3 = "$testName Relation3"

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2),
            createLegalEntityRequest(legalEntityId3),
            createLegalEntityRequest(legalEntityId4)
        ))

        val beforeCreation = Instant.now()
        val postResponse = gateClient.relation.put(
            createIfNotExist = true,
            RelationPutRequest(
                relations = listOf(
                    RelationPutEntry(
                        externalId = relationId1,
                        relationType = relationType,
                        businessPartnerSourceExternalId = legalEntityId2,
                        businessPartnerTargetExternalId = legalEntityId1
                    ),
                    RelationPutEntry(
                        externalId = relationId2,
                        relationType = relationType,
                        businessPartnerSourceExternalId = legalEntityId3,
                        businessPartnerTargetExternalId = legalEntityId1
                    ),
                    RelationPutEntry(
                        externalId = relationId3,
                        relationType = relationType,
                        businessPartnerSourceExternalId = legalEntityId4,
                        businessPartnerTargetExternalId = legalEntityId1
                    )
                )
            )
        )
        val afterCreation = Instant.now()

        val response = gateClient.relation.postSearch(RelationSearchRequest(businessPartnerSourceExternalIds = listOf(legalEntityId2, legalEntityId3)))

        val expectedRelations = postResponse.upsertedRelations.filter { it.externalId in listOf(relationId1, relationId2) }
        val expectation = PageDto(2, 1, 0, 2, expectedRelations )

        mockAndAssertUtils.assertRelationPage(response, expectation, Timeframe(beforeCreation, afterCreation))
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun getRelationsViaUpdatedAtFrom(relationType: RelationType){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"
        val legalEntityId3 = "$testName LE 3"
        val legalEntityId4 = "$testName LE 4"
        val relationId1 = "$testName Relation1"
        val relationId2 = "$testName Relation2"
        val relationId3 = "$testName Relation3"

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2),
            createLegalEntityRequest(legalEntityId3),
            createLegalEntityRequest(legalEntityId4)
        ))

        gateClient.relation.put(
            createIfNotExist = true,
            singleUpserRequest(
                externalId = relationId1,
                relationType = relationType,
                businessPartnerSourceExternalId = legalEntityId2,
                businessPartnerTargetExternalId = legalEntityId1
            )
        )

        val beforeCreation = Instant.now()
        val postResponse = gateClient.relation.put(
            createIfNotExist = true,
            RelationPutRequest(
                relations = listOf(
                    RelationPutEntry(
                        externalId = relationId2,
                        relationType = relationType,
                        businessPartnerSourceExternalId = legalEntityId3,
                        businessPartnerTargetExternalId = legalEntityId1
                    ),
                    RelationPutEntry(
                        externalId = relationId3,
                        relationType = relationType,
                        businessPartnerSourceExternalId = legalEntityId4,
                        businessPartnerTargetExternalId = legalEntityId1
                    )
                )
            )
        )
        val afterCreation = Instant.now()

        val response = gateClient.relation.postSearch(RelationSearchRequest(updatedAtFrom = beforeCreation))

        val expectation = PageDto(2, 1, 0, 2, postResponse.upsertedRelations)

        mockAndAssertUtils.assertRelationPage(response, expectation, Timeframe(beforeCreation, afterCreation))
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun getRelationsViaAllFilters(relationType: RelationType){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"
        val legalEntityId3 = "$testName LE 3"
        val legalEntityId4 = "$testName LE 4"
        val relationId1 = "$testName Relation1"
        val relationId2 = "$testName Relation2"
        val relationId3 = "$testName Relation3"

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2),
            createLegalEntityRequest(legalEntityId3),
            createLegalEntityRequest(legalEntityId4)
        ))

        gateClient.relation.put(
            createIfNotExist = true,
            singleUpserRequest(
                externalId = relationId1,
                relationType = relationType,
                businessPartnerSourceExternalId = legalEntityId2,
                businessPartnerTargetExternalId = legalEntityId1
            )
        )

        val beforeCreation = Instant.now()
        val postResponse = gateClient.relation.put(
            createIfNotExist = true,
            RelationPutRequest(
                relations = listOf(
                    RelationPutEntry(
                        externalId = relationId2,
                        relationType = relationType,
                        businessPartnerSourceExternalId = legalEntityId3,
                        businessPartnerTargetExternalId = legalEntityId1
                    ),
                    RelationPutEntry(
                        externalId = relationId3,
                        relationType = relationType,
                        businessPartnerSourceExternalId = legalEntityId4,
                        businessPartnerTargetExternalId = legalEntityId1
                    )
                )
            )
        )
        val afterCreation = Instant.now()

        val response = gateClient.relation.postSearch(
            RelationSearchRequest(
                externalIds = listOf(relationId3),
                relationType = relationType,
                businessPartnerSourceExternalIds = listOf(legalEntityId4),
                businessPartnerTargetExternalIds = listOf(legalEntityId1),
                updatedAtFrom = beforeCreation
            )
        )

        val expectedRelations = postResponse.upsertedRelations.filter { it.externalId == relationId3 }
        val expectation = PageDto(1, 1, 0, 1, expectedRelations)

        mockAndAssertUtils.assertRelationPage(response, expectation, Timeframe(beforeCreation, afterCreation))
    }



    private fun createLegalEntityRequest(externalId: String) =
        inputFactory.createAllFieldsFilled(externalId).request
            .withAddressType(AddressType.LegalAddress)
            .copy(isOwnCompanyData = true)

    private fun singleUpserRequest(
        externalId: String,
        relationType: RelationType,
        businessPartnerSourceExternalId: String,
        businessPartnerTargetExternalId: String
    ) = RelationPutRequest(
        listOf(
            RelationPutEntry(
                externalId = externalId,
                relationType = relationType,
                businessPartnerSourceExternalId = businessPartnerSourceExternalId,
                businessPartnerTargetExternalId = businessPartnerTargetExternalId,
                validityPeriods = GateTestValues.alwaysActiveRelationValidity
            )
        )
    )
}