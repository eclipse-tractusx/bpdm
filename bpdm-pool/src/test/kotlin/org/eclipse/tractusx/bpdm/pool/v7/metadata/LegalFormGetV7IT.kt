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

class LegalFormGetV7IT : UnscheduledPoolTestBaseV7() {

    /**
     * GIVEN legal forms
     * WHEN participant requests legal forms
     * THEN participant gets legal forms
     */
    @Test
    fun `search legal forms`() {
        //WHEN
        val response = poolClient.metadata.getLegalForms(PaginationRequest())

        //THEN
        Assertions.assertThat(response.content).isNotEmpty
    }

    /**
     * GIVEN legal forms
     * WHEN participant requests page of legal forms with size 1
     * THEN participant gets exactly one legal form and total count exceeds 1
     */
    @Test
    fun `search legal forms with pagination`() {
        //WHEN
        val response = poolClient.metadata.getLegalForms(PaginationRequest(page = 0, size = 1))

        //THEN
        Assertions.assertThat(response.content).hasSize(1)
        Assertions.assertThat(response.totalElements).isGreaterThan(1)
    }
}
