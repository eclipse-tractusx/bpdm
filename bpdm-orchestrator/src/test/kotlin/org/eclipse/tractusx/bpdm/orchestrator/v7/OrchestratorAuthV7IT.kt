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

package org.eclipse.tractusx.bpdm.orchestrator.v7

import jakarta.annotation.PostConstruct
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.orchestrator.v7.util.OrchestratorTestClientProviderV7
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer
import org.eclipse.tractusx.bpdm.test.util.AuthAssertionHelper
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType.*
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.client.OrchestratorV7Client
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.sharingmember.SharingMemberRecordQueryV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.sharingmember.SharingMemberRecordUpdateV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskCreateRelationsV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskCreateV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskModeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStepResultV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskResultStateSearchV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStateV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepReservationV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepResultV7Request
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

class OrchestratorAuthV7IT: UnscheduledOrchestratorTestBaseV7() {

    @Autowired
    private lateinit var testClientProvider: OrchestratorTestClientProviderV7

    private lateinit var operatorClient: OrchestratorV7Client
    private lateinit var taskCreatorClient: OrchestratorV7Client
    private lateinit var poolSyncClient: OrchestratorV7Client
    private lateinit var cleanAndSyncClient: OrchestratorV7Client
    private lateinit var cleanClient: OrchestratorV7Client
    private lateinit var unauthorizedClient: OrchestratorV7Client
    private lateinit var anonymousClient: OrchestratorV7Client

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
    fun `create business partner task`(){
        assertExpectations(
            Authorized,
            Forbidden,
            Forbidden,
            Forbidden
        ){
            orchestratorClient.goldenRecordTasks.createTasks(TaskCreateV7Request(TaskModeV7.UpdateFromPool, emptyList()))
        }
    }

    @Test
    fun `search business partner task states`(){
        assertExpectations(
            Authorized,
            Forbidden,
            Forbidden,
            Forbidden
        ){
            orchestratorClient.goldenRecordTasks.searchTaskStates(TaskStateV7Request(emptyList()))
        }
    }

    @Test
    fun `search business partner task result states`(){
        assertExpectations(
            Authorized,
            Forbidden,
            Forbidden,
            Forbidden
        ){
            orchestratorClient.goldenRecordTasks.searchTaskResultStates(TaskResultStateSearchV7Request(emptyList()))
        }
    }

    @Test
    fun `search business partner finished task events`(){
        assertExpectations(
            Authorized,
            Forbidden,
            Forbidden,
            Forbidden
        ){
            orchestratorClient.finishedTaskEvents.getEvents(Instant.now(), PaginationRequest())
        }
    }

    @Test
    fun `reserve PoolSync business partner tasks`(){
        assertExpectations(
            Forbidden,
            Authorized,
            Forbidden,
            Forbidden
        ){
            orchestratorClient.goldenRecordTasks.reserveTasksForStep(TaskStepReservationV7Request(step = TaskStepV7.PoolSync))
        }
    }

    @Test
    fun `reserve CleanAndSync business partner tasks`(){
        assertExpectations(
            Forbidden,
            Forbidden,
            Authorized,
            Forbidden
        ){
            orchestratorClient.goldenRecordTasks.reserveTasksForStep(TaskStepReservationV7Request(step = TaskStepV7.CleanAndSync))
        }
    }

    @Test
    fun `reserve Clean business partner tasks`(){
        assertExpectations(
            Forbidden,
            Forbidden,
            Forbidden,
            Authorized
        ){
            orchestratorClient.goldenRecordTasks.reserveTasksForStep(TaskStepReservationV7Request(step = TaskStepV7.Clean))
        }
    }

    @Test
    fun `resolve PoolSync business partner tasks`(){
        assertExpectations(
            Forbidden,
            Authorized,
            Forbidden,
            Forbidden
        ){
            orchestratorClient.goldenRecordTasks.resolveStepResults(TaskStepResultV7Request(TaskStepV7.PoolSync, emptyList()))
        }
    }

    @Test
    fun `resolve CleanAndSync business partner tasks`(){
        assertExpectations(
            Forbidden,
            Forbidden,
            Authorized,
            Forbidden
        ){
            orchestratorClient.goldenRecordTasks.resolveStepResults(TaskStepResultV7Request(TaskStepV7.CleanAndSync, emptyList()))
        }
    }

