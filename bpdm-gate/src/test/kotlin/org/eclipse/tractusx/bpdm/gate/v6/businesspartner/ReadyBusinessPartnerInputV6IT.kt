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

package org.eclipse.tractusx.bpdm.gate.v6.businesspartner

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.v6.model.SharingStateTypeV6
import org.eclipse.tractusx.bpdm.gate.api.v6.model.request.PostSharingStateReadyV6Request
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.SharingStateV6Dto
import org.eclipse.tractusx.bpdm.gate.v6.UnscheduledGateV6TestBase
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClientResponseException

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
class ReadyBusinessPartnerInputV6IT: UnscheduledGateV6TestBase() {


    /**
     * GIVEN business partner not ready to be shared
     * WHEN input manager makes business partner ready to be shared
     * THEN input manager sees that business partner is ready
     */
    @Test
    fun `ready uploaded input for sharing`(){
        //GIVEN
        testDataClient.createBusinessPartnerInput(testName)

        //WHEN
        val request = PostSharingStateReadyV6Request(listOf(testName))
        gateClient.sharingStates.postSharingStateReady(request)

        //THEN
        val actualSharingStateResponse = gateClient.sharingStates.getSharingStates(PaginationRequest(), listOf(testName))

        val expectedSharingState = SharingStateV6Dto(testName, SharingStateTypeV6.Ready, null, null, null, null)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedSharingState))

        assertRepo.assertSharingStates(actualSharingStateResponse, expectedResponse)
    }

    /**
     * GIVEN business partner ready to be shared
     * WHEN input manager tries to make business partner ready again
     * THEN input manager sees 400 BAD REQUEST error
     */
    @Test
    fun `try ready already business partner`(){
        //GIVEN
        testDataClient.createBusinessPartnerInput(testName)
        gateClient.sharingStates.postSharingStateReady(PostSharingStateReadyV6Request(listOf(testName)))

        //WHEN THEN
        val request = PostSharingStateReadyV6Request(listOf(testName))
        Assertions.assertThatThrownBy { gateClient.sharingStates.postSharingStateReady(request) }
            .isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }

}