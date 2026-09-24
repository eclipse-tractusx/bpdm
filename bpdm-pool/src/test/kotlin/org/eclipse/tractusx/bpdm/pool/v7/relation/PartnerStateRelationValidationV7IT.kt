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
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityUpdateError
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.copyWithBpnRequests
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.TestDataV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.withParticipantData
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.withStates
import org.eclipse.tractusx.orchestrator.api.model.BusinessState
import org.eclipse.tractusx.orchestrator.api.model.RelationType
import org.eclipse.tractusx.orchestrator.api.model.RelationValidityPeriod
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Whether a business partner update is accepted depends on whether the states it states contradict the relations the
 * partner already takes part in, so a relation that was valid when shared cannot be made contradictory afterwards.
 */
class PartnerStateRelationValidationV7IT : UnscheduledPoolTestBaseV7() {

    /**
     * GIVEN a legal entity governing another over a validity period
     * WHEN a sharing member records the governing legal entity as inactive part way through that period
     * THEN the update is rejected, naming the relation and the governed legal entity
     */
    @ParameterizedTest
    @EnumSource(value = RelationType::class, names = ["IsOwnedBy", "IsManagedBy"])
    fun `reject deactivating a governing partner while its relation holds`(relationType: RelationType) {
        //GIVEN
        val governedBpn = createLegalEntity("$testName governed")
        val governingBpn = createLegalEntity("$testName governing")
        relate(relationType, governedBpn, governingBpn, relationPeriod)

        //WHEN
        val errors = updateLegalEntityStatesToErrors(governingBpn, active(stateStart, midPeriod), inactive(midPeriod, null))

        //THEN
        val expectedCode = when (relationType) {
            RelationType.IsOwnedBy -> LegalEntityUpdateError.StatesContradictOwnership
            else -> LegalEntityUpdateError.StatesContradictDataManagement
        }
        assertThat(errors).singleElement().satisfies({
            assertThat(it.errorCode).isEqualTo(expectedCode)
            assertThat(it.entityKey).isEqualTo(governingBpn)
            assertThat(it.message).contains(governedBpn).contains(governingBpn).contains(relationPeriod.validFrom.toString())
        })
    }

    /**
     * GIVEN a legal entity governed by another over a validity period
     * WHEN a sharing member records the governed legal entity as inactive during that period
     * THEN the update is accepted
     */
    @ParameterizedTest
    @EnumSource(value = RelationType::class, names = ["IsOwnedBy", "IsManagedBy"])
    fun `accept deactivating a governed partner while its relation holds`(relationType: RelationType) {
        //GIVEN
        val governedBpn = createLegalEntity("$testName governed")
        val governingBpn = createLegalEntity("$testName governing")
        relate(relationType, governedBpn, governingBpn, relationPeriod)

        //WHEN
        val errors = updateLegalEntityStatesToErrors(governedBpn, active(stateStart, midPeriod), inactive(midPeriod, null))

        //THEN
        assertThat(errors).isEmpty()
    }

    /**
     * GIVEN a legal entity governing another over a validity period
     * WHEN a sharing member records the governing legal entity as inactive from the day that period ends on
     * THEN the update is accepted, because a period no longer holds on its end date
     */
    @ParameterizedTest
    @EnumSource(value = RelationType::class, names = ["IsOwnedBy", "IsManagedBy"])
    fun `accept deactivating a governing partner once its relation has ended`(relationType: RelationType) {
        //GIVEN
        val governedBpn = createLegalEntity("$testName governed")
        val governingBpn = createLegalEntity("$testName governing")
        relate(relationType, governedBpn, governingBpn, relationPeriod)
        val periodEnd = relationPeriod.validTo!!.atStartOfDay()

        //WHEN
        val errors = updateLegalEntityStatesToErrors(governingBpn, active(stateStart, periodEnd), inactive(periodEnd, null))

        //THEN
        assertThat(errors).isEmpty()
    }

