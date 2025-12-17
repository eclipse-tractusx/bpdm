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

package org.eclipse.tractusx.bpdm.gate.v6.auth

import org.eclipse.tractusx.bpdm.gate.Application
import org.eclipse.tractusx.bpdm.gate.api.v6.client.GateClientV6
import org.eclipse.tractusx.bpdm.gate.v6.GateV6Test
import org.eclipse.tractusx.bpdm.gate.v6.util.*
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.util.AuthAssertionHelper
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class])
@ContextConfiguration(initializers = [
    PostgreSQLContextInitializer::class,
    KeyCloakInitializer::class
])
@ActiveProfiles("test-v6")
abstract class GateAuthV6Test: GateV6Test() {

    override lateinit var gateClient: GateClientV6

    @Autowired
    lateinit var operatorClient: GateOperatorClientV6
    @Autowired
    lateinit var inputConsumerClient: GateInputConsumerClientV6
    @Autowired
    lateinit var inputManagerClient: GateInputManagerClientV6
    @Autowired
    lateinit var outputConsumerClient: GateOutputConsumerClientV6
    @Autowired
    lateinit var unauthorizedClientV6: GateUnauthorizedClientV6
    @Autowired
    lateinit var anonymousClientV6: GateAnonymousClientV6
    @Autowired
    lateinit var authAssertionHelper: AuthAssertionHelper

    @BeforeEach
    override fun beforeEach(testInfo: TestInfo) {
        super.beforeEach(testInfo)
    }

    protected fun assertExpectations(
        inputManager: AuthExpectationType,
        inputConsumer: AuthExpectationType,
        outputConsumer: AuthExpectationType,
        request: () -> Unit
    ){
        gateClient = operatorClient
        authAssertionHelper.assert(Authorized, request)

        gateClient = inputManagerClient
        authAssertionHelper.assert(inputManager, request)

        gateClient = inputConsumerClient
        authAssertionHelper.assert(inputConsumer, request)

        gateClient = outputConsumerClient
        authAssertionHelper.assert(outputConsumer, request)

        gateClient = unauthorizedClientV6
        authAssertionHelper.assert(Forbidden, request)

        gateClient = anonymousClientV6
        authAssertionHelper.assert(Unauthorized, request)
    }
}