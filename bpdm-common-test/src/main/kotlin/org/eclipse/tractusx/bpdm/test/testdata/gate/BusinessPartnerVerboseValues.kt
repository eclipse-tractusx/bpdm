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

package org.eclipse.tractusx.bpdm.test.testdata.gate

import com.neovisionaries.i18n.CountryCode
import com.neovisionaries.i18n.LanguageCode
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.dto.TypeKeyNameVerboseDto
import org.eclipse.tractusx.bpdm.common.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.*
import java.time.Instant
import java.time.LocalDateTime

object BusinessPartnerVerboseValues {

    const val externalId1 = "external-1"
    const val externalId2 = "external-2"
    const val externalId3 = "external-3"
    const val externalId4 = "external-4"
    const val externalId5 = "external-5"

    const val externalIdSite1 = "site-external-1"
    const val externalIdSite2 = "site-external-2"

    const val externalIdAddress1 = "address-external-1"
    const val externalIdAddress2 = "address-external-2"

    const val legalEntityAddressId = "external-1_legalAddress"
    const val siteAddressId = "site-external-1_site"

    const val identifierValue1 = "DE123456789"
    const val identifierValue2 = "US123456789"
    const val identifierValue3 = "FR123456789"
    const val identifierValue4 = "NL123456789"
    const val identifierValue5 = "US777882222"


    const val identifierIssuingBodyName1 = "Agency X"
    const val identifierIssuingBodyName2 = "Body Y"
    const val identifierIssuingBodyName3 = "Official Z"
    const val identifierIssuingBodyName4 = "Gov A"
    const val identifierIssuingBodyName5 = "Gov B"


    const val identifierTypeTechnicalKey1 = "VAT_DE"
    const val identifierTypeTechnicalKey2 = "VAT_US"
    const val identifierTypeTechnicalKey3 = "VAT_FR"
    const val identifierTypeTechnicalKey4 = "VAT_NL"
    const val identifierTypeTechnicalKey5 = "VAT_US"

    const val businessStatusDescription1 = "Active"
    const val businessStatusDescription2 = "Insolvent"

    val externalSequenceTimestamp1 = Instant.now().minusSeconds(5)
    val externalSequenceTimestamp2 = Instant.now()
    val externalSequenceTimestamp3 = Instant.now().plusSeconds(5)

    val businessStatusValidFrom1 = LocalDateTime.of(2020, 1, 1, 0, 0)
    val businessStatusValidFrom2 = LocalDateTime.of(2019, 1, 1, 0, 0)

    val businessStatusValidUntil1 = LocalDateTime.of(2021, 1, 1, 0, 0)
    val businessStatusValidUntil2 = LocalDateTime.of(2022, 1, 1, 0, 0)

    val businessStateType1 = BusinessStateType.ACTIVE
    val businessStateType2 = BusinessStateType.INACTIVE

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


    val country1 = TypeKeyNameVerboseDto(
        technicalKey = CountryCode.DE,
        name = CountryCode.DE.getName()
    )

    val country2 = TypeKeyNameVerboseDto(
        technicalKey = CountryCode.US,
        name = CountryCode.US.getName()
    )

    val bpClassification1 = BusinessPartnerClassificationDto(
        type = ClassificationType.NACE,
        code = "code1",
        value = "Sale of motor vehicles"
    )

    val bpClassification2 = BusinessPartnerClassificationDto(
        type = ClassificationType.NACE,
        code = "code2",
        value = "Data processing, hosting and related activities"
    )

    val bpClassification3 = BusinessPartnerClassificationDto(
        type = ClassificationType.NACE,
        code = "code3",
        value = "Other information service activities"
    )

    val bpClassification4 = BusinessPartnerClassificationDto(
        type = ClassificationType.NACE,
        code = "code4",
        value = "Financial and insurance activities"
    )

    val bpClassificationChina = BusinessPartnerClassificationDto(
        type = ClassificationType.NACE,
        code = "code3",
        value = "北京市"
    )

    val legalEntityBusinessStatus1 = LegalEntityStateDto(
        description = businessStatusDescription1,
        validFrom = businessStatusValidFrom1,
        validTo = businessStatusValidUntil1,
        type = businessStateType1
    )

    val legalEntityBusinessStatus2 = LegalEntityStateDto(
        description = businessStatusDescription2,
        validFrom = businessStatusValidFrom2,
        validTo = businessStatusValidUntil2,
        type = businessStateType2
    )