    @Test
    fun `resolve Clean business partner tasks`(){
        assertExpectations(
            Forbidden,
            Forbidden,
            Forbidden,
            Authorized
        ){
            orchestratorClient.goldenRecordTasks.resolveStepResults(TaskStepResultV7Request(TaskStepV7.Clean, emptyList()))
        }
    }

    @Test
    fun `create relation task`(){
        assertExpectations(
            Authorized,
            Forbidden,
            Forbidden,
            Forbidden
        ){
            orchestratorClient.relationsGoldenRecordTasks.createTasks(TaskCreateRelationsV7Request(TaskModeV7.UpdateFromPool, emptyList()))
        }
    }

    @Test
    fun `search relation task states`(){
        assertExpectations(
            Authorized,
            Forbidden,
            Forbidden,
            Forbidden
        ){
            orchestratorClient.relationsGoldenRecordTasks.searchTaskStates(TaskStateV7Request(emptyList()))
        }
    }

    @Test
    fun `search relation task result states`(){
        assertExpectations(
            Authorized,
            Forbidden,
            Forbidden,
            Forbidden
        ){
            orchestratorClient.relationsGoldenRecordTasks.searchTaskResultStates(TaskResultStateSearchV7Request(emptyList()))
        }
    }

    @Test
    fun `search finished relation task events`(){
        assertExpectations(
            Authorized,
            Forbidden,
            Forbidden,
            Forbidden
        ){
            orchestratorClient.relationsFinishedTaskEvents.getRelationsEvents(Instant.now(), PaginationRequest())
        }
    }

    @Test
    fun `reserve PoolSync relation tasks`(){
        assertExpectations(
            Forbidden,
            Authorized,
            Forbidden,
            Forbidden
        ){
            orchestratorClient.relationsGoldenRecordTasks.reserveTasksForStep(TaskStepReservationV7Request(step = TaskStepV7.PoolSync))
        }
    }

    @Test
    fun `reserve CleanAndSync relation tasks`(){
        assertExpectations(
            Forbidden,
            Forbidden,
            Authorized,
            Forbidden
        ){
            orchestratorClient.relationsGoldenRecordTasks.reserveTasksForStep(TaskStepReservationV7Request(step = TaskStepV7.CleanAndSync))
        }
    }

    @Test
    fun `reserve Clean relation tasks`(){
        assertExpectations(
            Forbidden,
            Forbidden,
            Forbidden,
            Authorized
        ){
            orchestratorClient.relationsGoldenRecordTasks.reserveTasksForStep(TaskStepReservationV7Request(step = TaskStepV7.Clean))
        }
    }

    @Test
    fun `resolve PoolSync relation tasks`(){
        assertExpectations(
            Forbidden,
            Authorized,
            Forbidden,
            Forbidden
        ){
            orchestratorClient.relationsGoldenRecordTasks.resolveStepResults(TaskRelationsStepResultV7Request(TaskStepV7.PoolSync, emptyList()))
        }
    }

    @Test
    fun `resolve CleanAndSync relation tasks`(){
        assertExpectations(
            Forbidden,
            Forbidden,
            Authorized,
            Forbidden
        ){
            orchestratorClient.relationsGoldenRecordTasks.resolveStepResults(TaskRelationsStepResultV7Request(TaskStepV7.CleanAndSync, emptyList()))
        }
    }

    @Test
    fun `resolve Clean relation tasks`(){
        assertExpectations(
            Forbidden,
            Forbidden,
            Forbidden,
            Authorized
        ){
            orchestratorClient.relationsGoldenRecordTasks.resolveStepResults(TaskRelationsStepResultV7Request(TaskStepV7.Clean, emptyList()))
        }
    }

    @Test
    fun `get sharing member records`(){
        assertExpectations(
            Forbidden,
            Authorized,
            Forbidden,
            Forbidden
        ){
            orchestratorClient.sharingMemberRecords.queryRecords(SharingMemberRecordQueryV7Request(Instant.now()), PaginationRequest())
        }
    }

    @Test
    fun `update sharing member record`(){
        assertExpectations(
            Authorized,
            Forbidden,
            Forbidden,
            Forbidden
        ){
            orchestratorClient.sharingMemberRecords.update(SharingMemberRecordUpdateV7Request("any", true))
        }
    }

    private fun assertExpectations(
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