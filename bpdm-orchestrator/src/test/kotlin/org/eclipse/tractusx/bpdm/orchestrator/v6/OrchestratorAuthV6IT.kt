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

package org.eclipse.tractusx.bpdm.orchestrator.v6

import jakarta.annotation.PostConstruct
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.orchestrator.v6.util.OrchestratorTestClientProviderV6
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer
import org.eclipse.tractusx.bpdm.test.util.AuthAssertionHelper
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType.*
import org.eclipse.tractusx.orchestrator.api.model.TaskMode
import org.eclipse.tractusx.orchestrator.api.model.TaskStateRequest
import org.eclipse.tractusx.orchestrator.api.model.TaskStep
import org.eclipse.tractusx.orchestrator.api.model.TaskStepReservationRequest
import org.eclipse.tractusx.orchestrator.api.v6.client.OrchestratorApiClientV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskCreateRequest
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskStepResultRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

class OrchestratorAuthV6IT: UnscheduledOrchestratorTestV6() {

    @Autowired
    private lateinit var testClientProvider: OrchestratorTestClientProviderV6

    private lateinit var operatorClient: OrchestratorApiClientV6
    private lateinit var taskCreatorClient: OrchestratorApiClientV6
    private lateinit var poolSyncClient: OrchestratorApiClientV6
    private lateinit var cleanAndSyncClient: OrchestratorApiClientV6
    private lateinit var cleanClient: OrchestratorApiClientV6
    private lateinit var unauthorizedClient: OrchestratorApiClientV6
    private lateinit var anonymousClient: OrchestratorApiClientV6

    private val authAssertionHelper = AuthAssertionHelper()

    @PostConstruct
    fun init(){
        operatorClient = testClientProvider.createClient(KeyCloakInitializer.CLIENT_ID_OPERATOR)
        taskCreatorClient = testClientProvider.createClient(KeyCloakInitializer.CLIENT_ID_ORCHESTRATOR_TASK_CREATOR)
        poolSyncClient = testClientProvider.createClient(KeyCloakInitializer.CLIENT_ID_ORCHESTRATOR_PROCESSOR_POOL_SYNC)
        cleanAndSyncClient = testClientProvider.createClient(KeyCloakInitializer.CLIENT_ID_ORCHESTRATOR_PROCESSOR_CLEAN_AND_SYNC)
        cleanClient = testClientProvider.createClient(KeyCloakInitializer.CLIENT_ID_ORCHESTRATOR_PROCESSOR_CLEAN)
        unauthorizedClient = testClientProvider.createClient(KeyCloakInitializer.CLIENT_ID_UNAUTHORIZED)
        anonymousClient =  testClientProvider.createClient(null)
    }

    @Test
    fun `create task`(){
        assertExpectations(
            Authorized,
            Forbidden,
            Forbidden,
            Forbidden
        ){
            orchestratorClient.goldenRecordTasks.createTasks(TaskCreateRequest(TaskMode.UpdateFromPool, emptyList()))
        }
    }

    @Test
    fun `search task`(){
        assertExpectations(
            Authorized,
            Forbidden,
            Forbidden,
            Forbidden
        ){
            orchestratorClient.goldenRecordTasks.searchTaskStates(TaskStateRequest(emptyList()))
        }
    }

    @Test
    fun `reserve PoolSync tasks`(){
        assertExpectations(
            Forbidden,
            Authorized,
            Forbidden,
            Forbidden
        ){
            orchestratorClient.goldenRecordTasks.reserveTasksForStep(TaskStepReservationRequest(step = TaskStep.PoolSync))
        }
    }

    @Test
    fun `reserve CleanAndSync tasks`(){
        assertExpectations(
            Forbidden,
            Forbidden,
            Authorized,
            Forbidden
        ){
            orchestratorClient.goldenRecordTasks.reserveTasksForStep(TaskStepReservationRequest(step = TaskStep.CleanAndSync))
        }
    }

    @Test
    fun `reserve Clean tasks`(){
        assertExpectations(
            Forbidden,
            Forbidden,
            Forbidden,
            Authorized
        ){
            orchestratorClient.goldenRecordTasks.reserveTasksForStep(TaskStepReservationRequest(step = TaskStep.Clean))
        }
    }

    @Test
    fun `resolve PoolSync tasks`(){
        assertExpectations(
            Forbidden,
            Authorized,
            Forbidden,
            Forbidden
        ){
            orchestratorClient.goldenRecordTasks.resolveStepResults(TaskStepResultRequest(TaskStep.PoolSync, emptyList()))
        }
    }

    @Test
    fun `resolve CleanAndSync tasks`(){
        assertExpectations(
            Forbidden,
            Forbidden,
            Authorized,
            Forbidden
        ){
            orchestratorClient.goldenRecordTasks.resolveStepResults(TaskStepResultRequest(TaskStep.CleanAndSync, emptyList()))
        }
    }

    @Test
    fun `resolve Clean tasks`(){
        assertExpectations(
            Forbidden,
            Forbidden,
            Forbidden,
            Authorized
        ){
            orchestratorClient.goldenRecordTasks.resolveStepResults(TaskStepResultRequest(TaskStep.Clean, emptyList()))
        }
    }

    @Test
    fun `search finished task events`(){
        assertExpectations(
            Authorized,
            Forbidden,
            Forbidden,
            Forbidden
        ){
            orchestratorClient.finishedTaskEvents.getEvents(Instant.now(), PaginationRequest())
        }
    }

    protected fun assertExpectations(
        taskCreator: AuthExpectationType,
        poolSyncProcessor: AuthExpectationType,
        cleanAndSyncProcessor: AuthExpectationType,
        cleanProcessor: AuthExpectationType,
        request: () -> Unit
    ){
        orchestratorClient = operatorClient
        authAssertionHelper.assert(Authorized, request)

        orchestratorClient = taskCreatorClient
        authAssertionHelper.assert(taskCreator, request)

        orchestratorClient = poolSyncClient
        authAssertionHelper.assert(poolSyncProcessor, request)

        orchestratorClient = cleanAndSyncClient
        authAssertionHelper.assert(cleanAndSyncProcessor, request)

        orchestratorClient = cleanClient
        authAssertionHelper.assert(cleanProcessor, request)

        orchestratorClient = unauthorizedClient
        authAssertionHelper.assert(Forbidden, request)

        orchestratorClient = anonymousClient
        authAssertionHelper.assert(Unauthorized, request)
    }

}