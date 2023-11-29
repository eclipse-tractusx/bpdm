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

package org.eclipse.tractusx.bpdm.gate.util

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.*
import org.eclipse.tractusx.bpdm.common.dto.response.*
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameVerboseDto
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.ClassificationType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.common.service.toDto
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerOutputRequest
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

    const val identifierIssuingBodyName1 = "Agency X"
    const val identifierIssuingBodyName2 = "Body Y"
    const val identifierIssuingBodyName3 = "Official Z"
    const val identifierIssuingBodyName4 = "Gov A"

    const val identifierTypeTechnicalKey1 = "VAT_DE"
    const val identifierTypeTechnicalKey2 = "VAT_US"
    const val identifierTypeTechnicalKey3 = "VAT_FR"
    const val identifierTypeTechnicalKey4 = "VAT_NL"

    const val businessStatusDescription1 = "Active"
    const val businessStatusDescription2 = "Insolvent"

    val businessStatusValidFrom1 = LocalDateTime.of(2020, 1, 1, 0, 0)
    val businessStatusValidFrom2 = LocalDateTime.of(2019, 1, 1, 0, 0)

    val businessStatusValidUntil1 = LocalDateTime.of(2021, 1, 1, 0, 0)
    val businessStatusValidUntil2 = LocalDateTime.of(2022, 1, 1, 0, 0)

    val businessStateType1 = BusinessStateType.ACTIVE
    val businessStateType2 = BusinessStateType.INACTIVE

    val legalForm1 = LegalFormDto(
        technicalKey = "LF1",
        name = "Limited Liability Company",
        abbreviation = "LLC",
    )

    val legalForm2 = LegalFormDto(
        technicalKey = "LF2",
        name = "Gemeinschaft mit beschränkter Haftung",
        abbreviation = "GmbH",
    )

    val bpState1 = BusinessPartnerStateDto(
        validFrom = businessStatusValidFrom1,
        validTo = businessStatusValidUntil1,
        type = BusinessStateType.ACTIVE,
        description = businessStatusDescription1
    )

    val bpState2 = BusinessPartnerStateDto(
        validFrom = businessStatusValidFrom2,
        validTo = businessStatusValidUntil2,
        type = BusinessStateType.INACTIVE,
        description = businessStatusDescription2
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

    val classification1 = ClassificationVerboseDto(
        value = "Sale of motor vehicles",
        code = "code1",
        type = ClassificationType.NACE.toDto()
    )

    val classification2 = ClassificationVerboseDto(
        value = "Data processing, hosting and related activities",
        code = "code2",
        type = ClassificationType.NACE.toDto()
    )

    val classification3 = ClassificationVerboseDto(
        value = "Other information service activities",
        code = "code3",
        type = ClassificationType.NACE.toDto()
    )

    val classification4 = ClassificationVerboseDto(
        value = "Financial and insurance activities",
        code = "code4",
        type = ClassificationType.NACE.toDto()
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

    val bpClassification1Dto = ClassificationDto(
        type = ClassificationType.NACE,
        code = "code1",
        value = "Sale of motor vehicles"
    )

    val bpClassification2Dto = ClassificationDto(
        type = ClassificationType.NACE,
        code = "code2",
        value = "Data processing, hosting and related activities"
    )

    val bpClassification3Dto = ClassificationDto(
        type = ClassificationType.NACE,
        code = "code3",
        value = "Other information service activities"
    )

    val bpClassification4Dto = ClassificationDto(
        type = ClassificationType.NACE,
        code = "code4",
        value = "Financial and insurance activities"
    )

    val legalEntity1 = LegalEntityDto(
        legalShortName = "short1",
        legalForm = "LF1",
        states = listOf(legalEntityBusinessStatus1),
        classifications = listOf(bpClassification1Dto, bpClassification2Dto),
    )

    val legalEntity2 = LegalEntityDto(
        legalShortName = "short3",
        legalForm = "LF2",
        states = listOf(legalEntityBusinessStatus2),
        classifications = listOf(bpClassification3Dto, bpClassification4Dto),
    )

    val legalEntity3 = LegalEntityDto(
        legalShortName = "short1",
        legalForm = "LF1",
        states = listOf(legalEntityBusinessStatus1),
        classifications = listOf(bpClassification1Dto, bpClassification2Dto),
    )

    val leBusinessStatus1 = LegalEntityStateVerboseDto(
        description = "Active",
        validFrom = LocalDateTime.of(2020, 1, 1, 0, 0),
        validTo = LocalDateTime.of(2021, 1, 1, 0, 0),
        type = TypeKeyNameVerboseDto(
            technicalKey = BusinessStateType.ACTIVE,
            name = BusinessStateType.ACTIVE.getTypeName(),
        )
    )

    val leBusinessStatus2 = LegalEntityStateVerboseDto(
        description = "Insolvent",
        validFrom = LocalDateTime.of(2019, 1, 1, 0, 0),
        validTo = LocalDateTime.of(2022, 1, 1, 0, 0),
        type = TypeKeyNameVerboseDto(
            technicalKey = BusinessStateType.INACTIVE,
            name = BusinessStateType.INACTIVE.getTypeName(),
        )
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

    val alternativeAddressFull = AlternativePostalAddressGateDto(
        country = CountryCode.DE,
        city = "Stuttgart",
        deliveryServiceType = DeliveryServiceType.PO_BOX,
        deliveryServiceQualifier = "DHL",
        deliveryServiceNumber = "1234",
        geographicCoordinates = GeoCoordinateDto(7.619f, 45.976f, 4478f),
        postalCode = "70547",
        administrativeAreaLevel1 = "adminAreaLevel1RegionCode_2",
    )

    val postalAddress1 = PhysicalPostalAddressGateDto(
        geographicCoordinates = GeoCoordinateDto(13.178f, 48.946f),
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
        street = StreetGateDto(name = "Mercedesstraße", houseNumber = "", direction = "direction1"),
    )

    val postalAddress2 = PhysicalPostalAddressGateDto(
        geographicCoordinates = GeoCoordinateDto(7.619f, 45.976f, 4478f),
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
        street = StreetGateDto(name = "TODO", houseNumber = "", direction = "direction1"),
    )

    val postalAddress3 = PhysicalPostalAddressGateDto(
        geographicCoordinates = GeoCoordinateDto(13.178f, 48.946f),
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
        street = StreetGateDto(name = "Mercedesstraße", houseNumber = "", direction = "direction1"),
    )

    val bpPostalAddressInputDtoFull = BusinessPartnerPostalAddressDto(
        addressType = AddressType.LegalAddress,
        physicalPostalAddress = postalAddress2,
        alternativePostalAddress = alternativeAddressFull
    )

    val physicalAddress1 = LogisticAddressGateDto(
        physicalPostalAddress = postalAddress1,
    )

    val physicalAddress2 = LogisticAddressGateDto(
        physicalPostalAddress = postalAddress2,
    )

    val physicalAddress3 = LogisticAddressGateDto(
        physicalPostalAddress = postalAddress3,
    )

    val bpInputRequestFull = BusinessPartnerInputRequest(
        externalId = externalId1,
        nameParts = listOf("Business Partner Name", "Company ABC AG", "Another Organisation Corp", "Catena Test Name"),
        shortName = "short1",
        legalName = "Limited Liability Company Name",
        legalForm = "Limited Liability Company",
        isOwnCompanyData = true,
        identifiers = listOf(bpIdentifier1, bpIdentifier2, bpIdentifier3),
        classifications = listOf(bpClassification1, bpClassification2, bpClassification3),
        states = listOf(bpState1, bpState2),
        roles = listOf(BusinessPartnerRole.SUPPLIER),
        postalAddress = bpPostalAddressInputDtoFull,
        legalEntityBpn = "BPNL0000000000XY",
        addressBpn = "BPNA0000000001XY"
    )

    val address1 = PhysicalPostalAddressVerboseDto(
        geographicCoordinates = GeoCoordinateDto(13.178f, 48.946f),
        country = country1,
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
        street = StreetDto("Mercedesstraße", ""),
    )

    val address2 = PhysicalPostalAddressVerboseDto(
        geographicCoordinates = GeoCoordinateDto(7.619f, 45.976f, 4478f),
        country = country2,
        postalCode = "70547",
        city = "Atlanta",
        administrativeAreaLevel1 = null,
        administrativeAreaLevel2 = " Fulton County",
        administrativeAreaLevel3 = null,
        district = "TODO",
        companyPostalCode = null,
        industrialZone = "Industrial Zone Two",
        building = "Building Two",
        floor = "Floor Two",
        door = "Door Two",
        street = StreetDto("TODO", ""),
    )

    val legalEntityResponsePool1 = PoolLegalEntityVerboseDto(
        legalName = "Business Partner Name",
        legalAddress = LogisticAddressVerboseDto(
            bpna = "BPNA0000000001XY",
            physicalPostalAddress = address1,
            bpnLegalEntity = "BPNL0000000000XY",
            bpnSite = "BPNS0000000001XY",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        ), legalEntity = LegalEntityVerboseDto(
            bpnl = "BPNL0000000000XY",
            legalShortName = "short1",
            legalForm = legalForm1,
            states = listOf(leBusinessStatus1),
            classifications = listOf(classification1, classification2),
            currentness = Instant.now(),
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    )

    val legalEntityResponsePool2 = PoolLegalEntityVerboseDto(
        legalName = "Another Organisation Corp", legalAddress = LogisticAddressVerboseDto(
            bpna = "BPNA0000000002XY",
            physicalPostalAddress = address2,
            bpnLegalEntity = "BPNL0000000001XZ",
            bpnSite = "BPNS0000000002XY",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        ), legalEntity = LegalEntityVerboseDto(
            bpnl = "BPNL0000000001XZ",
            legalShortName = "short3",
            legalForm = legalForm2,
            states = listOf(leBusinessStatus2),
            classifications = listOf(classification3, classification4),
            currentness = Instant.now(),
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
    )

    val legalEntityResponseGate1 = LegalEntityVerboseDto(
        bpnl = "BPNL0000000000XY",
        legalShortName = "short1",
        legalForm = legalForm1,
        states = listOf(leBusinessStatus1),
        classifications = listOf(classification1, classification2),
        currentness = Instant.now(),
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    val legalEntityResponseGate2 = LegalEntityVerboseDto(
        bpnl = "BPNL0000000001XZ",
        legalShortName = "short3",
        legalForm = legalForm2,
        states = listOf(leBusinessStatus2),
        classifications = listOf(classification3, classification4),
        currentness = Instant.now(),
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    //New Values for Logistic Addresses Tests
    val postalAddressLogisticAddress1 = PhysicalPostalAddressGateDto(
        geographicCoordinates = GeoCoordinateDto(13.178f, 48.946f),
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
        street = StreetGateDto(name = "Mercedesstraße", houseNumber = "", direction = "direction1"),
    )

    val postalAddressLogisticAddress2 = PhysicalPostalAddressGateDto(
        geographicCoordinates = GeoCoordinateDto(7.619f, 45.976f, 4478f),
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
        street = StreetGateDto(name = "TODO", houseNumber = "", direction = "direction1"),
    )

    //New Values for Logistic Address Tests
    val logisticAddress1 = LogisticAddressGateDto(
        physicalPostalAddress = postalAddressLogisticAddress1,
    )

    val logisticAddress2 = LogisticAddressGateDto(
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
        legalNameParts = listOf("Business Partner Name"),
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
        legalNameParts = listOf("Company ABC AG"),
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
        legalNameParts = listOf("Business Partner Name"),
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
        legalNameParts = listOf("Company ABC AG"),
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

    val physicalAddressMinimal = PhysicalPostalAddressGateDto(
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

    val physicalAddressChina = PhysicalPostalAddressGateDto(
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

    val bpPostalAddressInputDtoChina = BusinessPartnerPostalAddressDto(
        addressType = AddressType.LegalAndSiteMainAddress,
        physicalPostalAddress = physicalAddressChina
    )

    val bpInputRequestChina = BusinessPartnerInputRequest(
        externalId = externalId3,
        nameParts = listOf("好公司  合伙制企业"),
        shortName = "short3",
        legalName = "姓名测试",
        legalForm = "股份有限",
        isOwnCompanyData = true,
        identifiers = listOf(
            bpIdentifier1, bpIdentifier2, bpIdentifier1
        ),          // duplicate, but they are elimnated
        classifications = listOf(
            bpClassificationChina, bpClassification3, bpClassificationChina
        ),    // duplicate, but they are elimnated
        states = listOf(bpState2, bpState1),
        roles = listOf(BusinessPartnerRole.CUSTOMER, BusinessPartnerRole.SUPPLIER),
        postalAddress = bpPostalAddressInputDtoChina,
        legalEntityBpn = "BPNL0000000002XY",
        siteBpn = "BPNS0000000003X9",
        addressBpn = "BPNA0000000001XY"
    )

    val bpInputRequestCleaned = BusinessPartnerInputRequest(
        externalId = externalId4,
        nameParts = listOf("Name Part Value"),
        shortName = "Random Short Name",
        legalName = "Random Name Value",
        legalForm = "Random Form Value",
        isOwnCompanyData = true,
        identifiers = listOf(
            bpIdentifier1, bpIdentifier2, bpIdentifier1
        ),
        classifications = listOf(
            bpClassification1, bpClassification3
        ),
        states = listOf(bpState2, bpState1),
        roles = listOf(BusinessPartnerRole.CUSTOMER, BusinessPartnerRole.SUPPLIER),
        postalAddress = bpPostalAddressInputDtoFull,
        legalEntityBpn = "BPNL0000000002XY",
        siteBpn = "BPNS0000000003X9",
        addressBpn = "BPNA0000000001XY"
    )

    val bpInputRequestError = BusinessPartnerInputRequest(
        externalId = externalId5,
        nameParts = listOf("Name Part Value"),
        shortName = "Random Short Name",
        legalName = "Random Name Value",
        legalForm = "Random Form Value",
        isOwnCompanyData = true,
        identifiers = listOf(
            bpIdentifier1, bpIdentifier2, bpIdentifier1
        ),
        classifications = listOf(
            bpClassification1, bpClassification3
        ),
        states = listOf(bpState2, bpState1),
        roles = listOf(BusinessPartnerRole.CUSTOMER, BusinessPartnerRole.SUPPLIER),
        postalAddress = bpPostalAddressInputDtoFull,
        legalEntityBpn = "BPNL0000000002XY",
        siteBpn = "BPNS0000000003X9",
        addressBpn = "BPNA0000000001XY"
    )

    val bpOutputRequestCleaned = BusinessPartnerOutputRequest(
        externalId = externalId4,
        nameParts = listOf("part-cleaned-1", "name-cleaned-2"),
        shortName = "shot-name-cleaned",
        legalName = "legal-name-cleaned",
        legalForm = "legal-form-cleaned",
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
        classifications = listOf(
            BusinessPartnerClassificationDto(
                type = ClassificationType.NACE,
                code = "code-1-cleaned",
                value = "value-1-cleaned"
            ),
            BusinessPartnerClassificationDto(
                type = ClassificationType.NAF,
                code = "code-2-cleaned",
                value = "value-2-cleaned"
            ),
        ),
        states = listOf(
            BusinessPartnerStateDto(
                validFrom = LocalDateTime.of(2020, 9, 22, 15, 50),
                validTo = LocalDateTime.of(2023, 10, 23, 16, 40),
                type = BusinessStateType.INACTIVE,
                description = "business-state-description-1"
            ),
            BusinessPartnerStateDto(
                validFrom = LocalDateTime.of(2000, 8, 21, 14, 30),
                validTo = LocalDateTime.of(2020, 9, 22, 15, 50),
                type = BusinessStateType.ACTIVE,
                description = "business-state-description-2"
            )
        ),
        roles = listOf(
            BusinessPartnerRole.CUSTOMER,
            BusinessPartnerRole.SUPPLIER
        ),
        postalAddress = BusinessPartnerPostalAddressDto(
            addressType = AddressType.AdditionalAddress,
            physicalPostalAddress = PhysicalPostalAddressGateDto(
                geographicCoordinates = GeoCoordinateDto(0.5f, 0.5f, 0.5f),
                country = CountryCode.PT,
                administrativeAreaLevel1 = "PT-PT",
                administrativeAreaLevel2 = "pt-admin-level-2-cleaned",
                administrativeAreaLevel3 = "pt-admin-level-3-cleaned",
                postalCode = "phys-postal-code-cleaned",
                city = "city",
                district = "district",
                street = StreetGateDto(
                    name = "name",
                    houseNumber = "house-number",
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
                door = "door"
            ),
            alternativePostalAddress = AlternativePostalAddressGateDto(
                geographicCoordinates = GeoCoordinateDto(0.6f, 0.6f, 0.6f),
                country = CountryCode.PT,
                administrativeAreaLevel1 = "PT-PT",
                postalCode = "postal-code-cleaned",
                city = "alt-city-cleaned",
                deliveryServiceNumber = "delivery-service-number-cleaned",
                deliveryServiceQualifier = "delivery-service-qualifier-cleaned",
                deliveryServiceType = DeliveryServiceType.PO_BOX
            )
        ),
        legalEntityBpn = "000000123AAA123",
        siteBpn = "000000123BBB222",
        addressBpn = "000000123CCC333"
    )

}