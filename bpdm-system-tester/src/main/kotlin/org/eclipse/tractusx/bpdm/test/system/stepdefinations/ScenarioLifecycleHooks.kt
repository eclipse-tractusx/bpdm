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
import io.cucumber.java.Scenario
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.test.system.utils.ScenarioContext
import org.eclipse.tractusx.bpdm.test.system.utils.SharingMember
import org.eclipse.tractusx.bpdm.test.system.utils.SharingMemberGates
import org.eclipse.tractusx.bpdm.test.testdata.gate.TestRunData
import org.eclipse.tractusx.bpdm.test.system.config.edc.EdcAccessNegotiators
import org.opentest4j.TestAbortedException

class ScenarioLifecycleHooks(
    private val testRunData: TestRunData,
    private val sharingMemberGates: SharingMemberGates,
    private val edcAccessNegotiators: EdcAccessNegotiators
) : SpringTestRunConfiguration() {

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    @Before
    fun setUp(scenario: Scenario) {
        ScenarioContext.set(ScenarioContext(scenario.name, scenario.id, testRunData.testTime, scenario))
        logger.info { "Starting scenario: '${scenario.name}'" }
    }

    /**
     * Skips a scenario tagged as needing two sharing members when this run acts for fewer.
     *
     * Most deployments give the tester a single Gate to share through, and such a scenario would then fail
     * for how the run is configured rather than for anything the golden record process did. Aborting is what
     * Cucumber reports as skipped, which keeps the scenario out of the passed and the failed count alike.
     */
    @Before("@TwoSharingMembers")
    fun skipWithoutSecondSharingMember(scenario: Scenario) {
        skipUnlessConfigured(scenario, SharingMember.SECOND)
    }

    /** Skips a scenario tagged as needing three sharing members when this run acts for fewer. */
    @Before("@ThreeSharingMembers")
    fun skipWithoutThirdSharingMember(scenario: Scenario) {
        skipUnlessConfigured(scenario, SharingMember.SECOND, SharingMember.THIRD)
    }

    /**
     * Skips a scenario about reaching an API over the EDC when this run reaches none that way.
     *
     * Most deployments give the tester no connector at all, and such a scenario would then report on how the
     * run is configured rather than on anything a dataspace did.
     */
    @Before("@EdcAccess")
    fun skipWithoutEdcAccess(scenario: Scenario) {
        if (edcAccessNegotiators.isAnyConfigured) return

        val reason = "Skipping scenario '${scenario.name}': it reaches an API over the EDC, and no client of this" +
                " run does. Set 'bpdm.client.<client>.edc.enabled' and name the connector to run it."
        logger.warn { reason }
        scenario.log(reason)
        throw TestAbortedException(reason)
    }

    private fun skipUnlessConfigured(scenario: Scenario, vararg members: SharingMember) {
        val missing = members.filterNot { sharingMemberGates.isConfigured(it) }
        if (missing.isEmpty()) return

        val reason = "Skipping scenario '${scenario.name}': it needs the ${members.joinToString(" and ") { it.name.lowercase() }}" +
                " sharing member, and this run has no ${missing.joinToString(" and ") { it.name.lowercase() }} one." +
                " Name that member's Gate and credentials under 'bpdm.client.gate-<n>-input' and" +
                " 'bpdm.client.gate-<n>-output' to run it."
        logger.warn { reason }
        scenario.log(reason)
        throw TestAbortedException(reason)
    }

    @After
    fun tearDown() {
        ScenarioContext.clear()
    }
}