    /**
     * GIVEN an alternative headquarter designation over a validity period
     * WHEN a sharing member records either of its legal entities as inactive part way through that period
     * THEN the update is rejected, naming the other legal entity of the designation
     */
    @ParameterizedTest
    @EnumSource(DesignatedRole::class)
    fun `reject deactivating a designated headquarter while its designation holds`(role: DesignatedRole) {
        //GIVEN
        val alternativeBpn = createLegalEntity("$testName alternative")
        val mainBpn = createLegalEntity("$testName main")
        relate(RelationType.IsAlternativeHeadquarterFor, alternativeBpn, mainBpn, relationPeriod)
        val (judgedBpn, otherBpn) = when (role) {
            DesignatedRole.ALTERNATIVE -> alternativeBpn to mainBpn
            DesignatedRole.MAIN -> mainBpn to alternativeBpn
        }

        //WHEN
        val errors = updateLegalEntityStatesToErrors(judgedBpn, active(stateStart, midPeriod), inactive(midPeriod, null))

        //THEN
        assertThat(errors).singleElement().satisfies({
            assertThat(it.errorCode).isEqualTo(LegalEntityUpdateError.StatesContradictAlternativeHeadquarter)
            assertThat(it.message).contains(otherBpn).contains(judgedBpn)
        })
    }

    /**
     * GIVEN a succession whose predecessor stopped being active on its start date
     * WHEN a sharing member records the predecessor as active again without end
     * THEN the update is rejected, naming the succession and the successor
     */
    @ParameterizedTest
    @EnumSource(PartnerLevel::class)
    fun `reject reactivating a replaced partner`(level: PartnerLevel) {
        //GIVEN
        val partners = createSuccession(level)

        //WHEN
        val errors = updateStatesToErrors(level, partners.predecessorBpn, active(stateStart, null))

        //THEN
        assertThat(errors).singleElement().satisfies({
            assertThat(it.errorCode.toString()).isEqualTo("StatesContradictSuccession")
            assertThat(it.message).contains(partners.successorBpn).contains(partners.predecessorBpn).contains("active")
        })
    }

    /**
     * GIVEN a succession between two partners
     * WHEN a sharing member records the successor as inactive from the succession's start date on
     * THEN the update is rejected, naming the succession and the predecessor
     */
    @ParameterizedTest
    @EnumSource(PartnerLevel::class)
    fun `reject deactivating a successor on the day it takes over`(level: PartnerLevel) {
        //GIVEN
        val partners = createSuccession(level)

        //WHEN
        val errors = updateStatesToErrors(level, partners.successorBpn, inactive(successionStart.atStartOfDay(), null))

        //THEN
        assertThat(errors).singleElement().satisfies({
            assertThat(it.errorCode.toString()).isEqualTo("StatesContradictSuccession")
            assertThat(it.message).contains(partners.predecessorBpn).contains(partners.successorBpn).contains("inactive")
        })
    }

    /**
     * GIVEN a legal entity that takes part in no relation
     * WHEN a sharing member records it as inactive
     * THEN the update is accepted
     */
    @Test
    fun `accept deactivating a partner without relations`() {
        //GIVEN
        val bpn = createLegalEntity(testName)

        //WHEN
        val errors = updateLegalEntityStatesToErrors(bpn, active(stateStart, midPeriod), inactive(midPeriod, null))

        //THEN
        assertThat(errors).isEmpty()
    }

    /**
     * GIVEN one legal entity owning another and one legal entity without relations
     * WHEN a sharing member records both as inactive in one request
     * THEN only the owner is rejected and the other legal entity is updated
     */
    @Test
    fun `reject only the conflicting entry of a batch`() {
        //GIVEN
        val ownedBpn = createLegalEntity("$testName owned")
        val ownerBpn = createLegalEntity("$testName owner")
        val unrelatedBpn = createLegalEntity("$testName unrelated")
        relate(RelationType.IsOwnedBy, ownedBpn, ownerBpn, relationPeriod)
        val deactivation = listOf(active(stateStart, midPeriod), inactive(midPeriod, null)).map { LegalEntityStateDto(it.validFrom, it.validTo, it.type) }

        //WHEN
        val response = poolClient.legalEntities.updateBusinessPartners(
            listOf(
                requestFactory.buildLegalEntityUpdateRequest("$testName owner update", ownerBpn).withStates(deactivation),
                requestFactory.buildLegalEntityUpdateRequest("$testName unrelated update", unrelatedBpn).withStates(deactivation)
            )
        )

        //THEN
        assertThat(response.entities.map { it.legalEntity.header.bpnl }).containsExactly(unrelatedBpn)
        assertThat(response.errors.map { it.entityKey }).containsExactly(ownerBpn)
    }

