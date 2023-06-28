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

import org.eclipse.tractusx.bpdm.common.dto.BasePhysicalAddressDto
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.dto.StreetDto
import org.eclipse.tractusx.bpdm.common.dto.response.*
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameVerboseDto
import org.eclipse.tractusx.bpdm.common.service.toDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.*
import java.time.Instant

object ResponseValues {
    val language1 = TypeKeyNameVerboseDto(
        technicalKey = CommonValues.language1,
        name = CommonValues.language1.getName()
    )

    val language2 = TypeKeyNameVerboseDto(
        technicalKey = CommonValues.language2,
        name = CommonValues.language2.getName()
    )

    val identifier1 = LegalEntityIdentifierVerboseDto(
        value = CommonValues.identifierValue1,
        type = TypeKeyNameVerboseDto(
            technicalKey = CommonValues.identifierTypeTechnicalKey1,
            name = CommonValues.identifierTypeName1,
        ),
        issuingBody = CommonValues.identifierIssuingBody1
    )
    val identifier2 = LegalEntityIdentifierVerboseDto(
        value = CommonValues.identifierValue2,
        type = TypeKeyNameVerboseDto(
            technicalKey = CommonValues.identifierTypeTechnicalKey2,
            name = CommonValues.identifierTypeName2,
        ),
        issuingBody = CommonValues.identifierIssuingBody2

    )
    val identifier3 = LegalEntityIdentifierVerboseDto(
        value = CommonValues.identifierValue3,
        type = TypeKeyNameVerboseDto(
            technicalKey = CommonValues.identifierTypeTechnicalKey3,
            name = CommonValues.identifierTypeName3,
        ),
        issuingBody = CommonValues.identifierIssuingBody3

    )
    val identifier4 = LegalEntityIdentifierVerboseDto(
        value = CommonValues.identifierValue4,
        type = TypeKeyNameVerboseDto(
            technicalKey = CommonValues.identifierTypeTechnicalKey4,
            name = CommonValues.identifierTypeName4,
        ),
        issuingBody = CommonValues.identifierIssuingBody4

    )

    val legalForm1 = LegalFormDto(
        technicalKey = CommonValues.legalFormTechnicalKey1,
        name = CommonValues.legalFormName1,
        abbreviation = CommonValues.legalFormAbbreviation1,
    )

    val legalForm2 = LegalFormDto(
        technicalKey = CommonValues.legalFormTechnicalKey2,
        name = CommonValues.legalFormName2,
        abbreviation = CommonValues.legalFormAbbreviation2,
    )

    val leBusinessStatus1 = LegalEntityStateVerboseDto(
        officialDenotation = CommonValues.businessStatusOfficialDenotation1,
        validFrom = CommonValues.businessStatusValidFrom1,
        validTo = CommonValues.businessStatusValidUntil1,
        type = TypeKeyNameVerboseDto(
            technicalKey = CommonValues.businessStateType1,
            name = CommonValues.businessStateType1.getTypeName(),
        )
    )

    val leBusinessStatus2 = LegalEntityStateVerboseDto(
        officialDenotation = CommonValues.businessStatusOfficialDenotation2,
        validFrom = CommonValues.businessStatusValidFrom2,
        validTo = CommonValues.businessStatusValidUntil2,
        type = TypeKeyNameVerboseDto(
            technicalKey = CommonValues.businessStateType2,
            name = CommonValues.businessStateType2.getTypeName(),
        )
    )

    val classification1 = ClassificationVerboseDto(
        value = CommonValues.classificationValue1,
        code = CommonValues.classificationCode1,
        type = CommonValues.classificationType.toDto()
    )

    val classification2 = ClassificationVerboseDto(
        value = CommonValues.classificationValue2,
        code = CommonValues.classificationCode2,
        type = CommonValues.classificationType.toDto()
    )

    val classification3 = ClassificationVerboseDto(
        value = CommonValues.classificationValue3,
        code = CommonValues.classificationCode3,
        type = CommonValues.classificationType.toDto()
    )

    val classification4 = ClassificationVerboseDto(
        value = CommonValues.classificationValue4,
        code = CommonValues.classificationCode4,
        type = CommonValues.classificationType.toDto()
    )


    val country1 = TypeKeyNameVerboseDto(
        technicalKey = CommonValues.country1,
        name = CommonValues.country1.getName()
    )
    val country2 = TypeKeyNameVerboseDto(
        technicalKey = CommonValues.country2,
        name = CommonValues.country2.getName()
    )

    val geoCoordinate1 = GeoCoordinateDto(CommonValues.geoCoordinates1.first, CommonValues.geoCoordinates1.second)
    val geoCoordinate2 = GeoCoordinateDto(CommonValues.geoCoordinates2.first, CommonValues.geoCoordinates2.second)

