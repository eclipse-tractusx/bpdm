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

package org.eclipse.tractusx.bpdm.gate.v7.upload

import com.neovisionaries.i18n.CountryCode
import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.gate.api.model.AlternativePostalAddressDto
import org.eclipse.tractusx.bpdm.gate.api.model.BusinessPartnerIdentifierDto
import org.eclipse.tractusx.bpdm.gate.api.model.PhysicalPostalAddressDto
import org.eclipse.tractusx.bpdm.gate.api.model.StreetDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.AddressRepresentationInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.LegalEntityRepresentationInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SiteRepresentationInputDto
import org.eclipse.tractusx.bpdm.gate.v7.UnscheduledGateTestBaseV7
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.springframework.core.io.ClassPathResource
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Instant

class UploadPartnersV7IT: UnscheduledGateTestBaseV7() {


    @BeforeEach
    override fun beforeEach(testInfo: TestInfo) {
        super.beforeEach(testInfo)

        // All tests expect that there is a golden record legal entity in the Pool which represents the tenant
        mockExpectedTenantLegalEntity()
    }

    /**
     * WHEN uploading valid business partner CSV file
     * THEN uploaded business partner inputs returned
     */
    @Test
    fun `upload valid CSV file`() {
        //WHEN
        val partnerUploadFile = ClassPathResource("testData/valid_partner_data.csv")
        val response = gateClient.partnerUpload.uploadPartnerCsvFile(partnerUploadFile).body!!

        //THEN
        val expectedResponse = listOf(ExpectedResponses.validEntry1, ExpectedResponses.validEntry2)

        assertRepo.assertBusinessPartnerInput(response, expectedResponse)
    }

    /**
     * GIVEN business partners
     * WHEN uploading valid CSV file referencing these business partners
     * THEN updated business partner inputs returned
     */
    @Test
    fun `update business partners by CSV`() {
        //GIVEN
        testDataClient.businessPartner.upsertInput(ExpectedResponses.validEntry1.externalId)
        testDataClient.businessPartner.upsertInput(ExpectedResponses.validEntry2.externalId)

        //WHEN
        val partnerUploadFile = ClassPathResource("testData/valid_partner_data.csv")
        val response = gateClient.partnerUpload.uploadPartnerCsvFile(partnerUploadFile).body!!

        //THEN
        val expectedResponse = listOf(ExpectedResponses.validEntry1, ExpectedResponses.validEntry2)

        assertRepo.assertBusinessPartnerInput(response, expectedResponse)
    }


    /**
     * WHEN uploading empty  CSV file
     * THEN user sees 400 HTTP error
     */
    @Test
    fun `try to upload empty CSV file`() {
        //WHEN
        val uploadRequest = {
            val partnerUploadFile = ClassPathResource("testData/empty_partner_data.csv")
            gateClient.partnerUpload.uploadPartnerCsvFile(partnerUploadFile).body!!
            Unit
        }

        //THEN
        Assertions.assertThatExceptionOfType(WebClientResponseException.BadRequest::class.java).isThrownBy(uploadRequest)
    }

    /**
     * WHEN uploading non-CSV file
     * THEN user sees 400 HTTP error
     */
    @Test
    fun `try to upload non-CSV file`() {
        //WHEN
        val uploadRequest = {
            val partnerUploadFile = ClassPathResource("testData/non_csv_partner_data.xls")
            gateClient.partnerUpload.uploadPartnerCsvFile(partnerUploadFile).body!!
            Unit
        }

        //THEN
        Assertions.assertThatExceptionOfType(WebClientResponseException.BadRequest::class.java).isThrownBy(uploadRequest)
    }

    /**
     * WHEN uploading CSV file with invalid business partner data
     * THEN user sees 400 HTTP error
     */
    @Test
    fun `try to upload file with invalid business partner data`() {
        //WHEN
        val uploadRequest = {
            val partnerUploadFile = ClassPathResource("testData/invalid_partner_data.csv")
            gateClient.partnerUpload.uploadPartnerCsvFile(partnerUploadFile).body!!
            Unit
        }

        //THEN
        Assertions.assertThatExceptionOfType(WebClientResponseException.BadRequest::class.java).isThrownBy(uploadRequest)
    }

