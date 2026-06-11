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

package org.eclipse.tractusx.bpdm.pool.v7.metadata

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.ReasonCodeDto
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.junit.jupiter.api.Test

class ReasonCodeGetV7IT : UnscheduledPoolTestBaseV7() {

    /**
     * GIVEN the Pool is freshly deployed with the default Flyway migration
     * WHEN an operator requests reason codes
     * THEN the default reason code HEADQUARTER_RELOCATION is present
     */
    @Test
    fun `default reason codes are seeded and returned by GET`() {
        //WHEN
        val response = poolClient.metadata.getReasonCodes(PaginationRequest(page = 0, size = 100))

        //THEN
        val expected = PageDto(5L, 1, 0, 5, listOf(
            ReasonCodeDto("HEADQUARTER_RELOCATION", "Complete relocation of a legal entity headquarter to a new physical location"),
            ReasonCodeDto("LEGAL_ENTITY_COURT_DISTRICT_CHANGE", "Re-registration of a legal entity in a different court district, generating a new BPNL with new legal registration values"),
            ReasonCodeDto("MERGER", "Merger of two or more legal entities into a single successor entity"),
            ReasonCodeDto("SPLIT_SPIN_OFF", "Split or spin-off of a legal entity into one or more new entities"),
            ReasonCodeDto("INSOLVENCY_ABSORPTION", "Absorption of an insolvent business partner's remaining assets by another entity"),
        ))
        assertThat(response).isEqualTo(expected)
    }



}