    val address1 = PhysicalPostalAddressVerboseDto(
        basePhysicalAddress = BasePhysicalAddressDto(
            industrialZone = CommonValues.industrialZone1,
            building = CommonValues.building1,
            floor = CommonValues.floor1,
            door = CommonValues.door1
        ),
        areaPart = AreaDistrictVerboseDto(
            administrativeAreaLevel1 = CommonValues.adminAreaLevel1Region1,
            administrativeAreaLevel2 = CommonValues.county1,
            district = CommonValues.district1,
        ),
        street = StreetDto(CommonValues.street1, CommonValues.houseNumber1),
        baseAddress = BasePostalAddressVerboseDto(
            geographicCoordinates = geoCoordinate1,
            country = country1,
            postalCode = CommonValues.postCode1,
            city = CommonValues.city1,
        )
    )

    val address2 = PhysicalPostalAddressVerboseDto(
        basePhysicalAddress = BasePhysicalAddressDto(
            industrialZone = CommonValues.industrialZone2,
            building = CommonValues.building2,
            floor = CommonValues.floor2,
            door = CommonValues.door2
        ),
        areaPart = AreaDistrictVerboseDto(
            administrativeAreaLevel1 = CommonValues.adminAreaLevel1Region2,
            administrativeAreaLevel2 = CommonValues.county2,
            district = CommonValues.district2,
        ),
        street = StreetDto(CommonValues.street2, CommonValues.houseNumber2),
        baseAddress = BasePostalAddressVerboseDto(
            geographicCoordinates = geoCoordinate2,
            country = country2,
            postalCode = CommonValues.postCode2,
            city = CommonValues.city2,
        )
    )

