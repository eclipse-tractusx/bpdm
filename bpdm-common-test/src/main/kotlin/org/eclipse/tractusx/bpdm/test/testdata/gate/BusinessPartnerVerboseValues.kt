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

package org.eclipse.tractusx.bpdm.test.testdata.gate

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.AddressRepresentationInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.LegalEntityRepresentationInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SiteRepresentationInputDto
import java.time.Instant
import java.time.LocalDateTime

object BusinessPartnerVerboseValues {

    const val externalId1 = "external-1"
    const val externalId2 = "external-2"
    const val externalId3 = "external-3"
    const val externalId4 = "external-4"
    const val externalId5 = "external-5"

    const val identifierValue1 = "DE123456789"
    const val identifierValue2 = "US123456789"
    const val identifierValue3 = "FR123456789"


    const val identifierIssuingBodyName1 = "Agency X"
    const val identifierIssuingBodyName2 = "Body Y"


    const val identifierTypeTechnicalKey1 = "VAT_DE"
    const val identifierTypeTechnicalKey2 = "VAT_US"
    const val identifierTypeTechnicalKey3 = "VAT_FR"

    val externalSequenceTimestamp1 = Instant.now().minusSeconds(5)
    val externalSequenceTimestamp2 = Instant.now()
    val externalSequenceTimestamp3 = Instant.now().plusSeconds(5)

    val businessStatusValidFrom1 = LocalDateTime.of(2020, 1, 1, 0, 0)
    val businessStatusValidFrom2 = LocalDateTime.of(2019, 1, 1, 0, 0)

    val businessStatusValidUntil1 = LocalDateTime.of(2021, 1, 1, 0, 0)
    val businessStatusValidUntil2 = LocalDateTime.of(2022, 1, 1, 0, 0)

    val bpState1 = BusinessPartnerStateDto(
        validFrom = businessStatusValidFrom1,
        validTo = businessStatusValidUntil1,
        type = BusinessStateType.ACTIVE
    )

    val bpState2 = BusinessPartnerStateDto(
        validFrom = businessStatusValidFrom2,
        validTo = businessStatusValidUntil2,
        type = BusinessStateType.INACTIVE
    )

    val bpIdentifier1 = BusinessPartnerIdentifierDto(
        type = identifierTypeTechnicalKey1,
        value = identifierValue1,
        issuingBody = identifierIssuingBodyName1
    )

    val bpIdentifier2 = BusinessPartnerIdentifierDto(
        type = identifierTypeTechnicalKey2,
        value = identifierValue2,
        issuingBody = identifierIssuingBodyName2
    )

    val bpIdentifier3 = BusinessPartnerIdentifierDto(
        type = identifierTypeTechnicalKey3,
        value = identifierValue3,
        issuingBody = null
    )

    val alternativeAddressFull = AlternativePostalAddressDto(
        country = CountryCode.DE,
        city = "Stuttgart",
        deliveryServiceType = DeliveryServiceType.PO_BOX,
        deliveryServiceQualifier = "DHL",
        deliveryServiceNumber = "1234",
        geographicCoordinates = GeoCoordinateDto(7.619, 45.976, 4478.0),
        postalCode = "70547",
        administrativeAreaLevel1 = "adminAreaLevel1RegionCode_2",
    )

    val postalAddress2 = PhysicalPostalAddressDto(
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
    )

    val bpInputRequestFull = BusinessPartnerInputRequest(
        externalId = externalId1,
        nameParts = listOf("Business Partner Name", "Company ABC AG", "Another Organisation Corp", "Catena Test Name"),
        isOwnCompanyData = true,
        identifiers = listOf(bpIdentifier1, bpIdentifier2, bpIdentifier3),
        states = listOf(bpState1, bpState2),
        roles = listOf(BusinessPartnerRole.SUPPLIER),
        legalEntity = LegalEntityRepresentationInputDto(
            legalEntityBpn = "BPNL0000000000XY",
            shortName = "short1",
            legalName = "Limited Liability Company Name",
            legalForm = "Limited Liability Company"
        ),
        site = SiteRepresentationInputDto(
            siteBpn = null,
            name = "Site Name"
        ),
        address = AddressRepresentationInputDto(
            addressBpn = "BPNA0000000001XY",
            name = "Address Name",
            addressType = AddressType.LegalAddress,
            physicalPostalAddress = postalAddress2,
            alternativePostalAddress = alternativeAddressFull
        ),
        externalSequenceTimestamp = null

    )

