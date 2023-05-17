/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.gate.controller

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateDto
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.response.LsaType
import org.eclipse.tractusx.bpdm.gate.util.DbTestHelpers
import org.eclipse.tractusx.bpdm.gate.util.PostgreSQLContextInitializer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class SharingStateControllerIT @Autowired constructor(

    private val testHelpers: DbTestHelpers,
    val gateClient: GateClient,
) {

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
    }

    /**
     * Given externalId exists in database
     * When getting changeLog by external id
     * Then changeLog mapped to the catena data model should be returned
     */
    @Test
    fun `get changeLog by external id`() {

        val state1 = SharingStateDto(
            lsaType = LsaType.Address, externalId = "exId1", sharingStateType = SharingStateType.Success,
            sharingErrorCode = null, sharingProcessStarted = null, sharingErrorMessage = null, bpn = null
        )
        gateClient.sharingState().upsertSharingState(state1)

        val searchResult = gateClient.sharingState().getSharingStates(PaginationRequest(), LsaType.Address, listOf("exId1"))
        Assertions.assertThat(searchResult.content.size).isEqualTo(1)
        val searchResult2 = gateClient.sharingState().getSharingStates(PaginationRequest(), LsaType.Site, listOf("exId1"))
        Assertions.assertThat(searchResult2.content.size).isEqualTo(0)
    }
}