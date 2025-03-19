/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutRequest
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.gate.GateInputFactory
import org.eclipse.tractusx.bpdm.test.testdata.gate.withAddressType
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.eclipse.tractusx.bpdm.test.util.Timeframe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.reactive.function.client.WebClientResponseException.*
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [
    PostgreSQLContextInitializer::class,
    KeyCloakInitializer::class,
    SelfClientAsPartnerUploaderInitializer::class
])
class RelationIsManagedByIT @Autowired constructor(
    private val gateClient: GateClient,
    private val testHelpers: DbTestHelpers,
    private val inputFactory: GateInputFactory
) {
    var testName: String = ""

    @BeforeEach
    fun beforeEach(testInfo: TestInfo) {
        testHelpers.truncateDbTables()
        testName = testInfo.displayName
    }

    @Test
    fun putUpdateRelation(){
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
            RelationPutRequest(
                externalId = relationId,
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalId = legalEntityId1,
                businessPartnerTargetExternalId = legalEntityId2
            )
        )
        val afterCreation = Instant.now()

        val beforeUpdate = Instant.now()
        val response = gateClient.relation.put(
            createIfNotExist = false,
            RelationPutRequest(
                externalId = relationId,
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalId = legalEntityId3,
                businessPartnerTargetExternalId = legalEntityId4
            )
        )
        val afterUpdate = Instant.now()

        val expectation = RelationDto(
            externalId = relationId,
            relationType = RelationType.IsManagedBy,
            businessPartnerSourceExternalId = legalEntityId3,
            businessPartnerTargetExternalId = legalEntityId4,
            updatedAt = Instant.now(),
            createdAt = Instant.now()
        )

        assertRelation(
            actual = response,
            expectation = expectation,
            updateTimeframe = Timeframe(beforeUpdate, afterUpdate),
            createTimeframe = Timeframe(beforeCreation, afterCreation),
            ignoreExternalId = false)
    }

    @Test
    fun putUpdateRelationWithLegalAndSiteMainAddress(){
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
            RelationPutRequest(
                externalId = relationId,
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalId = legalEntityId1,
                businessPartnerTargetExternalId = legalEntityId2
            )
        )
        val afterCreation = Instant.now()

        val beforeUpdate = Instant.now()
        val response = gateClient.relation.put(
            createIfNotExist = false,
            RelationPutRequest(
                externalId = relationId,
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalId = legalEntityId3,
                businessPartnerTargetExternalId = legalEntityId4
            )
        )
        val afterUpdate = Instant.now()

        val expectation = RelationDto(
            externalId = relationId,
            relationType = RelationType.IsManagedBy,
            businessPartnerSourceExternalId = legalEntityId3,
            businessPartnerTargetExternalId = legalEntityId4,
            updatedAt = Instant.now(),
            createdAt = Instant.now()
        )

        assertRelation(
            actual = response,
            expectation = expectation,
            updateTimeframe = Timeframe(beforeUpdate, afterUpdate),
            createTimeframe = Timeframe(beforeCreation, afterCreation),
            ignoreExternalId = false)
    }

    @Test
    fun putUpdateRelationWithTenant(){
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
            RelationPutRequest(
                externalId = relationId,
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalId = legalEntityId1,
                businessPartnerTargetExternalId = legalEntityId2
            )
        )
        val afterCreation = Instant.now()

        val beforeUpdate = Instant.now()
        val response = gateClient.relation.put(
            createIfNotExist = false,
            RelationPutRequest(
                externalId = relationId,
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalId = legalEntityId3,
                businessPartnerTargetExternalId = legalEntityId4
            )
        )
        val afterUpdate = Instant.now()

        val expectation = RelationDto(
            externalId = relationId,
            relationType = RelationType.IsManagedBy,
            businessPartnerSourceExternalId = legalEntityId3,
            businessPartnerTargetExternalId = legalEntityId4,
            updatedAt = Instant.now(),
            createdAt = Instant.now()
        )

        assertRelation(
            actual = response,
            expectation = expectation,
            updateTimeframe = Timeframe(beforeUpdate, afterUpdate),
            createTimeframe = Timeframe(beforeCreation, afterCreation),
            ignoreExternalId = false)
    }

    @Test
    fun putUpdateRelationWithMultipleSources(){
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
            RelationPutRequest(
                externalId = relationId1,
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalId = legalEntityId2,
                businessPartnerTargetExternalId = legalEntityId1
            )
        )
        gateClient.relation.put(
            createIfNotExist = true,
            RelationPutRequest(
                externalId = relationId2,
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalId = legalEntityId3,
                businessPartnerTargetExternalId = legalEntityId1
            )
        )
        val afterCreation = Instant.now()

        val beforeUpdate = Instant.now()
        val response = gateClient.relation.put(
            createIfNotExist = false,
            RelationPutRequest(
                externalId = relationId2,
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalId = legalEntityId4,
                businessPartnerTargetExternalId = legalEntityId1
            )
        )
        val afterUpdate = Instant.now()

        val expectation = RelationDto(
            externalId = relationId2,
            relationType = RelationType.IsManagedBy,
            businessPartnerSourceExternalId = legalEntityId4,
            businessPartnerTargetExternalId = legalEntityId1,
            updatedAt = Instant.now(),
            createdAt = Instant.now()
        )

        assertRelation(
            actual = response,
            expectation = expectation,
            updateTimeframe = Timeframe(beforeUpdate, afterUpdate),
            createTimeframe = Timeframe(beforeCreation, afterCreation),
            ignoreExternalId = false)
    }

    @Test
    fun putUpdateRelationIdNotExist(){
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
                RelationPutRequest(
                    externalId = testName,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId1,
                    businessPartnerTargetExternalId = legalEntityId1
                )
            )
        }
    }

    @Test
    fun putUpdateRelationWithSourceEqualsTarget(){
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
            RelationPutRequest(
                externalId = relationId,
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalId = legalEntityId1,
                businessPartnerTargetExternalId = legalEntityId2
            )
        )

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = false,
                RelationPutRequest(
                    externalId = relationId,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId3,
                    businessPartnerTargetExternalId = legalEntityId3
                )
            )
        }
    }

    @Test
    fun putUpdateRelationWithSourceNotExist(){
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
            RelationPutRequest(
                externalId = relationId,
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalId = legalEntityId1,
                businessPartnerTargetExternalId = legalEntityId2
            )
        )

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = false,
                RelationPutRequest(
                    externalId = relationId,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = "NOT EXISTS",
                    businessPartnerTargetExternalId = legalEntityId3
                )
            )
        }
    }

    @Test
    fun putUpdateRelationWithTargetNotExist(){
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
            RelationPutRequest(
                externalId = relationId,
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalId = legalEntityId1,
                businessPartnerTargetExternalId = legalEntityId2
            )
        )

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = false,
                RelationPutRequest(
                    externalId = relationId,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId3,
                    businessPartnerTargetExternalId = "NOT EXISTS"
                )
            )
        }
    }

    @Test
    fun putUpdateRelationMultipleTargets(){
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

        gateClient.relation.put(
            createIfNotExist = true,
            RelationPutRequest(
                externalId = relationId1,
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalId = legalEntityId2,
                businessPartnerTargetExternalId = legalEntityId1
            )
        )
        gateClient.relation.put(
            createIfNotExist = true,
            RelationPutRequest(
                externalId = relationId2,
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalId = legalEntityId4,
                businessPartnerTargetExternalId = legalEntityId3
            )
        )

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = false,
                RelationPutRequest(
                    externalId = relationId2,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId2,
                    businessPartnerTargetExternalId = legalEntityId3
                )
            )
        }
    }

    @Test
    fun putUpdateRelationTargetIsAlreadySource(){
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

        gateClient.relation.put(
            createIfNotExist = true,
            RelationPutRequest(
                externalId = relationId1,
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalId = legalEntityId1,
                businessPartnerTargetExternalId = legalEntityId2
            )
        )
        gateClient.relation.put(
            createIfNotExist = true,
            RelationPutRequest(
                externalId = relationId2,
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalId = legalEntityId3,
                businessPartnerTargetExternalId = legalEntityId4
            )
        )

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = false,
                RelationPutRequest(
                    externalId = relationId2,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId3,
                    businessPartnerTargetExternalId = legalEntityId1
                )
            )
        }
    }

    @Test
    fun putUpdateRelationSourceIsAlreadyTarget(){
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

        gateClient.relation.put(
            createIfNotExist = true,
            RelationPutRequest(
                externalId = relationId1,
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalId = legalEntityId1,
                businessPartnerTargetExternalId = legalEntityId2
            )
        )
        gateClient.relation.put(
            createIfNotExist = true,
            RelationPutRequest(
                externalId = relationId2,
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalId = legalEntityId3,
                businessPartnerTargetExternalId = legalEntityId4
            )
        )

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = false,
                RelationPutRequest(
                    externalId = relationId2,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId2,
                    businessPartnerTargetExternalId = legalEntityId4
                )
            )
        }
    }

    @Test
    fun putUpsertRelation(){
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
            RelationPutRequest(
                externalId = relationId,
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalId = legalEntityId1,
                businessPartnerTargetExternalId = legalEntityId2
            )
        )
        val afterPost = Instant.now()

        val expectation = RelationDto(
            externalId = relationId,
            relationType = RelationType.IsManagedBy,
            businessPartnerSourceExternalId = legalEntityId1,
            businessPartnerTargetExternalId = legalEntityId2,
            updatedAt = Instant.now(),
            createdAt = Instant.now()
        )

        assertRelation(response, expectation, Timeframe(beforePost, afterPost), ignoreExternalId = false)
    }

    @Test
    fun putUpsertRelationWithTenant(){
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
            RelationPutRequest(
                externalId = relationId,
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalId = legalEntityId1,
                businessPartnerTargetExternalId = legalEntityId2
            )
        )
        val afterPost = Instant.now()

        val expectation = RelationDto(
            externalId = relationId,
            relationType = RelationType.IsManagedBy,
            businessPartnerSourceExternalId = legalEntityId1,
            businessPartnerTargetExternalId = legalEntityId2,
            updatedAt = Instant.now(),
            createdAt = Instant.now()
        )

        assertRelation(response, expectation, Timeframe(beforePost, afterPost), ignoreExternalId =  false)
    }

    @Test
    fun putUpsertRelationWithMultipleSources(){
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
            RelationPutRequest(
                externalId = relationId1,
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalId = legalEntityId2,
                businessPartnerTargetExternalId = legalEntityId1
            )
        )

        val beforePost = Instant.now()
        val response = gateClient.relation.put(
            createIfNotExist = true,
            RelationPutRequest(
                externalId = relationId2,
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalId = legalEntityId3,
                businessPartnerTargetExternalId = legalEntityId1
            )
        )
        val afterPost = Instant.now()

        val expectation = RelationDto(
            externalId = relationId2,
            relationType = RelationType.IsManagedBy,
            businessPartnerSourceExternalId = legalEntityId3,
            businessPartnerTargetExternalId = legalEntityId1,
            updatedAt = Instant.now(),
            createdAt = Instant.now()
        )

        assertRelation(response, expectation, Timeframe(beforePost, afterPost), ignoreExternalId = false)
    }

    @Test
    fun putUpsertRelationWithSameId(){
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
            RelationPutRequest(
                externalId = testName,
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalId = legalEntityId1,
                businessPartnerTargetExternalId = legalEntityId2
            )
        )
        val afterCreate = Instant.now()

        val beforePut = Instant.now()
        val response = gateClient.relation.put(
            createIfNotExist = true,
            RelationPutRequest(
                externalId = testName,
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalId = legalEntityId3,
                businessPartnerTargetExternalId = legalEntityId4
            )
        )
        val afterPut = Instant.now()

        val expectation = RelationDto(
            externalId = testName,
            relationType = RelationType.IsManagedBy,
            businessPartnerSourceExternalId = legalEntityId3,
            businessPartnerTargetExternalId = legalEntityId4,
            updatedAt = Instant.now(),
            createdAt = Instant.now()
        )

        assertRelation(
            actual = response,
            expectation = expectation,
            updateTimeframe = Timeframe(beforePut, afterPut),
            createTimeframe = Timeframe(beforeCreate, afterCreate),
            ignoreExternalId = false
        )
    }

    @Test
    fun putUpsertRelationWithSourceNotOwnCompanyData(){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"

        val legalEntities = listOf(
            createLegalEntityRequest(legalEntityId1).copy(isOwnCompanyData = false),
            createLegalEntityRequest(legalEntityId2)
        )
        gateClient.businessParters.upsertBusinessPartnersInput(legalEntities)

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                    externalId = testName,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId1,
                    businessPartnerTargetExternalId = legalEntityId2
                )
            )
        }
    }

    @Test
    fun putUpsertRelationWithTargetNotOwnCompanyData(){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"

        val legalEntities = listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2).copy(isOwnCompanyData = false)
        )
        gateClient.businessParters.upsertBusinessPartnersInput(legalEntities)

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                    externalId = testName,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId1,
                    businessPartnerTargetExternalId = legalEntityId2
                )
            )
        }
    }

    @Test
    fun putUpsertRelationWithSourceSite(){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"

        val legalEntities = listOf(
            createLegalEntityRequest(legalEntityId1).withAddressType(AddressType.SiteMainAddress),
            createLegalEntityRequest(legalEntityId2)
        )
        gateClient.businessParters.upsertBusinessPartnersInput(legalEntities)

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                    externalId = testName,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId1,
                    businessPartnerTargetExternalId = legalEntityId2
                )
            )
        }
    }

    @Test
    fun putUpsertRelationWithTargetSite(){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"

        val legalEntities = listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2).withAddressType(AddressType.SiteMainAddress)
        )
        gateClient.businessParters.upsertBusinessPartnersInput(legalEntities)

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                    externalId = testName,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId1,
                    businessPartnerTargetExternalId = legalEntityId2
                )
            )
        }
    }

    @Test
    fun putUpsertRelationWithSourceAdditionalAddress(){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"

        val legalEntities = listOf(
            createLegalEntityRequest(legalEntityId1).withAddressType(AddressType.AdditionalAddress),
            createLegalEntityRequest(legalEntityId2)
        )
        gateClient.businessParters.upsertBusinessPartnersInput(legalEntities)

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                    externalId = testName,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId1,
                    businessPartnerTargetExternalId = legalEntityId2
                )
            )
        }
    }

    @Test
    fun putUpsertRelationWithTargetAdditionalAddress(){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"

        val legalEntities = listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2).withAddressType(AddressType.AdditionalAddress)
        )
        gateClient.businessParters.upsertBusinessPartnersInput(legalEntities)

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                    externalId = testName,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId1,
                    businessPartnerTargetExternalId = legalEntityId2
                )
            )
        }
    }

    @Test
    fun putUpsertRelationWithSourceEqualsTarget(){
        val legalEntityId1 = "$testName LE 1"
        gateClient.businessParters.upsertBusinessPartnersInput(
            listOf(
                createLegalEntityRequest(legalEntityId1)
            )
        )

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                    externalId = testName,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId1,
                    businessPartnerTargetExternalId = legalEntityId1
                )
            )
        }
    }

    @Test
    fun putUpsertRelationWithSourceNotExist(){
        val legalEntityId1 = "$testName LE 1"
        gateClient.businessParters.upsertBusinessPartnersInput(
            listOf(
                createLegalEntityRequest(legalEntityId1)
            )
        )

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                    externalId = testName,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = "NOT EXIST",
                    businessPartnerTargetExternalId = legalEntityId1
                )
            )
        }
    }

    @Test
    fun putUpsertRelationWithTargetNotExist(){
        val legalEntityId1 = "$testName LE 1"
        gateClient.businessParters.upsertBusinessPartnersInput(
            listOf(
                createLegalEntityRequest(legalEntityId1)
            )
        )

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                    externalId = testName,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId1,
                    businessPartnerTargetExternalId = "NOT EXIST"
                )
            )
        }
    }

    @Test
    fun putUpsertRelationMultipleTargets(){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"
        val legalEntityId3 = "$testName LE 3"

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2),
            createLegalEntityRequest(legalEntityId3),
        ))
        gateClient.relation.put(
            createIfNotExist = true,
            RelationPutRequest(
                externalId = "$testName Relation 1",
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalId = legalEntityId2,
                businessPartnerTargetExternalId = legalEntityId1
            )
        )

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                    externalId = "$testName Relation 2",
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId2,
                    businessPartnerTargetExternalId = legalEntityId3
                )
            )
        }
    }

    @Test
    fun putUpsertRelationTargetIsAlreadySource(){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"
        val legalEntityId3 = "$testName LE 3"

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2),
            createLegalEntityRequest(legalEntityId3),
        ))
        gateClient.relation.put(
            createIfNotExist = true,
            RelationPutRequest(
                externalId = "$testName Relation 1",
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalId = legalEntityId1,
                businessPartnerTargetExternalId = legalEntityId2
            )
        )

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                    externalId = "$testName Relation 2",
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId3,
                    businessPartnerTargetExternalId = legalEntityId1
                )
            )
        }
    }

    @Test
    fun putUpsertRelationSourceIsAlreadyTarget(){
        val legalEntityId1 = "$testName LE 1"
        val legalEntityId2 = "$testName LE 2"
        val legalEntityId3 = "$testName LE 3"

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            createLegalEntityRequest(legalEntityId1),
            createLegalEntityRequest(legalEntityId2),
            createLegalEntityRequest(legalEntityId3),
        ))
        gateClient.relation.put(
            createIfNotExist = true,
            RelationPutRequest(
                externalId = "$testName Relation 1",
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalId = legalEntityId1,
                businessPartnerTargetExternalId = legalEntityId2
            )
        )

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                    externalId = "$testName Relation 2",
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId2,
                    businessPartnerTargetExternalId = legalEntityId3
                )
            )
        }
    }

    @Test
    fun getRelations(){
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
        val postResponses = listOf(
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                    externalId = relationId1,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId2,
                    businessPartnerTargetExternalId = legalEntityId1
                )
            ),
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                        externalId = relationId2,
                        relationType = RelationType.IsManagedBy,
                        businessPartnerSourceExternalId = legalEntityId3,
                        businessPartnerTargetExternalId = legalEntityId1)
            ),
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                        externalId = relationId3,
                        relationType = RelationType.IsManagedBy,
                        businessPartnerSourceExternalId = legalEntityId4,
                        businessPartnerTargetExternalId = legalEntityId1
                    )
            )
        )
        val afterCreation = Instant.now()


        val response = gateClient.relation.get()

        val expectation = PageDto(3, 1, 0, 3, postResponses)

        assertRelationPage(response, expectation, Timeframe(beforeCreation, afterCreation))
    }

    @Test
    fun getRelationsViaExternalId(){
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
        val postResponses = listOf(
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                    externalId = relationId1,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId2,
                    businessPartnerTargetExternalId = legalEntityId1
                )
            ),
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                    externalId = relationId2,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId3,
                    businessPartnerTargetExternalId = legalEntityId1)
            ),
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                    externalId = relationId3,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId4,
                    businessPartnerTargetExternalId = legalEntityId1
                )
            )
        )
        val afterCreation = Instant.now()

        val response = gateClient.relation.get(externalIds = listOf(relationId1, relationId2))

        val expectation = PageDto(2, 1, 0, 2, postResponses.filter { it.externalId in listOf(relationId1, relationId2)  })

        assertRelationPage(response, expectation, Timeframe(beforeCreation, afterCreation))
    }

    @Test
    fun getRelationsViaRelationType(){
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
        val postResponses = listOf(
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                    externalId = relationId1,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId2,
                    businessPartnerTargetExternalId = legalEntityId1
                )
            ),
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                    externalId = relationId2,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId3,
                    businessPartnerTargetExternalId = legalEntityId1)
            ),
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                    externalId = relationId3,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId4,
                    businessPartnerTargetExternalId = legalEntityId1
                )
            )
        )
        val afterCreation = Instant.now()

        val response = gateClient.relation.get(relationType = RelationType.IsManagedBy)

        val expectation = PageDto(3, 1, 0, 3, postResponses)

        assertRelationPage(response, expectation, Timeframe(beforeCreation, afterCreation))
    }

    @Test
    fun getRelationsViaTarget(){
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
        val postResponses = listOf(
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                    externalId = relationId1,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId2,
                    businessPartnerTargetExternalId = legalEntityId1
                )
            ),
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                    externalId = relationId2,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId3,
                    businessPartnerTargetExternalId = legalEntityId1)
            ),
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                    externalId = relationId3,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId4,
                    businessPartnerTargetExternalId = legalEntityId1
                )
            ),
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                    externalId = relationId4,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId5,
                    businessPartnerTargetExternalId = legalEntityId6
                )
            )
        )
        val afterCreation = Instant.now()

        val response = gateClient.relation.get(businessPartnerTargetExternalIds = listOf(legalEntityId1))

        val expectation = PageDto(3, 1, 0, 3, postResponses.filter { it.externalId in listOf(relationId1, relationId2, relationId3) })

        assertRelationPage(response, expectation, Timeframe(beforeCreation, afterCreation))
    }

    @Test
    fun getRelationsViaSource(){
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
        val postResponses = listOf(
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                    externalId = relationId1,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId2,
                    businessPartnerTargetExternalId = legalEntityId1
                )
            ),
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                    externalId = relationId2,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId3,
                    businessPartnerTargetExternalId = legalEntityId1)
            ),
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                    externalId = relationId3,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId4,
                    businessPartnerTargetExternalId = legalEntityId1
                )
            )
        )
        val afterCreation = Instant.now()

        val response = gateClient.relation.get(businessPartnerSourceExternalIds = listOf(legalEntityId2, legalEntityId3))

        val expectation = PageDto(2, 1, 0, 2, postResponses.filter { it.externalId in listOf(relationId1, relationId2) })

        assertRelationPage(response, expectation, Timeframe(beforeCreation, afterCreation))
    }

    @Test
    fun getRelationsViaUpdatedAtFrom(){
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
            RelationPutRequest(
                externalId = relationId1,
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalId = legalEntityId2,
                businessPartnerTargetExternalId = legalEntityId1
            )
        )

        val beforeCreation = Instant.now()
        val postResponses = listOf(
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                    externalId = relationId2,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId3,
                    businessPartnerTargetExternalId = legalEntityId1)
            ),
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                    externalId = relationId3,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId4,
                    businessPartnerTargetExternalId = legalEntityId1
                )
            )
        )
        val afterCreation = Instant.now()

        val response = gateClient.relation.get(updatedAtFrom = beforeCreation)

        val expectation = PageDto(2, 1, 0, 2, postResponses)

        assertRelationPage(response, expectation, Timeframe(beforeCreation, afterCreation))
    }

    @Test
    fun getRelationsViaAllFilters(){
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
            RelationPutRequest(
                externalId = relationId1,
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalId = legalEntityId2,
                businessPartnerTargetExternalId = legalEntityId1
            )
        )

        val beforeCreation = Instant.now()
        val postResponses = listOf(
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                    externalId = relationId2,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId3,
                    businessPartnerTargetExternalId = legalEntityId1)
            ),
            gateClient.relation.put(
                createIfNotExist = true,
                RelationPutRequest(
                    externalId = relationId3,
                    relationType = RelationType.IsManagedBy,
                    businessPartnerSourceExternalId = legalEntityId4,
                    businessPartnerTargetExternalId = legalEntityId1
                )
            )
        )
        val afterCreation = Instant.now()

        val response = gateClient.relation.get(
            externalIds = listOf(relationId3),
            relationType = RelationType.IsManagedBy,
            businessPartnerSourceExternalIds = listOf(legalEntityId4),
            businessPartnerTargetExternalIds = listOf(legalEntityId1),
            updatedAtFrom = beforeCreation
        )

        val expectation = PageDto(1, 1, 0, 1, postResponses.filter { it.externalId == relationId3 })

        assertRelationPage(response, expectation, Timeframe(beforeCreation, afterCreation))
    }


    private fun assertRelationPage(
        actual: PageDto<RelationDto>,
        expectation: PageDto<RelationDto>,
        updateTimeframe: Timeframe,
        createTimeframe: Timeframe = updateTimeframe
    ){
        Assertions
            .assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(PageDto<RelationDto>::content.name)
            .isEqualTo(expectation)

        Assertions.assertThat(actual.content.size).isEqualTo(expectation.content.size)
        actual.content.sortedBy { it.externalId }.zip(expectation.content.sortedBy { it.externalId }).forEach { (actualEntry, expectationEntry) ->
            assertRelation(
                actual = actualEntry,
                expectation = expectationEntry,
                updateTimeframe = updateTimeframe,
                createTimeframe = createTimeframe, ignoreExternalId = false)
        }


    }


    private fun assertRelation(
        actual: RelationDto,
        expectation: RelationDto,
        updateTimeframe: Timeframe,
        createTimeframe: Timeframe = updateTimeframe,
        ignoreExternalId: Boolean = true
    ){
        Assertions
            .assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                RelationDto::externalId.name,
                RelationDto::updatedAt.name,
                RelationDto::createdAt.name)
            .isEqualTo(expectation)

        Assertions.assertThat(actual.externalId).isNotBlank()
        if(!ignoreExternalId) Assertions.assertThat(actual.externalId).isEqualTo(expectation.externalId)
        Assertions.assertThat(actual.createdAt).isBetween(createTimeframe.startTime, createTimeframe.endTime)
        Assertions.assertThat(actual.updatedAt).isBetween(updateTimeframe.startTime, updateTimeframe.endTime)
    }

    private fun createLegalEntityRequest(externalId: String) =
        inputFactory.createAllFieldsFilled(externalId).request
            .withAddressType(AddressType.LegalAddress)
            .copy(isOwnCompanyData = true)
}