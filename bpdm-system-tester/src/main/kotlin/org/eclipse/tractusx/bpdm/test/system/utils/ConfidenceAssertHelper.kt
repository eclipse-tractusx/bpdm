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

package org.eclipse.tractusx.bpdm.test.system.utils

import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.gate.api.model.ConfidenceCriteriaDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

enum class OutputComponent { LEGAL_ENTITY, LEGAL_ADDRESS, ADDITIONAL_ADDRESS, SITE }

enum class ConfidenceLevel(val sharedByOwner: Boolean, val checkedByExternalDataSource: Boolean) {
    NO_CONFIDENCE(sharedByOwner = false, checkedByExternalDataSource = false),
    OWNER_SHARED(sharedByOwner = true,  checkedByExternalDataSource = false),
    VERIFIED(sharedByOwner = false, checkedByExternalDataSource = true),
    VERIFIED_OWNER_SHARED(sharedByOwner = true,  checkedByExternalDataSource = true)
}

class ConfidenceAssertHelper(
    private val sharingMemberGates: SharingMemberGates,
    private val apiCallEvidence: ApiCallEvidence
) {

    companion object {
        private val logger = KotlinLogging.logger { }

        private val CRITERION_WAIT_TIMEOUT = Duration.ofMinutes(6)
        private const val CRITERION_POLL_INTERVAL_SECONDS = 10L
    }

    private val context: ScenarioContext get() = ScenarioContext.current()!!

    private fun gateOf(recordId: String) = sharingMemberGates.of(context.memberOf(recordId))

    fun assertConfidence(recordId: String, component: OutputComponent, level: ConfidenceLevel) {
        val runId = context.runId(recordId)
        val outputPage = gateOf(recordId).businessParters.getBusinessPartnersOutput(listOf(runId))
        apiCallEvidence.attach("POST", "/v7/output/business-partners/search", listOf(runId), outputPage)
        val actual = confidenceOf(outputPage.content.single(), component)

        assertThat(actual.sharedByOwner)
            .describedAs("$component of '$recordId': sharedByOwner")
            .isEqualTo(level.sharedByOwner)
        assertThat(actual.checkedByExternalDataSource)
            .describedAs("$component of '$recordId': checkedByExternalDataSource")
            .isEqualTo(level.checkedByExternalDataSource)
    }

    /**
     * Asserts the record's output reflects [expectedCount] sharing members for [component], waiting for that
     * count to arrive.
     *
     * Unlike the other confidence criteria, this one is not stated by the refinement: the Gate reports which of
     * its records count towards the golden record, the Pool recounts them, and the Gate then refreshes its
     * output from the Pool - three schedules the completed sharing state says nothing about. Only the last read
     * is attached as evidence, so a scenario is not buried under one attachment per poll.
     */
    fun assertSharingMemberCount(recordId: String, component: OutputComponent, expectedCount: Int) {
        awaitCriterion(recordId, component, expectedCount, "numberOfSharingMembers") { it.numberOfSharingMembers }
    }

    /**
     * Asserts the record's output reflects [expectedLevel] as the confidence level for [component], waiting
     * for that level to arrive.
     *
     * The level is derived from the criteria, and the sharing member count is one of them - so it moves on the
     * same schedules the count does, long after the sharing state reported Success.
     */
    fun assertConfidenceLevel(recordId: String, component: OutputComponent, expectedLevel: Int) {
        awaitCriterion(recordId, component, expectedLevel, "confidenceLevel") { it.confidenceLevel }
    }

    private fun awaitCriterion(
        recordId: String,
        component: OutputComponent,
        expected: Int,
        criterion: String,
        actualOf: (ConfidenceCriteriaDto) -> Int
    ) {
        val runId = context.runId(recordId)
        val deadline = Instant.now().plus(CRITERION_WAIT_TIMEOUT)

        var outputPage = gateOf(recordId).businessParters.getBusinessPartnersOutput(listOf(runId))
        var actual = actualOf(confidenceOf(outputPage.content.single(), component))
        while (actual != expected && Instant.now().isBefore(deadline)) {
            logger.info {
                "[${context.scenarioName}] Waiting for $criterion of $component of '$recordId' to reach" +
                        " $expected, currently $actual"
            }
            TimeUnit.SECONDS.sleep(CRITERION_POLL_INTERVAL_SECONDS)
            outputPage = gateOf(recordId).businessParters.getBusinessPartnersOutput(listOf(runId))
            actual = actualOf(confidenceOf(outputPage.content.single(), component))
        }

        apiCallEvidence.attach("POST", "/v7/output/business-partners/search", listOf(runId), outputPage)
        assertThat(actual)
            .describedAs(
                "$component of '$recordId': $criterion, still $actual after ${CRITERION_WAIT_TIMEOUT.toMinutes()} minutes"
            )
            .isEqualTo(expected)
    }

    private fun confidenceOf(output: BusinessPartnerOutputDto, component: OutputComponent): ConfidenceCriteriaDto =
        when (component) {
            OutputComponent.LEGAL_ENTITY       -> output.legalEntity.confidenceCriteria
            OutputComponent.LEGAL_ADDRESS      -> output.address.confidenceCriteria
            OutputComponent.ADDITIONAL_ADDRESS -> output.address.confidenceCriteria
            OutputComponent.SITE               -> output.site!!.confidenceCriteria
        }
}
