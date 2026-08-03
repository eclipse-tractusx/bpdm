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
import org.eclipse.tractusx.bpdm.gate.api.model.response.AdditionalSiteOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerboseDto
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

    @Given("record {string} reflects site {string} of legal entity {string} with master data {string}")
    fun `given record reflects site master data`(
        recordId: String,
        siteId: String,
        legalEntityId: String,
        masterDataSeed: String
    ) {
        logger.info {
            "[$scenarioName] Given: record '$recordId' reflects site '$siteId' of legal entity '$legalEntityId' " +
                "with master data '$masterDataSeed'"
        }
        // Compose the established share -> refine flow so scenarios can start from a record that already
        // reflects the site. The refine step waits for the sharing process to complete, so no separate
        // assertion or wait is needed here.
        `when shares record`(recordId)
        `when refines to site of legal entity`(recordId, siteId, legalEntityId, masterDataSeed)
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

    @Given("record {string} reflects additional address {string} of site {string} of legal entity {string} with master data {string}")
    fun `given record reflects additional address of site master data`(
        recordId: String,
        addressId: String,
        siteId: String,
        legalEntityId: String,
        masterDataSeed: String
    ) {
        logger.info {
            "[$scenarioName] Given: record '$recordId' reflects additional address '$addressId' of site '$siteId' " +
                "of legal entity '$legalEntityId' with master data '$masterDataSeed'"
        }
        // Compose the established share -> refine flow so scenarios can start from a record that already
        // reflects the additional address of the site. The refine step waits for the sharing process to
        // complete, so no separate assertion or wait is needed here.
        `when shares record`(recordId)
        `when refines to additional address of site`(recordId, addressId, siteId, legalEntityId, masterDataSeed)
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
        val legalEntity = shareActions.refineAsLegalEntity(recordId, masterDataSeed, legalEntityId)
        context.legalEntities[legalEntityId] = legalEntity
    }

    @When("the golden record process refines record {string} to site {string} of legal entity {string} with master data {string}")
    fun `when refines to site of legal entity`(
        recordId: String,
        siteId: String,
        legalEntityId: String,
        masterDataSeed: String
    ) {
        logger.info {
            "[$scenarioName] When: the golden record process refines record '$recordId' to site '$siteId' " +
                "of legal entity '$legalEntityId' with master data '$masterDataSeed'"
        }
        // Store the resulting site (and its parent legal entity) under their labels so the Then can assert by
        // reference and re-refinement automatically updates the expectation.
        val siteWithParent = shareActions.refineAsSite(recordId, masterDataSeed, siteId, legalEntityId)
        context.legalEntities[legalEntityId] = siteWithParent.legalEntity
        context.sites[siteId] = siteWithParent
    }

    @When("the golden record process refines record {string} to site {string} with shared main address {string} of legal entity {string} with master data {string}")
    fun `when refines to site with shared main address`(
        recordId: String,
        siteId: String,
        mainAddressId: String,
        legalEntityId: String,
        masterDataSeed: String
    ) {
        logger.info {
            "[$scenarioName] When: the golden record process refines record '$recordId' to site '$siteId' " +
                "with shared main address '$mainAddressId' of legal entity '$legalEntityId' with master data '$masterDataSeed'"
        }
        // Pin the site's main address to the shared [mainAddressId] label so several records can refine to
        // distinct sites that all point their main address at the same address - i.e. one address belonging
        // to several sites as their main address.
        val siteWithParent = shareActions.refineAsSite(recordId, masterDataSeed, siteId, legalEntityId, mainAddressId)
        context.legalEntities[legalEntityId] = siteWithParent.legalEntity
        context.sites[siteId] = siteWithParent
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
        val addressWithParent = shareActions.refineAsAdditionalAddressOfLegalEntity(recordId, masterDataSeed, addressId, legalEntityId)
        context.legalEntities[legalEntityId] = addressWithParent.legalEntity
        context.additionalLegalEntityAddresses[addressId] = addressWithParent
    }

    @When("the golden record process refines record {string} to additional address {string} of site {string} of legal entity {string} with master data {string}")
    fun `when refines to additional address of site`(
        recordId: String,
        addressId: String,
        siteId: String,
        legalEntityId: String,
        masterDataSeed: String
    ) {
        logger.info {
            "[$scenarioName] When: the golden record process refines record '$recordId' to additional address " +
                "'$addressId' of site '$siteId' of legal entity '$legalEntityId' with master data '$masterDataSeed'"
        }
        // Store the resulting address (and its parent site and legal entity) under their labels so the Then can
        // assert by reference and re-refinement automatically updates the expectation.
        val addressWithParent = shareActions.refineAsAdditionalAddressOfSite(recordId, masterDataSeed, addressId, siteId, legalEntityId)
        context.legalEntities[legalEntityId] = addressWithParent.siteWithParent.legalEntity
        context.sites[siteId] = addressWithParent.siteWithParent
        context.additionalSiteAddresses[addressId] = addressWithParent
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

    @Then("{string} output reflects site {string} of legal entity {string} in its master data")
    fun `then output reflects site master data`(recordId: String, siteId: String, legalEntityId: String) {
        logger.info {
            "[$scenarioName] Then: '$recordId' output reflects site '$siteId' of legal entity '$legalEntityId' " +
                "in its master data"
        }
        // Expected master data is the currently defined site (and its parent legal entity) for this label, set
        // by the golden record refinement step. Reflecting the site implies reflecting its main address,
        // reachable as expected.site.mainAddress.
        val expected = context.sites[siteId]
            ?: error("site '$siteId' must be defined by an earlier golden record refinement step")

        val runId = context.runId(recordId)
        val inputResponse = inputResponseOf(recordId)
        val outputFactory = testDataFactoryGate.businessPartner.output
        val expectedOutput = outputFactory
            .fromSite(inputResponse, expected.legalEntity, expected.site)
            .copy(externalId = runId)

        // (1) The Gate output carries the expected master data inline.
        val actualOutput = assertGateOutputCarriesMasterData(runId, expectedOutput)

        // (2) The output's BPNs really reference a Pool golden record with the same master data. Reduce the
        // Pool legal entity and site through the same output factory so we can reuse the comparison.
        val referencedBpnl = actualOutput.legalEntity.legalEntityBpn
        val referencedBpns = actualOutput.site!!.siteBpn
        val poolLegalEntity = poolClient.legalEntities.getLegalEntity(referencedBpnl, "BPN")
        attachCall("GET", "/v7/legal-entities/$referencedBpnl", response = poolLegalEntity)
        val poolSite = poolClient.sites.getSite(referencedBpns)
        attachCall("GET", "/v7/sites/$referencedBpns", response = poolSite)
        val poolAsOutput = outputFactory
            .fromSite(inputResponse, poolLegalEntity, poolSite)
            .copy(externalId = runId)
        assertPoolGoldenRecordReflectsMasterData(expectedOutput, poolAsOutput)

        // The output's BPNs must point to this legal entity, its site and the site's main address in the Pool.
        assertThat(actualOutput.legalEntity.legalEntityBpn)
            .describedAs("output legal entity BPN must reference legal entity '%s' in the Pool", legalEntityId)
            .isEqualTo(poolLegalEntity.header.bpnl)
        assertThat(actualOutput.site!!.siteBpn)
            .describedAs("output site BPN must reference site '%s' of legal entity '%s' in the Pool", siteId, legalEntityId)
            .isEqualTo(poolSite.site.bpns)
        assertThat(actualOutput.address.addressBpn)
            .describedAs("output address BPN must reference site '%s's main address in the Pool", siteId)
            .isEqualTo(poolSite.mainAddress.bpna)

        // The referenced Pool address must actually be the site's main address.
        assertThat(actualOutput.address.addressType)
            .describedAs("Pool main address of site '%s' must be typed as a site main address", siteId)
            .isEqualTo(AddressType.SiteMainAddress)
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

    @Then("{string} output reflects additional address {string} of site {string} of legal entity {string} in its master data")
    fun `then output reflects additional address of site master data`(
        recordId: String,
        addressId: String,
        siteId: String,
        legalEntityId: String
    ) {
        logger.info {
            "[$scenarioName] Then: '$recordId' output reflects additional address '$addressId' of site '$siteId' " +
                "of legal entity '$legalEntityId' in its master data"
        }
        // Expected master data is the currently defined additional address (and its parent site and legal
        // entity) for this label, set by the golden record refinement step.
        val expected = context.additionalSiteAddresses[addressId]
            ?: error("additional address '$addressId' must be defined by an earlier golden record refinement step")

        val runId = context.runId(recordId)
        val inputResponse = inputResponseOf(recordId)
        val outputFactory = testDataFactoryGate.businessPartner.output
        val expectedOutput = outputFactory
            .fromAdditionalAddressOnSite(inputResponse, expected.siteWithParent.legalEntity, expected.siteWithParent.site, expected.address)
            .copy(externalId = runId)

        // (1) The Gate output carries the expected master data inline.
        val actualOutput = assertGateOutputCarriesMasterData(runId, expectedOutput)

        // (2) The output's BPNs really reference a Pool golden record with the same master data. Reduce the
        // Pool legal entity, site and address through the same output factory so we can reuse the comparison.
        val referencedBpnl = actualOutput.legalEntity.legalEntityBpn
        val referencedBpns = actualOutput.site!!.siteBpn
        val referencedBpna = actualOutput.address.addressBpn
        val poolLegalEntity = poolClient.legalEntities.getLegalEntity(referencedBpnl, "BPN")
        attachCall("GET", "/v7/legal-entities/$referencedBpnl", response = poolLegalEntity)
        val poolSite = poolClient.sites.getSite(referencedBpns)
        attachCall("GET", "/v7/sites/$referencedBpns", response = poolSite)
        val poolAddress = poolClient.addresses.getAddress(referencedBpna)
        attachCall("GET", "/v7/addresses/$referencedBpna", response = poolAddress)
        val poolAsOutput = outputFactory
            .fromAdditionalAddressOnSite(inputResponse, poolLegalEntity, poolSite, poolAddress)
            .copy(externalId = runId)
        assertPoolGoldenRecordReflectsMasterData(expectedOutput, poolAsOutput)

        // The output's BPNs must point to this legal entity, its site and the site's additional address in the Pool.
        assertThat(actualOutput.legalEntity.legalEntityBpn)
            .describedAs("output legal entity BPN must reference legal entity '%s' in the Pool", legalEntityId)
            .isEqualTo(poolLegalEntity.header.bpnl)
        assertThat(actualOutput.site!!.siteBpn)
            .describedAs("output site BPN must reference site '%s' of legal entity '%s' in the Pool", siteId, legalEntityId)
            .isEqualTo(poolSite.site.bpns)
        assertThat(actualOutput.address.addressBpn)
            .describedAs("output address BPN must reference additional address '%s' of site '%s' in the Pool", addressId, siteId)
            .isEqualTo(poolAddress.address.bpna)

        // The referenced Pool address must actually be an additional address.
        assertThat(poolAddress.address.addressType)
            .describedAs("Pool address '%s' of site '%s' must be typed as an additional address", addressId, siteId)
            .isEqualTo(AddressType.AdditionalAddress)
    }

    // -------------------------------------------------------------------------
    // Then - top-level identifiers and states surfacing rule
    //
    // The record's top-level identifiers and states surface the identifiers/states of the entity the record
    // reflects, and which entity that is depends on the record type. The expected output is built by the same
    // output factory used for master data, which already encodes the rule (legal entity -> the legal entity's,
    // site -> none + the site's, additional address -> the address's), so these steps simply pin that rule by
    // comparing ONLY the top-level identifiers and states.
    // -------------------------------------------------------------------------

    @Then("{string} output top-level identifiers and states reflect legal entity {string}")
    fun `then output reflects legal entity identifiers and states`(recordId: String, legalEntityId: String) {
        logger.info {
            "[$scenarioName] Then: '$recordId' output top-level identifiers and states reflect legal entity '$legalEntityId'"
        }
        val expectedLegalEntity = context.legalEntities[legalEntityId]
            ?: error("legal entity '$legalEntityId' must be defined by an earlier golden record refinement step")

        val runId = context.runId(recordId)
        val outputFactory = testDataFactoryGate.businessPartner.output
        val expectedOutput = outputFactory.fromLegalEntity(inputResponseOf(recordId), expectedLegalEntity).copy(externalId = runId)

        assertGateOutputTopLevelIdentifiersAndStates(runId, expectedOutput)
    }

    @Then("{string} output has no top-level identifiers and its states reflect site {string} of legal entity {string}")
    fun `then output has no identifiers and reflects site states`(recordId: String, siteId: String, legalEntityId: String) {
        logger.info {
            "[$scenarioName] Then: '$recordId' output has no top-level identifiers and its states reflect site " +
                "'$siteId' of legal entity '$legalEntityId'"
        }
        val expected = context.sites[siteId]
            ?: error("site '$siteId' must be defined by an earlier golden record refinement step")

        val runId = context.runId(recordId)
        val outputFactory = testDataFactoryGate.businessPartner.output
        // The factory surfaces no identifiers for a site record; the empty expectation below asserts exactly that.
        val expectedOutput = outputFactory
            .fromSite(inputResponseOf(recordId), expected.legalEntity, expected.site)
            .copy(externalId = runId)

        assertGateOutputTopLevelIdentifiersAndStates(runId, expectedOutput)
    }

    @Then("{string} output top-level identifiers and states reflect additional address {string} of legal entity {string}")
    fun `then output reflects additional address of legal entity identifiers and states`(
        recordId: String,
        addressId: String,
        legalEntityId: String
    ) {
        logger.info {
            "[$scenarioName] Then: '$recordId' output top-level identifiers and states reflect additional address " +
                "'$addressId' of legal entity '$legalEntityId'"
        }
        val expected = context.additionalLegalEntityAddresses[addressId]
            ?: error("additional address '$addressId' must be defined by an earlier golden record refinement step")

        val runId = context.runId(recordId)
        val outputFactory = testDataFactoryGate.businessPartner.output
        val expectedOutput = outputFactory
            .fromAdditionalAddressOnLegalEntity(inputResponseOf(recordId), expected.legalEntity, expected.address)
            .copy(externalId = runId)

        assertGateOutputTopLevelIdentifiersAndStates(runId, expectedOutput)
    }

    @Then("{string} output top-level identifiers and states reflect additional address {string} of site {string} of legal entity {string}")
    fun `then output reflects additional address of site identifiers and states`(
        recordId: String,
        addressId: String,
        siteId: String,
        legalEntityId: String
    ) {
        logger.info {
            "[$scenarioName] Then: '$recordId' output top-level identifiers and states reflect additional address " +
                "'$addressId' of site '$siteId' of legal entity '$legalEntityId'"
        }
        val expected = context.additionalSiteAddresses[addressId]
            ?: error("additional address '$addressId' must be defined by an earlier golden record refinement step")

        val runId = context.runId(recordId)
        val outputFactory = testDataFactoryGate.businessPartner.output
        val expectedOutput = outputFactory
            .fromAdditionalAddressOnSite(inputResponseOf(recordId), expected.siteWithParent.legalEntity, expected.siteWithParent.site, expected.address)
            .copy(externalId = runId)

        assertGateOutputTopLevelIdentifiersAndStates(runId, expectedOutput)
    }

    // -------------------------------------------------------------------------
    // Then - additional sites an address belongs to
    //
    // When several records refine the SAME additional address under DIFFERENT sites of the same legal
    // entity, that address ends up belonging to all of those sites. Each record follows its own site, so
    // its output's primary "site" is that site and the OTHER sites surface as the address's additional
    // sites. The expected additional sites are read from the OTHER records' actual outputs (their primary
    // "site"), which carry the real Pool-assigned BPNS/name - the context-stored entities only hold
    // placeholder BPNs. The Pool address query independently proves the same membership.
    // -------------------------------------------------------------------------

    @Then("{string} output lists the site of record {string} as an additional site of its address")
    fun `then output lists additional site`(recordId: String, otherRecordId: String) {
        logger.info {
            "[$scenarioName] Then: '$recordId' output lists the site of record '$otherRecordId' as an additional site of its address"
        }
        assertOutputAdditionalSites(recordId, listOf(otherRecordId))
    }

    @Then("{string} output lists the sites of records {string} as additional sites of its address")
    fun `then output lists additional sites`(recordId: String, otherRecordIds: String) {
        logger.info {
            "[$scenarioName] Then: '$recordId' output lists the sites of records '$otherRecordIds' as additional sites of its address"
        }
        assertOutputAdditionalSites(recordId, otherRecordIds.splitRecordIds())
    }

    @Then("{string} output lists no additional sites")
    fun `then output lists no additional sites`(recordId: String) {
        logger.info { "[$scenarioName] Then: '$recordId' output lists no additional sites" }
        assertOutputAdditionalSites(recordId, emptyList())
    }

    @Then("the Pool address of {string} belongs to the sites of records {string}")
    fun `then pool address belongs to sites`(recordId: String, siteRecordIds: String) {
        logger.info {
            "[$scenarioName] Then: the Pool address of '$recordId' belongs to the sites of records '$siteRecordIds'"
        }
        val expectedMembership = siteRecordIds.splitRecordIds().map { siteBpnOf(it) }.toSet()
        val poolAddress = poolAddressOf(recordId)
        val actualMembership = (listOfNotNull(poolAddress.address.bpnSite) + poolAddress.address.additionalSites).toSet()
        assertThat(actualMembership)
            .describedAs("Pool address of '%s' must belong to exactly the sites of records %s", recordId, siteRecordIds)
            .isEqualTo(expectedMembership)
    }

    @Then("the Pool address of {string} belongs to a single site")
    fun `then pool address belongs to single site`(recordId: String) {
        logger.info { "[$scenarioName] Then: the Pool address of '$recordId' belongs to a single site" }
        val poolAddress = poolAddressOf(recordId)
        assertThat(poolAddress.address.additionalSites)
            .describedAs("Pool address of '%s' must not list any additional sites", recordId)
            .isEmpty()
        assertThat(poolAddress.address.bpnSite)
            .describedAs("Pool address of '%s' must still belong to its single primary site", recordId)
            .isEqualTo(siteBpnOf(recordId))
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Asserts [recordId]'s Gate output surfaces exactly the primary sites of [otherRecordIds] as its
     * address's additional sites (and nothing else). Expected additional sites are built from the other
     * records' actual outputs so the BPNS/name are the real Pool-assigned ones; the comparison is
     * restricted to the additionalSites field. Works regardless of what kind of record [recordId] is (an
     * additional address of a site, or a site whose main address is shared) - it compares the actual output
     * against itself with only the additionalSites replaced, so no reflected-entity lookup is needed.
     */
    private fun assertOutputAdditionalSites(recordId: String, otherRecordIds: List<String>) {
        val expectedAdditionalSites = otherRecordIds.map { other ->
            val otherSite = outputOf(other).site
                ?: error("record '$other' output has no site to surface as an additional site")
            AdditionalSiteOutputDto(otherSite.siteBpn, otherSite.name)
        }

        val actualOutput = outputOf(recordId)
        assertRepository.assertBusinessPartnerOutput(
            listOf(actualOutput),
            listOf(actualOutput.copy(additionalSites = expectedAdditionalSites)),
            assertRepository.outputAdditionalSitesComparisonConfig
        )
    }

    /** Reads the single Gate output for [recordId], attaching the call for scenario diagnostics. */
    private fun outputOf(recordId: String): BusinessPartnerOutputDto {
        val runId = context.runId(recordId)
        val outputPage = gateClient.businessParters.getBusinessPartnersOutput(listOf(runId))
        attachCall("POST", "/v7/output/business-partners/search", request = listOf(runId), response = outputPage)
        return outputPage.content.single()
    }

    /** The real Pool-assigned BPNS of the primary site [recordId] was refined under, read from its output. */
    private fun siteBpnOf(recordId: String): String =
        outputOf(recordId).site?.siteBpn
            ?: error("record '$recordId' output has no site")

    /** The Pool golden record address [recordId]'s output references, fetched by its BPNA. */
    private fun poolAddressOf(recordId: String): LogisticAddressVerboseDto {
        val bpna = outputOf(recordId).address.addressBpn
        val poolAddress = poolClient.addresses.getAddress(bpna)
        attachCall("GET", "/v7/addresses/$bpna", response = poolAddress)
        return poolAddress
    }

    private fun String.splitRecordIds(): List<String> =
        split(",").map { it.trim() }.filter { it.isNotEmpty() }

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
     * Asserts the single Gate output for [runId] carries exactly [expectedOutput]'s top-level identifiers and
     * states - the fields that surface the identifiers/states of the reflected entity - and nothing else is
     * compared.
     */
    private fun assertGateOutputTopLevelIdentifiersAndStates(runId: String, expectedOutput: BusinessPartnerOutputDto) {
        val actualOutputPage = gateClient.businessParters.getBusinessPartnersOutput(listOf(runId))
        attachCall("POST", "/v7/output/business-partners/search", request = listOf(runId), response = actualOutputPage)
        assertRepository.assertBusinessPartnerOutput(
            actualOutputPage,
            PageDto(1, 1, 0, 1, listOf(expectedOutput)),
            assertRepository.outputTopLevelIdentifiersAndStatesComparisonConfig
        )
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
