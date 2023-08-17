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

package com.catenax.bpdm.bridge.dummy

import com.catenax.bpdm.bridge.dummy.service.SyncService
import com.catenax.bpdm.bridge.dummy.util.DbTestHelpers
import com.catenax.bpdm.bridge.dummy.util.PostgreSQLContextInitializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.github.tomakehurst.wiremock.verification.LoggedRequest
import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.service.BaseSyncRecordService
import org.eclipse.tractusx.bpdm.gate.api.model.request.ChangelogSearchRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.ChangelogGateDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.PageChangeLogDto
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Instant

val TS_INITIAL_POLL_FROM: Instant = BaseSyncRecordService.INITIAL_FROM_TIME

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class]
)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class SyncStateIT @Autowired constructor(
    val syncService: SyncService,
    val testHelpers: DbTestHelpers,
    val objectMapper: ObjectMapper
) {

    companion object {
        const val GATE_GET_INPUT_CHANGELOG_PATH = "/api/catena/input/changelog/search"

        @JvmField
        @RegisterExtension
        val gateWireMockServer: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.gate.base-url") { gateWireMockServer.baseUrl() }
        }
    }

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()

        gateWireMockServer.resetAll()
    }

    /**
     * When a sync is successful,
     * Then for the next sync the bridge polls for changes only after the time the last sync started.
     */
    @Test
    fun `successful sync`() {
        // all 3 syncs successful

        val responseGateChangelog: PageChangeLogDto<ChangelogGateDto> =
            PageChangeLogDto(
                totalElements = 0,
                totalPages = 0,
                page = 0,
                contentSize = 0,
                content = listOf(),
                invalidEntries = 0,
                errors = listOf()
            )

        // Gate changelog endpoint returns okay with no changes
        gateWireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(GATE_GET_INPUT_CHANGELOG_PATH))
                .willReturn(
                    WireMock.okJson(objectMapper.writeValueAsString(responseGateChangelog))
                )
        )

        val tsBefore1stSuccessfulSync = Instant.now()
        syncService.sync()
        val tsAfter1stSuccessfulSync = Instant.now()

        val tsBefore2ndSuccessfulSync = Instant.now()
        syncService.sync()
        val tsAfter2ndSuccessfulSync = Instant.now()

        syncService.sync()

        val loggedRequests = gateWireMockServer.findAll(
            WireMock.postRequestedFor(WireMock.urlPathEqualTo(GATE_GET_INPUT_CHANGELOG_PATH))
        )

        Assertions.assertThat(loggedRequests.size).isEqualTo(3)

        // 1st sync polls from initial timestamp (2000-01-01)
        val pollFrom1stSync = parseBody<ChangelogSearchRequest>(loggedRequests[0]).timestampAfter
        Assertions.assertThat(pollFrom1stSync).isEqualTo(TS_INITIAL_POLL_FROM)

        // 2nd sync polls from around timestamp of 1st successful sync
        val pollFrom2ndSync = parseBody<ChangelogSearchRequest>(loggedRequests[1]).timestampAfter
        Assertions.assertThat(pollFrom2ndSync).isBetween(tsBefore1stSuccessfulSync, tsAfter1stSuccessfulSync)

        // 3rd sync polls from around timestamp of 2nd successful sync
        val pollFrom3rdSync = parseBody<ChangelogSearchRequest>(loggedRequests[2]).timestampAfter
        Assertions.assertThat(pollFrom3rdSync).isBetween(tsBefore2ndSuccessfulSync, tsAfter2ndSuccessfulSync)
    }

    /**
     * When a sync fails,
     * Then for the next sync the bridge polls for changes after the exact same time as for the last sync.
     */
    @Test
    fun `sync with errors`() {
        // 2nd & 3rd sync fail; 1st, 4th, 5th sync successful

        val responseGateChangelog: PageChangeLogDto<ChangelogGateDto> =
            PageChangeLogDto(
                totalElements = 0,
                totalPages = 0,
                page = 0,
                contentSize = 0,
                content = listOf(),
                invalidEntries = 0,
                errors = listOf()
            )

        // Gate changelog endpoint returns okay with no changes
        gateWireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(GATE_GET_INPUT_CHANGELOG_PATH))
                .willReturn(
                    WireMock.okJson(objectMapper.writeValueAsString(responseGateChangelog))
                )
        )

        // 1st sync is successful
        val tsBefore1stSuccessfulSync = Instant.now()
        syncService.sync()
        val tsAfter1stSuccessfulSync = Instant.now()

        // Gate changelog endpoint returns error code
        gateWireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(GATE_GET_INPUT_CHANGELOG_PATH))
                .willReturn(
                    WireMock.serverError()
                )
        )

        // 2nd sync fails
        assertThrows(WebClientResponseException.InternalServerError::class.java) {
            syncService.sync()
        }

        // 3rd sync fails
        assertThrows(WebClientResponseException.InternalServerError::class.java) {
            syncService.sync()
        }

        // Gate changelog endpoint again returns okay with no changes
        gateWireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(GATE_GET_INPUT_CHANGELOG_PATH))
                .willReturn(
                    WireMock.okJson(objectMapper.writeValueAsString(responseGateChangelog))
                )
        )

        // 4th sync successful
        val tsBefore2ndSuccessfulSync = Instant.now()
        syncService.sync()
        val tsAfter2ndSuccessfulSync = Instant.now()

        // 5th sync successful
        syncService.sync()

        val loggedRequests = gateWireMockServer.findAll(
            WireMock.postRequestedFor(WireMock.urlPathEqualTo(GATE_GET_INPUT_CHANGELOG_PATH))
        )

        // 5 sync requests -> changelog endpoint was polled 5 times
        Assertions.assertThat(loggedRequests.size).isEqualTo(5)

        // 1st sync polls from initial timestamp (2000-01-01)
        val pollFrom1stSync = parseBody<ChangelogSearchRequest>(loggedRequests[0]).timestampAfter
        Assertions.assertThat(pollFrom1stSync).isEqualTo(TS_INITIAL_POLL_FROM)

        // 2nd sync polls from around timestamp of 1st successful sync
        val pollFrom2ndSync = parseBody<ChangelogSearchRequest>(loggedRequests[1]).timestampAfter
        Assertions.assertThat(pollFrom2ndSync).isBetween(tsBefore1stSuccessfulSync, tsAfter1stSuccessfulSync)

        // 3rd sync still polls from same timestamp because last sync has failed!
        val pollFrom3rdSync = parseBody<ChangelogSearchRequest>(loggedRequests[2]).timestampAfter
        Assertions.assertThat(pollFrom3rdSync).isEqualTo(pollFrom2ndSync)

        // 4th sync still polls from same timestamp because last sync has failed!
        val pollFrom4thSync = parseBody<ChangelogSearchRequest>(loggedRequests[3]).timestampAfter
        Assertions.assertThat(pollFrom4thSync).isEqualTo(pollFrom2ndSync)

        // 5th sync polls from around timestamp of 2nd successful sync
        val pollFrom5thSync = parseBody<ChangelogSearchRequest>(loggedRequests[4]).timestampAfter
        Assertions.assertThat(pollFrom5thSync).isBetween(tsBefore2ndSuccessfulSync, tsAfter2ndSuccessfulSync)
    }

    private inline fun <reified T> parseBody(loggedRequest: LoggedRequest): T {
        return objectMapper.readValue(
            loggedRequest.bodyAsString,
            T::class.java
        )
    }
}