    val logisticAddress1 = LogisticAddressGateResponse(
        bpna = CommonValues.bpnAddress1,
        physicalPostalAddress = address1,
        bpnLegalEntity = CommonValues.bpn1,
        bpnSite = "BPNS0000000001XY",
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    val logisticAddress2 = LogisticAddressGateResponse(
        bpna = CommonValues.bpnAddress2,
        physicalPostalAddress = address2,
        bpnLegalEntity = CommonValues.bpn2,
        bpnSite = "BPNS0000000002XY",
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    val legalEntityResponsePool1 = PoolLegalEntityVerboseDto(
        legalName = CommonValues.name1,
        legalAddress = LogisticAddressVerboseDto(
            bpna = CommonValues.bpnAddress1,
            physicalPostalAddress = address1,
            bpnLegalEntity = CommonValues.bpn1,
            bpnSite = "BPNS0000000001XY",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        ),
        legalEntity = LegalEntityVerboseDto(
            bpnl = CommonValues.bpn1,
            legalShortName = CommonValues.shortName1,
            legalForm = legalForm1,
            states = listOf(leBusinessStatus1),
            classifications = listOf(classification1, classification2),
            currentness = CommonValues.now,
            createdAt = CommonValues.now,
            updatedAt = CommonValues.now,
        )

    )

    val legalEntityResponsePool2 = PoolLegalEntityVerboseDto(
        legalName = CommonValues.name3,
        legalAddress = LogisticAddressVerboseDto(
            bpna = CommonValues.bpnAddress2,
            physicalPostalAddress = address2,
            bpnLegalEntity = CommonValues.bpn2,
            bpnSite = "BPNS0000000002XY",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        ),
        legalEntity = LegalEntityVerboseDto(
            bpnl = CommonValues.bpn2,
            legalShortName = CommonValues.shortName3,
            legalForm = legalForm2,
            states = listOf(leBusinessStatus2),
            classifications = listOf(classification3, classification4),
            currentness = CommonValues.now,
            createdAt = CommonValues.now,
            updatedAt = CommonValues.now,
        )
    )

    val legalEntityResponseGate1 = LegalEntityVerboseDto(
        bpnl = CommonValues.bpn1,
        legalShortName = CommonValues.shortName1,
        legalForm = legalForm1,
        states = listOf(leBusinessStatus1),
        classifications = listOf(classification1, classification2),
        currentness = CommonValues.now,
        createdAt = CommonValues.now,
        updatedAt = CommonValues.now,
    )

    val legalEntityResponseGate2 = LegalEntityVerboseDto(
        bpnl = CommonValues.bpn2,
        legalShortName = CommonValues.shortName3,
        legalForm = legalForm2,
        states = listOf(leBusinessStatus2),
        classifications = listOf(classification3, classification4),
        currentness = CommonValues.now,
        createdAt = CommonValues.now,
        updatedAt = CommonValues.now,
    )

    val addressGateInputResponse1 = AddressGateInputResponse(
        address = RequestValues.address1
            .copy(
                nameParts = listOf(CommonValues.name1),
            ),
        externalId = CommonValues.externalIdAddress1,
        legalEntityExternalId = CommonValues.externalId1,
    )

    val addressGateInputResponse2 = AddressGateInputResponse(
        address = RequestValues.address2
            .copy(
                nameParts = listOf(CommonValues.nameSite1),
            ),
        externalId = CommonValues.externalIdAddress2,
        siteExternalId = CommonValues.externalIdSite1,
    )

    val logisticAddressGateInputResponse1 = AddressGateInputResponse(
        address = RequestValues.logisticAddress1.copy(
            nameParts = listOf(CommonValues.name1),
        ),
        externalId = CommonValues.externalIdAddress1,
        legalEntityExternalId = null,
    )

    val logisticAddressGateInputResponse2 = AddressGateInputResponse(
        address = RequestValues.logisticAddress2.copy(
            nameParts = listOf(CommonValues.name2),
        ),
        externalId = CommonValues.externalIdAddress2,
        siteExternalId = null,
    )

    //Output Response Values
    val logisticAddressGateOutputResponse1 = AddressGateOutputResponse(
        address = RequestValues.logisticAddress1.copy(
            nameParts = listOf(CommonValues.name1),
        ),
        externalId = CommonValues.externalIdAddress1,
        legalEntityExternalId = null,
        bpn = CommonValues.bpnAddress1
    )

    val logisticAddressGateOutputResponse2 = AddressGateOutputResponse(
        address = RequestValues.logisticAddress2.copy(
            nameParts = listOf(CommonValues.name2),
        ),
        externalId = CommonValues.externalIdAddress2,
        siteExternalId = null,
        bpn = CommonValues.bpnAddress2
    )

    val legalEntityGateInputResponse1 = LegalEntityGateInputResponse(
        legalEntity = RequestValues.legalEntity1,
        legalNameParts = listOf(CommonValues.name1),
        legalAddress = AddressGateInputResponse(
            address = RequestValues.logisticAddress1,
            externalId = "${CommonValues.externalId1}_legalAddress",
            legalEntityExternalId = CommonValues.externalId1,
            siteExternalId = null
        ),
        externalId = CommonValues.externalId1,
    )

    val legalEntityGateInputResponse2 = LegalEntityGateInputResponse(
        legalEntity = RequestValues.legalEntity2,
        legalNameParts = listOf(CommonValues.name2),
        legalAddress = AddressGateInputResponse(
            address = RequestValues.logisticAddress2,
            externalId = "${CommonValues.externalId2}_legalAddress",
            legalEntityExternalId = CommonValues.externalId2,
            siteExternalId = null
        ),
        externalId = CommonValues.externalId2,
    )


    //Gate Output Legal Entities Response
    val legalEntityGateOutputResponse1 = LegalEntityGateOutputResponse(
        legalEntity = RequestValues.legalEntity1,
        legalNameParts = listOf(CommonValues.name1),
        externalId = CommonValues.externalId1,
        bpn = CommonValues.bpn1,
        legalAddress = AddressGateOutputResponse(
            address = RequestValues.address1,
            externalId = "${CommonValues.externalId1}_legalAddress",
            legalEntityExternalId = CommonValues.externalId1,
            siteExternalId = null,
            bpn = CommonValues.bpnAddress1
        )
    )

    val legalEntityGateOutputResponse2 = LegalEntityGateOutputResponse(
        legalEntity = RequestValues.legalEntity2,
        externalId = CommonValues.externalId2,
        legalNameParts = listOf(CommonValues.name2),
        bpn = CommonValues.bpn2,
        legalAddress = AddressGateOutputResponse(
            address = RequestValues.address2,
            externalId = "${CommonValues.externalId2}_legalAddress",
            legalEntityExternalId = CommonValues.externalId2,
            siteExternalId = null,
            bpn = CommonValues.bpnAddress2
        )
    )

    val persistencesiteGateInputResponse1 = SiteGateInputResponse(
        site = RequestValues.site1,
        externalId = CommonValues.externalIdSite1,
        legalEntityExternalId = CommonValues.externalId1,
        mainAddress = AddressGateInputResponse(
            address = RequestValues.address1,
            externalId = "${CommonValues.externalIdSite1}_site",
            legalEntityExternalId = null,
            siteExternalId = CommonValues.externalIdSite1,
        )
    )

    val persistenceSiteGateInputResponse2 = SiteGateInputResponse(
        site = RequestValues.site2,
        externalId = CommonValues.externalIdSite2,
        legalEntityExternalId = CommonValues.externalId2,
        mainAddress = AddressGateInputResponse(
            address = RequestValues.address2,
            externalId = "${CommonValues.externalIdSite2}_site",
            legalEntityExternalId = null,
            siteExternalId = CommonValues.externalIdSite2,
        )
    )

    val persistencesiteGateOutputResponse1 = SiteGateOutputResponse(
        site = RequestValues.site1,
        externalId = CommonValues.externalIdSite1,
        legalEntityExternalId = CommonValues.externalId1,
        bpn = CommonValues.bpnSite1,
        mainAddress = AddressGateOutputResponse(
            address = RequestValues.address1,
            externalId = "${CommonValues.externalIdSite1}_site",
            legalEntityExternalId = null,
            siteExternalId = CommonValues.externalIdSite1,
            bpn = CommonValues.bpnAddress1
        )
    )

    val persistencesiteGateOutputResponse2 = SiteGateOutputResponse(
        site = RequestValues.site2,
        externalId = CommonValues.externalIdSite2,
        legalEntityExternalId = CommonValues.externalId2,
        bpn = CommonValues.bpnSite2,
        mainAddress = AddressGateOutputResponse(
            address = RequestValues.address2,
            externalId = "${CommonValues.externalIdSite2}_site",
            legalEntityExternalId = null,
            siteExternalId = CommonValues.externalIdSite2,
            bpn = CommonValues.bpnAddress2
        )
    )
}