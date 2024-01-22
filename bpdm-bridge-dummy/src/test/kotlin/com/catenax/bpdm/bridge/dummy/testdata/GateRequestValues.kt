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

package com.catenax.bpdm.bridge.dummy.testdata

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.request.AddressGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.LegalEntityGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.SiteGateInputRequest
import org.eclipse.tractusx.bpdm.test.testdata.gate.BusinessPartnerVerboseValues

object GateRequestValues {

    val identifier1 =
        LegalEntityIdentifierDto(
            value = BusinessPartnerVerboseValues.identifierValue1,
            type = BusinessPartnerVerboseValues.identifierTypeTechnicalKey1,
            issuingBody = BusinessPartnerVerboseValues.identifierIssuingBodyName1,
        )
    val identifier2 =
        LegalEntityIdentifierDto(
            value = BusinessPartnerVerboseValues.identifierValue2,
            BusinessPartnerVerboseValues.identifierTypeTechnicalKey2,
            BusinessPartnerVerboseValues.identifierIssuingBodyName2,
        )
    val identifier3 =
        LegalEntityIdentifierDto(
            value = BusinessPartnerVerboseValues.identifierValue3,
            type = BusinessPartnerVerboseValues.identifierTypeTechnicalKey3,
            issuingBody = BusinessPartnerVerboseValues.identifierIssuingBodyName3,
        )
    val identifier4 =
        LegalEntityIdentifierDto(
            value = BusinessPartnerVerboseValues.identifierValue5,
            type = BusinessPartnerVerboseValues.identifierTypeTechnicalKey4,
            issuingBody = BusinessPartnerVerboseValues.identifierIssuingBodyName4,
        )

    val identifier5 =
        LegalEntityIdentifierDto(
            value = BusinessPartnerVerboseValues.identifierValue5,
            type = BusinessPartnerVerboseValues.identifierTypeTechnicalKey5,
            issuingBody = BusinessPartnerVerboseValues.identifierIssuingBodyName5,
        )

    val leBusinessStatus1 = LegalEntityStateDto(
        description = BusinessPartnerVerboseValues.businessStatusOfficialDenotation1,
        validFrom = BusinessPartnerVerboseValues.businessStatusValidFrom1,
        validTo = BusinessPartnerVerboseValues.businessStatusValidUntil1,
        type = BusinessPartnerVerboseValues.businessStateType1
    )

    val leBusinessStatus2 = LegalEntityStateDto(
        description = BusinessPartnerVerboseValues.businessStatusOfficialDenotation2,
        validFrom = BusinessPartnerVerboseValues.businessStatusValidFrom2,
        validTo = BusinessPartnerVerboseValues.businessStatusValidUntil2,
        type = BusinessPartnerVerboseValues.businessStateType2
    )

    val siteBusinessStatus1 = SiteStateDto(
        description = BusinessPartnerVerboseValues.businessStatusOfficialDenotation1,
        validFrom = BusinessPartnerVerboseValues.businessStatusValidFrom1,
        validTo = BusinessPartnerVerboseValues.businessStatusValidUntil1,
        type = BusinessPartnerVerboseValues.businessStateType1
    )

    val siteBusinessStatus2 = SiteStateDto(
        description = BusinessPartnerVerboseValues.businessStatusOfficialDenotation2,
        validFrom = BusinessPartnerVerboseValues.businessStatusValidFrom2,
        validTo = BusinessPartnerVerboseValues.businessStatusValidUntil2,
        type = BusinessPartnerVerboseValues.businessStateType2
    )

    val classification1 = LegalEntityClassificationDto(
        type = BusinessPartnerVerboseValues.classificationType,
        code = BusinessPartnerVerboseValues.classificationCode1,
        value = BusinessPartnerVerboseValues.classificationValue1
    )

    val classification2 = LegalEntityClassificationDto(
        type = BusinessPartnerVerboseValues.classificationType,
        code = BusinessPartnerVerboseValues.classificationCode2,
        value = BusinessPartnerVerboseValues.classificationValue2
    )

    val classification3 = LegalEntityClassificationDto(
        type = BusinessPartnerVerboseValues.classificationType,
        code = BusinessPartnerVerboseValues.classificationCode3,
        value = BusinessPartnerVerboseValues.classificationValue3
    )

    val classification4 = LegalEntityClassificationDto(
        type = BusinessPartnerVerboseValues.classificationType,
        code = BusinessPartnerVerboseValues.classificationCode4,
        value = BusinessPartnerVerboseValues.classificationValue4
    )

    val geoCoordinate1 = GeoCoordinateDto(BusinessPartnerVerboseValues.geoCoordinates1.first, BusinessPartnerVerboseValues.geoCoordinates1.second)
    val geoCoordinate2 = GeoCoordinateDto(BusinessPartnerVerboseValues.geoCoordinates2.first, BusinessPartnerVerboseValues.geoCoordinates2.second)

