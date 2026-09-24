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

package org.eclipse.tractusx.bpdm.pool.v7.relation

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityStateDto
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.TestDataV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.withStates
import org.eclipse.tractusx.orchestrator.api.model.RelationType
import org.eclipse.tractusx.orchestrator.api.model.RelationValidityPeriod
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsErrorDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Whether an alternative headquarter designation is accepted depends on what both of its legal entities record about
 * being in use, and the rule reads the same for the alternative and the main.
 */
class AlternativeHeadquarterStateValidationV7IT : UnscheduledPoolTestBaseV7() {

    /**
     * GIVEN a legal entity recorded as inactive part way through the designation's validity period
     * WHEN a sharing member designates it as alternative or as main
     * THEN the designation is rejected, naming that legal entity and the period
     */
    @ParameterizedTest
    @EnumSource(DesignatedRole::class)
    fun `reject designation whose partner is recorded as inactive during the validity period`(role: DesignatedRole) {
        //GIVEN
        val judgedBpn = createLegalEntity(
            "$testName judged",
            active(stateStart, relationStart.plusMonths(1).atStartOfDay()),
            inactive(relationStart.plusMonths(1).atStartOfDay(), null)
        )
        val otherBpn = createLegalEntity("$testName other")

        //WHEN
        val errors = designate(role, judgedBpn, otherBpn, listOf(firstPeriod))

        //THEN
        assertThat(errors.map { it.description }).singleElement().asString()
            .contains(judgedBpn).contains("recorded as inactive").contains(firstPeriod.validFrom.toString())
    }

    /**
     * GIVEN an alternative and a main both recorded as inactive during the designation's validity period
     * WHEN a sharing member designates the one as alternative of the other
     * THEN the designation is rejected, naming each of them
     */
    @Test
    fun `reject designation naming both partners when both are recorded as inactive`() {
        //GIVEN
        val alternativeBpn = createLegalEntity("$testName judged", inactive(stateStart, null))
        val mainBpn = createLegalEntity("$testName other", inactive(stateStart, null))

        //WHEN
        val errors = designate(DesignatedRole.ALTERNATIVE, alternativeBpn, mainBpn, listOf(firstPeriod))

        //THEN
        assertThat(errors.map { it.description })
            .anySatisfy { assertThat(it).contains(alternativeBpn).contains("recorded as inactive") }
            .anySatisfy { assertThat(it).contains(mainBpn).contains("recorded as inactive") }
            .hasSize(2)
    }

    /**
     * GIVEN a legal entity recorded as inactive only during the second of two validity periods
     * WHEN a sharing member designates it over both periods
     * THEN the designation is rejected, naming the second period only
     */
    @ParameterizedTest
    @EnumSource(DesignatedRole::class)
    fun `reject designation naming only the validity period the partner is inactive in`(role: DesignatedRole) {
        //GIVEN
        val judgedBpn = createLegalEntity(
            "$testName judged",
            active(stateStart, secondPeriod.validFrom.plusMonths(1).atStartOfDay()),
            inactive(secondPeriod.validFrom.plusMonths(1).atStartOfDay(), null)
        )
        val otherBpn = createLegalEntity("$testName other")

        //WHEN
        val errors = designate(role, judgedBpn, otherBpn, listOf(firstPeriod, secondPeriod))

        //THEN
        assertThat(errors.map { it.description }).singleElement().asString()
            .contains(secondPeriod.validFrom.toString()).doesNotContain(firstPeriod.validFrom.toString())
    }

