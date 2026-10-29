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
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.TestDataV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.withStates
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * A former legal address may still be succeeded after its headquarter relocated, but an address a succession replaces
 * cannot relocate the headquarter over the same time.
 */
class HeadquarterRelocationV7IT : UnscheduledPoolTestBaseV7() {

    /**
     * GIVEN a headquarter relocation that has taken effect, and the former legal address recorded as active until a
     * later date
     * WHEN a sharing member replaces the former legal address by another address from that date
     * THEN the succession is accepted next to the relocation
     */
    @Test
    fun `accept succession of a former legal address after the headquarter relocated`() {
        //GIVEN
        val legalEntity = testDataClient.createLegalEntity(testName)
        val formerLegalAddressBpn = legalEntity.legalAddress.bpna
        val newLegalAddressBpn = testDataClient.createAdditionalAddress(legalEntity, "$testName new legal address").address.bpna
        val successorBpn = testDataClient.createAdditionalAddress(legalEntity, "$testName successor").address.bpna
        relocate(formerLegalAddressBpn, newLegalAddressBpn, pastRelocationStart)
        recordActiveUntil(formerLegalAddressBpn, pastSuccessionStart)

        //WHEN
        val errors = testDataClient.createSuccessionToErrors(formerLegalAddressBpn, successorBpn, pastSuccessionStart)

        //THEN
        assertThat(errors).isEmpty()
    }

    /**
     * GIVEN a headquarter relocation to an additional address that starts in the future
     * WHEN a sharing member replaces that additional address by another one from a later date
     * THEN the succession is accepted
     */
    @Test
    fun `accept succession of an address a pending headquarter relocation makes the legal address`() {
        //GIVEN
        val legalEntity = testDataClient.createLegalEntity(testName)
        val newLegalAddressBpn = testDataClient.createAdditionalAddress(legalEntity, "$testName new legal address").address.bpna
        val successorBpn = testDataClient.createAdditionalAddress(legalEntity, "$testName successor").address.bpna
        relocate(legalEntity.legalAddress.bpna, newLegalAddressBpn, futureRelocationStart)
        recordActiveUntil(newLegalAddressBpn, futureSuccessionStart)

        //WHEN
        val errors = testDataClient.createSuccessionToErrors(newLegalAddressBpn, successorBpn, futureSuccessionStart)

        //THEN
        assertThat(errors).isEmpty()
    }

    /**
     * GIVEN a legal address that a succession replaces from a future date
     * WHEN a sharing member relocates the headquarter from it to another address over an overlapping time
     * THEN the relocation is rejected, naming the address that already replaces it
     */
    @Test
    fun `reject headquarter relocation from a legal address a succession replaces`() {
        //GIVEN
        val legalEntity = testDataClient.createLegalEntity(testName)
        val newLegalAddressBpn = testDataClient.createAdditionalAddress(legalEntity, "$testName new legal address").address.bpna
        val successorBpn = testDataClient.createAdditionalAddress(legalEntity, "$testName successor").address.bpna
        val relocationTargetBpn = testDataClient.createAdditionalAddress(legalEntity, "$testName relocation target").address.bpna
        recordActiveUntil(newLegalAddressBpn, futureSuccessionStart)
        val successionErrors = testDataClient.createSuccessionToErrors(newLegalAddressBpn, successorBpn, futureSuccessionStart)
        check(successionErrors.isEmpty()) { "Could not replace '$newLegalAddressBpn': $successionErrors" }
        relocate(legalEntity.legalAddress.bpna, newLegalAddressBpn, pastRelocationStart)

        //WHEN
        val errors = testDataClient.createSuccessionToErrors(newLegalAddressBpn, relocationTargetBpn, futureRelocationStart)

        //THEN
        assertThat(errors.map { it.description }).singleElement().asString()
            .contains(newLegalAddressBpn).contains("already replaced by '$successorBpn'")
    }

    private val pastRelocationStart: LocalDate = LocalDate.of(2024, 6, 1)

    private val pastSuccessionStart: LocalDate = LocalDate.of(2025, 6, 1)

    private val futureRelocationStart: LocalDate = LocalDate.now().plusYears(1)

    private val futureSuccessionStart: LocalDate = LocalDate.now().plusYears(2)

    private fun relocate(legalAddressBpn: String, newLegalAddressBpn: String, validFrom: LocalDate) {
        val errors = testDataClient.createSuccessionToErrors(legalAddressBpn, newLegalAddressBpn, validFrom)
        check(errors.isEmpty()) { "Could not relocate the headquarter: $errors" }
    }

    private fun recordActiveUntil(addressBpn: String, validTo: LocalDate) {
        testDataClient.updateAddress(
            requestFactory.buildAddressUpdateRequest("$testName $addressBpn states", addressBpn)
                .withStates(listOf(AddressStateDto(TestDataV7.currentStateValidFrom, validTo.atStartOfDay(), BusinessStateType.ACTIVE)))
        )
    }
}
