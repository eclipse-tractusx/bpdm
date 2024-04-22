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

import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.RecursiveComparisonAssert
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.exception.ChangeLogOutputError
import org.eclipse.tractusx.bpdm.gate.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.gate.api.model.request.ChangelogSearchRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.ChangelogGateDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.gate.entity.ChangelogEntryDb
import org.eclipse.tractusx.bpdm.gate.util.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.gate.BusinessPartnerNonVerboseValues
import org.eclipse.tractusx.bpdm.test.testdata.gate.BusinessPartnerVerboseValues
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test-no-auth")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
internal class ChangeLogControllerIT @Autowired constructor(
    val gateClient: GateClient,
    private val testHelpers: DbTestHelpers,
) {
    companion object {
        @RegisterExtension
        private val wireMockServer: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

    }


    val instant = Instant.now()

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
        wireMockServer.resetAll()
        createChangeLogs()
    }

    /**
     * Given externalId exists in database
     * When getting changeLog by external id
     * Then changeLog mapped to the catena data model should be returned
     */
    @Test
    fun `get changeLog by external id`() {

        val searchRequest = ChangelogSearchRequest(externalIds = setOf(BusinessPartnerNonVerboseValues.bpInputRequestFull.externalId))

        val searchResult = gateClient.changelog.getInputChangelog(PaginationRequest(), searchRequest)

        assertRecursively(searchResult.content)
            .ignoringFieldsMatchingRegexes(".*${ChangelogGateDto::timestamp.name}")
            .isEqualTo(listOf(ChangelogGateDto(BusinessPartnerNonVerboseValues.bpInputRequestFull.externalId, instant, ChangelogType.CREATE)))
    }


    /**
     * Given externalId does not exist in database
     * When getting changeLog by external id
     * Then changeLog mapped to the catena data model should not be returned
     */
    @Test
    fun `get changeLog by external id not found`() {

        val searchRequest = ChangelogSearchRequest(externalIds = setOf("NONEXIST"))

        val searchResult = gateClient.changelog.getInputChangelog(PaginationRequest(), searchRequest)

        assertThat(searchResult.content)
            .usingRecursiveComparison()
            .ignoringAllOverriddenEquals()
            .ignoringCollectionOrder()
            .isEqualTo(emptyList<ChangelogEntryDb>())

        assertRecursively(searchResult.content)
            .isEqualTo(emptyList<ChangelogEntryDb>())

        assertRecursively(searchResult.errors)
            .isEqualTo(
                listOf(
                    ErrorInfo(
                        ChangeLogOutputError.ExternalIdNotFound,
                        "NONEXIST not found",
                        "NONEXIST"
                    )
                )
            )


    }

    /**
     * Given externalId and timestamp a changeLog exist in database
     * When getting changeLog by external id and timestamp
     * Then changeLog mapped to the catena data model should be returned
     */
    @Test
    fun `get changeLog by external id and timeStamp`() {

        val searchRequest = ChangelogSearchRequest(externalIds = setOf(BusinessPartnerNonVerboseValues.bpInputRequestFull.externalId), timestampAfter = instant)

        val searchResult = gateClient.changelog.getInputChangelog(PaginationRequest(), searchRequest)

        assertRecursively(searchResult.content).ignoringFieldsMatchingRegexes(".*${ChangelogGateDto::timestamp.name}")
            .isEqualTo(listOf(ChangelogGateDto(BusinessPartnerVerboseValues.bpInputRequestFull.externalId, instant, ChangelogType.CREATE)))
    }

    /**
     * Given a timeStamp a changeLog exists in database
     * When getting changeLog by timeStamp
     * Then changeLog from that instant until now is mapped to the catena data model should be returned
     */
    @Test
    fun `get changeLog from timeStamp`() {

        val searchRequest = ChangelogSearchRequest(timestampAfter = instant)

        val searchResult = gateClient.changelog.getInputChangelog(paginationRequest = PaginationRequest(), searchRequest)

        assertRecursively(searchResult.content).ignoringFieldsMatchingRegexes(".*${ChangelogGateDto::timestamp.name}")
            .isEqualTo(
                listOf(
                    ChangelogGateDto(BusinessPartnerNonVerboseValues.bpInputRequestFull.externalId, instant, ChangelogType.CREATE),
                    ChangelogGateDto(BusinessPartnerNonVerboseValues.bpInputRequestChina.externalId, instant, ChangelogType.CREATE)
                )
            )
    }

    fun <T> assertRecursively(actual: T): RecursiveComparisonAssert<*> {
        return assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringAllOverriddenEquals()

    }

    /**
     * Creates changelog entities
     * Retains the order: All response objects will be in the same order as their request counterparts
     * Assumption: Changelog entities have unique indexes among them each
     */
    fun createChangeLogs() {
        gateClient.businessParters.upsertBusinessPartnersInput(listOf(BusinessPartnerNonVerboseValues.bpInputRequestFull, BusinessPartnerNonVerboseValues.bpInputRequestChina))

    }

}