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
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutEntry
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutRequest
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.gate.GateInputFactory
import org.eclipse.tractusx.bpdm.test.testdata.gate.withAddressType
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.reactive.function.client.WebClientResponseException.BadRequest

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [
    PostgreSQLContextInitializer::class,
    KeyCloakInitializer::class,
    SelfClientAsPartnerUploaderInitializer::class
])
@ActiveProfiles("test")
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
            singleUpserRequest(
                externalId = relationId1,
                businessPartnerSourceExternalId = legalEntityId2,
                businessPartnerTargetExternalId = legalEntityId1
            )
        )
        gateClient.relation.put(
            createIfNotExist = true,
            singleUpserRequest(
                externalId = relationId2,
                businessPartnerSourceExternalId = legalEntityId4,
                businessPartnerTargetExternalId = legalEntityId3
            )
        )

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = false,
                singleUpserRequest(
                    externalId = relationId2,
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
            singleUpserRequest(
                externalId = relationId1,
                businessPartnerSourceExternalId = legalEntityId1,
                businessPartnerTargetExternalId = legalEntityId2
            )
        )
        gateClient.relation.put(
            createIfNotExist = true,
            singleUpserRequest(
                externalId = relationId2,
                businessPartnerSourceExternalId = legalEntityId3,
                businessPartnerTargetExternalId = legalEntityId4
            )
        )

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = false,
                singleUpserRequest(
                    externalId = relationId2,
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
            singleUpserRequest(
                externalId = relationId1,
                businessPartnerSourceExternalId = legalEntityId1,
                businessPartnerTargetExternalId = legalEntityId2
            )
        )
        gateClient.relation.put(
            createIfNotExist = true,
            singleUpserRequest(
                externalId = relationId2,
                businessPartnerSourceExternalId = legalEntityId3,
                businessPartnerTargetExternalId = legalEntityId4
            )
        )

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = false,
                singleUpserRequest(
                    externalId = relationId2,
                    businessPartnerSourceExternalId = legalEntityId2,
                    businessPartnerTargetExternalId = legalEntityId4
                )
            )
        }
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
                singleUpserRequest(
                    externalId = testName,
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
                singleUpserRequest(
                    externalId = testName,
                    businessPartnerSourceExternalId = legalEntityId1,
                    businessPartnerTargetExternalId = legalEntityId2
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
            singleUpserRequest(
                externalId = "$testName Relation 1",
                businessPartnerSourceExternalId = legalEntityId2,
                businessPartnerTargetExternalId = legalEntityId1
            )
        )

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = true,
                singleUpserRequest(
                    externalId = "$testName Relation 2",
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
            singleUpserRequest(
                externalId = "$testName Relation 1",
                businessPartnerSourceExternalId = legalEntityId1,
                businessPartnerTargetExternalId = legalEntityId2
            )
        )

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = true,
                singleUpserRequest(
                    externalId = "$testName Relation 2",
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
            singleUpserRequest(
                externalId = "$testName Relation 1",
                businessPartnerSourceExternalId = legalEntityId1,
                businessPartnerTargetExternalId = legalEntityId2
            )
        )

        Assertions.assertThatExceptionOfType(BadRequest::class.java).isThrownBy {
            gateClient.relation.put(
                createIfNotExist = true,
                singleUpserRequest(
                    externalId = "$testName Relation 2",
                    businessPartnerSourceExternalId = legalEntityId2,
                    businessPartnerTargetExternalId = legalEntityId3
                )
            )
        }
    }

    private fun createLegalEntityRequest(externalId: String) =
        inputFactory.createAllFieldsFilled(externalId).request
            .withAddressType(AddressType.LegalAddress)
            .copy(isOwnCompanyData = true)

    private fun singleUpserRequest(
        externalId: String,
        businessPartnerSourceExternalId: String,
        businessPartnerTargetExternalId: String
    ) = RelationPutRequest(
        listOf(
            RelationPutEntry(
                externalId = externalId,
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalId = businessPartnerSourceExternalId,
                businessPartnerTargetExternalId = businessPartnerTargetExternalId
        )
        )
    )
}