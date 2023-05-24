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

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.exception.BusinessPartnerSharingError
import org.eclipse.tractusx.bpdm.gate.api.exception.BusinessPartnerSharingError.*
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
import java.time.LocalDateTime

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

    @Test
    fun `insert and get sharing states `() {

        val stateAddress = insertSharingStateSuccess(LsaType.Address, externalId = "exIdAddress")
        val stateSite = insertSharingStateSuccess(LsaType.Site, externalId = "exIdSite")
        val stateLegalEntity1 = insertSharingStateSuccess(LsaType.LegalEntity, externalId = "exIdEntity1")
        val stateLegalEntity2 = insertSharingStateSuccess(LsaType.LegalEntity, externalId = "exIdEntity2")
        insertSharingStateSuccess(LsaType.Address, externalId = "exIdMultiple")
        insertSharingStateSuccess(LsaType.Site, externalId = "exIdMultiple")
        insertSharingStateSuccess(LsaType.LegalEntity, externalId = "exIdMultiple")

        val searchAddressById = readSharingStates(LsaType.Address, "exIdAddress")
        assertThat(searchAddressById).hasSize(1)
        assertThat(searchAddressById.first()).isEqualTo(stateAddress)

        val searchSitesById = readSharingStates(LsaType.Site, "exIdSite")
        assertThat(searchSitesById).hasSize(1)
        assertThat(searchSitesById.first()).isEqualTo(stateSite)

        val searchAddressWrongId = readSharingStates(LsaType.Address, "exIdEntity")
        assertThat(searchAddressWrongId).hasSize(0)

        val searchEntityMultiple = readSharingStates(LsaType.LegalEntity, "exIdEntity1", "exIdEntity2")
        assertThat(searchEntityMultiple).hasSize(2)

        val searchEntitySingle = readSharingStates(LsaType.LegalEntity, "exIdEntity2")
        assertThat(searchEntitySingle).hasSize(1)
        assertThat(searchEntitySingle.first()).isEqualTo(stateLegalEntity2)

        val searchAll = readSharingStates(null)
        assertThat(searchAll).hasSize(7)

        val searchEntityAllLegalEntities = readSharingStates(LsaType.LegalEntity)
        assertThat(searchEntityAllLegalEntities).hasSize(3)
        assertThat(searchEntityAllLegalEntities).extracting(SharingStateDto::externalId.name)
            .contains(stateLegalEntity1.externalId, stateLegalEntity2.externalId, "exIdMultiple")

        val searchAllWithSameId = readSharingStates(null, "exIdMultiple")
        assertThat(searchAllWithSameId).hasSize(3)
        assertThat(searchAllWithSameId).extracting(SharingStateDto::externalId.name)
            .containsOnly("exIdMultiple")

    }

    @Test
    fun `insert and get sharing states with error code`() {

        val stateAddress1 = insertSharingStateError(LsaType.Address, externalId = "exIdAddress1", errorCode = SharingTimeout)
        insertSharingStateError(LsaType.Address, externalId = "exIdAddress2", errorCode = SharingProcessError)
        insertSharingStateError(LsaType.Address, externalId = "exIdAddress3", errorCode = BpnNotInPool)

        val searchAddress = readSharingStates(LsaType.Address, "exIdAddress1")
        assertThat(searchAddress).hasSize(1)
        assertThat(searchAddress.first()).isEqualTo(stateAddress1)
    }

    @Test
    fun `insert and update states`() {

        val stateAddress1 = insertSharingStateError(LsaType.Address, externalId = "exIdAddress1", errorCode = SharingTimeout)
        insertSharingStateError(LsaType.Address, externalId = "exIdAddress2", errorCode = SharingProcessError)
        insertSharingStateError(LsaType.Address, externalId = "exIdAddress3", errorCode = BpnNotInPool)

        val searchAddress = readSharingStates(LsaType.Address, "exIdAddress1")
        assertThat(searchAddress).hasSize(1)
        assertThat(searchAddress.first()).isEqualTo(stateAddress1)

        val updatedAddress1 = stateAddress1.copy(
            sharingStateType = SharingStateType.Success,
            sharingErrorCode = BpnNotInPool,
            sharingProcessStarted = LocalDateTime.now().withNano(0),
            sharingErrorMessage = "Changed ",
            bpn = null
        )

        gateClient.sharingState().upsertSharingState(updatedAddress1)

        val readUpdatedAddress = readSharingStates(LsaType.Address, "exIdAddress1")
        assertThat(readUpdatedAddress).hasSize(1)
        assertThat(readUpdatedAddress.first()).isEqualTo(updatedAddress1)
    }

    @Test
    fun `insert and update states with sharingProcessStarted`() {

        val startTime = LocalDateTime.now().withNano(0)
        val stateAddress1 = insertSharingStateSuccess(
            lsaType = LsaType.Address, externalId = "exIdAddress1",
            sharingProcessStarted = startTime
        )

        val readInsertedAddress = readSharingStates(LsaType.Address, "exIdAddress1")
        assertThat(readInsertedAddress.first().sharingProcessStarted).isEqualTo(startTime)

        val updatedWithEmpyStarted = stateAddress1.copy(
            sharingStateType = SharingStateType.Error,
            sharingProcessStarted = null,
            sharingErrorMessage = "Changed",
        )
        gateClient.sharingState().upsertSharingState(updatedWithEmpyStarted)

        val readUpdatedAddress = readSharingStates(LsaType.Address, "exIdAddress1")
        assertThat(readUpdatedAddress.first().sharingStateType).isEqualTo(SharingStateType.Error)
        assertThat(readUpdatedAddress.first().sharingProcessStarted).isEqualTo(startTime).describedAs("Update with null - sharingProcessStarted not changed ")
        assertThat(readUpdatedAddress.first().sharingErrorMessage).isEqualTo("Changed")

    }

    /**
     * Insert Sharing State only with required fields filled
     */
    fun insertSharingStateSuccess(lsaType: LsaType, externalId: String, sharingProcessStarted: LocalDateTime? = null): SharingStateDto {

        val newState = SharingStateDto(
            lsaType = lsaType,
            externalId = externalId,
            sharingStateType = SharingStateType.Success,
            sharingErrorCode = null,
            sharingProcessStarted = sharingProcessStarted,
            sharingErrorMessage = null,
            bpn = null
        )
        gateClient.sharingState().upsertSharingState(newState)
        return newState
    }

    /**
     * Insert Sharing State with all Fields Field
     */
    fun insertSharingStateError(lsaType: LsaType, externalId: String, errorCode: BusinessPartnerSharingError): SharingStateDto {

        val newState = SharingStateDto(
            lsaType = lsaType,
            externalId = externalId,
            sharingStateType = SharingStateType.Error,
            sharingErrorCode = errorCode,
            sharingProcessStarted = LocalDateTime.now().withNano(0),
            sharingErrorMessage = "Error in $lsaType with external id $externalId",
            bpn = "BPN" + externalId
        )
        gateClient.sharingState().upsertSharingState(newState)
        return newState
    }

    fun readSharingStates(lsaType: LsaType?, vararg externalIds: String): Collection<SharingStateDto> {

        return gateClient.sharingState().getSharingStates(PaginationRequest(), lsaType, externalIds.asList()).content
    }


}