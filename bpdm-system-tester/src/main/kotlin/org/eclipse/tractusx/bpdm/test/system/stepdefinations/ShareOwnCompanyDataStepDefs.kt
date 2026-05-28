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

package org.eclipse.tractusx.bpdm.test.system.stepdefinations

import io.cucumber.datatable.DataTable
import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.java.Scenario
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.response.SharingStateDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteWithMainAddressVerboseDto
import org.eclipse.tractusx.bpdm.test.system.utils.SharingStateWatcher
import org.eclipse.tractusx.bpdm.test.system.utils.ScenarioContext
import org.eclipse.tractusx.bpdm.test.system.utils.SiteBasedLegalEntity
import org.eclipse.tractusx.bpdm.test.system.utils.TaskReservationWatcher
import org.eclipse.tractusx.bpdm.test.testdata.gate.TestRunData
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.GateAssertRepositoryV7
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.TestDataFactoryGateV7
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.RefinementTestDataFactory
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.copyWithBpnReferenceType
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.copyWithBpnRequests
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.copyWithConfidenceCriteria
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.PoolRequestFactoryV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.PoolResponseFactoryV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.TestDataV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.withConfidence
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.withParticipantData
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.BpnReferenceType
import org.eclipse.tractusx.orchestrator.api.model.TaskStep
import org.eclipse.tractusx.orchestrator.api.model.TaskStepResultEntryDto
import org.eclipse.tractusx.orchestrator.api.model.TaskStepResultRequest
import java.time.Instant