    val bpUploadRequestFull = BusinessPartnerInputRequest(
        externalId = externalId1,
        nameParts = emptyList(),
        isOwnCompanyData = true,
        identifiers = listOf(bpIdentifier1, bpIdentifier2, bpIdentifier3),
        states = emptyList(),
        roles = emptyList(),
        legalEntity = LegalEntityRepresentationInputDto(
            legalEntityBpn = "BPNL0000000000XY",
            shortName = "short1",
            legalName = "Limited Liability Company Name",
            legalForm = "Limited Liability Company"
        ),
        site = SiteRepresentationInputDto(
            siteBpn = null,
            name = "Site Name"
        ),
        address = AddressRepresentationInputDto(
            addressBpn = "BPNA0000000001XY",
            name = "Address Name",
            addressType = AddressType.SiteMainAddress,
            physicalPostalAddress = postalAddress2,
            alternativePostalAddress = alternativeAddressFull
        ),
        externalSequenceTimestamp = null
    )

    val physicalAddressMinimal = PhysicalPostalAddressDto(
        country = CountryCode.DE,
        city = "Stuttgart",
        geographicCoordinates = null,
        postalCode = null,
        administrativeAreaLevel1 = null,
        administrativeAreaLevel2 = null,
        administrativeAreaLevel3 = null,
        district = null,
        companyPostalCode = null,
        industrialZone = null,
        building = null,
        floor = null,
        door = null,
        street = null,
    )

    val physicalAddressChina = PhysicalPostalAddressDto(
        country = CountryCode.CH,
        city = "北京市",
        geographicCoordinates = null,
        postalCode = null,
        administrativeAreaLevel1 = "河北省",
        administrativeAreaLevel2 = null,
        administrativeAreaLevel3 = null,
        district = null,
        companyPostalCode = null,
        industrialZone = null,
        building = null,
        floor = null,
        door = null,
        street = null,
    )

    val bpInputRequestChina = BusinessPartnerInputRequest(
        externalId = externalId3,
        nameParts = listOf("好公司  合伙制企业"),
        isOwnCompanyData = true,
        identifiers = listOf(
            bpIdentifier1, bpIdentifier2, bpIdentifier1
        ),          // duplicate, but they are eliminated
        states = listOf(bpState2, bpState1),
        roles = listOf(BusinessPartnerRole.CUSTOMER, BusinessPartnerRole.SUPPLIER),
        legalEntity = LegalEntityRepresentationInputDto(
            legalEntityBpn = "BPNL0000000002XY",
            shortName = "short3",
            legalName = "姓名测试",
            legalForm = "股份有限"
        ),
        site = SiteRepresentationInputDto(
            siteBpn = "BPNS0000000003X9",
            name = "Site Name 3"
        ),
        address = AddressRepresentationInputDto(
            addressBpn = "BPNA0000000001XY",
            name = "Address Name 3",
            addressType = AddressType.LegalAndSiteMainAddress,
            physicalPostalAddress = physicalAddressChina,
            alternativePostalAddress = AlternativePostalAddressDto()
        ),
        externalSequenceTimestamp = null
    )

