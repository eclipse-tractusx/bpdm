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

package org.eclipse.tractusx.bpdm.orchestrator.auth

import org.eclipse.tractusx.bpdm.orchestrator.Application
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.containers.SelfClientInitializer
import org.eclipse.tractusx.bpdm.test.util.AuthAssertionHelper
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class])
@ContextConfiguration(initializers = [
    PostgreSQLContextInitializer::class,
    KeyCloakInitializer::class,
    AuthTaskProcessorPoolTaskProcessingJobApiSyncIT.SelfClientAsTaskProcessorPoolSyncInitializer::class
])
class AuthTaskProcessorPoolTaskProcessingJobApiSyncIT @Autowired constructor(
    orchestratorClient: OrchestrationApiClient,
    authAssertionHelper: AuthAssertionHelper
): AuthTestBase(
    orchestratorClient,
    authAssertionHelper,
    OrchestratorAuthExpectations(
        tasks = TaskAuthExpectations(
            postTask = AuthExpectationType.Forbidden,
            postStateSearch = AuthExpectationType.Forbidden
        ),
        stepClean = TaskStepAuthExpectations(
            postReservation = AuthExpectationType.Forbidden,
            postResult = AuthExpectationType.Forbidden
        ),
        stepCleanAndSync = TaskStepAuthExpectations(
            postReservation = AuthExpectationType.Forbidden,
            postResult = AuthExpectationType.Forbidden
        ),
        stepPoolSync = TaskStepAuthExpectations(
            postReservation = AuthExpectationType.Authorized,
            postResult = AuthExpectationType.Authorized
        )
    )
){

    class SelfClientAsTaskProcessorPoolSyncInitializer : SelfClientInitializer() {
        override val clientId: String
            get() = "POOL-ORCHESTRATOR-TASK-PROCESSOR"
    }
}