    val postalAddress1 = PhysicalPostalAddressDto(
        geographicCoordinates = geoCoordinate1,
        country = CountryCode.DE,
        postalCode = BusinessPartnerVerboseValues.postCode1,
        city = BusinessPartnerVerboseValues.city1,
        administrativeAreaLevel1 = null,
        administrativeAreaLevel2 = BusinessPartnerVerboseValues.county1,
        administrativeAreaLevel3 = null,
        district = BusinessPartnerVerboseValues.district1,
        companyPostalCode = null,
        industrialZone = BusinessPartnerVerboseValues.industrialZone1,
        building = BusinessPartnerVerboseValues.building1,
        floor = BusinessPartnerVerboseValues.floor1,
        door = BusinessPartnerVerboseValues.door1,
        street = StreetDto(name = BusinessPartnerVerboseValues.street1, houseNumber = BusinessPartnerVerboseValues.houseNumber1, direction = BusinessPartnerVerboseValues.direction1),
    )

    val postalAddress2 = PhysicalPostalAddressDto(
        geographicCoordinates = geoCoordinate2,
        country = CountryCode.US,
        postalCode = BusinessPartnerVerboseValues.postCode2,
        city = BusinessPartnerVerboseValues.city2,
        administrativeAreaLevel1 = null,
        administrativeAreaLevel2 = BusinessPartnerVerboseValues.county2,
        administrativeAreaLevel3 = null,
        district = BusinessPartnerVerboseValues.district2,
        companyPostalCode = null,
        industrialZone = BusinessPartnerVerboseValues.industrialZone2,
        building = BusinessPartnerVerboseValues.building2,
        floor = BusinessPartnerVerboseValues.floor2,
        door = BusinessPartnerVerboseValues.door2,
        street = StreetDto(name = BusinessPartnerVerboseValues.street2, houseNumber = BusinessPartnerVerboseValues.houseNumber2, direction = BusinessPartnerVerboseValues.direction2),
    )
    val postalAddress3 = PhysicalPostalAddressDto(
        geographicCoordinates = geoCoordinate1,
        country = CountryCode.DE,
        postalCode = BusinessPartnerVerboseValues.postCode1,
        city = BusinessPartnerVerboseValues.city1,
        administrativeAreaLevel1 = null,
        administrativeAreaLevel2 = BusinessPartnerVerboseValues.county1,
        administrativeAreaLevel3 = null,
        district = BusinessPartnerVerboseValues.district1,
        companyPostalCode = null,
        industrialZone = BusinessPartnerVerboseValues.industrialZone1,
        building = BusinessPartnerVerboseValues.building1,
        floor = BusinessPartnerVerboseValues.floor1,
        door = BusinessPartnerVerboseValues.door1,
        street = StreetDto(name = BusinessPartnerVerboseValues.street1, houseNumber = BusinessPartnerVerboseValues.houseNumber1, direction = BusinessPartnerVerboseValues.direction1),
    )

    //New Values for Logistic Addresses Tests
    val postalAddressLogisticAddress1 = PhysicalPostalAddressDto(
        geographicCoordinates = geoCoordinate1,
        country = CountryCode.DE,
        postalCode = BusinessPartnerVerboseValues.postCode1,
        city = BusinessPartnerVerboseValues.city1,
        administrativeAreaLevel1 = null,
        administrativeAreaLevel2 = BusinessPartnerVerboseValues.county1,
        administrativeAreaLevel3 = null,
        district = BusinessPartnerVerboseValues.district1,
        companyPostalCode = null,
        industrialZone = BusinessPartnerVerboseValues.industrialZone1,
        building = BusinessPartnerVerboseValues.building1,
        floor = BusinessPartnerVerboseValues.floor1,
        door = BusinessPartnerVerboseValues.door1,
        street = StreetDto(name = BusinessPartnerVerboseValues.street1, houseNumber = BusinessPartnerVerboseValues.houseNumber1, direction = BusinessPartnerVerboseValues.direction1),
    )

    val postalAddressLogisticAddress2 = PhysicalPostalAddressDto(
        geographicCoordinates = geoCoordinate2,
        country = CountryCode.US,
        postalCode = BusinessPartnerVerboseValues.postCode2,
        city = BusinessPartnerVerboseValues.city2,
        administrativeAreaLevel1 = null,
        administrativeAreaLevel2 = BusinessPartnerVerboseValues.county2,
        administrativeAreaLevel3 = null,
        district = BusinessPartnerVerboseValues.district2,
        companyPostalCode = null,
        industrialZone = BusinessPartnerVerboseValues.industrialZone2,
        building = BusinessPartnerVerboseValues.building2,
        floor = BusinessPartnerVerboseValues.floor2,
        door = BusinessPartnerVerboseValues.door2,
        street = StreetDto(name = BusinessPartnerVerboseValues.street2, houseNumber = BusinessPartnerVerboseValues.houseNumber2, direction = BusinessPartnerVerboseValues.direction2),
    )

