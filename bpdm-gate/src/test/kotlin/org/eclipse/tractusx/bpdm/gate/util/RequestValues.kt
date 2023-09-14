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

import org.eclipse.tractusx.bpdm.common.dto.*
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.request.*

object RequestValues {

    val bpIdentifier1 = BusinessPartnerIdentifierDto(
        type = CommonValues.identifierTypeTechnicalKey1,
        value = CommonValues.identifierValue1,
        issuingBody = CommonValues.identifierIssuingBody1
    )

    val bpIdentifier2 = BusinessPartnerIdentifierDto(
        type = CommonValues.identifierTypeTechnicalKey2,
        value = CommonValues.identifierValue2,
        issuingBody = CommonValues.identifierIssuingBody2
    )

    val bpIdentifier3 = BusinessPartnerIdentifierDto(
        type = CommonValues.identifierTypeTechnicalKey3,
        value = CommonValues.identifierValue3,
        issuingBody = null
    )

    val identifier1 =
        LegalEntityIdentifierDto(
            value = CommonValues.identifierValue1,
            type = CommonValues.identifierTypeTechnicalKey1,
            issuingBody = CommonValues.identifierIssuingBodyName1,
        )
    val identifier2 =
        LegalEntityIdentifierDto(
            value = CommonValues.identifierValue2,
            CommonValues.identifierTypeTechnicalKey2,
            CommonValues.identifierIssuingBodyName2,
        )
    val identifier3 =
        LegalEntityIdentifierDto(
            value = CommonValues.identifierValue3,
            type = CommonValues.identifierTypeTechnicalKey3,
            issuingBody = CommonValues.identifierIssuingBodyName3,
        )
    val identifier4 =
        LegalEntityIdentifierDto(
            value = CommonValues.identifierValue4,
            type = CommonValues.identifierTypeTechnicalKey4,
            issuingBody = CommonValues.identifierIssuingBodyName4,
        )

    val genericIdentifier =
        GenericIdentifierDto(
            CommonValues.identifierValue1,
            CommonValues.identifierTypeTechnicalKey1
        )

    val name1 = NameDto(value = CommonValues.name1, shortName = CommonValues.shortName1)

    val bpState1 = BusinessPartnerStateDto(
        validFrom = CommonValues.businessStatusValidFrom1,
        validTo = CommonValues.businessStatusValidUntil1,
        type = BusinessStateType.ACTIVE,
        description = CommonValues.businessStatusDescription1
    )

    val bpState2 = BusinessPartnerStateDto(
        validFrom = CommonValues.businessStatusValidFrom2,
        validTo = CommonValues.businessStatusValidUntil2,
        type = BusinessStateType.INACTIVE,
        description = CommonValues.businessStatusDescription2
    )

    val leBusinessStatus1 = LegalEntityStateDto(
        description = CommonValues.businessStatusDescription1,
        validFrom = CommonValues.businessStatusValidFrom1,
        validTo = CommonValues.businessStatusValidUntil1,
        type = CommonValues.businessStateType1
    )

    val leBusinessStatus2 = LegalEntityStateDto(
        description = CommonValues.businessStatusDescription2,
        validFrom = CommonValues.businessStatusValidFrom2,
        validTo = CommonValues.businessStatusValidUntil2,
        type = CommonValues.businessStateType2
    )

    val siteBusinessStatus1 = SiteStateDto(
        description = CommonValues.businessStatusDescription1,
        validFrom = CommonValues.businessStatusValidFrom1,
        validTo = CommonValues.businessStatusValidUntil1,
        type = CommonValues.businessStateType1
    )

    val siteBusinessStatus2 = SiteStateDto(
        description = CommonValues.businessStatusDescription2,
        validFrom = CommonValues.businessStatusValidFrom2,
        validTo = CommonValues.businessStatusValidUntil2,
        type = CommonValues.businessStateType2
    )

    val classification1 = ClassificationDto(
        type = CommonValues.classificationType,
        code = CommonValues.classificationCode1,
        value = CommonValues.classificationValue1
    )

    val classification2 = ClassificationDto(
        type = CommonValues.classificationType,
        code = CommonValues.classificationCode2,
        value = CommonValues.classificationValue2
    )