    val bpClassification1Dto = LegalEntityClassificationDto(
        type = ClassificationType.NACE,
        code = "code1",
        value = "Sale of motor vehicles"
    )

    val bpClassification2Dto = LegalEntityClassificationDto(
        type = ClassificationType.NACE,
        code = "code2",
        value = "Data processing, hosting and related activities"
    )

    val bpClassification3Dto = LegalEntityClassificationDto(
        type = ClassificationType.NACE,
        code = "code3",
        value = "Other information service activities"
    )

    val bpClassification4Dto = LegalEntityClassificationDto(
        type = ClassificationType.NACE,
        code = "code4",
        value = "Financial and insurance activities"
    )

    val legalEntity1 = LegalEntityDto(
        legalNameParts = listOf("Business Partner Name"),
        legalShortName = "short1",
        legalForm = "LF1",
        states = listOf(legalEntityBusinessStatus1),
        isCatenaXMemberData = false
    )

    val legalEntity2 = LegalEntityDto(
        legalNameParts = listOf("Company ABC AG"),
        legalShortName = "short3",
        legalForm = "LF2",
        states = listOf(legalEntityBusinessStatus2),
        isCatenaXMemberData = false
    )

    val legalEntity3 = LegalEntityDto(
        legalNameParts = listOf("Business Partner Name"),
        legalShortName = "short1",
        legalForm = "LF1",
        states = listOf(legalEntityBusinessStatus1),
        isCatenaXMemberData = false
    )

    val siteBusinessStatus1 = SiteStateDto(
        description = businessStatusDescription1,
        validFrom = businessStatusValidFrom1,
        validTo = businessStatusValidUntil1,
        type = businessStateType1
    )