    val address1 = LogisticAddressDto(
        physicalPostalAddress = postalAddress1,
    )

    val address2 = LogisticAddressDto(
        physicalPostalAddress = postalAddress2,
    )

    val address3 = LogisticAddressDto(
        physicalPostalAddress = postalAddress3,
    )

    //New Values for Logistic Address Tests
    val logisticAddress1 = LogisticAddressDto(
        physicalPostalAddress = postalAddressLogisticAddress1,
    )

    val logisticAddress2 = LogisticAddressDto(
        physicalPostalAddress = postalAddressLogisticAddress2,
    )


    val legalEntity1 = LegalEntityDto(
        identifiers = listOf(identifier1, identifier2),
        legalNameParts = listOf(BusinessPartnerVerboseValues.name1),
        legalShortName = BusinessPartnerVerboseValues.shortName1,
        legalForm = BusinessPartnerVerboseValues.legalFormTechnicalKey1,
        states = listOf(leBusinessStatus1),
        classifications = listOf(classification1, classification2),
    )

    val legalEntity2 = LegalEntityDto(
        identifiers = listOf(identifier3, identifier4),
        legalNameParts = listOf(BusinessPartnerVerboseValues.name3),
        legalShortName = BusinessPartnerVerboseValues.shortName3,
        legalForm = BusinessPartnerVerboseValues.legalFormTechnicalKey2,
        states = listOf(leBusinessStatus2),
        classifications = listOf(classification3, classification4),
    )

    val legalEntity3 = LegalEntityDto(
        identifiers = listOf(identifier5),
        legalNameParts = listOf(BusinessPartnerVerboseValues.name1),
        legalShortName = BusinessPartnerVerboseValues.shortName1,
        legalForm = BusinessPartnerVerboseValues.legalFormTechnicalKey1,
        states = listOf(leBusinessStatus1),
        classifications = listOf(classification1, classification2),
    )

    val legalEntityGateInputRequest1 = LegalEntityGateInputRequest(
        legalEntity = legalEntity1,
        legalAddress = address1,
        externalId = BusinessPartnerVerboseValues.externalId1,
    )

    val legalEntityGateInputRequest2 = LegalEntityGateInputRequest(
        legalEntity = legalEntity2,
        legalAddress = address2,
        externalId = BusinessPartnerVerboseValues.externalId2,
    )

    val legalEntityGateInputRequest3 = LegalEntityGateInputRequest(
        legalEntity = legalEntity3,
        legalAddress = address3,
        externalId = BusinessPartnerVerboseValues.externalId3,
    )


    val site1 = SiteGateDto(
        nameParts = listOf(BusinessPartnerVerboseValues.nameSite1),
        states = listOf(siteBusinessStatus1)
    )

    val site2 = SiteGateDto(
        nameParts = listOf(BusinessPartnerVerboseValues.nameSite2),
        states = listOf(siteBusinessStatus2)
    )

    val siteGateInputRequest1 = SiteGateInputRequest(
        site = site1,
        externalId = BusinessPartnerVerboseValues.externalIdSite1,
        legalEntityExternalId = BusinessPartnerVerboseValues.externalId1,
        mainAddress = address1
    )

    val siteGateInputRequest2 = SiteGateInputRequest(
        site = site2,
        externalId = BusinessPartnerVerboseValues.externalIdSite2,
        legalEntityExternalId = BusinessPartnerVerboseValues.externalId2,
        mainAddress = address2
    )

    val addressGateInputRequest1 = AddressGateInputRequest(
        address = address1.copy(
            nameParts = listOf(BusinessPartnerVerboseValues.name1),
            identifiers = listOf(
                //AddressIdentifierDto(BusinessPartnerVerboseValues.identifierValue1, BusinessPartnerVerboseValues.identifierTypeTechnicalKey1)
            )
        ),
        externalId = BusinessPartnerVerboseValues.externalIdAddress1,
        legalEntityExternalId = BusinessPartnerVerboseValues.externalId1,

        )

    val addressGateInputRequest2 = AddressGateInputRequest(
        address = address2.copy(
            nameParts = listOf(BusinessPartnerVerboseValues.name2),
            identifiers = listOf(
                //AddressIdentifierDto(BusinessPartnerVerboseValues.identifierValue2, BusinessPartnerVerboseValues.identifierTypeTechnicalKey2)
            )
        ),
        externalId = BusinessPartnerVerboseValues.externalIdAddress2,
        siteExternalId = BusinessPartnerVerboseValues.externalIdSite1,

        )

}