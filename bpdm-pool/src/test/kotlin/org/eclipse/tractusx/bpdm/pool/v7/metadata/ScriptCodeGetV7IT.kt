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

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.junit.jupiter.api.Test

class ScriptCodeGetV7IT : UnscheduledPoolTestBaseV7() {

    /**
     * GIVEN script codes
     * WHEN participant requests available script codes
     * THEN participant gets script codes
     */
    @Test
    fun `search script codes`() {
        //WHEN
        val response = poolClient.metadata.getScriptCodes(PaginationRequest())

        //THEN
        Assertions.assertThat(response.content).isNotEmpty
    }

    /**
     * GIVEN script codes
     * WHEN participant requests page of script codes with size 1
     * THEN participant gets exactly one script code and total count exceeds 1
     */
    @Test
    fun `search script codes with pagination`() {
        //WHEN
        val response = poolClient.metadata.getScriptCodes(PaginationRequest(page = 0, size = 1))

        //THEN
        Assertions.assertThat(response.content).hasSize(1)
        Assertions.assertThat(response.totalElements).isGreaterThan(1)
    }

    /**
     * GIVEN script codes
     * WHEN participant requests script codes
     * THEN all returned entries have non-blank technical keys
     */
    @Test
    fun `search script codes returns valid entries`() {
        //WHEN
        val response = poolClient.metadata.getScriptCodes(PaginationRequest(size = 100))

        //THEN
        response.content.forEach { scriptCode ->
            Assertions.assertThat(scriptCode.technicalKey).isNotBlank
        }
    }
}
