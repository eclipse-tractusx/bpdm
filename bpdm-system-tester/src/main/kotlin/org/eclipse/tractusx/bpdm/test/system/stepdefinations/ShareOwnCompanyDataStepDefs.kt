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
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import mu.KotlinLogging
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
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.GateAssertRepositoryV7
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.TestDataFactoryGateV7
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.TaskStep
import org.eclipse.tractusx.orchestrator.api.model.TaskStepResultEntryDto
import org.eclipse.tractusx.orchestrator.api.model.TaskStepResultRequest
import tools.jackson.databind.json.JsonMapper
import java.time.Instant

class ShareOwnCompanyDataStepDefs(
    private val gateClient: GateClient,
    private val orchestratorClient: OrchestrationApiClient,
    private val sharingStateWatcher: SharingStateWatcher,
    private val taskReservationWatcher: TaskReservationWatcher,
    private val testDataGenerator: ShareOwnCompanyDataTestDataGenerator,
    private val testDataFactoryGate: TestDataFactoryGateV7,
    private val assertRepository: GateAssertRepositoryV7,
    private val jsonMapper: JsonMapper
) : SpringTestRunConfiguration() {

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    private val context: ScenarioContext get() = ScenarioContext.current()!!
    private val scenarioName: String get() = context.scenarioName

    private fun attachGateCall(method: String, path: String, request: Any? = null, response: Any? = null) {
        val content = buildMap {
            put("uri", "$method $path")
            if (request != null) put("request", request)
            if (response != null) put("response", response)
        }
        context.scenario.attach(
            jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(content),
            "application/json",
            "$method $path"
        )
    }

    @Given("site-based legal entity {string}")
    fun `given site-based legal entity`(legalEntityDataId: String) {
        logger.info { "[$scenarioName] Given: site-based legal entity '$legalEntityDataId'" }
        val result = testDataGenerator.buildSiteBasedLegalEntity(legalEntityDataId)
        context.siteLegalEntities[legalEntityDataId] = result.siteBasedLegalEntity
        context.taskData[legalEntityDataId] = result.taskData
    }

    @Given("legal entity {string}")
    fun `given legal entity`(legalEntityDataId: String) {
        logger.info { "[$scenarioName] Given: legal entity '$legalEntityDataId'" }
        val result = testDataGenerator.buildLegalEntity(legalEntityDataId)
        context.legalEntities[legalEntityDataId] = result.legalEntity
        context.taskData[legalEntityDataId] = result.taskData
    }

    @Given("site {string} of legal entity {string}")
    fun `given site`(siteDataId: String, legalEntityDataId: String) {
        logger.info { "[$scenarioName] Given: site '$siteDataId' of legal entity '$legalEntityDataId'" }
        val legalEntity = context.legalEntities[legalEntityDataId]!!
        val result = testDataGenerator.buildSite(siteDataId, legalEntity)
        context.sites[siteDataId] = result.siteWithParent
        context.taskData[siteDataId] = result.taskData
    }

    @Given("input data {string}")
    fun `given input data`(inputDataId: String, dataTable: DataTable) {
        logger.info { "[$scenarioName] Given: input data '$inputDataId'" }
        val overrides = dataTable.asMap()
        var inputData = testDataGenerator.buildInputData(inputDataId)
        overrides["isOwnCompanyData"]?.let { inputData = inputData.copy(isOwnCompanyData = it.toBoolean()) }
        context.inputData[inputDataId] = inputData
    }

    @Given("output data {string} based on input {string} for site-based legal entity {string}")
    fun `given output data for legal entity on site based on`(outputDataId: String, inputDataId: String, legalEntityDataId: String) {
        logger.info { "[$scenarioName] Given: output data '$outputDataId' based on input '$inputDataId' for site-based legal entity '$legalEntityDataId'" }
        val siteBasedLegalEntity = context.siteLegalEntities[legalEntityDataId]!!
        val inputData = context.inputData[inputDataId]!!
        val input = testDataFactoryGate.businessPartner.input.response.fromRequest(inputData)
        val siteWithMainAddress = SiteWithMainAddressVerboseDto(siteBasedLegalEntity.site, siteBasedLegalEntity.legalEntity.legalAddress)
        context.outputData[outputDataId] = testDataFactoryGate.businessPartner.output.fromLegalEntityOnSite(input, siteBasedLegalEntity.legalEntity, siteWithMainAddress)
    }

    @Given("output data {string} based on input {string} for legal entity {string}")
    fun `given output data for legal entity based on`(outputDataId: String, inputDataId: String, legalEntityDataId: String) {
        logger.info { "[$scenarioName] Given: output data '$outputDataId' based on input '$inputDataId' for legal entity '$legalEntityDataId'" }
        val legalEntity = context.legalEntities[legalEntityDataId]!!
        val inputData = context.inputData[inputDataId]!!
        val input = testDataFactoryGate.businessPartner.input.response.fromRequest(inputData)
        context.outputData[outputDataId] = testDataFactoryGate.businessPartner.output.fromLegalEntity(input, legalEntity)
    }

    @Given("output data {string} based on input {string} for site {string}")
    fun `given output data based on input for site`(outputDataId: String, inputDataId: String, siteDataId: String) {
        logger.info { "[$scenarioName] Given: output data '$outputDataId' based on input '$inputDataId' for site '$siteDataId'" }
        val siteWithParent = context.sites[siteDataId]!!
        val inputData = context.inputData[inputDataId]!!
        val input = testDataFactoryGate.businessPartner.input.response.fromRequest(inputData)
        context.outputData[outputDataId] = testDataFactoryGate.businessPartner.output.fromSite(input, siteWithParent.legalEntity, siteWithParent.site)
    }

    @Given("additional address {string} of site {string}")
    fun `given additional address of site`(addressDataId: String, siteDataId: String) {
        logger.info { "[$scenarioName] Given: additional address '$addressDataId' of site '$siteDataId'" }
        val siteWithParent = context.sites[siteDataId]!!
        val result = testDataGenerator.buildAdditionalSiteAddress(addressDataId, siteWithParent)
        context.additionalSiteAddresses[addressDataId] = result.additionalSiteAddressWithParent
        context.taskData[addressDataId] = result.taskData
    }

    @Given("output data {string} based on input {string} for additional address {string} of site")
    fun `given output data based on input for additional address of site`(outputDataId: String, inputDataId: String, addressDataId: String) {
        logger.info { "[$scenarioName] Given: output data '$outputDataId' based on input '$inputDataId' for additional address '$addressDataId' of site" }
        val additionalAddressWithParent = context.additionalSiteAddresses[addressDataId]!!
        val inputData = context.inputData[inputDataId]!!
        val input = testDataFactoryGate.businessPartner.input.response.fromRequest(inputData)
        context.outputData[outputDataId] = testDataFactoryGate.businessPartner.output.fromAdditionalAddressOnSite(
            input,
            additionalAddressWithParent.siteWithParent.legalEntity,
            additionalAddressWithParent.siteWithParent.site,
            additionalAddressWithParent.address
        )
    }

    @Given("additional address {string} of legal entity {string}")
    fun `given additional address of legal entity`(addressDataId: String, legalEntityDataId: String) {
        logger.info { "[$scenarioName] Given: additional address '$addressDataId' of legal entity '$legalEntityDataId'" }
        val legalEntity = context.legalEntities[legalEntityDataId]!!
        val result = testDataGenerator.buildAdditionalLegalEntityAddress(addressDataId, legalEntity)
        context.additionalLegalEntityAddresses[addressDataId] = result.additionalLegalEntityAddressWithParent
        context.taskData[addressDataId] = result.taskData
    }

    @Given("output data {string} based on input {string} for additional address {string} of legal entity")
    fun `given output data based on input for additional address of legal entity`(outputDataId: String, inputDataId: String, addressDataId: String) {
        logger.info { "[$scenarioName] Given: output data '$outputDataId' based on input '$inputDataId' for additional address '$addressDataId' of legal entity" }
        val additionalAddressWithParent = context.additionalLegalEntityAddresses[addressDataId]!!
        val inputData = context.inputData[inputDataId]!!
        val input = testDataFactoryGate.businessPartner.input.response.fromRequest(inputData)
        context.outputData[outputDataId] = testDataFactoryGate.businessPartner.output.fromAdditionalAddressOnLegalEntity(
            input,
            additionalAddressWithParent.legalEntity,
            additionalAddressWithParent.address
        )
    }

    @When("uploading into business partner record {string} input data {string}")
    fun `when uploading into business partner record input data`(recordId: String, inputDataId: String) {
        logger.info { "[$scenarioName] When: uploading into business partner record '$recordId' input data '$inputDataId'" }
        val inputData = context.inputData[inputDataId]!!
        val request = listOf(inputData.copy(externalId = context.runId(recordId)))
        val response = gateClient.businessParters.upsertBusinessPartnersInput(request)
        attachGateCall("PUT", "/v7/input/business-partners", request = request, response = response.body)
    }

    @When("record {string} is refined to {string}")
    fun `when record is refined to`(recordId: String, taskDataId: String) {
        logger.info { "[$scenarioName] When: record '$recordId' is refined to '$taskDataId'" }
        val taskData = context.taskData[taskDataId]!!
        val recordRunId = context.runId(recordId)

        sharingStateWatcher.waitForTaskId(recordId)

        val sharingStatesPage = gateClient.sharingState.getSharingStates(PaginationRequest(), listOf(recordRunId))
        attachGateCall("GET", "/v7/business-partners/sharing-state", request = mapOf("externalIds" to listOf(recordRunId)), response = sharingStatesPage)
        val taskId = sharingStatesPage.content.single().taskId!!
        taskReservationWatcher.waitForReservedTask(taskId)

        orchestratorClient.goldenRecordTasks.resolveStepResults(TaskStepResultRequest(TaskStep.CleanAndSync, listOf(TaskStepResultEntryDto(taskId, taskData))))
    }

    @Then("polling business partner record {string} sharing state leads to success")
    fun `then polling business partner record sharing state leads to success`(recordId: String) {
        logger.info { "[$scenarioName] Then: polling business partner record '$recordId' sharing state leads to success" }
        val recordRunId = context.runId(recordId)

        sharingStateWatcher.waitForCompletedState(recordId)

        val sharingStatesPage = gateClient.sharingState.getSharingStates(PaginationRequest(), listOf(recordRunId))
        attachGateCall("GET", "/v7/business-partners/sharing-state", request = mapOf("externalIds" to listOf(recordRunId)), response = sharingStatesPage)
        val sharingStates = sharingStatesPage.content
        val expectedSharingStates = listOf(SharingStateDto(recordRunId, SharingStateType.Success, updatedAt = Instant.now()))
        assertRepository.assertSharingStates(sharingStates, expectedSharingStates)
    }

    @Then("business partner record {string} output data matches {string}")
    fun `then business partner record output data matches`(recordId: String, outputDataId: String) {
        logger.info { "[$scenarioName] Then: business partner record '$recordId' output data matches '$outputDataId'" }
        val recordRunId = context.runId(recordId)
        val expectedOutputData = context.outputData[outputDataId]!!.copy(externalId = recordRunId)

        val externalIds = listOf(recordRunId)
        val output = gateClient.businessParters.getBusinessPartnersOutput(externalIds)
        attachGateCall("POST", "/v7/output/business-partners/search", request = externalIds, response = output)
        assertRepository.assertBusinessPartnerOutput(output, PageDto(1, 1, 0, 1, listOf(expectedOutputData)), assertRepository.outputComparisonConfigNoBpn)
    }
}