    /**
     * GIVEN a legal entity recorded as inactive only in the gap between two validity periods
     * WHEN a sharing member designates it over both periods
     * THEN the designation is accepted
     */
    @ParameterizedTest
    @EnumSource(DesignatedRole::class)
    fun `accept designation whose partner is inactive only between its validity periods`(role: DesignatedRole) {
        //GIVEN
        val judgedBpn = createLegalEntity(
            "$testName judged",
            active(stateStart, firstPeriod.validTo!!.atStartOfDay()),
            inactive(firstPeriod.validTo!!.atStartOfDay(), secondPeriod.validFrom.atStartOfDay()),
            active(secondPeriod.validFrom.atStartOfDay(), null)
        )
        val otherBpn = createLegalEntity("$testName other")

        //WHEN
        val errors = designate(role, judgedBpn, otherBpn, listOf(firstPeriod, secondPeriod))

        //THEN
        assertThat(errors).isEmpty()
    }

    /**
     * GIVEN an alternative and a main that record no state at all
     * WHEN a sharing member designates the one as alternative of the other
     * THEN the designation is accepted
     */
    @Test
    fun `accept designation whose partners record no state`() {
        //GIVEN
        val alternativeBpn = createLegalEntityWithoutStates("$testName alternative")
        val mainBpn = createLegalEntityWithoutStates("$testName main")

        //WHEN
        val errors = designate(DesignatedRole.ALTERNATIVE, alternativeBpn, mainBpn, listOf(firstPeriod))

        //THEN
        assertThat(errors).isEmpty()
    }

    /**
     * GIVEN a legal entity whose inactive state ends at midnight of the day the designation starts
     * WHEN a sharing member designates it from that day on
     * THEN the designation is accepted
     */
    @ParameterizedTest
    @EnumSource(DesignatedRole::class)
    fun `accept designation starting on the midnight the partner's inactive state ends`(role: DesignatedRole) {
        //GIVEN
        val judgedBpn = createLegalEntity(
            "$testName judged",
            inactive(stateStart, relationStart.atStartOfDay()),
            active(relationStart.atStartOfDay(), null)
        )
        val otherBpn = createLegalEntity("$testName other")

        //WHEN
        val errors = designate(role, judgedBpn, otherBpn, listOf(firstPeriod))

        //THEN
        assertThat(errors).isEmpty()
    }

    enum class DesignatedRole { ALTERNATIVE, MAIN }

    private val stateStart = TestDataV7.currentStateValidFrom

    private val relationStart: LocalDate = LocalDate.now().plusYears(1)

    private val firstPeriod = RelationValidityPeriod(relationStart, relationStart.plusYears(1))

    private val secondPeriod = RelationValidityPeriod(relationStart.plusYears(2), relationStart.plusYears(3))

    private fun active(validFrom: LocalDateTime?, validTo: LocalDateTime?) = LegalEntityStateDto(validFrom, validTo, BusinessStateType.ACTIVE)

    private fun inactive(validFrom: LocalDateTime?, validTo: LocalDateTime?) = LegalEntityStateDto(validFrom, validTo, BusinessStateType.INACTIVE)

    private fun designate(
        role: DesignatedRole,
        judgedBpn: String,
        otherBpn: String,
        validityPeriods: List<RelationValidityPeriod>
    ): List<TaskRelationsErrorDto> =
        when (role) {
            DesignatedRole.ALTERNATIVE ->
                testDataClient.createRelationToErrors(RelationType.IsAlternativeHeadquarterFor, judgedBpn, otherBpn, validityPeriods)
            DesignatedRole.MAIN ->
                testDataClient.createRelationToErrors(RelationType.IsAlternativeHeadquarterFor, otherBpn, judgedBpn, validityPeriods)
        }

    private fun createLegalEntity(seed: String, vararg states: LegalEntityStateDto): String {
        val request = requestFactory.buildLegalEntity(seed)
        val requestWithStates = if (states.isEmpty()) request else request.withStates(states.toList())
        return testDataClient.createLegalEntity(requestWithStates).header.bpnl
    }

    private fun createLegalEntityWithoutStates(seed: String): String =
        testDataClient.createLegalEntity(requestFactory.buildLegalEntity(seed).withStates(emptyList())).header.bpnl
}
