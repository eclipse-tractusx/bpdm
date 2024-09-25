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

package org.eclipse.tractusx.bpdm.gate.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.BusinessPartnerIdentifierDto
import org.eclipse.tractusx.bpdm.gate.api.model.BusinessPartnerStateDto
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SharingStateDto
import org.eclipse.tractusx.bpdm.pool.api.PoolChangelogApi
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.api.model.response.ChangelogEntryVerboseDto
import org.eclipse.tractusx.bpdm.test.testdata.gate.BusinessPartnerGenericCommonValues
import org.eclipse.tractusx.bpdm.test.testdata.gate.BusinessPartnerVerboseValues
import org.eclipse.tractusx.bpdm.test.util.AssertHelpers
import org.eclipse.tractusx.orchestrator.api.FinishedTaskEventApi
import org.eclipse.tractusx.orchestrator.api.GoldenRecordTaskApi
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class MockAndAssertUtils @Autowired constructor(
    val gateClient: GateClient,
    val objectMapper: ObjectMapper,
    val assertHelpers: AssertHelpers
) {

    val ORCHESTRATOR_CREATE_TASKS_URL = GoldenRecordTaskApi.TASKS_PATH
    val ORCHESTRATOR_SEARCH_TASK_STATES_URL = "${GoldenRecordTaskApi.TASKS_PATH}/state/search"
    val ORCHESTRATOR_SEARCH_TASK_RESULT_STATES_URL = "${GoldenRecordTaskApi.TASKS_PATH}/result-state/search"
    val POOL_API_SEARCH_CHANGE_LOG_URL = "${PoolChangelogApi.CHANGELOG_PATH}/search"

    fun mockOrchestratorApi(gateWireMockServer: WireMockExtension) {
        val taskCreateResponse =
            TaskCreateResponse(
                listOf(
                    TaskClientStateDto(
                        taskId = "0",
                        businessPartnerResult = BusinessPartnerGenericCommonValues.businessPartner1,
                        recordId = "e3a05ebc-ff59-4d09-bd58-da31d6245701",
                        processingState = TaskProcessingStateDto(
                            resultState = ResultState.Pending,
                            step = TaskStep.CleanAndSync,
                            stepState = StepState.Queued,
                            errors = emptyList(),
                            createdAt = Instant.now(),
                            modifiedAt = Instant.now(),
                            timeout = Instant.now()
                        )
                    ),
                    TaskClientStateDto(
                        taskId = "1",
                        businessPartnerResult = BusinessPartnerGenericCommonValues.businessPartner1,
                        recordId = "f05574ff-4ddd-4360-821a-923203711f85",
                        processingState = TaskProcessingStateDto(
                            resultState = ResultState.Pending,
                            step = TaskStep.CleanAndSync,
                            stepState = StepState.Queued,
                            errors = emptyList(),
                            createdAt = Instant.now(),
                            modifiedAt = Instant.now(),
                            timeout = Instant.now()
                        )
                    ),
                    TaskClientStateDto(
                        taskId = "2",
                        businessPartnerResult = BusinessPartnerGenericCommonValues.businessPartner1,
                        recordId = "8c03850d-d772-4a2e-9845-65211231b38c",
                        processingState = TaskProcessingStateDto(
                            resultState = ResultState.Pending,
                            step = TaskStep.CleanAndSync,
                            stepState = StepState.Queued,
                            errors = emptyList(),
                            createdAt = Instant.now(),
                            modifiedAt = Instant.now(),
                            timeout = Instant.now()
                        )
                    ),
                    TaskClientStateDto(
                        taskId = "3",
                        businessPartnerResult = BusinessPartnerGenericCommonValues.businessPartner1,
                        recordId = "6bebb1aa-935d-467a-afd4-7e8623420a18",
                        processingState = TaskProcessingStateDto(
                            resultState = ResultState.Pending,
                            step = TaskStep.CleanAndSync,
                            stepState = StepState.Queued,
                            errors = emptyList(),
                            createdAt = Instant.now(),
                            modifiedAt = Instant.now(),
                            timeout = Instant.now()
                        )
                    )
                )
            )

        // Orchestrator request new cleaning endpoint
        gateWireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(ORCHESTRATOR_CREATE_TASKS_URL))
                .willReturn(
                    WireMock.okJson(objectMapper.writeValueAsString(taskCreateResponse))
                )
        )
    }

    fun mockOrchestratorApiCleaned(gateWireMockServer: WireMockExtension) {
        val taskStateResponse =
            TaskStateResponse(
                listOf(
                    TaskClientStateDto(
                        taskId = "0",
                        businessPartnerResult = BusinessPartnerGenericCommonValues.businessPartner1,
                        recordId = "e3a05ebc-ff59-4d09-bd58-da31d6245701",
                        processingState = TaskProcessingStateDto(
                            resultState = ResultState.Success,
                            step = TaskStep.CleanAndSync,
                            stepState = StepState.Queued,
                            errors = emptyList(),
                            createdAt = Instant.now(),
                            modifiedAt = Instant.now(),
                            timeout = Instant.now()
                        )
                    ),
                    TaskClientStateDto(
                        taskId = "1",
                        businessPartnerResult = BusinessPartnerGenericCommonValues.businessPartner1,
                        recordId = "f05574ff-4ddd-4360-821a-923203711f85",
                        processingState = TaskProcessingStateDto(
                            resultState = ResultState.Error,
                            step = TaskStep.CleanAndSync,
                            stepState = StepState.Queued,
                            errors = listOf(
                                TaskErrorDto(TaskErrorType.Timeout, "Major Error"),
                                TaskErrorDto(TaskErrorType.Unspecified, "Minor Error")
                            ),
                            createdAt = Instant.now(),
                            modifiedAt = Instant.now(),
                            timeout = Instant.now()
                        )
                    )
                )
            )

        gateWireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(ORCHESTRATOR_SEARCH_TASK_STATES_URL)).willReturn(
                WireMock.okJson(objectMapper.writeValueAsString(taskStateResponse))
            )
        )

        gateWireMockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo(FinishedTaskEventApi.FINISHED_TASK_EVENT_PATH)).willReturn(
                WireMock.okJson(objectMapper.writeValueAsString(
                    FinishedTaskEventsResponse(2, 1, 0, 2, content =
                        listOf(
                            FinishedTaskEventsResponse.Event(Instant.now(), ResultState.Success, "0"),
                            FinishedTaskEventsResponse.Event(Instant.now(), ResultState.Error, "1")
                        )
                    )
                ))
            )
        )


    }

    fun mockOrchestratorApiResultStates(gateWireMockServer: WireMockExtension, resultStates: List<ResultState?>){
        gateWireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(ORCHESTRATOR_SEARCH_TASK_RESULT_STATES_URL)).willReturn(
                WireMock.okJson(objectMapper.writeValueAsString(TaskResultStateSearchResponse( resultStates = resultStates )))
            )
        )
    }

    fun mockOrchestratorApiCleanedResponseSizeOne(gateWireMockServer: WireMockExtension) {
        val taskStateResponse = TaskStateResponse(
            listOf(
                TaskClientStateDto(
                    taskId = "0",
                    businessPartnerResult = BusinessPartnerGenericCommonValues.businessPartner1,
                    recordId = "e3a05ebc-ff59-4d09-bd58-da31d6245701",
                    processingState = TaskProcessingStateDto(
                        resultState = ResultState.Success,
                        step = TaskStep.CleanAndSync,
                        stepState = StepState.Queued,
                        errors = emptyList(),
                        createdAt = Instant.now(),
                        modifiedAt = Instant.now(),
                        timeout = Instant.now()
                    )
                )
            )
        )
        gateWireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(ORCHESTRATOR_SEARCH_TASK_STATES_URL))
                .willReturn(
                    WireMock.okJson(objectMapper.writeValueAsString(taskStateResponse))
                )
        )

        gateWireMockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo(FinishedTaskEventApi.FINISHED_TASK_EVENT_PATH)).willReturn(
                WireMock.okJson(objectMapper.writeValueAsString(
                    FinishedTaskEventsResponse(1, 1, 0, 1, content =
                    listOf(
                        FinishedTaskEventsResponse.Event(Instant.now(), ResultState.Success, "0")
                    )
                    )
                ))
            )
        )
    }

    fun mockPoolApiGetChangeLogs(poolWireMockServer: WireMockExtension) {

        val poolChangelogEntries = PageDto(
            totalElements = 3,
            totalPages = 1,
            page = 0,
            contentSize = 3,
            content = listOf(
                ChangelogEntryVerboseDto(
                    bpn = BusinessPartnerVerboseValues.bpOutputDtoCleaned.legalEntity.legalEntityBpn,
                    businessPartnerType = BusinessPartnerType.LEGAL_ENTITY,
                    timestamp = Instant.now(),
                    changelogType = ChangelogType.UPDATE
                ),
                ChangelogEntryVerboseDto(
                    bpn = BusinessPartnerVerboseValues.bpOutputDtoCleaned.address.addressBpn,
                    businessPartnerType = BusinessPartnerType.ADDRESS,
                    timestamp = Instant.now(),
                    changelogType = ChangelogType.UPDATE
                ),
                ChangelogEntryVerboseDto(
                    bpn = BusinessPartnerVerboseValues.bpOutputDtoCleaned.site!!.siteBpn!!,
                    businessPartnerType = BusinessPartnerType.SITE,
                    timestamp = Instant.now(),
                    changelogType = ChangelogType.UPDATE
                )
            )
        )
        // Pool APi get changelogs endpoint
        poolWireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(POOL_API_SEARCH_CHANGE_LOG_URL))
                .willReturn(
                    WireMock.okJson(objectMapper.writeValueAsString(poolChangelogEntries))
                )
        )
    }

    fun readSharingStates(externalIds: Collection<String>?): Collection<SharingStateDto> {

        return gateClient.sharingState.getSharingStates(PaginationRequest(), externalIds).content
    }

    fun assertUpsertOutputResponsesMatchRequests(responses: Collection<BusinessPartnerOutputDto>, requests: List<BusinessPartnerOutputDto>) {
        Assertions.assertThat(responses)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFieldsOfTypes(Instant::class.java)
            .isEqualTo(requests)
    }

    fun assertUpsertResponsesMatchRequests(responses: Collection<BusinessPartnerInputDto>, requests: List<BusinessPartnerInputRequest>) {
        Assertions.assertThat(responses)
            .usingRecursiveComparison()
            .ignoringFieldsOfTypes(Instant::class.java)
            .isEqualTo(requests.map(::toExpectedResponse))
    }

    private fun toExpectedResponse(request: BusinessPartnerInputRequest): BusinessPartnerInputDto {
        // same sorting order as defined for entity
        return BusinessPartnerInputDto(
            externalId = request.externalId,
            nameParts = request.nameParts,
            identifiers = request.identifiers.toSortedSet(identifierDtoComparator),
            states = request.states.toSortedSet(stateDtoComparator),
            roles = request.roles.toSortedSet(),
            isOwnCompanyData = request.isOwnCompanyData,
            legalEntity = request.legalEntity,
            site = request.site,
            address = request.address,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    val identifierDtoComparator = compareBy(
        BusinessPartnerIdentifierDto::type,
        BusinessPartnerIdentifierDto::value,
        BusinessPartnerIdentifierDto::issuingBody
    )

    val stateDtoComparator = compareBy(nullsFirst(), BusinessPartnerStateDto::validFrom)       // here null means MIN
        .thenBy(nullsLast(), BusinessPartnerStateDto::validTo)        // here null means MAX
        .thenBy(BusinessPartnerStateDto::type)

    fun assertBusinessPartnersUpsertedCorrectly(upsertedBusinessPartners: Collection<BusinessPartnerInputDto>) {
        val searchResponsePage = gateClient.businessParters.getBusinessPartnersInput(null)
        org.junit.jupiter.api.Assertions.assertEquals(upsertedBusinessPartners.size.toLong(), searchResponsePage.totalElements)
        assertHelpers.assertRecursively(searchResponsePage.content).isEqualTo(upsertedBusinessPartners)

        val sharingStateResponse = gateClient.sharingState.getSharingStates(PaginationRequest(), externalIds = null)
        org.junit.jupiter.api.Assertions.assertEquals(upsertedBusinessPartners.size.toLong(), sharingStateResponse.totalElements)
        Assertions.assertThat(sharingStateResponse.content).isEqualTo(
            upsertedBusinessPartners.map {
                SharingStateDto(
                    externalId = it.externalId,
                    sharingStateType = SharingStateType.Ready
                )
            }
        )
    }
}