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
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.withParticipantData
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.withStates
import org.eclipse.tractusx.orchestrator.api.model.RelationType
import org.eclipse.tractusx.orchestrator.api.model.RelationValidityPeriod
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Whether an ownership or data management relation is accepted depends on what its governing legal entity records
 * about being in use, and only on that side.
 */
class GoverningPartnerStateValidationV7IT : UnscheduledPoolTestBaseV7() {

    /**
     * GIVEN a governing legal entity recorded as inactive part way through the relation's validity period
     * WHEN a sharing member relates a legal entity to it
     * THEN the relation is rejected, naming the governing legal entity and the period
     */
    @ParameterizedTest
    @EnumSource(value = RelationType::class, names = ["IsOwnedBy", "IsManagedBy"])
    fun `reject relation whose governing partner is recorded as inactive during the validity period`(relationType: RelationType) {
        //GIVEN
        val governedBpn = createLegalEntity("$testName governed")
        val governingBpn = createLegalEntity(
            "$testName governing",
            active(stateStart, relationStart.plusMonths(1).atStartOfDay()),
            inactive(relationStart.plusMonths(1).atStartOfDay(), null)
        )

        //WHEN
        val errors = testDataClient.createRelationToErrors(relationType, governedBpn, governingBpn, listOf(firstPeriod))

        //THEN
        assertThat(errors.map { it.description }).singleElement().asString()
            .contains(governingBpn).contains("recorded as inactive").contains(firstPeriod.validFrom.toString())
    }

    /**
     * GIVEN a governed legal entity recorded as inactive throughout the relation's validity period
     * WHEN a sharing member relates it to an active governing legal entity
     * THEN the relation is accepted
     */
    @ParameterizedTest
    @EnumSource(value = RelationType::class, names = ["IsOwnedBy", "IsManagedBy"])
    fun `accept relation whose governed partner is recorded as inactive`(relationType: RelationType) {
        //GIVEN
        val governedBpn = createLegalEntity("$testName governed", inactive(stateStart, null))
        val governingBpn = createLegalEntity("$testName governing")

        //WHEN
        val errors = testDataClient.createRelationToErrors(relationType, governedBpn, governingBpn, listOf(firstPeriod))

        //THEN
        assertThat(errors).isEmpty()
    }

    /**
     * GIVEN a governing legal entity recorded as inactive only during the second of two validity periods
     * WHEN a sharing member relates a legal entity to it over both periods
     * THEN the relation is rejected, naming the second period only
     */
    @ParameterizedTest
    @EnumSource(value = RelationType::class, names = ["IsOwnedBy", "IsManagedBy"])
    fun `reject relation naming only the validity period the governing partner is inactive in`(relationType: RelationType) {
        //GIVEN
        val governedBpn = createLegalEntity("$testName governed")
        val governingBpn = createLegalEntity(
            "$testName governing",
            active(stateStart, secondPeriod.validFrom.plusMonths(1).atStartOfDay()),
            inactive(secondPeriod.validFrom.plusMonths(1).atStartOfDay(), null)
        )

        //WHEN
        val errors = testDataClient.createRelationToErrors(relationType, governedBpn, governingBpn, listOf(firstPeriod, secondPeriod))

        //THEN
        assertThat(errors.map { it.description }).singleElement().asString()
            .contains(secondPeriod.validFrom.toString()).doesNotContain(firstPeriod.validFrom.toString())
    }

    /**
     * GIVEN a governing legal entity recorded as inactive only in the gap between two validity periods
     * WHEN a sharing member relates a legal entity to it over both periods
     * THEN the relation is accepted
     */
    @ParameterizedTest
    @EnumSource(value = RelationType::class, names = ["IsOwnedBy", "IsManagedBy"])
    fun `accept relation whose governing partner is inactive only between its validity periods`(relationType: RelationType) {
        //GIVEN
        val governedBpn = createLegalEntity("$testName governed")
        val governingBpn = createLegalEntity(
            "$testName governing",
            active(stateStart, firstPeriod.validTo!!.atStartOfDay()),
            inactive(firstPeriod.validTo!!.atStartOfDay(), secondPeriod.validFrom.atStartOfDay()),
            active(secondPeriod.validFrom.atStartOfDay(), null)
        )

        //WHEN
        val errors = testDataClient.createRelationToErrors(relationType, governedBpn, governingBpn, listOf(firstPeriod, secondPeriod))

        //THEN
        assertThat(errors).isEmpty()
    }