    val classification3 = ClassificationDto(
        type = CommonValues.classificationType,
        code = CommonValues.classificationCode3,
        value = CommonValues.classificationValue3
    )

    val classification4 = ClassificationDto(
        type = CommonValues.classificationType,
        code = CommonValues.classificationCode4,
        value = CommonValues.classificationValue4
    )

    val geoCoordinate1 = GeoCoordinateDto(CommonValues.geoCoordinates1.first, CommonValues.geoCoordinates1.second)
    val geoCoordinate2 = GeoCoordinateDto(CommonValues.geoCoordinates2.first, CommonValues.geoCoordinates2.second)

    val physicalAddressMinimal = PhysicalPostalAddressGateDto(
        country = CommonValues.country1,
        city = CommonValues.city1,
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

    val alternativeAddressMinimal = AlternativePostalAddressDto(
        country = CommonValues.country1,
        city = CommonValues.city1,
        deliveryServiceType = DeliveryServiceType.PO_BOX,
        deliveryServiceQualifier = null,
        deliveryServiceNumber = "1234",
        geographicCoordinates = null,
        postalCode = null,
        administrativeAreaLevel1 = null,
    )

    val postalAddress1 = PhysicalPostalAddressGateDto(
        geographicCoordinates = geoCoordinate1,
        country = CommonValues.country1,
        postalCode = CommonValues.postCode1,
        city = CommonValues.city1,
        administrativeAreaLevel1 = CommonValues.adminAreaLevel1RegionCode_1, //null,
        administrativeAreaLevel2 = CommonValues.county1,
        administrativeAreaLevel3 = null,
        district = CommonValues.district1,
        companyPostalCode = null,
        industrialZone = CommonValues.industrialZone1,
        building = CommonValues.building1,
        floor = CommonValues.floor1,
        door = CommonValues.door1,
        street = StreetGateDto(name = CommonValues.street1, houseNumber = CommonValues.houseNumber1, direction = CommonValues.direction1),
    )

    val postalAddress2 = PhysicalPostalAddressGateDto(
        geographicCoordinates = geoCoordinate2,
        country = CommonValues.country2,
        postalCode = CommonValues.postCode2,
        city = CommonValues.city2,
        administrativeAreaLevel1 = CommonValues.adminAreaLevel1RegionCode_2,
        administrativeAreaLevel2 = CommonValues.county2,
        administrativeAreaLevel3 = null,
        district = CommonValues.district2,
        companyPostalCode = null,
        industrialZone = CommonValues.industrialZone2,
        building = CommonValues.building2,
        floor = CommonValues.floor2,
        door = CommonValues.door2,
        street = StreetGateDto(name = CommonValues.street2, houseNumber = CommonValues.houseNumber2, direction = CommonValues.direction2),
    )
    val postalAddress3 = PhysicalPostalAddressGateDto(
        geographicCoordinates = geoCoordinate1,
        country = CommonValues.country1,
        postalCode = CommonValues.postCode1,
        city = CommonValues.city1,
        administrativeAreaLevel1 = null,
        administrativeAreaLevel2 = CommonValues.county1,
        administrativeAreaLevel3 = null,
        district = CommonValues.district1,
        companyPostalCode = null,
        industrialZone = CommonValues.industrialZone1,
        building = CommonValues.building1,
        floor = CommonValues.floor1,
        door = CommonValues.door1,
        street = StreetGateDto(name = CommonValues.street1, houseNumber = CommonValues.houseNumber1, direction = CommonValues.direction1),
    )

    //New Values for Logistic Addresses Tests
    val postalAddressLogisticAddress1 = PhysicalPostalAddressGateDto(
        geographicCoordinates = geoCoordinate1,
        country = CommonValues.country1,
        postalCode = CommonValues.postCode1,
        city = CommonValues.city1,
        administrativeAreaLevel1 = CommonValues.adminAreaLevel1RegionCode_1,
        administrativeAreaLevel2 = CommonValues.county1,
        administrativeAreaLevel3 = null,
        district = CommonValues.district1,
        companyPostalCode = null,
        industrialZone = CommonValues.industrialZone1,
        building = CommonValues.building1,
        floor = CommonValues.floor1,
        door = CommonValues.door1,
        street = StreetGateDto(name = CommonValues.street1, houseNumber = CommonValues.houseNumber1, direction = CommonValues.direction1),
    )

    val postalAddressLogisticAddress2 = PhysicalPostalAddressGateDto(
        geographicCoordinates = geoCoordinate2,
        country = CommonValues.country2,
        postalCode = CommonValues.postCode2,
        city = CommonValues.city2,
        administrativeAreaLevel1 = CommonValues.adminAreaLevel1RegionCode_2,
        administrativeAreaLevel2 = CommonValues.county2,
        administrativeAreaLevel3 = null,
        district = CommonValues.district2,
        companyPostalCode = null,
        industrialZone = CommonValues.industrialZone2,
        building = CommonValues.building2,
        floor = CommonValues.floor2,
        door = CommonValues.door2,
        street = StreetGateDto(name = CommonValues.street2, houseNumber = CommonValues.houseNumber2, direction = CommonValues.direction2),
    )

    val address1 = LogisticAddressGateDto(
        physicalPostalAddress = postalAddress1,
    )

    val address2 = LogisticAddressGateDto(
        physicalPostalAddress = postalAddress2,
    )

    val address3 = LogisticAddressGateDto(
        physicalPostalAddress = postalAddress3,
    )

    //New Values for Logistic Address Tests
    val logisticAddress1 = LogisticAddressGateDto(
        physicalPostalAddress = postalAddressLogisticAddress1,
    )

    val logisticAddress2 = LogisticAddressGateDto(
        physicalPostalAddress = postalAddressLogisticAddress2,
    )


    val bpPostalAddressInputDtoMinimal = BusinessPartnerPostalAddressInputDto(
        addressType = null,
        physicalPostalAddress = physicalAddressMinimal
    )

    val bpPostalAddressInputDtoDefault = BusinessPartnerPostalAddressInputDto(
        addressType = AddressType.LegalAddress,
        physicalPostalAddress = postalAddress2,
        alternativePostalAddress = alternativeAddressMinimal
    )

    val bpInputRequestMinimal = BusinessPartnerInputRequest(
        externalId = CommonValues.externalId2,
        postalAddress = bpPostalAddressInputDtoMinimal
    )

    val bpInputRequestDefault = BusinessPartnerInputRequest(
        externalId = CommonValues.externalId1,
        nameParts = listOf(CommonValues.name1, CommonValues.name2, CommonValues.name3, CommonValues.name4),
        shortName = CommonValues.shortName1,
        legalForm = CommonValues.legalFormName1,
        isOwner = true,
        identifiers = listOf(bpIdentifier1, bpIdentifier2, bpIdentifier3),
        classifications = listOf(classification1, classification2, classification3),
        states = listOf(bpState1, bpState2),
        roles = listOf(BusinessPartnerRole.SUPPLIER),
        postalAddress = bpPostalAddressInputDtoDefault
    )

    val legalEntity1 = LegalEntityDto(
        legalShortName = CommonValues.shortName1,
        legalForm = CommonValues.legalFormTechnicalKey1,
        states = listOf(leBusinessStatus1),
        classifications = listOf(classification1, classification2),
    )

    val legalEntity2 = LegalEntityDto(
        legalShortName = CommonValues.shortName3,
        legalForm = CommonValues.legalFormTechnicalKey2,
        states = listOf(leBusinessStatus2),
        classifications = listOf(classification3, classification4),
    )

    val legalEntity3 = LegalEntityDto(
        legalShortName = CommonValues.shortName1,
        legalForm = CommonValues.legalFormTechnicalKey1,
        states = listOf(leBusinessStatus1),
        classifications = listOf(classification1, classification2),
    )

    val legalEntityGateInputRequest1 = LegalEntityGateInputRequest(
        legalEntity = legalEntity1,
        legalAddress = address1,
        legalNameParts = listOf(CommonValues.name1),
        externalId = CommonValues.externalId1,

        )

    val legalEntityGateInputRequest2 = LegalEntityGateInputRequest(
        legalEntity = legalEntity2,
        legalAddress = address2,
        legalNameParts = listOf(CommonValues.name2),
        externalId = CommonValues.externalId2,

        )

    val legalEntityGateInputRequest3 = LegalEntityGateInputRequest(
        legalEntity = legalEntity3,
        legalAddress = address3,
        legalNameParts = listOf(CommonValues.name1),
        externalId = CommonValues.externalId3,

        )

    val site1 = SiteGateDto(
        nameParts = listOf(CommonValues.nameSite1),
        states = listOf(siteBusinessStatus1)
    )

    val site2 = SiteGateDto(
        nameParts = listOf(CommonValues.nameSite2),
        states = listOf(siteBusinessStatus2)
    )

    val siteGateInputRequest1 = SiteGateInputRequest(
        site = site1,
        externalId = CommonValues.externalIdSite1,
        legalEntityExternalId = CommonValues.externalId1,
        mainAddress = address1
    )

    val siteGateInputRequest2 = SiteGateInputRequest(
        site = site2,
        externalId = CommonValues.externalIdSite2,
        legalEntityExternalId = CommonValues.externalId2,
        mainAddress = address2
    )

    //Output values for sites
    val siteGateOutputRequest1 = SiteGateOutputRequest(
        site = site1,
        externalId = CommonValues.externalIdSite1,
        legalEntityExternalId = CommonValues.externalId1,
        mainAddress = AddressGateOutputChildRequest(address1, CommonValues.bpnAddress1),
        bpn = CommonValues.bpnSite1
    )

    val siteGateOutputRequest2 = SiteGateOutputRequest(
        site = site2,
        externalId = CommonValues.externalIdSite2,
        legalEntityExternalId = CommonValues.externalId2,
        mainAddress = AddressGateOutputChildRequest(address2, CommonValues.bpnAddress2),
        bpn = CommonValues.bpnSite2
    )

    val addressGateInputRequest1 = AddressGateInputRequest(
        address = address1.copy(
            nameParts = listOf(CommonValues.name1),
            identifiers = listOf(
                AddressIdentifierDto(identifier1.value!!, identifier1.type!!)
            )
        ),
        externalId = CommonValues.externalIdAddress1,
        legalEntityExternalId = CommonValues.externalId1,

        )

    val addressGateInputRequest2 = AddressGateInputRequest(
        address = address2.copy(
            nameParts = listOf(CommonValues.name2),
            identifiers = listOf(
                AddressIdentifierDto(identifier1.value!!, identifier1.type!!)
            )
        ),
        externalId = CommonValues.externalIdAddress2,
        siteExternalId = CommonValues.externalIdSite1,

        )

    //Output Endpoint Values
    val addressGateOutputRequest1 = AddressGateOutputRequest(
        address = address1.copy(
            nameParts = listOf(CommonValues.name1),
            identifiers = listOf(
                AddressIdentifierDto(identifier1.value!!, identifier1.type!!)
            )
        ),
        externalId = CommonValues.externalIdAddress1,
        legalEntityExternalId = CommonValues.externalId1,
        bpn = CommonValues.bpnAddress1
    )

    val addressGateOutputRequest2 = AddressGateOutputRequest(
        address = address2.copy(
            nameParts = listOf(CommonValues.name2),
            identifiers = listOf(
                AddressIdentifierDto(identifier1.value!!, identifier1.type!!)
            )
        ),
        externalId = CommonValues.externalIdAddress2,
        siteExternalId = CommonValues.externalIdSite1,
        bpn = CommonValues.bpnAddress2
    )

    //Output Values
    val legalEntityGateOutputRequest1 = LegalEntityGateOutputRequest(
        legalEntity = legalEntity1,
        legalAddress = AddressGateOutputChildRequest(address1, CommonValues.bpnAddress1),
        legalNameParts = listOf(CommonValues.name1),
        externalId = CommonValues.externalId1,
        bpn = CommonValues.bpn1
    )

    val legalEntityGateOutputRequest2 = LegalEntityGateOutputRequest(
        legalEntity = legalEntity2,
        legalAddress = AddressGateOutputChildRequest(address2, CommonValues.bpnAddress2),
        legalNameParts = listOf(CommonValues.name2),
        externalId = CommonValues.externalId2,
        bpn = CommonValues.bpn2
    )
}