    /**
     * GIVEN a succession from an additional address to the legal address of the same legal entity
     * WHEN a sharing member records the legal address as inactive from the succession's start date on in a legal entity update
     * THEN the update is rejected with the legal address error code
     */
    @Test
    fun `reject deactivating a legal address that takes over from another address`() {
        //GIVEN
        val legalEntity = testDataClient.createLegalEntity(testName)
        val predecessorBpn = testDataClient.createAdditionalAddress(legalEntity, "$testName predecessor").address.bpna
        val legalAddressBpn = legalEntity.legalAddress.bpna
        endActivityOnSuccessionStart(PartnerLevel.ADDRESS, predecessorBpn)
        replace(predecessorBpn, legalAddressBpn)

        //WHEN
        val request = requestFactory.buildLegalEntityUpdateRequest("$testName update", legalEntity.header.bpnl)
        val deactivating = request.copy(
            legalEntity = request.legalEntity.copy(
                legalAddress = request.legalEntity.legalAddress.copy(states = listOf(AddressStateDto(successionStart.atStartOfDay(), null, BusinessStateType.INACTIVE)))
            )
        )
        val errors = poolClient.legalEntities.updateBusinessPartners(listOf(deactivating)).errors

        //THEN
        assertThat(errors).singleElement().satisfies({
            assertThat(it.errorCode).isEqualTo(LegalEntityUpdateError.LegalAddressStatesContradictSuccession)
            assertThat(it.message).contains(predecessorBpn).contains(legalAddressBpn)
        })
    }

    /**
     * GIVEN a legal entity owning another, shared through the golden record process
     * WHEN a golden record task records the owner as inactive while the ownership holds
     * THEN the task is rejected with a description that names the owned legal entity within the length that reaches the
     * sharing member
     */
    @Test
    fun `reject a golden record task deactivating an owner`() {
        //GIVEN
        val activeFromStart = BusinessState(stateStart.toInstant(ZoneOffset.UTC), null, BusinessStateType.ACTIVE)
        val ownerTask = orchestratorRequestFactory.buildLegalEntityBusinessPartner("$testName owner").copyWithBpnRequests()
        val ownerRecord = testDataClient.processTask(
            "$testName owner",
            ownerTask.copy(legalEntity = ownerTask.legalEntity.copy(states = listOf(activeFromStart)))
        )
        val ownerBpn = ownerRecord.legalEntity.bpnReference.referenceValue!!
        val ownedBpn = createLegalEntity("$testName owned")
        relate(RelationType.IsOwnedBy, ownedBpn, ownerBpn, relationPeriod)

        //WHEN
        val deactivation = listOf(
            BusinessState(stateStart.toInstant(ZoneOffset.UTC), midPeriod.toInstant(ZoneOffset.UTC), BusinessStateType.ACTIVE),
            BusinessState(midPeriod.toInstant(ZoneOffset.UTC), null, BusinessStateType.INACTIVE)
        )
        val errors = testDataClient.processTaskToErrors(
            "$testName owner",
            ownerRecord.copy(legalEntity = ownerRecord.legalEntity.copy(states = deactivation, hasChanged = true))
        )

        //THEN
        assertThat(errors).singleElement().satisfies({
            assertThat(it.description).contains(ownedBpn).contains(ownerBpn)
            assertThat(it.description.length).isLessThanOrEqualTo(250)
        })
    }

    enum class PartnerLevel { LEGAL_ENTITY, SITE, ADDRESS }

    enum class DesignatedRole { ALTERNATIVE, MAIN }

    private data class SuccessionPartners(val predecessorBpn: String, val successorBpn: String)

    private val stateStart = TestDataV7.currentStateValidFrom

    // A data management relation cannot be stated in the past, so every governing relation here starts in the future.
    private val relationStart: LocalDate = LocalDate.now().plusYears(1)

    private val relationPeriod = RelationValidityPeriod(relationStart, relationStart.plusYears(1))

