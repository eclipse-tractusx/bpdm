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

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.TypeKeyNameVerboseDto
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.LegalEntityRepresentationInputDto
import org.eclipse.tractusx.bpdm.gate.util.MockAndAssertUtils
import org.eclipse.tractusx.bpdm.pool.api.ApiCommons
import org.eclipse.tractusx.bpdm.pool.api.model.ConfidenceCriteriaDto
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.PhysicalPostalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.containers.SelfClientInitializer
import org.eclipse.tractusx.bpdm.test.testdata.gate.BusinessPartnerVerboseValues
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDateTime

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@ActiveProfiles("test")
@ContextConfiguration(
    initializers = [
        PostgreSQLContextInitializer::class,
        KeyCloakInitializer::class,
        SelfClientAsPartnerUploaderInitializer::class
    ]
)
class PartnerUploadControllerIT @Autowired constructor(
    val testHelpers: DbTestHelpers,
    val gateClient: GateClient,
    val jacksonObjectMapper: ObjectMapper,
    val mockAndAssertUtils: MockAndAssertUtils
) {

    companion object {

        const val TENANT_BPNL = KeyCloakInitializer.TENANT_BPNL
        const val MOCKED_LEGAL_NAME = "Mocked Legal Name"

        @JvmField
        @RegisterExtension
        val poolWireMockApi: WireMockExtension = WireMockExtension.newInstance().options(WireMockConfiguration.wireMockConfig().dynamicPort()).build()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.client.pool.base-url") { poolWireMockApi.baseUrl() }
        }

    }

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
        poolWireMockApi.resetAll()
        poolMockGetLegalEntitiesApi(TENANT_BPNL, MOCKED_LEGAL_NAME)
    }

    @Test
    fun testPartnerDataFileUploadScenarios() {
        // Test valid CSV file upload
        testFileUpload("src/test/resources/testData/valid_partner_data.csv", HttpStatus.OK)

        // Test empty CSV file upload
        testFileUpload("src/test/resources/testData/empty_partner_data.csv", HttpStatus.BAD_REQUEST)

        // Test non-CSV file upload
        testFileUpload("src/test/resources/testData/non_csv_partner_data.xls", HttpStatus.BAD_REQUEST)

        // Test invalid CSV file upload - contains bad business partner data
        testFileUpload("src/test/resources/testData/invalid_partner_data.csv", HttpStatus.BAD_REQUEST)
    }

    @Test
    fun testUploadCsvAndValidateBusinessPartnerData() {
        // Read the bytes of the CSV file
        val bytes = Files.readAllBytes(Paths.get("src/test/resources/testData/valid_partner_data.csv"))
        val uploadedFile = MockMultipartFile("valid_partner_data.csv", "valid_partner_data.csv", "text/csv", bytes)

        // Only Site and Address expected to be updated from upload partner process.
        val expectedSiteAndAddressPartner = BusinessPartnerVerboseValues.bpUploadRequestFull.copy(
            legalEntity = LegalEntityRepresentationInputDto(
                legalEntityBpn = TENANT_BPNL,
                legalName = MOCKED_LEGAL_NAME,
                shortName = null,
                legalForm = null,
                states = emptyList()
            )
        )
        // Upload the CSV file
        val uploadResponse = gateClient.partnerUpload.uploadPartnerCsvFile(uploadedFile).body!!
        val expectedResponse = listOf(
            expectedSiteAndAddressPartner,
            expectedSiteAndAddressPartner.fastCopy(externalId = BusinessPartnerVerboseValues.externalId2, siteName = "Site Name 2")
        )

        val searchResponsePage = gateClient.businessParters.getBusinessPartnersInput(
            listOf(
                BusinessPartnerVerboseValues.externalId1,
                BusinessPartnerVerboseValues.externalId2
            )
        ).content
        this.mockAndAssertUtils.assertUpsertResponsesMatchRequests(uploadResponse, expectedResponse)
        this.mockAndAssertUtils.assertUpsertResponsesMatchRequests(searchResponsePage, expectedResponse)
    }

    @Test
    fun testGetCsvTemplateAndUploadWithExistingRecords() {
        // Fetch the CSV template
        val templateResponse = gateClient.partnerUpload.getPartnerCsvTemplate().body!!
        val templateBytes = templateResponse.inputStream.readAllBytes()
        val templateCsv = String(templateBytes)

        // Read the existing records from an existing test data file
        val existingTestRecordsPath = Paths.get("src/test/resources/testData/valid_partner_data.csv")
        val existingTestRecords = Files.readString(existingTestRecordsPath)

        // Combine the header from the template with the existing records
        val combinedCsv = templateCsv + existingTestRecords.lines().drop(1).joinToString("\n")

        // Upload the combined CSV file
        val combinedFile = MockMultipartFile("combined_partner_data.csv", "combined_partner_data.csv", "text/csv", combinedCsv.toByteArray())
        val uploadResponse = gateClient.partnerUpload.uploadPartnerCsvFile(combinedFile).body!!

        val expectedSiteAndAddressPartner = BusinessPartnerVerboseValues.bpUploadRequestFull.copy(
            legalEntity = LegalEntityRepresentationInputDto(
                legalEntityBpn = TENANT_BPNL,
                legalName = MOCKED_LEGAL_NAME,
                shortName = null,
                legalForm = null,
                states = emptyList()
            )
        )

        // Perform assertions
        val expectedResponse = listOf(
            expectedSiteAndAddressPartner,
            expectedSiteAndAddressPartner.fastCopy(externalId = BusinessPartnerVerboseValues.externalId2, siteName = "Site Name 2")
        )

        val searchResponsePage = gateClient.businessParters.getBusinessPartnersInput(
            listOf(
                BusinessPartnerVerboseValues.externalId1,
                BusinessPartnerVerboseValues.externalId2
            )
        ).content
        this.mockAndAssertUtils.assertUpsertResponsesMatchRequests(uploadResponse, expectedResponse)
        this.mockAndAssertUtils.assertUpsertResponsesMatchRequests(searchResponsePage, expectedResponse)
    }

    fun poolMockGetLegalEntitiesApi(tenantBpnl: String, legalName: String) {
        val legalEntity1 = LegalEntityVerboseDto(
            bpnl = tenantBpnl,
            legalName = legalName,
            legalShortName = null,
            legalFormVerbose = null,
            identifiers = emptyList(),
            states = emptyList(),
            relations = emptyList(),
            currentness = Instant.now(),
            confidenceCriteria = ConfidenceCriteriaDto(
                sharedByOwner = true,
                checkedByExternalDataSource = true,
                numberOfSharingMembers = 0,
                lastConfidenceCheckAt = LocalDateTime.of(2023, 10, 10, 10, 10, 10),
                nextConfidenceCheckAt = LocalDateTime.of(2024, 10, 10, 10, 10, 10),
                confidenceLevel = 0
            ),
            isCatenaXMemberData = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val legalAddress1 = LogisticAddressVerboseDto(
            bpna = "BPNA00000000009W",
            physicalPostalAddress = PhysicalPostalAddressVerboseDto(
                geographicCoordinates = null,
                countryVerbose = TypeKeyNameVerboseDto(CountryCode.DE, CountryCode.DE.getName()),
                postalCode = null,
                city = "Stuttgart",
                administrativeAreaLevel1Verbose = null,
                administrativeAreaLevel2 = null,
                administrativeAreaLevel3 = null,
                district = null,
                companyPostalCode = null,
                industrialZone = null,
                building = null,
                floor = null,
                door = null,
                street = null,
                taxJurisdictionCode = null
            ),
            bpnLegalEntity = null,
            bpnSite = null,
            confidenceCriteria = ConfidenceCriteriaDto(
                sharedByOwner = true,
                checkedByExternalDataSource = true,
                numberOfSharingMembers = 0,
                lastConfidenceCheckAt = LocalDateTime.of(2023, 10, 10, 10, 10, 10),
                nextConfidenceCheckAt = LocalDateTime.of(2024, 10, 10, 10, 10, 10),
                confidenceLevel = 0
            ),
            isCatenaXMemberData = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val responseBody = PageDto(
            1, 1, 0, 1,
            listOf(
                LegalEntityWithLegalAddressVerboseDto(legalEntity = legalEntity1, legalAddress = legalAddress1)
            )
        )

        poolWireMockApi.stubFor(
            WireMock.get(WireMock.urlPathEqualTo(ApiCommons.LEGAL_ENTITY_BASE_PATH_V7))
                .withQueryParam("bpnLs", WireMock.equalTo(tenantBpnl))
                .withQueryParam("page", WireMock.equalTo("0"))
                .withQueryParam("size", WireMock.equalTo("1"))
                .willReturn(
                    okJson(jacksonObjectMapper.writeValueAsString(responseBody))
                )
        )
    }


    private fun testFileUpload(filePath: String, expectedStatus: HttpStatus) {
        val bytes = Files.readAllBytes(Paths.get(filePath))
        val file = MockMultipartFile(filePath.substringAfterLast('/'), filePath.substringAfterLast('/'), determineFileType(filePath), bytes)

        if (expectedStatus == HttpStatus.OK) {
            val response = gateClient.partnerUpload.uploadPartnerCsvFile(file)
            assertEquals(expectedStatus, response.statusCode)
        } else {
            val exception = assertThrows<WebClientResponseException> {
                gateClient.partnerUpload.uploadPartnerCsvFile(file)
            }
            assertEquals(expectedStatus, exception.statusCode)
        }
    }

    private fun determineFileType(filePath: String): String {
        return when (filePath.substringAfterLast('.')) {
            "csv" -> "text/csv"
            "xls" -> "application/vnd.ms-excel"
            else -> "application/octet-stream"
        }
    }

    private fun BusinessPartnerInputRequest.fastCopy(externalId: String, siteName: String) =
        copy(externalId = externalId, site = site.copy(name = siteName))

}

class SelfClientAsPartnerUploaderInitializer : SelfClientInitializer() {
    override val clientId: String
        get() = "sa-cl7-cx-5"
}

