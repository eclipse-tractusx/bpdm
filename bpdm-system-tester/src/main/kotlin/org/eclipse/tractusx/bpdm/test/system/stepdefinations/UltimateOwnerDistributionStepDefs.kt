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

import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.test.system.utils.ApiCallEvidence
import org.eclipse.tractusx.bpdm.test.system.utils.BusinessPartnerShareActions
import org.eclipse.tractusx.bpdm.test.system.utils.ScenarioContext
import org.eclipse.tractusx.bpdm.test.system.utils.SharingMemberGates

/**
 * Steps for the "Ultimate Owner Distribution" feature.
 *
 * The expected owner BPNL is read from the flag holder's own Gate output rather than from the golden record the
 * refinement generated, because the generated BPNs are placeholders the Pool replaces with the ones it assigns.
 */
class UltimateOwnerDistributionStepDefs(
    private val sharingMemberGates: SharingMemberGates,
    private val shareActions: BusinessPartnerShareActions,
    private val apiCallEvidence: ApiCallEvidence
) : SpringTestRunConfiguration() {

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    private val context: ScenarioContext get() = ScenarioContext.current()!!

    private fun gateOf(recordId: String) = sharingMemberGates.of(context.memberOf(recordId))
    private val scenarioName: String get() = context.scenarioName

    // -------------------------------------------------------------------------
    // When
    // -------------------------------------------------------------------------

    @When("the sharing member marks {string} as the ultimate owner")
    fun `when marks record as ultimate owner`(recordId: String) {
        logger.info { "[$scenarioName] When: the sharing member marks '$recordId' as the ultimate owner" }
        shareActions.uploadAsUltimateOwner(recordId)
    }

    @When("the golden record process refines record {string} to legal entity {string} as the ultimate owner")
    fun `when refines record to legal entity as ultimate owner`(recordId: String, legalEntityId: String) {
        logger.info {
            "[$scenarioName] When: the golden record process refines record '$recordId' to legal entity " +
                "'$legalEntityId' as the ultimate owner"
        }
        // Like the relation Given steps, the master data seed follows the legal entity label, so the refinement lands
        // on the same golden record with the same master data and only the ownership flag changes.
        val legalEntity = shareActions.refineAsUltimateOwner(recordId, masterDataSeed = legalEntityId, legalEntityLabel = legalEntityId)
        context.legalEntities[legalEntityId] = legalEntity
    }

    // -------------------------------------------------------------------------
    // Then
    // -------------------------------------------------------------------------

    @Then("{string} output reflects {string} as the ultimate owner")
    fun `then output reflects ultimate owner`(recordId: String, ownerRecordId: String) {
        logger.info { "[$scenarioName] Then: '$recordId' output reflects '$ownerRecordId' as the ultimate owner" }
        assertUltimateOwnerIs(recordId, ownerRecordId)
    }

    @Then("{string} output reports itself as the ultimate owner")
    fun `then output reports itself as ultimate owner`(recordId: String) {
        logger.info { "[$scenarioName] Then: '$recordId' output reports itself as the ultimate owner" }
        val output = outputOf(recordId)
        assertThat(output.legalEntity.ownershipUltimate)
            .describedAs("Ultimate owner flag of record '%s'", recordId)
            .isTrue()
        // The flag alone marks the ultimate owner itself: only the entities below it carry its BPNL.
        assertThat(output.legalEntity.ultimateOwnerBpnl)
            .describedAs("Ultimate owner of record '%s', which is the flag holder itself", recordId)
            .isNull()
    }

    @Then("{string} output has no ultimate owner")
    fun `then output has no ultimate owner`(recordId: String) {
        logger.info { "[$scenarioName] Then: '$recordId' output has no ultimate owner" }
        assertThat(outputOf(recordId).legalEntity.ultimateOwnerBpnl)
            .describedAs("Ultimate owner of record '%s'", recordId)
            .isNull()
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun assertUltimateOwnerIs(recordId: String, ownerRecordId: String) {
        val expectedBpnl = outputOf(ownerRecordId).legalEntity.legalEntityBpn
        assertThat(outputOf(recordId).legalEntity.ultimateOwnerBpnl)
            .describedAs("Ultimate owner of record '%s', expected to be record '%s'", recordId, ownerRecordId)
            .isEqualTo(expectedBpnl)
    }

    private fun outputOf(recordId: String): BusinessPartnerOutputDto {
        val runId = context.runId(recordId)
        val outputPage = gateOf(recordId).businessParters.getBusinessPartnersOutput(listOf(runId))
        apiCallEvidence.attach("POST", "/v7/output/business-partners/search", request = listOf(runId), response = outputPage)
        return outputPage.content.singleOrNull() ?: error("no output for record '$recordId'")
    }
}
