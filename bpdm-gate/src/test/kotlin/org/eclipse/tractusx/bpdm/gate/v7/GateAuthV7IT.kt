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

package org.eclipse.tractusx.bpdm.gate.v7

import jakarta.annotation.PostConstruct
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.request.ChangelogSearchRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.PostSharingStateReadyRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationSearchRequest
import org.eclipse.tractusx.bpdm.gate.v7.util.GateTestClientProviderV7
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer
import org.eclipse.tractusx.bpdm.test.util.AuthAssertionHelper
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class GateAuthV7IT : UnscheduledGateTestBaseV7() {

    @Autowired
    lateinit var gateClient: GateClient

    @Autowired
    lateinit var authAssertionHelper: AuthAssertionHelper

    @Autowired
    lateinit var testClientProvider: GateTestClientProviderV7

    lateinit var operatorClient: GateClient
    lateinit var inputManagerClient: GateClient
    lateinit var inputConsumerClient: GateClient
    lateinit var outputConsumerClient: GateClient
    lateinit var unauthorizedClient: GateClient
    lateinit var anonymousClient: GateClient

    @PostConstruct
    fun init() {
        operatorClient = testClientProvider.createClient(KeyCloakInitializer.CLIENT_ID_OPERATOR)
        inputManagerClient = testClientProvider.createClient(KeyCloakInitializer.CLIENT_ID_GATE_INPUT_MANAGER)
        inputConsumerClient = testClientProvider.createClient(KeyCloakInitializer.CLIENT_ID_GATE_INPUT_CONSUMER)
        outputConsumerClient = testClientProvider.createClient(KeyCloakInitializer.CLIENT_ID_GATE_OUTPUT_CONSUMER)
        unauthorizedClient = testClientProvider.createClient(KeyCloakInitializer.CLIENT_ID_UNAUTHORIZED)
        anonymousClient = testClientProvider.createClient(null)
    }

    @Test
    fun upsertBusinessPartnersInput() {
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Forbidden,
            outputConsumer = AuthExpectationType.Forbidden
        ) { gateClient.businessParters.upsertBusinessPartnersInput(emptyList()) }
    }

    @Test
    fun getBusinessPartnersInput() {
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Authorized,
            outputConsumer = AuthExpectationType.Forbidden
        ) { gateClient.businessParters.getBusinessPartnersInput() }
    }

    @Test
    fun getBusinessPartnersOutput() {
        assertExpectations(
            inputManager = AuthExpectationType.Forbidden,
            inputConsumer = AuthExpectationType.Forbidden,
            outputConsumer = AuthExpectationType.Authorized
        ) { gateClient.businessParters.getBusinessPartnersOutput() }
    }

    @Test
    fun getInputChangelog() {
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Authorized,
            outputConsumer = AuthExpectationType.Forbidden
        ) { gateClient.changelog.getInputChangelog(PaginationRequest(), ChangelogSearchRequest()) }
    }

    @Test
    fun getOutputChangelog() {
        assertExpectations(
            inputManager = AuthExpectationType.Forbidden,
            inputConsumer = AuthExpectationType.Forbidden,
            outputConsumer = AuthExpectationType.Authorized
        ) { gateClient.changelog.getOutputChangelog(PaginationRequest(), ChangelogSearchRequest()) }
    }

    @Test
    fun getSharingStates() {
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Authorized,
            outputConsumer = AuthExpectationType.Authorized
        ) { gateClient.sharingState.getSharingStates(PaginationRequest(), null) }
    }

    @Test
    fun postSharingStateReady() {
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Forbidden,
            outputConsumer = AuthExpectationType.Forbidden
        ) { gateClient.sharingState.postSharingStateReady(PostSharingStateReadyRequest(emptyList())) }
    }

    @Test
    fun countPartnersBySharingState() {
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Authorized,
            outputConsumer = AuthExpectationType.Authorized
        ) { gateClient.stats.countPartnersBySharingState() }
    }

    @Test
    fun countPartnersPerStage() {
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Authorized,
            outputConsumer = AuthExpectationType.Authorized
        ) { gateClient.stats.countPartnersPerStage() }
    }

    @Test
    fun countAddressTypes() {
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Authorized,
            outputConsumer = AuthExpectationType.Authorized
        ) { gateClient.stats.countAddressTypes(StageType.Input) }
    }

    @Test
    fun getConfidenceCriteriaStats() {
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Authorized,
            outputConsumer = AuthExpectationType.Authorized
        ) { gateClient.stats.getConfidenceCriteriaStats() }
    }

    @Test
    fun postSearchRelations() {
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Authorized,
            outputConsumer = AuthExpectationType.Forbidden
        ) { gateClient.relation.postSearch(RelationSearchRequest()) }
    }

    @Test
    fun putRelation() {
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Forbidden,
            outputConsumer = AuthExpectationType.Forbidden
        ) { gateClient.relation.put(true, RelationPutRequest(emptyList())) }
    }

    @Test
    fun postSearchRelationOutput() {
        assertExpectations(
            inputManager = AuthExpectationType.Forbidden,
            inputConsumer = AuthExpectationType.Forbidden,
            outputConsumer = AuthExpectationType.Authorized
        ) { gateClient.relationOutput.postSearch() }
    }

    @Test
    fun getRelationSharingState() {
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Authorized,
            outputConsumer = AuthExpectationType.Authorized
        ) { gateClient.relationSharingState.get() }
    }

    @Test
    fun getRelationInputChangelog() {
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Authorized,
            outputConsumer = AuthExpectationType.Forbidden
        ) { gateClient.relationChangelog.getInputChangelog(PaginationRequest(), ChangelogSearchRequest()) }
    }

    @Test
    fun getRelationOutputChangelog() {
        assertExpectations(
            inputManager = AuthExpectationType.Forbidden,
            inputConsumer = AuthExpectationType.Forbidden,
            outputConsumer = AuthExpectationType.Authorized
        ) { gateClient.relationChangelog.getOutputChangelog(PaginationRequest(), ChangelogSearchRequest()) }
    }

    private fun assertExpectations(
        inputManager: AuthExpectationType,
        inputConsumer: AuthExpectationType,
        outputConsumer: AuthExpectationType,
        request: () -> Unit
    ) {
        val savedClient = gateClient

        gateClient = operatorClient
        authAssertionHelper.assert(AuthExpectationType.Authorized, request)

        gateClient = inputManagerClient
        authAssertionHelper.assert(inputManager, request)

        gateClient = inputConsumerClient
        authAssertionHelper.assert(inputConsumer, request)

        gateClient = outputConsumerClient
        authAssertionHelper.assert(outputConsumer, request)

        gateClient = unauthorizedClient
        authAssertionHelper.assert(AuthExpectationType.Forbidden, request)

        gateClient = anonymousClient
        authAssertionHelper.assert(AuthExpectationType.Unauthorized, request)

        gateClient = savedClient
    }
}
