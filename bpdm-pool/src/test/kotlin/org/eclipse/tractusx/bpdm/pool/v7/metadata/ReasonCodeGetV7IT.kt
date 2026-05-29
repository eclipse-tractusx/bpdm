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
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
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
        val expectedTechnicalKeys = setOf(
            "HEADQUARTER_RELOCATION"
        )

        val response = poolClient.metadata.getReasonCodes(PaginationRequest(page = 0, size = 100))

        val actualTechnicalKeys = response.content.map { it.technicalKey }.toSet()
        assertThat(actualTechnicalKeys).containsAll(expectedTechnicalKeys)
    }



}