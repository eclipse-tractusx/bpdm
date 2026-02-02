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

package org.eclipse.tractusx.bpdm.test.testdata.orchestrator

import com.github.tomakehurst.wiremock.client.WireMock
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.SiteVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteWithMainAddressVerboseDto
import org.eclipse.tractusx.bpdm.test.containers.OrchestratorMockContextInitializer
import org.eclipse.tractusx.orchestrator.api.ApiCommons.BASE_PATH_V7_BUSINESS_PARTNERS
import org.eclipse.tractusx.orchestrator.api.model.*
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.util.*

class OrchestratorMockDataFactory(
    private val refinementTestDataFactory: RefinementTestDataFactory,
    private val jsonMapper: JsonMapper
) {

    private val orchestratorMockServer = OrchestratorMockContextInitializer.wiremockServer

    fun mockRefineToLegalEntity(
        seed: String,
        legalEntityGoldenRecord: LegalEntityWithLegalAddressVerboseDto,
        owningCompany: String?,
        nameParts: List<String>
    ): TaskClientStateDto{
        configureWireMock()

        val mockedCreatedTask = mockCreateTask(seed)

        val refinementTaskData = refinementTestDataFactory.buildLegalEntityBusinessPartner(legalEntityGoldenRecord, owningCompany, nameParts)
        val mockedRefinedTask = mockTaskRefinedSuccessfully(mockedCreatedTask.taskId, refinementTaskData, seed)

        return mockedRefinedTask
    }

    fun mockRefineToLegalEntityOnSite(
        seed: String,
        legalEntityGoldenRecord: LegalEntityWithLegalAddressVerboseDto,
        siteGoldenRecord: SiteVerboseDto,
        owningCompany: String?,
        nameParts: List<String>
    ): TaskClientStateDto{
        configureWireMock()

        val mockedCreatedTask = mockCreateTask(seed)

        val refinementTaskData = refinementTestDataFactory.buildLegalEntityOnSiteBusinessPartner(
            legalEntityGoldenRecord,
            siteGoldenRecord,
            owningCompany,
            nameParts
        )
        val mockedRefinedTask = mockTaskRefinedSuccessfully(mockedCreatedTask.taskId, refinementTaskData, seed)

        return mockedRefinedTask
    }

    fun mockRefineToSite(
        seed: String,
        legalEntityGoldenRecord: LegalEntityWithLegalAddressVerboseDto,
        siteGoldenRecord: SiteWithMainAddressVerboseDto,
        owningCompany: String?,
        nameParts: List<String>
    ): TaskClientStateDto{
        configureWireMock()

        val mockedCreatedTask = mockCreateTask(seed)

        val refinementTaskData = refinementTestDataFactory.buildSiteBusinessPartner(legalEntityGoldenRecord, siteGoldenRecord, owningCompany, nameParts)
        val mockedRefinedTask = mockTaskRefinedSuccessfully(mockedCreatedTask.taskId, refinementTaskData, seed)

        return mockedRefinedTask
    }

    fun mockRefineToAdditionalAddressOfSite(
        seed: String,
        legalEntityGoldenRecord: LegalEntityWithLegalAddressVerboseDto,
        siteGoldenRecord: SiteWithMainAddressVerboseDto,
        addressGoldenRecord: LogisticAddressVerboseDto,
        owningCompany: String?,
        nameParts: List<String>
    ): TaskClientStateDto{
        configureWireMock()

        val mockedCreatedTask = mockCreateTask(seed)

        val refinementTaskData = refinementTestDataFactory.buildAdditionSiteAddressBusinessPartner(legalEntityGoldenRecord, siteGoldenRecord, addressGoldenRecord, owningCompany, nameParts)
        val mockedRefinedTask = mockTaskRefinedSuccessfully(mockedCreatedTask.taskId, refinementTaskData, seed)

        return mockedRefinedTask
    }

    fun mockCreateTask(seed: String): TaskClientStateDto{
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        val mockedCreatedTaskResponse = buildInitialTaskCreationResponse(seed)
        val mockedCreatedTask = mockedCreatedTaskResponse.createdTasks.single()
        WireMock.stubFor(
            WireMock
                .post(BASE_PATH_V7_BUSINESS_PARTNERS)
                .willReturn(WireMock.okJson(jsonMapper.writeValueAsString(mockedCreatedTaskResponse)))
        )

        return mockedCreatedTask
    }

    fun mockTaskRefinedSuccessfully(taskId: String, refinementTaskData: BusinessPartner, seed: String): TaskClientStateDto{
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("${BASE_PATH_V7_BUSINESS_PARTNERS}/finished-events")).willReturn(WireMock.okJson(
            jsonMapper.writeValueAsString(
                FinishedTaskEventsResponse(1, 1, 0, 1, listOf(
                    FinishedTaskEventsResponse.Event(Instant.now(), ResultState.Success, taskId)
                ))
            )
        )))

        val mockedRefinedTasks = buildSuccessTaskState(seed, refinementTaskData)
        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("${BASE_PATH_V7_BUSINESS_PARTNERS}/state/search")).willReturn(WireMock.okJson(
            jsonMapper.writeValueAsString(mockedRefinedTasks)
        )))

        return mockedRefinedTasks.tasks.single()
    }

    fun mockSharingError(seed: String, errorType: TaskErrorType): TaskClientStateDto{
        WireMock.configureFor("localhost", orchestratorMockServer.port())

        val mockedCreatedTask = mockCreateTask(seed)

        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("${BASE_PATH_V7_BUSINESS_PARTNERS}/finished-events")).willReturn(WireMock.okJson(
            jsonMapper.writeValueAsString(
                FinishedTaskEventsResponse(1, 1, 0, 1, listOf(
                    FinishedTaskEventsResponse.Event(Instant.now(), ResultState.Error, mockedCreatedTask.taskId)
                ))
            )
        )))

        val mockedErrorTasks = buildErrorTaskState(seed, errorType)
        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("${BASE_PATH_V7_BUSINESS_PARTNERS}/state/search")).willReturn(WireMock.okJson(
            jsonMapper.writeValueAsString(mockedErrorTasks)
        )))

        return mockedErrorTasks.tasks.single()
    }

    fun buildInitialTaskCreationResponse(seed: String): TaskCreateResponse{
        return TaskCreateResponse(
            listOf(
                TaskClientStateDto(
                    UUID.nameUUIDFromBytes("TaskID_$seed".encodeToByteArray()).toString(),
                    UUID.nameUUIDFromBytes("RecordID_$seed".encodeToByteArray()).toString(),
                    BusinessPartner.empty,
                    TaskProcessingStateDto(
                        ResultState.Pending,
                        TaskStep.CleanAndSync,
                        StepState.Queued,
                        emptyList(),
                        Instant.now(),
                        Instant.now(),
                        Instant.now()
                    )
                )
            )
        )
    }

    fun buildSuccessTaskState(seed: String, finishedBusinessPartner: BusinessPartner): TaskStateResponse{
        return TaskStateResponse(
            listOf(
                TaskClientStateDto(
                    UUID.nameUUIDFromBytes("TaskID_$seed".encodeToByteArray()).toString(),
                    UUID.nameUUIDFromBytes("RecordID_$seed".encodeToByteArray()).toString(),
                    finishedBusinessPartner,
                    TaskProcessingStateDto(
                        ResultState.Success,
                        TaskStep.PoolSync,
                        StepState.Success,
                        emptyList(),
                        Instant.now(),
                        Instant.now(),
                        Instant.now()
                    )
                )
            )
        )
    }

    private fun buildErrorTaskState(seed: String, errorType: TaskErrorType): TaskStateResponse{
        return TaskStateResponse(
            listOf(
                TaskClientStateDto(
                    UUID.nameUUIDFromBytes("TaskID_$seed".encodeToByteArray()).toString(),
                    UUID.nameUUIDFromBytes("RecordID_$seed".encodeToByteArray()).toString(),
                    BusinessPartner.empty,
                    TaskProcessingStateDto(
                        ResultState.Error,
                        TaskStep.PoolSync,
                        StepState.Error,
                        listOf(TaskErrorDto(errorType, "$seed Description")),
                        Instant.now(),
                        Instant.now(),
                        Instant.now()
                    )
                )
            )
        )
    }

    private fun configureWireMock(){
        WireMock.configureFor("localhost", orchestratorMockServer.port())
        WireMock.reset()
    }

}