    private val midPeriod: LocalDateTime = relationStart.plusMonths(6).atStartOfDay()

    private val successionStart: LocalDate = LocalDate.of(2024, 6, 1)

    private fun active(validFrom: LocalDateTime?, validTo: LocalDateTime?) = GivenState(validFrom, validTo, BusinessStateType.ACTIVE)

    private fun inactive(validFrom: LocalDateTime?, validTo: LocalDateTime?) = GivenState(validFrom, validTo, BusinessStateType.INACTIVE)

    private data class GivenState(val validFrom: LocalDateTime?, val validTo: LocalDateTime?, val type: BusinessStateType)

    // Only a data space participant may manage another legal entity, so every legal entity created here is one.
    private fun createLegalEntity(seed: String): String =
        testDataClient.createLegalEntity(requestFactory.buildLegalEntity(seed).withParticipantData(true)).header.bpnl

    private fun relate(relationType: RelationType, sourceBpn: String, targetBpn: String, validityPeriod: RelationValidityPeriod) {
        val errors = testDataClient.createRelationToErrors(relationType, sourceBpn, targetBpn, listOf(validityPeriod))
        check(errors.isEmpty()) { "Could not relate '$sourceBpn' $relationType '$targetBpn': $errors" }
    }

    private fun replace(predecessorBpn: String, successorBpn: String) {
        val errors = testDataClient.createSuccessionToErrors(predecessorBpn, successorBpn, successionStart)
        check(errors.isEmpty()) { "Could not replace '$predecessorBpn' by '$successorBpn': $errors" }
    }

    private fun createSuccession(level: PartnerLevel): SuccessionPartners {
        val legalEntity = testDataClient.createLegalEntity("$testName $level")
        val partners = when (level) {
            PartnerLevel.LEGAL_ENTITY -> SuccessionPartners(legalEntity.header.bpnl, createLegalEntity("$testName $level successor"))
            PartnerLevel.SITE -> SuccessionPartners(
                testDataClient.createSite(legalEntity, "$testName $level predecessor").site.bpns,
                testDataClient.createSite(legalEntity, "$testName $level successor").site.bpns
            )
            PartnerLevel.ADDRESS -> SuccessionPartners(
                testDataClient.createAdditionalAddress(legalEntity, "$testName $level predecessor").address.bpna,
                testDataClient.createAdditionalAddress(legalEntity, "$testName $level successor").address.bpna
            )
        }
        endActivityOnSuccessionStart(level, partners.predecessorBpn)
        replace(partners.predecessorBpn, partners.successorBpn)
        return partners
    }

    private fun endActivityOnSuccessionStart(level: PartnerLevel, bpn: String) {
        val errors = updateStatesToErrors(level, bpn, active(stateStart, successionStart.atStartOfDay()))
        check(errors.isEmpty()) { "Could not end the activity of '$bpn': $errors" }
    }

    private fun updateLegalEntityStatesToErrors(bpn: String, vararg states: GivenState): List<ErrorInfo<*>> =
        updateStatesToErrors(PartnerLevel.LEGAL_ENTITY, bpn, *states)

    private fun updateStatesToErrors(level: PartnerLevel, bpn: String, vararg states: GivenState): List<ErrorInfo<*>> {
        val seed = "$testName $bpn states"

        return when (level) {
            PartnerLevel.LEGAL_ENTITY -> poolClient.legalEntities.updateBusinessPartners(
                listOf(
                    requestFactory.buildLegalEntityUpdateRequest(seed, bpn)
                        .withParticipantData(true)
                        .withStates(states.map { LegalEntityStateDto(it.validFrom, it.validTo, it.type) })
                )
            ).errors.toList()
            PartnerLevel.SITE -> poolClient.sites.updateSite(
                listOf(requestFactory.createSiteUpdateRequest(seed, bpn).withStates(states.map { SiteStateDto(it.validFrom, it.validTo, it.type) }))
            ).errors.toList()
            PartnerLevel.ADDRESS -> poolClient.addresses.updateAddresses(
                listOf(requestFactory.buildAddressUpdateRequest(seed, bpn).withStates(states.map { AddressStateDto(it.validFrom, it.validTo, it.type) }))
            ).errors.toList()
        }
    }
}
