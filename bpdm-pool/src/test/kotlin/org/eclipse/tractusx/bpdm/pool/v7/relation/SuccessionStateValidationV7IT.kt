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
import org.eclipse.tractusx.bpdm.pool.api.model.AddressStateDto
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityStateDto
import org.eclipse.tractusx.bpdm.pool.api.model.SiteStateDto
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.TestDataV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.withStates
import org.eclipse.tractusx.orchestrator.api.model.RelationValidityPeriod
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Whether a succession is accepted depends on what its two partners record about being in use, and the rule reads the
 * same at legal entity, site and address level.
 */
class SuccessionStateValidationV7IT : UnscheduledPoolTestBaseV7() {

    /**
     * GIVEN a predecessor recorded as active without end
     * WHEN a sharing member replaces it
     * THEN the succession is rejected, naming the predecessor
     */
    @ParameterizedTest
    @EnumSource(SuccessionLevel::class)
    fun `reject succession whose predecessor is still recorded as active on the start date`(level: SuccessionLevel) {
        //GIVEN
        val partners = createPartners(level)

        //WHEN
        val errors = testDataClient.createSuccessionToErrors(partners.predecessorBpn, partners.successorBpn, successionStart)

        //THEN
        assertThat(errors.map { it.description }).singleElement().asString()
            .contains(partners.predecessorBpn).contains("still recorded as active")
    }

    /**
     * GIVEN a predecessor recorded as active again after the succession starts
     * WHEN a sharing member replaces it from before that
     * THEN the succession is rejected, naming the predecessor
     */
    @ParameterizedTest
    @EnumSource(SuccessionLevel::class)
    fun `reject succession whose predecessor is recorded as active after the start date`(level: SuccessionLevel) {
        //GIVEN
        val partners = createPartners(level)
        recordStates(level, partners.predecessorBpn, active(stateStart, successionStart.minusMonths(1).atStartOfDay()), active(successionStart.plusMonths(1).atStartOfDay(), null))

        //WHEN
        val errors = testDataClient.createSuccessionToErrors(partners.predecessorBpn, partners.successorBpn, successionStart)

        //THEN
        assertThat(errors.map { it.description }).singleElement().asString()
            .contains(partners.predecessorBpn).contains("still recorded as active")
    }

    /**
     * GIVEN a predecessor whose active period ends on the day the succession starts
     * WHEN a sharing member replaces it from that day
     * THEN the succession is accepted
     */
    @ParameterizedTest
    @EnumSource(SuccessionLevel::class)
    fun `accept succession starting on the day the predecessor stops being active`(level: SuccessionLevel) {
        //GIVEN
        val partners = createPartners(level)
        recordStates(level, partners.predecessorBpn, active(stateStart, successionStart.atStartOfDay()))

        //WHEN
        val errors = testDataClient.createSuccessionToErrors(partners.predecessorBpn, partners.successorBpn, successionStart)

        //THEN
        assertThat(errors).isEmpty()
    }

    /**
     * GIVEN a successor recorded as inactive on the day the succession starts
     * WHEN a sharing member has it take the predecessor's place
     * THEN the succession is rejected, naming the successor
     */
    @ParameterizedTest
    @EnumSource(SuccessionLevel::class)
    fun `reject succession whose successor is recorded as inactive on the start date`(level: SuccessionLevel) {
        //GIVEN
        val partners = createPartners(level)
        recordStates(level, partners.predecessorBpn, active(stateStart, successionStart.atStartOfDay()))
        recordStates(level, partners.successorBpn, inactive(successionStart.atStartOfDay(), null))

        //WHEN
        val errors = testDataClient.createSuccessionToErrors(partners.predecessorBpn, partners.successorBpn, successionStart)

        //THEN
        assertThat(errors.map { it.description }).singleElement().asString()
            .contains(partners.successorBpn).contains("recorded as inactive")
    }

    /**
     * GIVEN two partners that record no states at all
     * WHEN a sharing member replaces one by the other
     * THEN the succession is accepted
     */
    @ParameterizedTest
    @EnumSource(SuccessionLevel::class)
    fun `accept succession between partners that record no states`(level: SuccessionLevel) {
        //GIVEN
        val partners = createPartners(level)
        recordStates(level, partners.predecessorBpn)
        recordStates(level, partners.successorBpn)

        //WHEN
        val errors = testDataClient.createSuccessionToErrors(partners.predecessorBpn, partners.successorBpn, successionStart)

        //THEN
        assertThat(errors).isEmpty()
    }

    /**
     * GIVEN a predecessor whose states leave the succession's start date uncovered
     * WHEN a sharing member replaces it from that date
     * THEN the succession is accepted
     */
    @ParameterizedTest
    @EnumSource(SuccessionLevel::class)
    fun `accept succession starting in a gap between the predecessor's states`(level: SuccessionLevel) {
        //GIVEN
        val partners = createPartners(level)
        recordStates(
            level,
            partners.predecessorBpn,
            active(stateStart, successionStart.minusMonths(1).atStartOfDay()),
            inactive(successionStart.plusMonths(1).atStartOfDay(), null)
        )

        //WHEN
        val errors = testDataClient.createSuccessionToErrors(partners.predecessorBpn, partners.successorBpn, successionStart)

        //THEN
        assertThat(errors).isEmpty()
    }

