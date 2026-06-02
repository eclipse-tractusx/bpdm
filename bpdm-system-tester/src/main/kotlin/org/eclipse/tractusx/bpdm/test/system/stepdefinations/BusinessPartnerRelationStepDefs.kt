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

import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.java.BeforeStep
import io.cucumber.java.Scenario
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import mu.KotlinLogging
import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.gate.api.model.RelationOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.SharableRelationType
import org.eclipse.tractusx.bpdm.gate.api.model.request.ChangelogSearchRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationOutputSearchRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutRequest
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.RelationValidityPeriod
import org.eclipse.tractusx.bpdm.pool.api.model.RelationVerboseDto
import org.eclipse.tractusx.bpdm.test.system.utils.BusinessPartnerRelationTestDataGenerator
import org.eclipse.tractusx.bpdm.test.system.utils.ScenarioContext
import org.eclipse.tractusx.bpdm.test.system.utils.StepUtils
import org.eclipse.tractusx.bpdm.test.system.utils.TestRepository
import org.eclipse.tractusx.bpdm.test.testdata.gate.GateInputFactory
import org.eclipse.tractusx.bpdm.test.testdata.gate.TestRunData
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.TestDataFactoryGateV7
import org.eclipse.tractusx.bpdm.test.testdata.gate.withAddressType
import org.eclipse.tractusx.bpdm.test.testdata.gate.withoutAnyBpn
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepResultEntryDto
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepResultRequest
import org.eclipse.tractusx.orchestrator.api.model.TaskStep
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType as GateRelationType
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityRelationType as PoolRelationType


class BusinessPartnerRelationStepDefs(
    private val testRunData: TestRunData,
    private val gateClient: GateClient,
    private val orchestratorClient: OrchestrationApiClient,
    private val testDataFactoryGate: TestDataFactoryGateV7,
    private val testDataGenerator: BusinessPartnerRelationTestDataGenerator
): SpringTestRunConfiguration() {

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    private val context: ScenarioContext get() = ScenarioContext.current()!!
    private val scenarioName: String get() = context.scenarioName

    @Before
    fun setUp(scenario: Scenario) {
        ScenarioContext.set(ScenarioContext(scenario.name, scenario.id, testRunData.testTime, scenario))
        logger.info { "Starting scenario: '${scenario.name}'" }
    }

    @After
    fun tearDown() {
        ScenarioContext.clear()
    }

    @Given("relation input data {string} of type {string} from {string} to {string}")
    fun `given relation input data`(relationInputDataId: String, relationType: String, fromRecordId: String, toRecordId: String) {
        logger.info { "[$scenarioName] Given: relation input data '$relationInputDataId' of type '$relationType' from '$fromRecordId' to '$toRecordId'" }
        val result = testDataGenerator.buildRelationInputData(relationInputDataId, relationType, fromRecordId, toRecordId)
        context.relationInputData[relationInputDataId] = result.relationInputEntry
    }

    @Given("relation output data {string} based on input {string}")
    fun `given relation output data`(relationOutputDataId: String, relationInputDataId: String) {
        logger.info { "[$scenarioName] Given: relation output data '$relationOutputDataId' based on input '$relationInputDataId'" }
        TODO()
    }

    @When("uploading into relation record {string} input data {string}")
    fun `when uploading into relation record input data`(recordId: String, inputDataId: String) {
        logger.info { "[$scenarioName] When: uploading into relation record '$recordId' input data '$inputDataId'" }
        TODO()
    }

    @When("relation record {string} is refined to {string}")
    fun `when relation record is refined to`(recordId: String, relationOutputDataId: String) {
        logger.info { "[$scenarioName] When: relation record '$recordId' is refined to '$relationOutputDataId'" }
        TODO()
    }

    @Then("polling relation record {string} sharing state leads to success")
    fun `then polling relation record sharing state leads to success`(recordId: String) {
        logger.info { "[$scenarioName] Then: polling relation record '$recordId' sharing state leads to success" }
        TODO()
    }

    @Then("relation record {string} output data matches {string}")
    fun `then relation record output data matches`(recordId: String, outputDataId: String) {
        logger.info { "[$scenarioName] Then: relation record '$recordId' output data matches '$outputDataId'" }
        TODO()
    }

}