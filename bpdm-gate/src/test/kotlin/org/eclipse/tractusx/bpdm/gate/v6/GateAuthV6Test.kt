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

package org.eclipse.tractusx.bpdm.gate.v6

import jakarta.annotation.PostConstruct
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.v6.client.GateV6Client
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.sharingstate.PostSharingStateReadyV6Request
import org.eclipse.tractusx.bpdm.gate.api.v6.model.changelog.ChangelogSearchV6Request
import org.eclipse.tractusx.bpdm.gate.api.v6.model.relation.RelationTypeV6
import org.eclipse.tractusx.bpdm.gate.api.v6.model.relation.input.RelationPostV6Request
import org.eclipse.tractusx.bpdm.gate.api.v6.model.relation.input.RelationPutV6Entry
import org.eclipse.tractusx.bpdm.gate.v6.util.GateTestClientProviderV6
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer
import org.eclipse.tractusx.bpdm.test.util.AuthAssertionHelper
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class GateAuthV6Test: UnscheduledGateV6TestBase() {
    @Autowired
    lateinit var authAssertionHelper: AuthAssertionHelper
    @Autowired
    lateinit var gateTestClientProvider: GateTestClientProviderV6

    lateinit var operatorClient: GateV6Client
    lateinit var inputConsumerClient: GateV6Client
    lateinit var inputManagerClient: GateV6Client
    lateinit var outputConsumerClient: GateV6Client
    lateinit var unauthorizedClientV6: GateV6Client
    lateinit var anonymousClientV6: GateV6Client

    @PostConstruct
    private fun init() {
        operatorClient = gateTestClientProvider.createClient(KeyCloakInitializer.CLIENT_ID_OPERATOR)
        inputConsumerClient = gateTestClientProvider.createClient(KeyCloakInitializer.CLIENT_ID_GATE_INPUT_CONSUMER)
        inputManagerClient = gateTestClientProvider.createClient(KeyCloakInitializer.CLIENT_ID_GATE_INPUT_MANAGER)
        outputConsumerClient = gateTestClientProvider.createClient(KeyCloakInitializer.CLIENT_ID_GATE_OUTPUT_CONSUMER)
        unauthorizedClientV6 = gateTestClientProvider.createClient(KeyCloakInitializer.CLIENT_ID_UNAUTHORIZED)
        anonymousClientV6 = gateTestClientProvider.createClient(null)
    }

    @Test
    fun upsertBusinessPartnersInput(){
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Forbidden,
            outputConsumer = AuthExpectationType.Forbidden
        ){ gateClient.businessPartners.upsertBusinessPartnersInput(emptyList()) }
    }

    @Test
    fun getBusinessPartnersInput(){
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Authorized,
            outputConsumer = AuthExpectationType.Forbidden
        ){ gateClient.businessPartners.getBusinessPartnersInput(emptyList()) }
    }

    @Test
    fun getBusinessPartnersOutput(){
        assertExpectations(
            inputManager = AuthExpectationType.Forbidden,
            inputConsumer = AuthExpectationType.Forbidden,
            outputConsumer = AuthExpectationType.Authorized
        ){ gateClient.businessPartners.getBusinessPartnersOutput(emptyList()) }
    }

    @Test
    fun getInputChangelog(){
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Authorized,
            outputConsumer = AuthExpectationType.Forbidden
        ){
            gateClient.changelog.getInputChangelog(PaginationRequest(), ChangelogSearchV6Request())
        }
    }

    @Test
    fun getOutputChangelog(){
        assertExpectations(
            inputManager = AuthExpectationType.Forbidden,
            inputConsumer = AuthExpectationType.Forbidden,
            outputConsumer = AuthExpectationType.Authorized
        ){
            gateClient.changelog.getOutputChangelog(PaginationRequest(), ChangelogSearchV6Request())
        }
    }

    @Test
    fun testGet(){
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Authorized,
            outputConsumer = AuthExpectationType.Forbidden
        ){
            gateClient.relations.get()
        }
    }

    @Test
    fun testPost(){
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Forbidden,
            outputConsumer = AuthExpectationType.Forbidden
        ){
            gateClient.relations.post(RelationPostV6Request(null, RelationTypeV6.IsManagedBy, "", ""))
        }
    }

    @Test
    fun testPut(){
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Forbidden,
            outputConsumer = AuthExpectationType.Forbidden
        ){
            gateClient.relations.put(true, RelationPutV6Entry("", RelationTypeV6.IsManagedBy, "", ""))
        }
    }

    @Test
    fun testDelete(){
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Forbidden,
            outputConsumer = AuthExpectationType.Forbidden
        ){
            gateClient.relations.delete("")
        }
    }

    @Test
    fun postSharingStateReady(){
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Forbidden,
            outputConsumer = AuthExpectationType.Forbidden
        ){
            gateClient.sharingStates.postSharingStateReady(PostSharingStateReadyV6Request(emptyList()))
        }
    }

    @Test
    fun getSharingStates(){
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Authorized,
            outputConsumer = AuthExpectationType.Authorized
        ){
            gateClient.sharingStates.getSharingStates(PaginationRequest(), emptyList())
        }
    }

    @Test
    fun testCountPartnersBySharingState(){
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Authorized,
            outputConsumer = AuthExpectationType.Authorized
        ){
            gateClient.stats.countPartnersBySharingState()
        }
    }

    @Test
    fun testCountPartnersPerStage(){
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Authorized,
            outputConsumer = AuthExpectationType.Authorized
        ){
            gateClient.stats.countPartnersPerStage()
        }
    }

    @Test
    fun testCountAddressTypes(){
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Authorized,
            outputConsumer = AuthExpectationType.Authorized
        ){
            gateClient.stats.countAddressTypes(StageType.Input)
        }
    }

    @Test
    fun testGetConfidenceCriteriaStats(){
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Authorized,
            outputConsumer = AuthExpectationType.Authorized
        ){
            gateClient.stats.getConfidenceCriteriaStats()
        }
    }

    protected fun assertExpectations(
        inputManager: AuthExpectationType,
        inputConsumer: AuthExpectationType,
        outputConsumer: AuthExpectationType,
        request: () -> Unit
    ){
        gateClient = operatorClient
        authAssertionHelper.assert(AuthExpectationType.Authorized, request)

        gateClient = inputManagerClient
        authAssertionHelper.assert(inputManager, request)

        gateClient = inputConsumerClient
        authAssertionHelper.assert(inputConsumer, request)

        gateClient = outputConsumerClient
        authAssertionHelper.assert(outputConsumer, request)

        gateClient = unauthorizedClientV6
        authAssertionHelper.assert(AuthExpectationType.Forbidden, request)

        gateClient = anonymousClientV6
        authAssertionHelper.assert(AuthExpectationType.Unauthorized, request)
    }
}