    /**
     * GIVEN a predecessor recorded as active and as inactive over the same time
     * WHEN a sharing member replaces it
     * THEN the succession is rejected, because the contradicting active record still counts
     */
    @ParameterizedTest
    @EnumSource(SuccessionLevel::class)
    fun `reject succession whose predecessor contradicts itself over the start date`(level: SuccessionLevel) {
        //GIVEN
        val partners = createPartners(level)
        recordStates(level, partners.predecessorBpn, active(stateStart, null), inactive(stateStart, null))

        //WHEN
        val errors = testDataClient.createSuccessionToErrors(partners.predecessorBpn, partners.successorBpn, successionStart)

        //THEN
        assertThat(errors.map { it.description }).singleElement().asString()
            .contains(partners.predecessorBpn).contains("still recorded as active")
    }

    /**
     * GIVEN two legal entities ready to be related
     * WHEN a sharing member states a succession that ends
     * THEN the succession is rejected
     */
    @Test
    fun `reject succession that carries an end date`() {
        //GIVEN
        val partners = createPartners(SuccessionLevel.LEGAL_ENTITY)
        recordStates(SuccessionLevel.LEGAL_ENTITY, partners.predecessorBpn, active(stateStart, successionStart.atStartOfDay()))

        //WHEN
        val errors = testDataClient.createSuccessionToErrors(
            partners.predecessorBpn,
            partners.successorBpn,
            successionStart,
            successionStart.plusYears(1)
        )

        //THEN
        assertThat(errors.map { it.description }).singleElement().asString().contains("does not end")
    }

    /**
     * GIVEN two legal entities ready to be related
     * WHEN a sharing member states a succession over two validity periods
     * THEN the succession is rejected
     */
    @Test
    fun `reject succession that states more than one validity period`() {
        //GIVEN
        val partners = createPartners(SuccessionLevel.LEGAL_ENTITY)
        recordStates(SuccessionLevel.LEGAL_ENTITY, partners.predecessorBpn, active(stateStart, successionStart.atStartOfDay()))

        //WHEN
        val errors = testDataClient.createSuccessionToErrors(
            partners.predecessorBpn,
            partners.successorBpn,
            listOf(RelationValidityPeriod(successionStart, null), RelationValidityPeriod(successionStart.plusYears(2), null))
        )

        //THEN
        assertThat(errors.map { it.description }).singleElement().asString().contains("states one validity period")
    }

    enum class SuccessionLevel { LEGAL_ENTITY, SITE, ADDRESS }

    private data class SuccessionPartners(val predecessorBpn: String, val successorBpn: String)

    private val stateStart = TestDataV7.currentStateValidFrom

    private val successionStart: LocalDate = LocalDate.of(2024, 6, 1)

    private fun active(validFrom: LocalDateTime?, validTo: LocalDateTime?) = GivenState(validFrom, validTo, BusinessStateType.ACTIVE)

    private fun inactive(validFrom: LocalDateTime?, validTo: LocalDateTime?) = GivenState(validFrom, validTo, BusinessStateType.INACTIVE)

    private data class GivenState(val validFrom: LocalDateTime?, val validTo: LocalDateTime?, val type: BusinessStateType)

    private fun createPartners(level: SuccessionLevel): SuccessionPartners {
        val legalEntity = testDataClient.createLegalEntity("$testName $level")

        return when (level) {
            SuccessionLevel.LEGAL_ENTITY -> SuccessionPartners(legalEntity.header.bpnl, testDataClient.createLegalEntity("$testName $level successor").header.bpnl)
            SuccessionLevel.SITE -> SuccessionPartners(
                testDataClient.createSite(legalEntity, "$testName $level predecessor").site.bpns,
                testDataClient.createSite(legalEntity, "$testName $level successor").site.bpns
            )
            SuccessionLevel.ADDRESS -> SuccessionPartners(
                testDataClient.createAdditionalAddress(legalEntity, "$testName $level predecessor").address.bpna,
                testDataClient.createAdditionalAddress(legalEntity, "$testName $level successor").address.bpna
            )
        }
    }

    private fun recordStates(level: SuccessionLevel, bpn: String, vararg states: GivenState) {
        val seed = "$testName $bpn states"

        when (level) {
            SuccessionLevel.LEGAL_ENTITY -> testDataClient.updateLegalEntity(
                requestFactory.buildLegalEntityUpdateRequest(seed, bpn)
                    .withStates(states.map { LegalEntityStateDto(it.validFrom, it.validTo, it.type) })
            )
            SuccessionLevel.SITE -> testDataClient.updateSite(
                requestFactory.createSiteUpdateRequest(seed, bpn)
                    .withStates(states.map { SiteStateDto(it.validFrom, it.validTo, it.type) })
            )
            SuccessionLevel.ADDRESS -> testDataClient.updateAddress(
                requestFactory.buildAddressUpdateRequest(seed, bpn)
                    .withStates(states.map { AddressStateDto(it.validFrom, it.validTo, it.type) })
            )
        }
    }
}