    val bpInputRequestCleaned = BusinessPartnerInputRequest(
        externalId = externalId4,
        nameParts = listOf("Name Part Value"),
        isOwnCompanyData = true,
        identifiers = listOf(
            bpIdentifier1, bpIdentifier2, bpIdentifier1
        ),
        states = listOf(bpState2, bpState1),
        roles = listOf(BusinessPartnerRole.CUSTOMER, BusinessPartnerRole.SUPPLIER),
        legalEntity = LegalEntityRepresentationInputDto(
            legalEntityBpn = "BPNL0000000002XY",
            shortName = "Random Short Name",
            legalName = "Random Name Value",
            legalForm = "Random Form Value",
        ),
        site = SiteRepresentationInputDto(
            siteBpn = "BPNS0000000003X9",
            name = "Site Name 4"
        ),
        address = AddressRepresentationInputDto(
            addressBpn = "BPNA0000000001XY",
            name = "Address Name 4",
            addressType = AddressType.LegalAddress,
            physicalPostalAddress = postalAddress2,
            alternativePostalAddress = alternativeAddressFull
        ),
        externalSequenceTimestamp = null
    )

    val bpInputRequestError = BusinessPartnerInputRequest(
        externalId = externalId5,
        nameParts = listOf("Name Part Value"),
        isOwnCompanyData = true,
        identifiers = listOf(
            bpIdentifier1, bpIdentifier2, bpIdentifier1
        ),
        states = listOf(bpState2, bpState1),
        roles = listOf(BusinessPartnerRole.CUSTOMER, BusinessPartnerRole.SUPPLIER),
        legalEntity = LegalEntityRepresentationInputDto(
            legalEntityBpn = "BPNL0000000002XY",
            shortName = "Random Short Name",
            legalName = "Random Name Value",
            legalForm = "Random Form Value"
        ),
        site = SiteRepresentationInputDto(
            siteBpn = "BPNS0000000003X9",
            name = "Site Name 5"
        ),
        address = AddressRepresentationInputDto(
            addressBpn = "BPNA0000000001XY",
            name = "Address Name 5",
            addressType = AddressType.LegalAddress,
            physicalPostalAddress = postalAddress2,
            alternativePostalAddress = alternativeAddressFull
        ),
        externalSequenceTimestamp = null
    )

    val bpInputRequestWithExternalSequenceTimestamp1 = BusinessPartnerInputRequest(
        externalId = externalId1,
        legalEntity = LegalEntityRepresentationInputDto(
            legalEntityBpn = "BPNL0000000000XY",
            shortName = "short",
            legalName = "Limited Liability Company Name",
            legalForm = "Limited Liability Company"
        ),
        address = AddressRepresentationInputDto(
            addressBpn = "BPNA0000000001XY",
            name = "Address Name",
            addressType = null,
            physicalPostalAddress = physicalAddressMinimal
        ),
        externalSequenceTimestamp = externalSequenceTimestamp1

    )

    val bpInputRequestWithExternalSequenceTimestamp2 = BusinessPartnerInputRequest(
        externalId = externalId1,
        legalEntity = LegalEntityRepresentationInputDto(
            legalEntityBpn = "BPNL0000000000XY",
            shortName = "short1",
            legalName = "Limited Liability Company Name",
            legalForm = "Limited Liability Company"
        ),
        address = AddressRepresentationInputDto(
            addressBpn = "BPNA0000000001XY",
            name = "Address Name",
            addressType = null,
            physicalPostalAddress = physicalAddressMinimal
        ),
        externalSequenceTimestamp = externalSequenceTimestamp2

    )

    val bpInputRequestWithExternalSequenceTimestamp3 = BusinessPartnerInputRequest(
        externalId = externalId1,
        legalEntity = LegalEntityRepresentationInputDto(
            legalEntityBpn = "BPNL0000000000XY",
            shortName = "short2",
            legalName = "Limited Liability Company Name",
            legalForm = "Limited Liability Company"
        ),
        address = AddressRepresentationInputDto(
            addressBpn = "BPNA0000000001XY",
            name = "Another address Name",
            addressType = null,
            physicalPostalAddress = physicalAddressMinimal
        ),
        externalSequenceTimestamp = externalSequenceTimestamp3

    )

    val now = Instant.now()
}