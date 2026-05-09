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
import org.eclipse.tractusx.bpdm.pool.api.model.request.ReasonCodeDeleteRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.ReasonCodeUpsertRequest
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.junit.jupiter.api.Test

class ReasonCodeGetV7IT: UnscheduledPoolTestBaseV7() {

    /**
     * GIVEN reason code
     * WHEN operator searches for reason codes
     * THEN operator finds reason code
     */
    @Test
    fun `find created reason code`(){
        //GIVEN
        val request = ReasonCodeUpsertRequest(ReasonCodeDto(testName, "$testName description"))
        poolClient.metadata.upsertReasonCode(request)

        //WHEN
        val response = poolClient.metadata.getReasonCodes(PaginationRequest())

        //THEN
        val expected = PageDto(1, 1, 0, 1, listOf(request.reasonCode))
        assertThat(response).isEqualTo(expected)
    }

    /**
     * GIVEN updated reason code
     * WHEN operator searches for reason codes
     * THEN operator finds updated reason code
     */
    @Test
    fun `find updated reason code`(){
        //GIVEN
        val createRequest = ReasonCodeUpsertRequest(ReasonCodeDto(testName, "$testName description"))
        poolClient.metadata.upsertReasonCode(createRequest)

        val updateRequest = ReasonCodeUpsertRequest(ReasonCodeDto(createRequest.reasonCode.technicalKey, "$testName updated description"))
        poolClient.metadata.upsertReasonCode(updateRequest)


        //WHEN
        val response = poolClient.metadata.getReasonCodes(PaginationRequest())

        //THEN
        val expected = PageDto(1, 1, 0, 1, listOf(updateRequest.reasonCode))
        assertThat(response).isEqualTo(expected)
    }

}