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
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.response.SharingStateDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteWithMainAddressVerboseDto
import org.eclipse.tractusx.bpdm.test.system.utils.ScenarioContext
import org.eclipse.tractusx.bpdm.test.system.utils.ShareOwnCompanyDataTestDataGenerator
import org.eclipse.tractusx.bpdm.test.system.utils.SharingStateWatcher
import org.eclipse.tractusx.bpdm.test.system.utils.TaskReservationWatcher
import org.eclipse.tractusx.bpdm.test.testdata.gate.TestRunData
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.GateAssertRepositoryV7
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.TestDataFactoryGateV7
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
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
    private val testDataGenerator: ShareOwnCompanyDataTestDataGenerator,
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
    fun `given site-based legal entity`(legalEntityDataId: String) {
        val scenarioUniqueId = legalEntityDataId.asScenarioUniqueId()
        val runUniqueId = legalEntityDataId.asRunUniqueId()

        val result = testDataGenerator.buildSiteBasedLegalEntity(scenarioUniqueId, runUniqueId)
        context.siteLegalEntities[runUniqueId] = result.siteBasedLegalEntity
        context.taskData[runUniqueId] = result.taskData
    }

    @Given("legal entity {string}")
    fun `given legal entity`(legalEntityDataId: String) {
        val scenarioUniqueId = legalEntityDataId.asScenarioUniqueId()
        val runUniqueId = legalEntityDataId.asRunUniqueId()

        val result = testDataGenerator.buildLegalEntity(scenarioUniqueId, runUniqueId)
        context.legalEntities[runUniqueId] = result.legalEntity
        context.taskData[runUniqueId] = result.taskData
    }

    @Given("site {string} of legal entity {string}")
    fun `given site`(siteDataId: String, legalEntityDataId: String) {
        val runUniqueLegalEntityId = legalEntityDataId.asRunUniqueId()
        val scenarioUniqueSiteId = siteDataId.asScenarioUniqueId()
        val runUniqueSiteId = siteDataId.asRunUniqueId()

        val legalEntity = context.legalEntities[runUniqueLegalEntityId]!!

        val result = testDataGenerator.buildSite(scenarioUniqueSiteId, runUniqueSiteId, legalEntity)
        context.sites[runUniqueSiteId] = result.siteWithParent
        context.taskData[runUniqueSiteId] = result.taskData
    }

    @Given("input data {string}")
    fun `given input data`(inputDataId: String, dataTable: DataTable) {
        val scenarioUniqueId = inputDataId.asScenarioUniqueId()
        val runUniqueId = inputDataId.asRunUniqueId()
        val overrides = dataTable.asMap()

        var inputData = testDataGenerator.buildInputData(scenarioUniqueId, runUniqueId)
        overrides["isOwnCompanyData"]?.let { inputData = inputData.copy(isOwnCompanyData = it.toBoolean()) }

        context.inputData[runUniqueId] = inputData
    }

    @Given("output data {string} based on input {string} for site-based legal entity {string}")
    fun `given output data for legal entity on site based on`(outputDataId: String, inputDataId: String, legalEntityDataId: String) {
        val outputRunUniqueId  = outputDataId.asRunUniqueId()
        val inputRunUniqueId = inputDataId.asRunUniqueId()
        val legalEntityRunUniqueId = legalEntityDataId.asRunUniqueId()

        val siteBasedLegalEntity = context.siteLegalEntities[legalEntityRunUniqueId]!!
        val inputData = context.inputData[inputRunUniqueId]!!

        val input = testDataFactoryGate.businessPartner.input.response.fromRequest(inputData)
        val siteWithMainAddress = SiteWithMainAddressVerboseDto(siteBasedLegalEntity.site, siteBasedLegalEntity.legalEntity.legalAddress)

        val outputData = testDataFactoryGate.businessPartner.output.fromLegalEntityOnSite(input, siteBasedLegalEntity.legalEntity, siteWithMainAddress)
        context.outputData[outputRunUniqueId] = outputData
    }

    @Given("output data {string} based on input {string} for legal entity {string}")
    fun `given output data for legal entity based on`(outputDataId: String, inputDataId: String, legalEntityDataId: String) {
        val outputRunUniqueId  = outputDataId.asRunUniqueId()
        val inputRunUniqueId = inputDataId.asRunUniqueId()
        val legalEntityRunUniqueId = legalEntityDataId.asRunUniqueId()

        val legalEntity = context.legalEntities[legalEntityRunUniqueId]!!
        val inputData = context.inputData[inputRunUniqueId]!!

        val input = testDataFactoryGate.businessPartner.input.response.fromRequest(inputData)

        val outputData = testDataFactoryGate.businessPartner.output.fromLegalEntity(input, legalEntity)
        context.outputData[outputRunUniqueId] = outputData
    }

    @Given("output data {string} based on input {string} for site {string}")
    fun `given output data based on input for site`(outputDataId: String, inputDataId: String, siteDataId: String) {
        val outputRunUniqueId  = outputDataId.asRunUniqueId()
        val inputRunUniqueId = inputDataId.asRunUniqueId()
        val siteRunUniqueId = siteDataId.asRunUniqueId()

        val siteWithParent = context.sites[siteRunUniqueId]!!
        val inputData = context.inputData[inputRunUniqueId]!!

        val input = testDataFactoryGate.businessPartner.input.response.fromRequest(inputData)

        val outputData = testDataFactoryGate.businessPartner.output.fromSite(input, siteWithParent.legalEntity, siteWithParent.site)
        context.outputData[outputRunUniqueId] = outputData
    }

    @Given("additional address {string} of site {string}")
    fun `given additional address`(addressDataId: String, siteDataId: String) {
        val scenarioUniqueAddressId = addressDataId.asScenarioUniqueId()
        val runUniqueAddressId = addressDataId.asRunUniqueId()
        val runUniqueSiteId = siteDataId.asRunUniqueId()

        val siteWithParent = context.sites[runUniqueSiteId]!!

        val result = testDataGenerator.buildAdditionalSiteAddress(scenarioUniqueAddressId, runUniqueAddressId, siteWithParent)
        context.additionalSiteAddresses[runUniqueAddressId] = result.additionalSiteAddressWithParent
        context.taskData[runUniqueSiteId] = result.taskData
    }

    @Given("output data {string} based on input {string} for additional address {string} of site")
    fun `given output data based on input for additional address of site`(outputDataId: String, inputDataId: String, addressDataId: String) {
        val outputRunUniqueId = outputDataId.asRunUniqueId()
        val inputRunUniqueId = inputDataId.asRunUniqueId()
        val addressRunUniqueId = addressDataId.asRunUniqueId()

        val additionalAddressWithParent = context.additionalSiteAddresses[addressRunUniqueId]!!
        val inputData = context.inputData[inputRunUniqueId]!!

        val input = testDataFactoryGate.businessPartner.input.response.fromRequest(inputData)

        val outputData = testDataFactoryGate.businessPartner.output.fromAdditionalAddressOnSite(
            input,
            additionalAddressWithParent.siteWithParent.legalEntity,
            additionalAddressWithParent.siteWithParent.site,
            additionalAddressWithParent.address
        )
        context.outputData[outputRunUniqueId] = outputData
    }

    @Given("additional address {string} of legal entity {string}")
    fun `given additional address of legal entity`(addressDataId: String, legalEntityDataId: String) {
        val scenarioUniqueAddressId = addressDataId.asScenarioUniqueId()
        val runUniqueAddressId = addressDataId.asRunUniqueId()
        val runUniqueLegalEntityId = legalEntityDataId.asRunUniqueId()

        val legalEntity = context.legalEntities[runUniqueLegalEntityId]!!

        val result = testDataGenerator.buildAdditionalLegalEntityAddress(scenarioUniqueAddressId, runUniqueAddressId, legalEntity)
        context.additionalLegalEntityAddresses[runUniqueAddressId] = result.additionalLegalEntityAddressWithParent
        context.taskData[runUniqueLegalEntityId] = result.taskData
    }

    @Given("output data {string} based on input {string} for additional address {string} of legal entity")
    fun `given output data based on input for additional address of legal entity`(outputDataId: String, inputDataId: String, addressDataId: String) {
        val outputRunUniqueId = outputDataId.asRunUniqueId()
        val inputRunUniqueId = inputDataId.asRunUniqueId()
        val addressRunUniqueId = addressDataId.asRunUniqueId()

        val additionalAddressWithParent = context.additionalLegalEntityAddresses[addressRunUniqueId]!!
        val inputData = context.inputData[inputRunUniqueId]!!

        val input = testDataFactoryGate.businessPartner.input.response.fromRequest(inputData)

        val outputData = testDataFactoryGate.businessPartner.output.fromAdditionalAddressOnLegalEntity(
            input,
            additionalAddressWithParent.legalEntity,
            additionalAddressWithParent.address
        )
        context.outputData[outputRunUniqueId] = outputData
    }

    @When("uploading into business partner record {string} input data {string}")
    fun `when uploading into business partner record input data`(recordId: String, inputDataId: String) {
        val recordRunUniqueId = recordId.asRunUniqueId()
        val inputRunUniqueId = inputDataId.asRunUniqueId()

        val inputData = context.inputData[inputRunUniqueId]!!

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(inputData.copy(externalId = recordRunUniqueId)))
    }

    @When("record {string} is refined to {string}")
    fun `when record is refined to`(recordId: String, taskDataId: String) {
        val recordRunUniqueId = recordId.asRunUniqueId()
        val taskRunUniqueId = taskDataId.asRunUniqueId()

        val taskData = context.taskData[taskRunUniqueId]!!

        sharingStateWatcher.waitForPendingState(recordRunUniqueId)

        val taskId = gateClient.sharingState.getSharingStates(PaginationRequest(),listOf(recordRunUniqueId)).content.single().taskId!!
        taskReservationWatcher.waitForReservedTask(taskId)

        orchestratorClient.goldenRecordTasks.resolveStepResults(TaskStepResultRequest(TaskStep.CleanAndSync, listOf(TaskStepResultEntryDto(taskId, taskData))))
    }

    @Then("polling business partner record {string} sharing state leads to success")
    fun `then polling business partner record sharing state leads to success`(recordId: String) {
        val recordRunUniqueId = recordId.asRunUniqueId()

        sharingStateWatcher.waitForCompletedState(recordRunUniqueId)

        val sharingStates = gateClient.sharingState.getSharingStates(PaginationRequest(),listOf(recordRunUniqueId)).content

        val expectedSharingStates = listOf(SharingStateDto(recordRunUniqueId, SharingStateType.Success, updatedAt = Instant.now()))
        assertRepository.assertSharingStates(sharingStates, expectedSharingStates)
    }

    @Then("business partner record {string} output data matches {string}")
    fun `then business partner record output data matches`(recordId: String, outputDataId: String) {
        val recordRunUniqueId = recordId.asRunUniqueId()
        val outputRunUniqueId  = outputDataId.asRunUniqueId()

        val expectedOutputData = context.outputData[outputRunUniqueId]!!.copy(externalId = recordRunUniqueId)

        val output = gateClient.businessParters.getBusinessPartnersOutput(listOf(recordRunUniqueId))

        assertRepository.assertBusinessPartnerOutput(output, PageDto(1, 1, 0, 1, listOf(expectedOutputData)), assertRepository.outputComparisonConfigNoBpn)
    }

    private fun String.asScenarioUniqueId(): String {
        return "$this${scenario.id}"
    }

    private fun String.asRunUniqueId(): String {
        return "${this.asScenarioUniqueId()}${testRunData.testTime}"
    }
}