class ShareOwnCompanyDataStepDefs(
    private val gateClient: GateClient,
    private val orchestratorClient: OrchestrationApiClient,
    private val sharingStateWatcher: SharingStateWatcher,
    private val taskReservationWatcher: TaskReservationWatcher,
    private val testRunData: TestRunData,
    private val poolRequestFactory: PoolRequestFactoryV7,
    private val poolResponseFactory: PoolResponseFactoryV7,
    private val refinementTestDataFactory: RefinementTestDataFactory,
    private val testDataFactoryGate: TestDataFactoryGateV7,
    private val assertRepository: GateAssertRepositoryV7
) : SpringTestRunConfiguration() {

    companion object {
        private val scenarioHolder = ThreadLocal<Scenario>()
        private val contextHolder  = ThreadLocal<ScenarioContext>()
    }

    private val scenario: Scenario        get() = scenarioHolder.get()
    private val context:  ScenarioContext get() = contextHolder.get()

    @Before
    fun setUp(scenario: Scenario) {
        scenarioHolder.set(scenario)
        contextHolder.set(ScenarioContext())
    }

    @After
    fun tearDown() {
        scenarioHolder.remove()
        contextHolder.remove()
    }

    @Given("site-based legal entity {string}")
    fun `legal entity on site data`(legalEntityDataId: String) {
        val runUniqueId = legalEntityDataId.asRunUniqueId()

        val legalEntity = with(poolRequestFactory.buildLegalEntity(runUniqueId).withParticipantData(true)
            .let{ poolResponseFactory.buildLegalEntityWithLegalAddress(it) }){
            copy(
                header = header.copy(bpnl = "BPNL$runUniqueId"),
                legalAddress = legalAddress.copy(
                    bpna = "BPNAL$runUniqueId",
                    addressType = AddressType.LegalAndSiteMainAddress
                )
            )
        }.withConfidence(TestDataV7.SharedByOwnerConfidence)

        val site = poolRequestFactory.buildSiteCreateRequest(runUniqueId, legalEntity.header.bpnl)
            .let { poolResponseFactory.buildSite(it.site) }
            .copy(
                bpns = "BPNS$runUniqueId",
                isParticipantData = legalEntity.header.isParticipantData
            )

        val siteBasedLegalEntity = SiteBasedLegalEntity(legalEntity, site)
        context.siteLegalEntities[runUniqueId] = siteBasedLegalEntity

        val taskData = refinementTestDataFactory.buildLegalEntityOnSiteBusinessPartner(legalEntity, site, "", emptyList())
            .copyWithBpnReferenceType(BpnReferenceType.BpnRequestIdentifier)
        context.taskData[runUniqueId] = taskData
    }

    @Given("input data {string}")
    fun `input data`(inputDataId: String) {
        val runUniqueId = inputDataId.asRunUniqueId()

        val inputData = testDataFactoryGate.businessPartner.input.request.fromSeed(runUniqueId)

        context.inputData[runUniqueId] = inputData
    }

    @Given("output data {string} based on input {string} for site-based legal entity {string}")
    fun `output data for legal entity on site based on`(outputDataId: String, inputDataId: String, legalEntityDataId: String) {
        val outputRunUniqueId  = outputDataId.asRunUniqueId()
        val inputRunUniqueId = inputDataId.asRunUniqueId()
        val legalEntityRunUniqueId = legalEntityDataId.asRunUniqueId()

        val siteBasedLegalEntity = context.siteLegalEntities[legalEntityRunUniqueId]!!
        val inputData = context.inputData[inputRunUniqueId]!!

        val input = testDataFactoryGate.businessPartner.input.response.fromRequest(inputData)
        val siteWithMainAddress = SiteWithMainAddressVerboseDto(siteBasedLegalEntity.site, siteBasedLegalEntity.legalEntity.legalAddress)

        val outputData = testDataFactoryGate.businessPartner.output.fromLegalEntityOnSite(input,siteBasedLegalEntity.legalEntity, siteWithMainAddress)
        context.outputData[outputRunUniqueId] = outputData

    }

    @When("uploading into business partner record {string} input data {string}")
    fun `uploading into business partner record input data`(recordId: String, inputDataId: String) {
        val recordRunUniqueId = recordId.asRunUniqueId()
        val inputRunUniqueId = inputDataId.asRunUniqueId()

        val inputData = context.inputData[inputRunUniqueId]!!

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(inputData.copy(externalId = recordRunUniqueId)))
    }

    @When("record {string} is refined to {string}")
    fun `record is refined to`(recordId: String, taskDataId: String) {
        val recordRunUniqueId = recordId.asRunUniqueId()
        val taskRunUniqueId = taskDataId.asRunUniqueId()

        val taskData = context.taskData[taskRunUniqueId]!!

        sharingStateWatcher.waitForPendingState(recordRunUniqueId)

        val taskId = gateClient.sharingState.getSharingStates(PaginationRequest(),listOf(recordRunUniqueId)).content.single().taskId!!
        taskReservationWatcher.waitForReservedTask(taskId)

        orchestratorClient.goldenRecordTasks.resolveStepResults(TaskStepResultRequest(TaskStep.CleanAndSync, listOf(TaskStepResultEntryDto(taskId, taskData))))
    }

    @Then("polling business partner record {string} sharing state leads to success")
    fun `polling business partner record sharing state leads to success`(recordId: String) {
        val recordRunUniqueId = recordId.asRunUniqueId()

        sharingStateWatcher.waitForCompletedState(recordRunUniqueId)

        val sharingStates = gateClient.sharingState.getSharingStates(PaginationRequest(),listOf(recordRunUniqueId)).content

        val expectedSharingStates = listOf(SharingStateDto(recordRunUniqueId, SharingStateType.Success, updatedAt = Instant.now()))
        assertRepository.assertSharingStates(sharingStates, expectedSharingStates)
    }

    @Then("business partner record {string} output data matches {string}")
    fun `business partner record output data matches`(recordId: String, outputDataId: String) {
        val recordRunUniqueId = recordId.asRunUniqueId()
        val outputRunUniqueId  = outputDataId.asRunUniqueId()

        val expectedOutputData = context.outputData[outputRunUniqueId]!!.copy(externalId = recordRunUniqueId)

        val output = gateClient.businessParters.getBusinessPartnersOutput(listOf(recordRunUniqueId))

        assertRepository.assertBusinessPartnerOutput(output, PageDto(1, 1, 0, 1, listOf(expectedOutputData)))
    }

    private fun String.asScenarioUniqueId(): String{
        return "$this${scenario.id}"
    }

    private fun String.asRunUniqueId(): String{
        return "${this.asScenarioUniqueId()}${testRunData.testTime}"
    }
}