    private fun mockExpectedTenantLegalEntity(){
        poolMockDataFactory.configureWireMock()
        val tenantLegalEntityRequest = with(poolMockDataFactory.requestFactory.createLegalEntityRequest(testName)) { copy(legalEntity = legalEntity.copy(header = legalEntity.header.copy(legalName = ExpectedResponses.tenantLegalName))) }
        val tenantLegalEntityResponse = with(poolMockDataFactory.expectedResultFactory.mapToExpectedLegalEntity(tenantLegalEntityRequest)) { copy(header = header.copy(bpnl = tenantBPNL)) }
        poolMockDataFactory.mockLegalEntitySearchResult(tenantLegalEntityResponse)
    }

    private object ExpectedResponses{

        val tenantLegalName = "Limited Liability Company Name"

        val validEntry1 = BusinessPartnerInputDto(
            externalId = "external-1",
            nameParts = emptyList(),
            isOwnCompanyData = true,
            identifiers = listOf(
                BusinessPartnerIdentifierDto(
                    value = "DE123456789",
                    type = "VAT_DE",
                    issuingBody = "Agency X"
                ),
                BusinessPartnerIdentifierDto(
                    value = "US123456789",
                    type = "VAT_US",
                    issuingBody = "Body Y"
                ),
                BusinessPartnerIdentifierDto(
                    value = "FR123456789",
                    type = "VAT_FR",
                    issuingBody = null
                )
            ),
            states = emptyList(),
            roles = emptyList(),
            legalEntity = LegalEntityRepresentationInputDto(
                legalEntityBpn = "BPNL000000000001",
                shortName = null,
                legalName = tenantLegalName,
                legalForm = null
            ),
            site = SiteRepresentationInputDto(
                siteBpn = null,
                name = "Site Name"
            ),
            address = AddressRepresentationInputDto(
                addressBpn = "BPNA0000000001XY",
                name = "Address Name",
                addressType = AddressType.SiteMainAddress,
                physicalPostalAddress =  PhysicalPostalAddressDto(
                    geographicCoordinates = GeoCoordinateDto(7.619, 45.976, 4478.0),
                    country = CountryCode.US,
                    postalCode = "70547",
                    city = "Atlanta",
                    administrativeAreaLevel1 = "adminAreaLevel1RegionCode_2",
                    administrativeAreaLevel2 = " Fulton County",
                    administrativeAreaLevel3 = null,
                    district = "TODO",
                    companyPostalCode = null,
                    industrialZone = "Industrial Zone Two",
                    building = "Building Two",
                    floor = "Floor Two",
                    door = "Door Two",
                    street = StreetDto(name = "TODO", houseNumber = "", direction = "direction1", houseNumberSupplement = "B"),
                ),
                alternativePostalAddress = AlternativePostalAddressDto(
                    country = CountryCode.DE,
                    city = "Stuttgart",
                    deliveryServiceType = DeliveryServiceType.PO_BOX,
                    deliveryServiceQualifier = "DHL",
                    deliveryServiceNumber = "1234",
                    geographicCoordinates = GeoCoordinateDto(7.619, 45.976, 4478.0),
                    postalCode = "70547",
                    administrativeAreaLevel1 = "adminAreaLevel1RegionCode_2",
                )
            ),
            externalSequenceTimestamp = null,
            scriptVariants = emptyList(),
            createdAt = Instant.MIN,
            updatedAt = Instant.MIN
        )

        val validEntry2 = validEntry1.copy(
            externalId = "external-2",
            site = validEntry1.site.copy(name = "Site Name 2"),
            address = validEntry1.address.copy(addressBpn = "BPNA0000000002XY"),

        )
    }
}