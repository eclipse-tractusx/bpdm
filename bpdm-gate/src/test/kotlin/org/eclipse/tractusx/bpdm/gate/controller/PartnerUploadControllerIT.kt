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

import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.SharingStateDto
import org.eclipse.tractusx.bpdm.gate.service.TaskCreationService
import org.eclipse.tractusx.bpdm.gate.util.MockAndAssertUtils
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.gate.BusinessPartnerNonVerboseValues
import org.eclipse.tractusx.bpdm.test.testdata.gate.BusinessPartnerVerboseValues
import org.eclipse.tractusx.bpdm.test.util.AssertHelpers
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

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test-no-auth")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class PartnerUploadControllerIT @Autowired constructor(
    val testHelpers: DbTestHelpers,
    val assertHelpers: AssertHelpers,
    val gateClient: GateClient,
    val taskCreationService: TaskCreationService,
    val mockAndAssertUtils: MockAndAssertUtils
){

    companion object {

        @JvmField
        @RegisterExtension
        val orchestratorWireMockServer: WireMockExtension = WireMockExtension.newInstance().options(WireMockConfiguration.wireMockConfig().dynamicPort()).build()

        @JvmField
        @RegisterExtension
        val poolWireMockServer: WireMockExtension = WireMockExtension.newInstance().options(WireMockConfiguration.wireMockConfig().dynamicPort()).build()


        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.client.pool.base-url") { poolWireMockServer.baseUrl() }
            registry.add("bpdm.client.orchestrator.base-url") { orchestratorWireMockServer.baseUrl() }
        }
    }

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
        orchestratorWireMockServer.resetAll()
        poolWireMockServer.resetAll()
        this.mockAndAssertUtils.mockOrchestratorApi(orchestratorWireMockServer)
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
    fun testUploadPartnerDataAndCheckSharingState() {
        val bytes = Files.readAllBytes(Paths.get("src/test/resources/testData/valid_partner_data.csv"))
        val uploadedFile = MockMultipartFile("valid_partner_data.csv", "valid_partner_data.csv", "text/csv", bytes)

        uploadBusinessPartnerRecordAndShare(uploadedFile)

        val externalId1 = BusinessPartnerVerboseValues.externalId1
        val externalId2 = BusinessPartnerVerboseValues.externalId2

        val externalIds = listOf(externalId1, externalId2)

        val sharingStatesRequests = listOf(
            SharingStateDto(
                externalId = externalId1,
                sharingStateType = SharingStateType.Pending,
                sharingErrorCode = null,
                sharingErrorMessage = null,
                sharingProcessStarted = null,
                taskId = "0"
            ),
            SharingStateDto(
                externalId = externalId2,
                sharingStateType = SharingStateType.Pending,
                sharingErrorCode = null,
                sharingErrorMessage = null,
                sharingProcessStarted = null,
                taskId = "1"
            )
        )

        val sharingStateResponses = this.mockAndAssertUtils.readSharingStates(externalIds)
        assertHelpers.assertRecursively(sharingStateResponses).isEqualTo(sharingStatesRequests)
    }

    @Test
    fun testUploadCsvAndValidateBusinessPartnerData() {
        // Read the bytes of the CSV file
        val bytes = Files.readAllBytes(Paths.get("src/test/resources/testData/valid_partner_data.csv"))
        val uploadedFile = MockMultipartFile("valid_partner_data.csv", "valid_partner_data.csv", "text/csv", bytes)

        // Upload the CSV file
        val uploadResponse = gateClient.partnerUpload.uploadPartnerCsvFile(uploadedFile).body!!
        val expectedResponse = listOf(
            BusinessPartnerVerboseValues.bpInputRequestFull,
            BusinessPartnerNonVerboseValues.bpInputRequestFull.fastCopy(externalId = BusinessPartnerVerboseValues.externalId2, shortName = "2")
        )

        val searchResponsePage = gateClient.businessParters.getBusinessPartnersInput(listOf(BusinessPartnerVerboseValues.externalId1, BusinessPartnerVerboseValues.externalId2)).content
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

        // Perform assertions
        val expectedResponse = listOf(
            BusinessPartnerVerboseValues.bpInputRequestFull,
            BusinessPartnerNonVerboseValues.bpInputRequestFull.fastCopy(externalId = BusinessPartnerVerboseValues.externalId2, shortName = "2")
        )

        val searchResponsePage = gateClient.businessParters.getBusinessPartnersInput(listOf(BusinessPartnerVerboseValues.externalId1, BusinessPartnerVerboseValues.externalId2)).content
        this.mockAndAssertUtils.assertUpsertResponsesMatchRequests(uploadResponse, expectedResponse)
        this.mockAndAssertUtils.assertUpsertResponsesMatchRequests(searchResponsePage, expectedResponse)
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

    private fun uploadBusinessPartnerRecordAndShare(file: MockMultipartFile) {
        gateClient.partnerUpload.uploadPartnerCsvFile(file)
        taskCreationService.createTasksForReadyBusinessPartners()
    }

    private fun BusinessPartnerInputRequest.fastCopy(externalId: String, shortName: String) =
        copy(externalId = externalId, legalEntity = legalEntity.copy(shortName = shortName))

}