    /**
     * GIVEN a governing legal entity that records no state at all
     * WHEN a sharing member relates a legal entity to it
     * THEN the relation is accepted
     */
    @ParameterizedTest
    @EnumSource(value = RelationType::class, names = ["IsOwnedBy", "IsManagedBy"])
    fun `accept relation whose governing partner records no state`(relationType: RelationType) {
        //GIVEN
        val governedBpn = createLegalEntity("$testName governed")
        val governingBpn = createLegalEntityWithoutStates("$testName governing")

        //WHEN
        val errors = testDataClient.createRelationToErrors(relationType, governedBpn, governingBpn, listOf(firstPeriod))

        //THEN
        assertThat(errors).isEmpty()
    }

    /**
     * GIVEN a governing legal entity whose inactive state ends at midnight of the day the relation starts
     * WHEN a sharing member relates a legal entity to it from that day on
     * THEN the relation is accepted
     */
    @ParameterizedTest
    @EnumSource(value = RelationType::class, names = ["IsOwnedBy", "IsManagedBy"])
    fun `accept relation starting on the midnight the governing partner's inactive state ends`(relationType: RelationType) {
        //GIVEN
        val governedBpn = createLegalEntity("$testName governed")
        val governingBpn = createLegalEntity(
            "$testName governing",
            inactive(stateStart, relationStart.atStartOfDay()),
            active(relationStart.atStartOfDay(), null)
        )

        //WHEN
        val errors = testDataClient.createRelationToErrors(relationType, governedBpn, governingBpn, listOf(firstPeriod))

        //THEN
        assertThat(errors).isEmpty()
    }

    /**
     * GIVEN a governing legal entity whose inactive state ends late on the day the relation starts
     * WHEN a sharing member relates a legal entity to it from that day on
     * THEN the relation is rejected
     */
    @ParameterizedTest
    @EnumSource(value = RelationType::class, names = ["IsOwnedBy", "IsManagedBy"])
    fun `reject relation starting on a day the governing partner is still inactive in`(relationType: RelationType) {
        //GIVEN
        val governedBpn = createLegalEntity("$testName governed")
        val governingBpn = createLegalEntity(
            "$testName governing",
            inactive(stateStart, relationStart.atTime(23, 59)),
            active(relationStart.atTime(23, 59), null)
        )

        //WHEN
        val errors = testDataClient.createRelationToErrors(relationType, governedBpn, governingBpn, listOf(firstPeriod))

        //THEN
        assertThat(errors.map { it.description }).singleElement().asString()
            .contains(governingBpn).contains("recorded as inactive")
    }

    /**
     * GIVEN a governing legal entity recorded as inactive from a date far in the future on
     * WHEN a sharing member relates a legal entity to it without an end date
     * THEN the relation is rejected
     */
    @ParameterizedTest
    @EnumSource(value = RelationType::class, names = ["IsOwnedBy", "IsManagedBy"])
    fun `reject open-ended relation whose governing partner is recorded as inactive in the far future`(relationType: RelationType) {
        //GIVEN
        val governedBpn = createLegalEntity("$testName governed")
        val governingBpn = createLegalEntity(
            "$testName governing",
            active(stateStart, relationStart.plusYears(50).atStartOfDay()),
            inactive(relationStart.plusYears(50).atStartOfDay(), null)
        )

        //WHEN
        val errors = testDataClient.createRelationToErrors(
            relationType,
            governedBpn,
            governingBpn,
            listOf(RelationValidityPeriod(relationStart, null))
        )

        //THEN
        assertThat(errors.map { it.description }).singleElement().asString()
            .contains(governingBpn).contains("recorded as inactive")
    }

    private val stateStart = TestDataV7.currentStateValidFrom

    // A data management relation cannot be stated in the past, so every relation here starts in the future.
    private val relationStart: LocalDate = LocalDate.now().plusYears(1)

    private val firstPeriod = RelationValidityPeriod(relationStart, relationStart.plusYears(1))

    private val secondPeriod = RelationValidityPeriod(relationStart.plusYears(2), relationStart.plusYears(3))

    private fun active(validFrom: LocalDateTime?, validTo: LocalDateTime?) = LegalEntityStateDto(validFrom, validTo, BusinessStateType.ACTIVE)

    private fun inactive(validFrom: LocalDateTime?, validTo: LocalDateTime?) = LegalEntityStateDto(validFrom, validTo, BusinessStateType.INACTIVE)

    // Only a data space participant may manage another legal entity, so every partner created here is one.
    private fun createLegalEntity(seed: String, vararg states: LegalEntityStateDto): String {
        val request = requestFactory.buildLegalEntity(seed).withParticipantData(true)
        val requestWithStates = if (states.isEmpty()) request else request.withStates(states.toList())
        return testDataClient.createLegalEntity(requestWithStates).header.bpnl
    }

    private fun createLegalEntityWithoutStates(seed: String): String =
        testDataClient.createLegalEntity(requestFactory.buildLegalEntity(seed).withParticipantData(true).withStates(emptyList())).header.bpnl
}
