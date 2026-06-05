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

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.test.system.utils.BusinessPartnerShareActions
import org.eclipse.tractusx.bpdm.test.system.utils.ScenarioContext
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.GateAssertRepositoryV7
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.TestDataFactoryGateV7
import tools.jackson.databind.json.JsonMapper

class GoldenRecordContentsInOutputStepDefs(
    private val gateClient: GateClient,
    private val poolClient: PoolApiClient,
    private val shareActions: BusinessPartnerShareActions,
    private val testDataFactoryGate: TestDataFactoryGateV7,
    private val assertRepository: GateAssertRepositoryV7,
    private val jsonMapper: JsonMapper
) : SpringTestRunConfiguration() {

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    private val context: ScenarioContext get() = ScenarioContext.current()!!
    private val scenarioName: String get() = context.scenarioName

    // -------------------------------------------------------------------------
    // Given
    // -------------------------------------------------------------------------

    @Given("record {string} reflects legal entity {string} with master data {string}")
    fun `given record reflects legal entity master data`(recordId: String, legalEntityId: String, masterDataSeed: String) {
        logger.info {
            "[$scenarioName] Given: record '$recordId' reflects legal entity '$legalEntityId' with master data '$masterDataSeed'"
        }
        // Compose the established share -> refine flow so scenarios can start from a record that already
        // reflects the legal entity. The refine step waits for the sharing process to complete, so no
        // separate assertion or wait is needed here.
        `when shares record`(recordId)
        `when refines to legal entity with master data`(recordId, legalEntityId, masterDataSeed)
    }

    @Given("record {string} reflects additional address {string} of legal entity {string} with master data {string}")
    fun `given record reflects additional address master data`(
        recordId: String,
        addressId: String,
        legalEntityId: String,
        masterDataSeed: String
    ) {
        logger.info {
            "[$scenarioName] Given: record '$recordId' reflects additional address '$addressId' of legal entity " +
                "'$legalEntityId' with master data '$masterDataSeed'"
        }
        // Compose the established share -> refine flow so scenarios can start from a record that already
        // reflects the additional address. The refine step waits for the sharing process to complete, so no
        // separate assertion or wait is needed here.
        `when shares record`(recordId)
        `when refines to additional address of legal entity`(recordId, addressId, legalEntityId, masterDataSeed)
    }

    // -------------------------------------------------------------------------
    // When
    // -------------------------------------------------------------------------

    @When("the sharing member shares record {string}")
    fun `when shares record`(recordId: String) {
        logger.info { "[$scenarioName] When: the sharing member shares record '$recordId'" }
        shareActions.upload(recordId, isOwnCompanyData = true)
    }

    @When("the sharing member updates record {string}")
    fun `when updates record`(recordId: String) {
        logger.info { "[$scenarioName] When: the sharing member updates record '$recordId'" }
        // Re-upload under the same externalId (an upsert) but with a different content seed, so the input
        // really changes and we can prove the output reflects the updated share rather than the original.
        shareActions.upload(recordId, isOwnCompanyData = true, contentSeed = "$recordId-updated")
    }

    @When("the golden record process refines record {string} to legal entity {string} with master data {string}")
    fun `when refines to legal entity with master data`(
        recordId: String,
        legalEntityId: String,
        masterDataSeed: String
    ) {
        logger.info {
            "[$scenarioName] When: the golden record process refines record '$recordId' to legal entity " +
                "'$legalEntityId' with master data '$masterDataSeed'"
        }
        // The refinement is the single place that defines the master data. Store (and overwrite on
        // re-refinement) the resulting golden record under its label so the Then can assert by reference
        // and update scenarios automatically expect the latest master data.
        val legalEntity = shareActions.refineAsLegalEntity(recordId, masterDataSeed)
        context.legalEntities[legalEntityId] = legalEntity
    }

    @When("the golden record process refines record {string} to additional address {string} of legal entity {string} with master data {string}")
    fun `when refines to additional address of legal entity`(
        recordId: String,
        addressId: String,
        legalEntityId: String,
        masterDataSeed: String
    ) {
        logger.info {
            "[$scenarioName] When: the golden record process refines record '$recordId' to additional address " +
                "'$addressId' of legal entity '$legalEntityId' with master data '$masterDataSeed'"
        }
        // Store the resulting address (and its parent legal entity) under their labels so the Then can assert
        // by reference and re-refinement automatically updates the expectation.
        val addressWithParent = shareActions.refineAsAdditionalAddressOfLegalEntity(recordId, masterDataSeed)
        context.legalEntities[legalEntityId] = addressWithParent.legalEntity
        context.additionalLegalEntityAddresses[addressId] = addressWithParent
    }

    // -------------------------------------------------------------------------
    // Then
    // -------------------------------------------------------------------------

    @Then("{string} output reflects legal entity {string} in its master data")
    fun `then output reflects master data`(recordId: String, legalEntityId: String) {
        logger.info {
            "[$scenarioName] Then: '$recordId' output reflects legal entity '$legalEntityId' in its master data"
        }
        // Expected master data is the currently defined golden record for this legal entity. Reflecting the
        // legal entity implies reflecting its legal address, reachable as expectedLegalEntity.legalAddress.
        val expectedLegalEntity = context.legalEntities[legalEntityId]
            ?: error("legal entity '$legalEntityId' must be defined by an earlier golden record refinement step")

        val runId = context.runId(recordId)
        val inputResponse = inputResponseOf(recordId)
        val outputFactory = testDataFactoryGate.businessPartner.output
        val expectedOutput = outputFactory.fromLegalEntity(inputResponse, expectedLegalEntity).copy(externalId = runId)

        // (1) The Gate output carries the expected master data inline.
        val actualOutput = assertGateOutputCarriesMasterData(runId, expectedOutput)

        // (2) The output's BPN really references a Pool golden record with the same master data. Reduce the
        // Pool legal entity through the same output factory so we can reuse the master-data comparison.
        val referencedBpnl = actualOutput.legalEntity.legalEntityBpn
        val poolLegalEntity = poolClient.legalEntities.getLegalEntity(referencedBpnl, "BPN")
        attachCall("GET", "/v7/legal-entities/$referencedBpnl", response = poolLegalEntity)
        val poolAsOutput = outputFactory.fromLegalEntity(inputResponse, poolLegalEntity).copy(externalId = runId)
        assertPoolGoldenRecordReflectsMasterData(expectedOutput, poolAsOutput)

        // The output's BPNs must point to this legal entity and its legal address in the Pool.
        assertThat(actualOutput.legalEntity.legalEntityBpn)
            .describedAs("output legal entity BPN must reference legal entity '%s' in the Pool", legalEntityId)
            .isEqualTo(poolLegalEntity.header.bpnl)
        assertThat(actualOutput.address.addressBpn)
            .describedAs("output legal address BPN must reference legal entity '%s's legal address in the Pool", legalEntityId)
            .isEqualTo(poolLegalEntity.legalAddress.bpna)

        // The referenced Pool address must actually be the legal entity's legal address.
        assertThat(poolLegalEntity.legalAddress.addressType)
            .describedAs("Pool legal address of legal entity '%s' must be typed as a legal address", legalEntityId)
            .isEqualTo(AddressType.LegalAddress)
    }

    @Then("{string} output reflects additional address {string} of legal entity {string} in its master data")
    fun `then output reflects additional address master data`(recordId: String, addressId: String, legalEntityId: String) {
        logger.info {
            "[$scenarioName] Then: '$recordId' output reflects additional address '$addressId' of legal entity " +
                "'$legalEntityId' in its master data"
        }
        // Expected master data is the currently defined additional address (and its parent legal entity) for
        // this label, set by the golden record refinement step.
        val expected = context.additionalLegalEntityAddresses[addressId]
            ?: error("additional address '$addressId' must be defined by an earlier golden record refinement step")

        val runId = context.runId(recordId)
        val inputResponse = inputResponseOf(recordId)
        val outputFactory = testDataFactoryGate.businessPartner.output
        val expectedOutput = outputFactory
            .fromAdditionalAddressOnLegalEntity(inputResponse, expected.legalEntity, expected.address)
            .copy(externalId = runId)

        // (1) The Gate output carries the expected master data inline.
        val actualOutput = assertGateOutputCarriesMasterData(runId, expectedOutput)

        // (2) The output's BPNs really reference a Pool golden record with the same master data. Reduce the
        // Pool legal entity and address through the same output factory so we can reuse the comparison.
        val referencedBpnl = actualOutput.legalEntity.legalEntityBpn
        val referencedBpna = actualOutput.address.addressBpn
        val poolLegalEntity = poolClient.legalEntities.getLegalEntity(referencedBpnl, "BPN")
        attachCall("GET", "/v7/legal-entities/$referencedBpnl", response = poolLegalEntity)
        val poolAddress = poolClient.addresses.getAddress(referencedBpna)
        attachCall("GET", "/v7/addresses/$referencedBpna", response = poolAddress)
        val poolAsOutput = outputFactory
            .fromAdditionalAddressOnLegalEntity(inputResponse, poolLegalEntity, poolAddress)
            .copy(externalId = runId)
        assertPoolGoldenRecordReflectsMasterData(expectedOutput, poolAsOutput)

        // The output's BPNs must point to this legal entity and its additional address in the Pool.
        assertThat(actualOutput.legalEntity.legalEntityBpn)
            .describedAs("output legal entity BPN must reference legal entity '%s' in the Pool", legalEntityId)
            .isEqualTo(poolLegalEntity.header.bpnl)
        assertThat(actualOutput.address.addressBpn)
            .describedAs("output address BPN must reference additional address '%s' of legal entity '%s' in the Pool", addressId, legalEntityId)
            .isEqualTo(poolAddress.address.bpna)

        // The referenced Pool address must actually be an additional address.
        assertThat(poolAddress.address.addressType)
            .describedAs("Pool address '%s' of legal entity '%s' must be typed as an additional address", addressId, legalEntityId)
            .isEqualTo(AddressType.AdditionalAddress)
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun inputResponseOf(recordId: String): BusinessPartnerInputDto =
        testDataFactoryGate.businessPartner.input.response.fromRequest(context.records[recordId]!!.currentInput!!)

    /**
     * Asserts the single Gate output for [runId] carries [expectedOutput]'s master data inline and returns
     * that output partner for the follow-up Pool reference checks.
     */
    private fun assertGateOutputCarriesMasterData(runId: String, expectedOutput: BusinessPartnerOutputDto): BusinessPartnerOutputDto {
        val actualOutputPage = gateClient.businessParters.getBusinessPartnersOutput(listOf(runId))
        attachCall("POST", "/v7/output/business-partners/search", request = listOf(runId), response = actualOutputPage)
        assertRepository.assertBusinessPartnerOutput(
            actualOutputPage,
            PageDto(1, 1, 0, 1, listOf(expectedOutput)),
            assertRepository.outputMasterDataComparisonConfig
        )
        return actualOutputPage.content.single()
    }

    /**
     * Asserts the Pool golden record - already reduced to [poolAsOutput] through the same output factory -
     * carries the same master data as [expectedOutput], proving the output reflects the golden record rather
     * than just carrying matching content inline.
     */
    private fun assertPoolGoldenRecordReflectsMasterData(expectedOutput: BusinessPartnerOutputDto, poolAsOutput: BusinessPartnerOutputDto) {
        assertRepository.assertBusinessPartnerOutput(
            listOf(poolAsOutput),
            listOf(expectedOutput),
            assertRepository.outputMasterDataComparisonConfig
        )
    }

    private fun attachCall(method: String, path: String, request: Any? = null, response: Any? = null) {
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
}
