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

/**
 * Steps for the "Output Reflects Golden Record Script Variants" feature.
 *
 * A sharing member may attach script variants to a record; the golden record process refines them onto the
 * matched entity and its parents, and the record's output must then reflect the MERGED script variants of all
 * the golden records it reflects, keyed by script code.
 *
 * The steps are worded around script variants on purpose. The underlying refine utilities
 * ([BusinessPartnerShareActions]) generate master data and script variants together, but master data is
 * incidental here, so the {string} "script variant" argument is just the seed that determines the generated
 * variant content (a different seed yields different variants, which is what the update scenarios rely on).
 *
 * Each refine step routes through the seed-and-label refine overloads, which build the golden record with
 * stable request identifiers and wait for the sharing process to complete, so the output golden record is
 * ready for the Then assertions. Every record owns its golden records, so labels and seeds are derived from
 * the step arguments. The merge scenario is the exception: there the legal entity and one of its additional
 * addresses get explicit, distinct script codes so the merged output provably contains both.
 */
class ScriptVariantStepDefs(
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
    // Given - start from a record that already reflects an entity with script variants
    // (compose the share -> refine flow; the refine waits for completion internally)
    // -------------------------------------------------------------------------

    @Given("record {string} reflects legal entity {string} with script variant {string}")
    fun `given reflects legal entity`(recordId: String, legalEntityId: String, scriptVariantSeed: String) {
        logger.info { "[$scenarioName] Given: record '$recordId' reflects legal entity '$legalEntityId' with script variant '$scriptVariantSeed'" }
        share(recordId)
        `when refines to legal entity`(recordId, legalEntityId, scriptVariantSeed)
    }

    @Given("record {string} reflects site-based legal entity {string} with site {string} with script variant {string}")
    fun `given reflects site-based legal entity`(recordId: String, legalEntityId: String, siteId: String, scriptVariantSeed: String) {
        logger.info { "[$scenarioName] Given: record '$recordId' reflects site-based legal entity '$legalEntityId' with site '$siteId' with script variant '$scriptVariantSeed'" }
        share(recordId)
        `when refines to site-based legal entity`(recordId, legalEntityId, siteId, scriptVariantSeed)
    }

    @Given("record {string} reflects site {string} of legal entity {string} with script variant {string}")
    fun `given reflects site`(recordId: String, siteId: String, legalEntityId: String, scriptVariantSeed: String) {
        logger.info { "[$scenarioName] Given: record '$recordId' reflects site '$siteId' of legal entity '$legalEntityId' with script variant '$scriptVariantSeed'" }
        share(recordId)
        `when refines to site`(recordId, siteId, legalEntityId, scriptVariantSeed)
    }

    @Given("record {string} reflects additional address {string} of legal entity {string} with script variant {string}")
    fun `given reflects additional address of legal entity`(recordId: String, addressId: String, legalEntityId: String, scriptVariantSeed: String) {
        logger.info { "[$scenarioName] Given: record '$recordId' reflects additional address '$addressId' of legal entity '$legalEntityId' with script variant '$scriptVariantSeed'" }
        share(recordId)
        `when refines to additional address of legal entity`(recordId, addressId, legalEntityId, scriptVariantSeed)
    }

    @Given("record {string} reflects additional address {string} of site {string} of legal entity {string} with script variant {string}")
    fun `given reflects additional address of site`(recordId: String, addressId: String, siteId: String, legalEntityId: String, scriptVariantSeed: String) {
        logger.info { "[$scenarioName] Given: record '$recordId' reflects additional address '$addressId' of site '$siteId' of legal entity '$legalEntityId' with script variant '$scriptVariantSeed'" }
        share(recordId)
        `when refines to additional address of site`(recordId, addressId, siteId, legalEntityId, scriptVariantSeed)
    }

    @Given("record {string} reflects legal entity {string} with script code {string}")
    fun `given reflects legal entity with script code`(recordId: String, legalEntityId: String, scriptCode: String) {
        logger.info { "[$scenarioName] Given: record '$recordId' reflects legal entity '$legalEntityId' with script code '$scriptCode'" }
        share(recordId)
        `when refines to legal entity with script code`(recordId, legalEntityId, scriptCode)
    }

    // -------------------------------------------------------------------------
    // When - refine
    //
    // The share / update steps are the generic "the sharing member shares record {string}" /
    // "the sharing member updates record {string}" steps reused from the master-data step definitions; the
    // Given helpers above share via [share] directly so they can compose share -> refine in one step.
    // -------------------------------------------------------------------------

    @When("the golden record process refines record {string} to legal entity {string} with script variant {string}")
    fun `when refines to legal entity`(recordId: String, legalEntityId: String, scriptVariantSeed: String) {
        logger.info { "[$scenarioName] When: refines '$recordId' to legal entity '$legalEntityId' with script variant '$scriptVariantSeed'" }
        val legalEntity = shareActions.refineAsLegalEntity(recordId, masterDataSeed = scriptVariantSeed, legalEntityLabel = legalEntityId)
        context.legalEntities[legalEntityId] = legalEntity
    }

    @When("the golden record process refines record {string} to site-based legal entity {string} with site {string} with script variant {string}")
    fun `when refines to site-based legal entity`(recordId: String, legalEntityId: String, siteId: String, scriptVariantSeed: String) {
        logger.info { "[$scenarioName] When: refines '$recordId' to site-based legal entity '$legalEntityId' with site '$siteId' with script variant '$scriptVariantSeed'" }
        shareActions.refineAsSiteBasedLegalEntity(recordId, masterDataSeed = scriptVariantSeed, siteLabel = siteId, legalEntityLabel = legalEntityId)
    }

    @When("the golden record process refines record {string} to site {string} of legal entity {string} with script variant {string}")
    fun `when refines to site`(recordId: String, siteId: String, legalEntityId: String, scriptVariantSeed: String) {
        logger.info { "[$scenarioName] When: refines '$recordId' to site '$siteId' of legal entity '$legalEntityId' with script variant '$scriptVariantSeed'" }
        shareActions.refineAsSite(recordId, masterDataSeed = scriptVariantSeed, siteLabel = siteId, legalEntityLabel = legalEntityId)
    }

    @When("the golden record process refines record {string} to additional address {string} of legal entity {string} with script variant {string}")
    fun `when refines to additional address of legal entity`(recordId: String, addressId: String, legalEntityId: String, scriptVariantSeed: String) {
        logger.info { "[$scenarioName] When: refines '$recordId' to additional address '$addressId' of legal entity '$legalEntityId' with script variant '$scriptVariantSeed'" }
        shareActions.refineAsAdditionalAddressOfLegalEntity(recordId, masterDataSeed = scriptVariantSeed, additionalAddressLabel = addressId, legalEntityLabel = legalEntityId)
    }

    @When("the golden record process refines record {string} to additional address {string} of site {string} of legal entity {string} with script variant {string}")
    fun `when refines to additional address of site`(recordId: String, addressId: String, siteId: String, legalEntityId: String, scriptVariantSeed: String) {
        logger.info { "[$scenarioName] When: refines '$recordId' to additional address '$addressId' of site '$siteId' of legal entity '$legalEntityId' with script variant '$scriptVariantSeed'" }
        shareActions.refineAsAdditionalAddressOfSite(recordId, masterDataSeed = scriptVariantSeed, additionalAddressLabel = addressId, siteLabel = siteId, legalEntityLabel = legalEntityId)
    }

    @When("the golden record process refines record {string} to legal entity {string} with script code {string}")
    fun `when refines to legal entity with script code`(recordId: String, legalEntityId: String, scriptCode: String) {
        logger.info { "[$scenarioName] When: refines '$recordId' to legal entity '$legalEntityId' with script code '$scriptCode'" }
        val legalEntity = shareActions.refineAsLegalEntity(recordId, masterDataSeed = legalEntityId, legalEntityLabel = legalEntityId, verified = false, scriptCode = scriptCode)
        context.legalEntities[legalEntityId] = legalEntity
    }

    @When("the golden record process refines record {string} to additional address {string} with script code {string} of existing legal entity {string}")
    fun `when refines to additional address of existing legal entity with script code`(recordId: String, addressId: String, scriptCode: String, legalEntityId: String) {
        logger.info { "[$scenarioName] When: refines '$recordId' to additional address '$addressId' with script code '$scriptCode' of existing legal entity '$legalEntityId'" }
        shareActions.refineAsAdditionalAddressOfExistingLegalEntity(
            recordId, masterDataSeed = addressId, additionalAddressLabel = addressId, parentLegalEntityLabel = legalEntityId, scriptCode = scriptCode
        )
    }

    // -------------------------------------------------------------------------
    // Then - the output reflects the merged script variants of the golden records the record was refined to
    // -------------------------------------------------------------------------

    @Then("{string} output reflects the script variants of legal entity {string}")
    fun `then reflects legal entity script variants`(recordId: String, legalEntityId: String) {
        logger.info { "[$scenarioName] Then: '$recordId' output reflects the script variants of legal entity '$legalEntityId'" }
        val state = stateOf(recordId)
        val outputFactory = testDataFactoryGate.businessPartner.output
        val expected = outputFactory.fromLegalEntity(inputResponseOf(recordId), state.legalEntity!!)

        val actual = assertGateOutputScriptVariants(recordId, expected)

        val poolLegalEntity = poolClient.legalEntities.getLegalEntity(actual.legalEntity.legalEntityBpn, "BPN")
        attachCall("GET", "/v7/legal-entities/${actual.legalEntity.legalEntityBpn}", response = poolLegalEntity)
        assertPoolReflectsScriptVariants(expected, outputFactory.fromLegalEntity(inputResponseOf(recordId), poolLegalEntity))
    }

    @Then("{string} output reflects the script variants of site-based legal entity {string} with site {string}")
    fun `then reflects site-based legal entity script variants`(recordId: String, legalEntityId: String, siteId: String) {
        logger.info { "[$scenarioName] Then: '$recordId' output reflects the script variants of site-based legal entity '$legalEntityId' with site '$siteId'" }
        val state = stateOf(recordId)
        val outputFactory = testDataFactoryGate.businessPartner.output
        val expected = outputFactory.fromLegalEntityOnSite(inputResponseOf(recordId), state.legalEntity!!, state.poolSite!!)

        val actual = assertGateOutputScriptVariants(recordId, expected)

        val poolLegalEntity = poolClient.legalEntities.getLegalEntity(actual.legalEntity.legalEntityBpn, "BPN")
        attachCall("GET", "/v7/legal-entities/${actual.legalEntity.legalEntityBpn}", response = poolLegalEntity)
        val poolSite = poolClient.sites.getSite(actual.site!!.siteBpn)
        attachCall("GET", "/v7/sites/${actual.site!!.siteBpn}", response = poolSite)
        assertPoolReflectsScriptVariants(expected, outputFactory.fromLegalEntityOnSite(inputResponseOf(recordId), poolLegalEntity, poolSite))
    }

    @Then("{string} output reflects the script variants of site {string} of legal entity {string}")
    fun `then reflects site script variants`(recordId: String, siteId: String, legalEntityId: String) {
        logger.info { "[$scenarioName] Then: '$recordId' output reflects the script variants of site '$siteId' of legal entity '$legalEntityId'" }
        val state = stateOf(recordId)
        val outputFactory = testDataFactoryGate.businessPartner.output
        val expected = outputFactory.fromSite(inputResponseOf(recordId), state.legalEntity!!, state.poolSite!!)

        val actual = assertGateOutputScriptVariants(recordId, expected)

        val poolLegalEntity = poolClient.legalEntities.getLegalEntity(actual.legalEntity.legalEntityBpn, "BPN")
        attachCall("GET", "/v7/legal-entities/${actual.legalEntity.legalEntityBpn}", response = poolLegalEntity)
        val poolSite = poolClient.sites.getSite(actual.site!!.siteBpn)
        attachCall("GET", "/v7/sites/${actual.site!!.siteBpn}", response = poolSite)
        assertPoolReflectsScriptVariants(expected, outputFactory.fromSite(inputResponseOf(recordId), poolLegalEntity, poolSite))
    }

    @Then("{string} output reflects the script variants of additional address {string} of legal entity {string}")
    fun `then reflects additional address of legal entity script variants`(recordId: String, addressId: String, legalEntityId: String) {
        logger.info { "[$scenarioName] Then: '$recordId' output reflects the script variants of additional address '$addressId' of legal entity '$legalEntityId'" }
        assertAdditionalAddressOfLegalEntityScriptVariants(recordId)
    }

    @Then("{string} output reflects the merged script variants of additional address {string} of legal entity {string}")
    fun `then reflects merged additional address of legal entity script variants`(recordId: String, addressId: String, legalEntityId: String) {
        logger.info { "[$scenarioName] Then: '$recordId' output reflects the merged script variants of additional address '$addressId' of legal entity '$legalEntityId'" }
        assertAdditionalAddressOfLegalEntityScriptVariants(recordId)
    }

    @Then("{string} output reflects the script variants of additional address {string} of site {string} of legal entity {string}")
    fun `then reflects additional address of site script variants`(recordId: String, addressId: String, siteId: String, legalEntityId: String) {
        logger.info { "[$scenarioName] Then: '$recordId' output reflects the script variants of additional address '$addressId' of site '$siteId' of legal entity '$legalEntityId'" }
        val state = stateOf(recordId)
        val outputFactory = testDataFactoryGate.businessPartner.output
        val expected = outputFactory.fromAdditionalAddressOnSite(inputResponseOf(recordId), state.legalEntity!!, state.poolSite!!, state.poolAddress!!)

        val actual = assertGateOutputScriptVariants(recordId, expected)

        val poolLegalEntity = poolClient.legalEntities.getLegalEntity(actual.legalEntity.legalEntityBpn, "BPN")
        attachCall("GET", "/v7/legal-entities/${actual.legalEntity.legalEntityBpn}", response = poolLegalEntity)
        val poolSite = poolClient.sites.getSite(actual.site!!.siteBpn)
        attachCall("GET", "/v7/sites/${actual.site!!.siteBpn}", response = poolSite)
        val poolAddress = poolClient.addresses.getAddress(actual.address.addressBpn)
        attachCall("GET", "/v7/addresses/${actual.address.addressBpn}", response = poolAddress)
        assertPoolReflectsScriptVariants(expected, outputFactory.fromAdditionalAddressOnSite(inputResponseOf(recordId), poolLegalEntity, poolSite, poolAddress))
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun assertAdditionalAddressOfLegalEntityScriptVariants(recordId: String) {
        val state = stateOf(recordId)
        val outputFactory = testDataFactoryGate.businessPartner.output
        val expected = outputFactory.fromAdditionalAddressOnLegalEntity(inputResponseOf(recordId), state.legalEntity!!, state.poolAddress!!)

        val actual = assertGateOutputScriptVariants(recordId, expected)

        val poolLegalEntity = poolClient.legalEntities.getLegalEntity(actual.legalEntity.legalEntityBpn, "BPN")
        attachCall("GET", "/v7/legal-entities/${actual.legalEntity.legalEntityBpn}", response = poolLegalEntity)
        val poolAddress = poolClient.addresses.getAddress(actual.address.addressBpn)
        attachCall("GET", "/v7/addresses/${actual.address.addressBpn}", response = poolAddress)
        assertPoolReflectsScriptVariants(expected, outputFactory.fromAdditionalAddressOnLegalEntity(inputResponseOf(recordId), poolLegalEntity, poolAddress))
    }

    private fun share(recordId: String) {
        logger.info { "[$scenarioName] share record '$recordId'" }
        shareActions.upload(recordId, isOwnCompanyData = true)
    }

    private fun stateOf(recordId: String) =
        context.records[recordId] ?: error("record '$recordId' must be shared and refined by earlier steps")

    private fun inputResponseOf(recordId: String): BusinessPartnerInputDto =
        testDataFactoryGate.businessPartner.input.response.fromRequest(stateOf(recordId).currentInput!!)

    /**
     * Asserts the single Gate output for [recordId] carries [expected]'s script variants (and only those) and
     * returns that output partner so the caller can read the referenced BPNs for the Pool cross-check.
     */
    private fun assertGateOutputScriptVariants(recordId: String, expected: BusinessPartnerOutputDto): BusinessPartnerOutputDto {
        val runId = context.runId(recordId)
        val outputPage = gateClient.businessParters.getBusinessPartnersOutput(listOf(runId))
        attachCall("POST", "/v7/output/business-partners/search", request = listOf(runId), response = outputPage)
        assertRepository.assertBusinessPartnerOutput(
            outputPage,
            PageDto(1, 1, 0, 1, listOf(expected.copy(externalId = runId))),
            assertRepository.outputScriptVariantsComparisonConfig
        )
        return outputPage.content.single()
    }

    /**
     * Asserts the Pool golden record - already reduced to [poolAsOutput] through the same output factory - carries
     * the same script variants as [expected], proving the output reflects the golden record rather than just
     * carrying matching content inline.
     */
    private fun assertPoolReflectsScriptVariants(expected: BusinessPartnerOutputDto, poolAsOutput: BusinessPartnerOutputDto) {
        assertRepository.assertBusinessPartnerOutput(
            listOf(poolAsOutput),
            listOf(expected),
            assertRepository.outputScriptVariantsComparisonConfig
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
