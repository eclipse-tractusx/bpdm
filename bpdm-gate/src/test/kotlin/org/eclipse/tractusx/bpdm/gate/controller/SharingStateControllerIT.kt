/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.exception.BusinessPartnerSharingError.SharingProcessError
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.request.PostSharingStateReadyRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.SharingStateDto
import org.eclipse.tractusx.bpdm.gate.util.BusinessPartnerNonVerboseValues
import org.eclipse.tractusx.bpdm.gate.util.DbTestHelpers
import org.eclipse.tractusx.bpdm.gate.util.PostgreSQLContextInitializer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.reactive.function.client.WebClientResponseException
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

    /**
     * Sharing state in which all fields are filled
     * Useful for streamlining tests
     */
    val fullSharingState = SharingStateDto(
        businessPartnerType = BusinessPartnerType.ADDRESS,
        externalId = "exIdAddress",
        sharingStateType = SharingStateType.Error,
        sharingErrorCode = SharingProcessError,
        sharingErrorMessage = "Message",
        bpn = "TEST_BPN",
        sharingProcessStarted = LocalDateTime.of(1900, 1, 1, 1, 1)
    )

    val sharingProcessStartedTime = LocalDateTime.of(2000, 10, 9, 8, 7)

    @ParameterizedTest
    @EnumSource(BusinessPartnerType::class)
    fun `insert minimal initial sharing state accepted`(bpType: BusinessPartnerType) {
        val givenExternalId = "external-id"

        val givenSharingState = SharingStateDto(businessPartnerType = bpType, externalId = givenExternalId)
        gateClient.sharingState.upsertSharingState(givenSharingState)

        val expected = PageDto(1, 1, 0, 1, listOf(givenSharingState))
        val actual = gateClient.sharingState.getSharingStates(PaginationRequest(0, 1), bpType, listOf(givenExternalId))

        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @EnumSource(BusinessPartnerType::class)
    fun `insert initial sharing state with additional information ignored`(bpType: BusinessPartnerType) {
        val externalId = "external-id"
        val givenSharingState = fullSharingState.copy(
            sharingStateType = SharingStateType.Initial,
            businessPartnerType = bpType,
            externalId = externalId
        )

        gateClient.sharingState.upsertSharingState(givenSharingState)

        val expectedSharingState = SharingStateDto(businessPartnerType = bpType, externalId = externalId)
        val expected = PageDto(1, 1, 0, 1, listOf(expectedSharingState))
        val actual = gateClient.sharingState.getSharingStates(PaginationRequest(0, 1), bpType, listOf(externalId))

        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @EnumSource(BusinessPartnerType::class)
    fun `insert initial sharing state resets time`(bpType: BusinessPartnerType) {
        val externalId = "external-id"
        val givenBpn = "TEST_BPN"

        val initialState = SharingStateDto(
            sharingStateType = SharingStateType.Initial,
            businessPartnerType = bpType,
            externalId = externalId
        )

        val successState = initialState.copy(
            sharingStateType = SharingStateType.Success,
            bpn = givenBpn,
            sharingProcessStarted = LocalDateTime.now()
        )

        gateClient.sharingState.upsertSharingState(successState)
        gateClient.sharingState.upsertSharingState(initialState)

        val expectedSharingState = initialState.copy(bpn = givenBpn)
        val expected = PageDto(1, 1, 0, 1, listOf(expectedSharingState))
        val actual = gateClient.sharingState.getSharingStates(PaginationRequest(0, 1), bpType, listOf(externalId))

        assertThat(actual).isEqualTo(expected)
    }


    @ParameterizedTest
    @EnumSource(BusinessPartnerType::class)
    fun `insert minimal pending sharing state accepted`(bpType: BusinessPartnerType) {
        val givenBeforeInsert = LocalDateTime.now()
        val givenExternalId = "external-id"
        val givenTaskId = "task-id"

        val givenSharingState =
            SharingStateDto(sharingStateType = SharingStateType.Pending, taskId = givenTaskId, businessPartnerType = bpType, externalId = givenExternalId)
        gateClient.sharingState.upsertSharingState(givenSharingState)

        val expected = PageDto(1, 1, 0, 1, listOf(givenSharingState))
        val actual = gateClient.sharingState.getSharingStates(PaginationRequest(0, 1), bpType, listOf(givenExternalId))

        val afterInsert = LocalDateTime.now()
        assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*${SharingStateDto::sharingProcessStarted.name}")
            .isEqualTo(expected)

        assertThat(actual.content.single().sharingProcessStarted).isBetween(givenBeforeInsert, afterInsert)
    }

    @ParameterizedTest
    @EnumSource(BusinessPartnerType::class)
    fun `insert pending sharing state with sharingProcessStart accepted`(bpType: BusinessPartnerType) {
        val givenExternalId = "external-id"
        val givenTaskId = "task-id"
        val givenSharingProcessStart = sharingProcessStartedTime

        val givenSharingState = SharingStateDto(
            sharingStateType = SharingStateType.Pending,
            businessPartnerType = bpType,
            externalId = givenExternalId,
            taskId = givenTaskId,
            sharingProcessStarted = givenSharingProcessStart
        )
        gateClient.sharingState.upsertSharingState(givenSharingState)

        val expected = PageDto(1, 1, 0, 1, listOf(givenSharingState))
        val actual = gateClient.sharingState.getSharingStates(PaginationRequest(0, 1), bpType, listOf(givenExternalId))

        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @EnumSource(BusinessPartnerType::class)
    fun `insert pending sharing state with additional fields ignored`(bpType: BusinessPartnerType) {
        val givenExternalId = "external-id"
        val givenTaskId = "task-id"
        val givenSharingProcessStart = sharingProcessStartedTime

        val givenSharingState = fullSharingState.copy(
            sharingStateType = SharingStateType.Pending,
            businessPartnerType = bpType,
            externalId = givenExternalId,
            taskId = givenTaskId,
            sharingProcessStarted = givenSharingProcessStart
        )
        gateClient.sharingState.upsertSharingState(givenSharingState)

        val expectedSharingState = SharingStateDto(
            sharingStateType = SharingStateType.Pending,
            businessPartnerType = bpType,
            externalId = givenExternalId,
            taskId = givenTaskId,
            sharingProcessStarted = givenSharingProcessStart
        )
        val expected = PageDto(1, 1, 0, 1, listOf(expectedSharingState))
        val actual = gateClient.sharingState.getSharingStates(PaginationRequest(0, 1), bpType, listOf(givenExternalId))

        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @EnumSource(BusinessPartnerType::class)
    fun `insert minimal success sharing state accepted`(bpType: BusinessPartnerType) {
        val givenExternalId = "external-id"
        val givenBpn = "BPN_TEST"
        val beforeInsert = sharingProcessStartedTime

        val givenSharingState = SharingStateDto(
            sharingStateType = SharingStateType.Success,
            businessPartnerType = bpType,
            externalId = givenExternalId,
            bpn = givenBpn
        )
        gateClient.sharingState.upsertSharingState(givenSharingState)


        val expected = PageDto(1, 1, 0, 1, listOf(givenSharingState))
        val actual = gateClient.sharingState.getSharingStates(PaginationRequest(0, 1), bpType, listOf(givenExternalId))

        val afterInsert = LocalDateTime.now()

        assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*${SharingStateDto::sharingProcessStarted.name}")
            .isEqualTo(expected)

        assertThat(actual.content.single().sharingProcessStarted).isBetween(beforeInsert, afterInsert)
    }

    @ParameterizedTest
    @EnumSource(BusinessPartnerType::class)
    fun `insert success sharing state with sharingProcessStart accepted`(bpType: BusinessPartnerType) {
        val givenExternalId = "external-id"
        val givenSharingProcessStart = sharingProcessStartedTime
        val givenBpn = "TEST_BPN"

        val givenSharingState = SharingStateDto(
            sharingStateType = SharingStateType.Success,
            businessPartnerType = bpType,
            externalId = givenExternalId,
            sharingProcessStarted = givenSharingProcessStart,
            bpn = givenBpn
        )
        gateClient.sharingState.upsertSharingState(givenSharingState)

        val expected = PageDto(1, 1, 0, 1, listOf(givenSharingState))
        val actual = gateClient.sharingState.getSharingStates(PaginationRequest(0, 1), bpType, listOf(givenExternalId))

        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @EnumSource(BusinessPartnerType::class)
    fun `insert success sharing state with additional properties ignored`(bpType: BusinessPartnerType) {
        val givenExternalId = "external-id"
        val givenSharingProcessStart = sharingProcessStartedTime
        val givenBpn = "TEST_BPN"

        val givenSharingState = fullSharingState.copy(
            sharingStateType = SharingStateType.Success,
            businessPartnerType = bpType,
            externalId = givenExternalId,
            sharingProcessStarted = givenSharingProcessStart,
            bpn = givenBpn
        )
        gateClient.sharingState.upsertSharingState(givenSharingState)

        val expectedSharingState = SharingStateDto(
            sharingStateType = SharingStateType.Success,
            businessPartnerType = bpType,
            externalId = givenExternalId,
            sharingProcessStarted = givenSharingProcessStart,
            bpn = givenBpn
        )
        val expected = PageDto(1, 1, 0, 1, listOf(expectedSharingState))
        val actual = gateClient.sharingState.getSharingStates(PaginationRequest(0, 1), bpType, listOf(givenExternalId))

        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @EnumSource(BusinessPartnerType::class)
    fun `insert success sharing state without BPN throws exception`(bpType: BusinessPartnerType) {
        val givenExternalId = "external-id"

        val sharingState = SharingStateDto(
            sharingStateType = SharingStateType.Success,
            businessPartnerType = bpType,
            externalId = givenExternalId
        )

        assertThatThrownBy {
            gateClient.sharingState.upsertSharingState(sharingState)
        }.isInstanceOf(WebClientResponseException::class.java)

    }

    @ParameterizedTest
    @EnumSource(BusinessPartnerType::class)
    fun `insert minimal error sharing state accepted`(bpType: BusinessPartnerType) {
        val givenExternalId = "external-id"
        val beforeInsert = LocalDateTime.now()

        val givenSharingState = SharingStateDto(
            sharingStateType = SharingStateType.Error,
            businessPartnerType = bpType,
            externalId = givenExternalId,
            sharingErrorCode = SharingProcessError
        )
        gateClient.sharingState.upsertSharingState(givenSharingState)


        val expected = PageDto(1, 1, 0, 1, listOf(givenSharingState))
        val actual = gateClient.sharingState.getSharingStates(PaginationRequest(0, 1), bpType, listOf(givenExternalId))

        val afterInsert = LocalDateTime.now()

        assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*${SharingStateDto::sharingProcessStarted.name}")
            .isEqualTo(expected)

        assertThat(actual.content.single().sharingProcessStarted).isBetween(beforeInsert, afterInsert)
    }

    @ParameterizedTest
    @EnumSource(BusinessPartnerType::class)
    fun `insert error sharing state with sharingProcessStart accepted`(bpType: BusinessPartnerType) {
        val givenExternalId = "external-id"
        val givenSharingProcessStart = sharingProcessStartedTime

        val givenSharingState = SharingStateDto(
            sharingStateType = SharingStateType.Error,
            businessPartnerType = bpType,
            externalId = givenExternalId,
            sharingProcessStarted = givenSharingProcessStart,
            sharingErrorCode = SharingProcessError
        )
        gateClient.sharingState.upsertSharingState(givenSharingState)

        val expected = PageDto(1, 1, 0, 1, listOf(givenSharingState))
        val actual = gateClient.sharingState.getSharingStates(PaginationRequest(0, 1), bpType, listOf(givenExternalId))

        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @EnumSource(BusinessPartnerType::class)
    fun `insert error sharing state with additional properties ignored`(bpType: BusinessPartnerType) {
        val givenExternalId = "external-id"
        val givenSharingProcessStart = sharingProcessStartedTime
        val givenBpn = "TEST_BPN"
        val givenErrorMessage = "test message"

        val givenSharingState = SharingStateDto(
            sharingStateType = SharingStateType.Error,
            businessPartnerType = bpType,
            externalId = givenExternalId,
            sharingProcessStarted = givenSharingProcessStart,
            sharingErrorCode = SharingProcessError,
            sharingErrorMessage = givenErrorMessage,
            bpn = givenBpn
        )
        gateClient.sharingState.upsertSharingState(givenSharingState)

        val expectedSharingState = givenSharingState.copy(bpn = null)
        val expected = PageDto(1, 1, 0, 1, listOf(expectedSharingState))
        val actual = gateClient.sharingState.getSharingStates(PaginationRequest(0, 1), bpType, listOf(givenExternalId))

        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @EnumSource(BusinessPartnerType::class)
    fun `insert error sharing state without error code throws exception`(bpType: BusinessPartnerType) {
        val givenExternalId = "external-id"

        val sharingState = SharingStateDto(
            sharingStateType = SharingStateType.Error,
            businessPartnerType = bpType,
            externalId = givenExternalId
        )

        assertThatThrownBy {
            gateClient.sharingState.upsertSharingState(sharingState)
        }.isInstanceOf(WebClientResponseException::class.java)

    }

    @ParameterizedTest
    @EnumSource(BusinessPartnerType::class)
    fun `keep BPN through sharing state lifecycle`(bpType: BusinessPartnerType) {
        val givenExternalId = "external-id"
        val givenBpn = "TEST_BPN"
        val givenSharingProcessStart = sharingProcessStartedTime
        val givenTaskId = "task-id"

        val successState = SharingStateDto(
            businessPartnerType = bpType,
            externalId = givenExternalId,
            sharingStateType = SharingStateType.Success,
            bpn = givenBpn
        )

        val initialState = successState.copy(
            sharingStateType = SharingStateType.Initial,
            bpn = null
        )

        val pendingState = successState.copy(
            sharingStateType = SharingStateType.Pending,
            sharingProcessStarted = givenSharingProcessStart,
            taskId = givenTaskId,
            bpn = null
        )

        val errorState = successState.copy(
            businessPartnerType = bpType,
            externalId = givenExternalId,
            sharingStateType = SharingStateType.Error,
            sharingErrorCode = SharingProcessError,
            bpn = null
        )

        gateClient.sharingState.upsertSharingState(successState)
        gateClient.sharingState.upsertSharingState(initialState)
        gateClient.sharingState.upsertSharingState(pendingState)
        gateClient.sharingState.upsertSharingState(errorState)

        val actual = gateClient.sharingState.getSharingStates(
            businessPartnerType = bpType,
            externalIds = listOf(givenExternalId),
            paginationRequest = PaginationRequest()
        )

        val expectedSharingState = errorState.copy(bpn = givenBpn, taskId = givenTaskId, sharingProcessStarted = givenSharingProcessStart)
        val expected = PageDto(1, 1, 0, 1, content = listOf(expectedSharingState))

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `insert and get sharing states `() {
        val stateAddress = insertSharingStateInitial(BusinessPartnerType.ADDRESS, externalId = "exIdAddress")
        val stateSite = insertSharingStateInitial(BusinessPartnerType.SITE, externalId = "exIdSite")
        val stateLegalEntity1 = insertSharingStateInitial(BusinessPartnerType.LEGAL_ENTITY, externalId = "exIdEntity1")
        val stateLegalEntity2 = insertSharingStateInitial(BusinessPartnerType.LEGAL_ENTITY, externalId = "exIdEntity2")
        val stateGeneric = insertSharingStateInitial(BusinessPartnerType.GENERIC, externalId = "exIdGeneric1")
        insertSharingStateInitial(BusinessPartnerType.ADDRESS, externalId = "exIdMultiple")
        insertSharingStateInitial(BusinessPartnerType.SITE, externalId = "exIdMultiple")
        insertSharingStateInitial(BusinessPartnerType.LEGAL_ENTITY, externalId = "exIdMultiple")
        insertSharingStateInitial(BusinessPartnerType.GENERIC, externalId = "exIdMultiple")


        val searchAddressById = readSharingStates(BusinessPartnerType.ADDRESS, "exIdAddress")
        assertThat(searchAddressById).hasSize(1)
        assertThat(searchAddressById.single()).isEqualTo(stateAddress)

        val searchSitesById = readSharingStates(BusinessPartnerType.SITE, "exIdSite")
        assertThat(searchSitesById).hasSize(1)
        assertThat(searchSitesById.single()).isEqualTo(stateSite)

        val searchAddressWrongId = readSharingStates(BusinessPartnerType.ADDRESS, "exIdEntity")
        assertThat(searchAddressWrongId).hasSize(0)

        val searchEntityMultiple = readSharingStates(BusinessPartnerType.LEGAL_ENTITY, "exIdEntity1", "exIdEntity2")
        assertThat(searchEntityMultiple).hasSize(2)

        val searchEntitySingle = readSharingStates(BusinessPartnerType.LEGAL_ENTITY, "exIdEntity2")
        assertThat(searchEntitySingle).hasSize(1)
        assertThat(searchEntitySingle.single()).isEqualTo(stateLegalEntity2)

        val searchGenericSingle = readSharingStates(BusinessPartnerType.GENERIC, "exIdGeneric1")
        assertThat(searchGenericSingle).hasSize(1)
        assertThat(searchGenericSingle.first()).isEqualTo(stateGeneric)

        val searchAll = readSharingStates(null)
        assertThat(searchAll).hasSize(9)

        val searchEntityAllLegalEntities = readSharingStates(BusinessPartnerType.LEGAL_ENTITY)
        assertThat(searchEntityAllLegalEntities).hasSize(3)
        assertThat(searchEntityAllLegalEntities).extracting(SharingStateDto::externalId.name)
            .contains(stateLegalEntity1.externalId, stateLegalEntity2.externalId, "exIdMultiple")

        val searchAllWithSameId = readSharingStates(null, "exIdMultiple")
        assertThat(searchAllWithSameId).hasSize(4)
        assertThat(searchAllWithSameId).extracting(SharingStateDto::externalId.name)
            .containsOnly("exIdMultiple")

    }

    /*
    GIVEN business partners in state Initial
    WHEN set as ready
    THEN business partners in state ready
     */
    @Test
    fun `set initial business partners ready`() {
        val givenBusinessPartners = listOf(
            BusinessPartnerNonVerboseValues.bpInputRequestChina,
            BusinessPartnerNonVerboseValues.bpInputRequestFull,
            BusinessPartnerNonVerboseValues.bpInputRequestMinimal,
        )
        gateClient.businessParters.upsertBusinessPartnersInput(givenBusinessPartners)

        gateClient.sharingState.postSharingStateReady(PostSharingStateReadyRequest(givenBusinessPartners.map { it.externalId }))


        val sharingStateResponse =
            gateClient.sharingState.getSharingStates(PaginationRequest(), businessPartnerType = null, externalIds = givenBusinessPartners.map { it.externalId })

        assertThat(sharingStateResponse.content).isEqualTo(givenBusinessPartners.map {
            SharingStateDto(
                businessPartnerType = BusinessPartnerType.GENERIC,
                externalId = it.externalId,
                sharingStateType = SharingStateType.Ready
            )
        })
    }

    /*
   GIVEN business partners in state Error
   WHEN set as ready
   THEN business partners in state ready
    */
    @Test
    fun `set error business partners ready`() {
        val givenBusinessPartners = listOf(
            BusinessPartnerNonVerboseValues.bpInputRequestChina,
            BusinessPartnerNonVerboseValues.bpInputRequestFull,
            BusinessPartnerNonVerboseValues.bpInputRequestMinimal,
        )
        gateClient.businessParters.upsertBusinessPartnersInput(givenBusinessPartners)

        val givenErrorStates = givenBusinessPartners.map {
            SharingStateDto(
                businessPartnerType = BusinessPartnerType.GENERIC,
                externalId = it.externalId,
                sharingStateType = SharingStateType.Error,
                sharingErrorCode = SharingProcessError,
                sharingErrorMessage = "message"
            )
        }

        givenErrorStates.forEach { gateClient.sharingState.upsertSharingState(it) }


        gateClient.sharingState.postSharingStateReady(PostSharingStateReadyRequest(givenBusinessPartners.map { it.externalId }))


        val sharingStateResponse =
            gateClient.sharingState.getSharingStates(PaginationRequest(), businessPartnerType = null, externalIds = givenBusinessPartners.map { it.externalId })

        assertThat(sharingStateResponse.content).isEqualTo(givenBusinessPartners.map {
            SharingStateDto(
                businessPartnerType = BusinessPartnerType.GENERIC,
                externalId = it.externalId,
                sharingStateType = SharingStateType.Ready
            )
        })
    }

    /*
    GIVEN business partners in invalid state to be shared
    WHEN set as ready
    THEN return error response
    */
    @Test
    fun `throw error response on business partners ready in invalid state`() {
        val givenBusinessPartner = BusinessPartnerNonVerboseValues.bpInputRequestChina
        gateClient.businessParters.upsertBusinessPartnersInput(listOf(givenBusinessPartner))

        val givenInvalidState = SharingStateDto(
            businessPartnerType = BusinessPartnerType.GENERIC,
            externalId = givenBusinessPartner.externalId,
            sharingStateType = SharingStateType.Ready
        )

        gateClient.sharingState.upsertSharingState(givenInvalidState)

        try {
            gateClient.sharingState.postSharingStateReady(PostSharingStateReadyRequest(listOf(givenBusinessPartner.externalId)))

        } catch (e: WebClientResponseException) {
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, e.statusCode)
        }
    }

    /**
     * Insert Sharing State only with required fields filled
     */
    fun insertSharingStateInitial(
        businessPartnerType: BusinessPartnerType,
        externalId: String
    ): SharingStateDto {

        val newState = SharingStateDto(
            businessPartnerType = businessPartnerType,
            externalId = externalId,
            sharingStateType = SharingStateType.Initial,
            sharingErrorCode = null,
            sharingProcessStarted = null,
            sharingErrorMessage = null,
            bpn = null
        )
        gateClient.sharingState.upsertSharingState(newState)
        return newState
    }

    fun readSharingStates(businessPartnerType: BusinessPartnerType?, vararg externalIds: String): Collection<SharingStateDto> {

        return gateClient.sharingState.getSharingStates(PaginationRequest(), businessPartnerType, externalIds.asList()).content
    }

}