    val siteBusinessStatus2 = SiteStateDto(
        description = businessStatusDescription2,
        validFrom = businessStatusValidFrom2,
        validTo = businessStatusValidUntil2,
        type = businessStateType2
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

    val postalAddress1 = PhysicalPostalAddressDto(
        geographicCoordinates = GeoCoordinateDto(13.178, 48.946),
        country = CountryCode.DE,
        postalCode = "70546 ",
        city = "Stuttgart",
        administrativeAreaLevel1 = "adminAreaLevel1RegionCode_1", //null,
        administrativeAreaLevel2 = "Stuttgart",
        administrativeAreaLevel3 = null,
        district = "Vaihingen",
        companyPostalCode = null,
        industrialZone = "Werk 1",
        building = "Bauteil A",
        floor = "Etage 1",
        door = "Door One",
        street = StreetDto(name = "Mercedesstraße", houseNumber = "", direction = "direction1", houseNumberSupplement = "A"),
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

    val postalAddress3 = PhysicalPostalAddressDto(
        geographicCoordinates = GeoCoordinateDto(13.178, 48.946),
        country = CountryCode.DE,
        postalCode = "70546 ",
        city = "Stuttgart",
        administrativeAreaLevel1 = null,
        administrativeAreaLevel2 = "Stuttgart",
        administrativeAreaLevel3 = null,
        district = "Vaihingen",
        companyPostalCode = null,
        industrialZone = "Werk 1",
        building = "Bauteil A",
        floor = "Etage 1",
        door = "Door One",
        street = StreetDto(name = "Mercedesstraße", houseNumber = "", direction = "direction1", houseNumberSupplement = "C"),
    )

    val bpPostalAddressInputDtoFull = BusinessPartnerPostalAddressDto(
        addressType = AddressType.LegalAddress,
        physicalPostalAddress = postalAddress2,
        alternativePostalAddress = alternativeAddressFull
    )

    val physicalAddress1 = LogisticAddressDto(
        physicalPostalAddress = postalAddress1,
    )

    val physicalAddress2 = LogisticAddressDto(
        physicalPostalAddress = postalAddress2,
    )

    val physicalAddress3 = LogisticAddressDto(
        physicalPostalAddress = postalAddress3,
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

    //New Values for Logistic Addresses Tests
    val postalAddressLogisticAddress1 = PhysicalPostalAddressDto(
        geographicCoordinates = GeoCoordinateDto(13.178, 48.946),
        country = CountryCode.DE,
        postalCode = "70546 ",
        city = "Stuttgart",
        administrativeAreaLevel1 = "adminAreaLevel1RegionCode_1",
        administrativeAreaLevel2 = "Stuttgart",
        administrativeAreaLevel3 = null,
        district = "Vaihingen",
        companyPostalCode = null,
        industrialZone = "Werk 1",
        building = "Bauteil A",
        floor = "Etage 1",
        door = "Door One",
        street = StreetDto(name = "Mercedesstraße", houseNumber = "", direction = "direction1", houseNumberSupplement = "A")
    )

    val postalAddressLogisticAddress2 = PhysicalPostalAddressDto(
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
        street = StreetDto(name = "TODO", houseNumber = "", direction = "direction1", houseNumberSupplement = "B")
    )

    //New Values for Logistic Address Tests
    val logisticAddress1 = LogisticAddressDto(
        physicalPostalAddress = postalAddressLogisticAddress1,
    )

    val logisticAddress2 = LogisticAddressDto(
        physicalPostalAddress = postalAddressLogisticAddress2,
    )

    //Response values for Site and LegalEntity created Addresses (Input)
    val addressGateInputResponseLegalEntity1 = AddressGateInputDto(
        address = physicalAddress1.copy(
            nameParts = emptyList(), //listOf(CommonValues.name1),
        ),
        externalId = legalEntityAddressId,
        legalEntityExternalId = externalId1,
    )

    val addressGateInputResponseSite1 = AddressGateInputDto(
        address = physicalAddress1.copy(
            nameParts = emptyList(), //listOf(CommonValues.nameSite1),
        ),
        externalId = siteAddressId,
        siteExternalId = externalIdSite1,
    )

    val logisticAddressGateInputResponse1 = AddressGateInputDto(
        address = logisticAddress1.copy(
            nameParts = listOf("Business Partner Name"),
            identifiers = listOf(
                AddressIdentifierDto(identifierValue1, identifierTypeTechnicalKey1)
            ),
            states = emptyList()
        ),
        externalId = externalIdAddress1,
        legalEntityExternalId = externalId1,
    )

    val logisticAddressGateInputResponse2 = AddressGateInputDto(
        address = logisticAddress2.copy(
            nameParts = listOf("Company ABC AG"),
            identifiers = listOf(
                AddressIdentifierDto(identifierValue1, identifierTypeTechnicalKey1)
            ),
            states = emptyList()
        ),
        externalId = externalIdAddress2,
        siteExternalId = externalIdSite1,
    )

    //Output Response Values
    val logisticAddressGateOutputResponse1 = AddressGateOutputDto(
        address = logisticAddress1.copy(
            nameParts = listOf("Business Partner Name"),
        ),
        externalId = externalIdAddress1,
        legalEntityExternalId = externalId1,
        bpna = "BPNA0000000001XY"
    )

    val logisticAddressGateOutputResponse2 = AddressGateOutputDto(
        address = logisticAddress2.copy(
            nameParts = listOf("Company ABC AG"),
        ),
        externalId = externalIdAddress2,
        siteExternalId = externalIdSite1,
        bpna = "BPNA0000000002XY"
    )

    val legalEntityGateInputResponse1 = LegalEntityGateInputDto(
        legalEntity = legalEntity1,
        legalAddress = AddressGateInputDto(
            address = logisticAddress1,
            externalId = "${externalId1}_legalAddress",
            legalEntityExternalId = externalId1,
            siteExternalId = null
        ),
        externalId = externalId1,
    )

    val legalEntityGateInputResponse2 = LegalEntityGateInputDto(
        legalEntity = legalEntity2,
        legalAddress = AddressGateInputDto(
            address = logisticAddress2,
            externalId = "${externalId2}_legalAddress",
            legalEntityExternalId = externalId2,
            siteExternalId = null
        ),
        externalId = externalId2,
    )

    val site1 = SiteGateDto(
        nameParts = listOf("Site A"),
        states = listOf(siteBusinessStatus1)
    )

    val site2 = SiteGateDto(
        nameParts = listOf("Site B"),
        states = listOf(siteBusinessStatus2)
    )

    //Gate Output Legal Entities Response
    val legalEntityGateOutputResponse1 = LegalEntityGateOutputResponse(
        legalEntity = legalEntity1,
        externalId = externalId1,
        bpnl = "BPNL0000000000XY",
        legalAddress = AddressGateOutputDto(
            address = physicalAddress1,
            externalId = "${externalId1}_legalAddress",
            legalEntityExternalId = externalId1,
            siteExternalId = null,
            bpna = "BPNA0000000001XY"
        )
    )


    val legalEntityGateOutputResponse2 = LegalEntityGateOutputResponse(
        legalEntity = legalEntity2,
        externalId = externalId2,
        bpnl = "BPNL0000000001XZ",
        legalAddress = AddressGateOutputDto(
            address = physicalAddress2,
            externalId = "${externalId2}_legalAddress",
            legalEntityExternalId = externalId2,
            siteExternalId = null,
            bpna = "BPNA0000000002XY"
        )
    )

    val persistencesiteGateInputResponse1 = SiteGateInputDto(
        site = site1, externalId = externalIdSite1,
        legalEntityExternalId = externalId1,
        mainAddress = AddressGateInputDto(
            address = physicalAddress1,
            externalId = "${externalIdSite1}_site",
            legalEntityExternalId = null,
            siteExternalId = externalIdSite1,
        )
    )

    val persistenceSiteGateInputResponse2 = SiteGateInputDto(
        site = site2, externalId = externalIdSite2,
        legalEntityExternalId = externalId2,
        mainAddress = AddressGateInputDto(
            address = physicalAddress2,
            externalId = "${externalIdSite2}_site",
            legalEntityExternalId = null,
            siteExternalId = externalIdSite2,
        )
    )

    val persistencesiteGateOutputResponse1 = SiteGateOutputResponse(
        site = site1, externalId = externalIdSite1,
        legalEntityExternalId = externalId1,
        bpns = "BPNS0000000001XY",
        mainAddress = AddressGateOutputDto(
            address = physicalAddress1,
            externalId = "${externalIdSite1}_site",
            legalEntityExternalId = null,
            siteExternalId = externalIdSite1,
            bpna = "BPNA0000000001XY"
        )
    )

    val persistencesiteGateOutputResponse2 = SiteGateOutputResponse(
        site = site2, externalId = externalIdSite2,
        legalEntityExternalId = externalId2,
        bpns = "BPNS0000000002XY",
        mainAddress = AddressGateOutputDto(
            address = physicalAddress2,
            externalId = "${externalIdSite2}_site",
            legalEntityExternalId = null,
            siteExternalId = externalIdSite2,
            bpna = "BPNA0000000002XY"
        )
    )

    //Response values for Site and LegalEntity created Addresses (Output)
    val addressGateOutputResponseLegalEntity1 = AddressGateOutputDto(
        address = physicalAddress1.copy(
            nameParts = emptyList(),
            identifiers = listOf(
                AddressIdentifierDto(identifierValue1, identifierTypeTechnicalKey1)
            )
        ),
        externalId = legalEntityAddressId,
        legalEntityExternalId = externalId1,
        bpna = "BPNA0000000001XY"
    )

    val addressGateOutputResponseSite1 = AddressGateOutputDto(
        address = physicalAddress1.copy(
            nameParts = emptyList(),
            identifiers = listOf(
                AddressIdentifierDto(identifierValue1, identifierTypeTechnicalKey1)
            )
        ),
        externalId = siteAddressId,
        siteExternalId = externalIdSite1,
        bpna = "BPNA0000000001XY"
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

    val bpOutputDtoCleaned = BusinessPartnerOutputDto(
        externalId = externalId4,
        nameParts = listOf("part-cleaned-1", "name-cleaned-2"),
        identifiers = listOf(
            BusinessPartnerIdentifierDto(
                type = "identifier-type-1-cleaned",
                value = "identifier-value-1-cleaned",
                issuingBody = "issuingBody-1-cleaned"
            ),
            BusinessPartnerIdentifierDto(
                type = "identifier-type-2-cleaned",
                value = "identifier-value-2-cleaned",
                issuingBody = "issuingBody-2-cleaned"
            ),
        ),
        states = listOf(
            BusinessPartnerStateDto(
                validFrom = LocalDateTime.of(2020, 9, 22, 15, 50),
                validTo = LocalDateTime.of(2023, 10, 23, 16, 40),
                type = BusinessStateType.INACTIVE
            ),
            BusinessPartnerStateDto(
                validFrom = LocalDateTime.of(2000, 8, 21, 14, 30),
                validTo = LocalDateTime.of(2020, 9, 22, 15, 50),
                type = BusinessStateType.ACTIVE
            )
        ),
        roles = listOf(
            BusinessPartnerRole.CUSTOMER,
            BusinessPartnerRole.SUPPLIER
        ),
        legalEntity = LegalEntityRepresentationOutputDto(
            legalEntityBpn = "000000123AAA123",
            shortName = "shot-name-cleaned",
            legalName = "legal-name-cleaned",
            legalForm = "legal-form-cleaned",
            confidenceCriteria = ConfidenceCriteriaDto(
                sharedByOwner = true,
                checkedByExternalDataSource = true,
                numberOfSharingMembers = 7,
                lastConfidenceCheckAt = LocalDateTime.of(2022, 4, 3, 2, 1),
                nextConfidenceCheckAt = LocalDateTime.of(2026, 4, 3, 2, 1),
                confidenceLevel = 1
            )
        ),
        site = SiteRepresentationOutputDto(
            siteBpn = "000000123BBB222",
            name = "Site Name",
            confidenceCriteria = ConfidenceCriteriaDto(
                sharedByOwner = false,
                checkedByExternalDataSource = false,
                numberOfSharingMembers = 8,
                lastConfidenceCheckAt = LocalDateTime.of(2023, 4, 3, 2, 1),
                nextConfidenceCheckAt = LocalDateTime.of(2024, 4, 3, 2, 1),
                confidenceLevel = 2
            )
        ),
        address = AddressComponentOutputDto(
            addressBpn = "000000123CCC333",
            name = "Address Name",
            addressType = AddressType.AdditionalAddress,
            physicalPostalAddress = PhysicalPostalAddressDto(
                geographicCoordinates = GeoCoordinateDto(0.5, 0.5, 0.5),
                country = CountryCode.PT,
                administrativeAreaLevel1 = "PT-PT",
                administrativeAreaLevel2 = "pt-admin-level-2-cleaned",
                administrativeAreaLevel3 = "pt-admin-level-3-cleaned",
                postalCode = "phys-postal-code-cleaned",
                city = "city",
                district = "district",
                street = StreetDto(
                    name = "name",
                    houseNumber = "house-number",
                    houseNumberSupplement = "house-number-supplement",
                    milestone = "milestone",
                    direction = "direction",
                    namePrefix = "name-prefix",
                    additionalNamePrefix = "add-name-prefix",
                    nameSuffix = "name-suffix",
                    additionalNameSuffix = "add-name-suffix"

                ),
                companyPostalCode = "comp-postal-code",
                industrialZone = "industrial-zone",
                building = "building",
                floor = "floor",
                door = "door",
                taxJurisdictionCode = "123"
            ),
            alternativePostalAddress = AlternativePostalAddressDto(
                geographicCoordinates = GeoCoordinateDto(0.6, 0.6, 0.6),
                country = CountryCode.PT,
                administrativeAreaLevel1 = "PT-PT",
                postalCode = "postal-code-cleaned",
                city = "alt-city-cleaned",
                deliveryServiceNumber = "delivery-service-number-cleaned",
                deliveryServiceQualifier = "delivery-service-qualifier-cleaned",
                deliveryServiceType = DeliveryServiceType.PO_BOX
            ),
            confidenceCriteria = ConfidenceCriteriaDto(
                sharedByOwner = false,
                checkedByExternalDataSource = true,
                numberOfSharingMembers = 4,
                lastConfidenceCheckAt = LocalDateTime.of(2020, 4, 3, 2, 1),
                nextConfidenceCheckAt = LocalDateTime.of(2028, 4, 3, 2, 1),
                confidenceLevel = 5
            )
        ),
        isOwnCompanyData = false,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
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


    const val bpn1 = "BPNL0000000000XY"
    const val bpn2 = "BPNL0000000001XZ"
    const val bpn3 = "BPNL0000000002XY"

    const val bpnSite1 = "BPNS0000000001XY"
    const val bpnSite2 = "BPNS0000000002XY"
    const val bpnSite3 = "BPNS0000000003X9"

    const val bpnAddress1 = "BPNA0000000001XY"
    const val bpnAddress2 = "BPNA0000000002XY"
    const val bpnAddress3 = "BPNA0000000003X9"

    val language1 = LanguageCode.de
    val language2 = LanguageCode.en

    val characterSet1 = CharacterSet.WESTERN_LATIN_STANDARD
    val characterSet2 = CharacterSet.GREEK



    const val name1 = "Business Partner Name"
    const val name2 = "Company ABC AG"
    const val name3 = "Another Organisation Corp"
    const val name4 = "Catena Test Name"

    const val nameSite1 = "Site A"
    const val nameSite2 = "Site B"


    const val identifierIssuingBodyTechnicalKey1 = "issuing body 1"
    const val identifierIssuingBodyTechnicalKey2 = "issuing body 2"
    const val identifierIssuingBodyTechnicalKey3 = "issuing body 3"
    const val identifierIssuingBodyTechnicalKey4 = "issuing body 4"


    const val identifierIssuingBody1 = "Agency X"
    const val identifierIssuingBody2 = "Body Y"
    const val identifierIssuingBody3 = "Official Z"
    const val identifierIssuingBody4 = "Gov A"

    const val identifierIssuingBodyUrl1 = "http://catenax-host/issuing-body1"
    const val identifierIssuingBodyUrl2 = "http://catenax-host/issuing-body2"
    const val identifierIssuingBodyUrl3 = "http://catenax-host/issuing-body3"
    const val identifierIssuingBodyUrl4 = "http://catenax-host/issuing-body4"

    const val identifierStatusTechnicalKey1 = "ACTIVE"
    const val identifierStatusTechnicalKey2 = "EXPIRED"
    const val identifierStatusTechnicalKey3 = "PENDING"
    const val identifierStatusTechnicalKey4 = "UNKNOWN"

    const val identifierStatusName1 = "Active"
    const val identifierStatusName2 = "Expired"
    const val identifierStatusName3 = "Pending"
    const val identifierStatusName4 = "Unknown Status"

    const val identifierTypeName1 = "Steuernummer"
    const val identifierTypeName2 = "VAT USA"
    const val identifierTypeName3 = "VAT France"
    const val identifierTypeName4 = "VAT Netherlands"

    const val identifierTypeUrl1 = "http://catenax-host/id-type1"
    const val identifierTypeUrl2 = "http://catenax-host/id-type2"
    const val identifierTypeUrl3 = "http://catenax-host/id-type3"
    const val identifierTypeUrl4 = "http://catenax-host/id-type4"


    val nameType1 = NameType.OTHER

    const val shortName1 = "short1"
    const val shortName2 = "short2"
    const val shortName3 = "short3"
    const val shortName4 = "short4"


    const val legalFormTechnicalKey1 = "LF1"
    const val legalFormTechnicalKey2 = "LF2"

    const val legalFormName1 = "Limited Liability Company"
    const val legalFormName2 = "Gemeinschaft mit beschränkter Haftung"

    const val legalFormAbbreviation1 = "LLC"
    const val legalFormAbbreviation2 = "GmbH"

    const val businessStatusOfficialDenotation1 = "Active"
    const val businessStatusOfficialDenotation2 = "Insolvent"


    val classificationType = ClassificationType.NACE

    const val classificationValue1 = "Sale of motor vehicles"
    const val classificationValue2 = "Data processing, hosting and related activities"
    const val classificationValue3 = "Other information service activities"
    const val classificationValue4 = "Financial and insurance activities"

    const val classificationCode1 = "code1"
    const val classificationCode2 = "code2"
    const val classificationCode3 = "code3"
    const val classificationCode4 = "code4"

    const val careOf1 = "Caring Entity Co"
    const val careOf2 = "Another Caring Entity"

    const val context1 = "Context1"
    const val context2 = "Context2"

    val adminAreaLevel1RegionCode_1: String = "adminAreaLevel1RegionCode_1"
    val adminAreaLevel1RegionCode_2: String = "adminAreaLevel1RegionCode_2"

    const val county1 = "Stuttgart"
    const val county2 = " Fulton County"

    const val city1 = "Stuttgart"
    const val city2 = "Atlanta"


    const val district1 = "Vaihingen"
    const val district2 = "TODO"
    const val district3 = "TODO"

    const val street1 = "Mercedesstraße"
    const val street2 = "TODO"
    const val street3 = "TODO"

    const val houseNumber1 = ""
    const val houseNumber2 = ""
    const val houseNumber3 = ""

    const val direction1 = "direction1"
    const val direction2 = "direction1"

    const val industrialZone1 = "Werk 1"
    const val industrialZone2 = "Industrial Zone Two"
    const val industrialZone3 = "Industrial Zone Three"

    const val building1 = "Bauteil A"
    const val building2 = "Building Two"
    const val building3 = "Building Two"

    const val floor1 = "Etage 1"
    const val floor2 = "Floor Two"
    const val floor3 = "Floor Two"

    const val door1 = "Door One"
    const val door2 = "Door Two"
    const val door3 = "Door Two"

    const val postCode1 = "70546 "
    const val postCode2 = "70547"

    val geoCoordinates1 = Triple(0f, 0f, 0f)
    val geoCoordinates2 = Triple(1f, 1f, 0f)

}