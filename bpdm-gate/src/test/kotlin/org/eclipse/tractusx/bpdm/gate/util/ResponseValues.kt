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

import org.eclipse.tractusx.bpdm.common.dto.AddressIdentifierDto
import org.eclipse.tractusx.bpdm.common.dto.BasePhysicalAddressDto
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.dto.StreetDto
import org.eclipse.tractusx.bpdm.common.dto.response.*
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.common.service.toDto
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.response.LogisticAddressGateResponse
import java.time.Instant

object ResponseValues {
    val language1 = TypeKeyNameDto(
        technicalKey = CommonValues.language1,
        name = CommonValues.language1.getName()
    )

    val language2 = TypeKeyNameDto(
        technicalKey = CommonValues.language2,
        name = CommonValues.language2.getName()
    )

    val identifier1 = LegalEntityIdentifierResponse(
        value = CommonValues.identifierValue1,
        type = TypeKeyNameDto(
            technicalKey = CommonValues.identifierTypeTechnicalKey1,
            name = CommonValues.identifierTypeName1,
        ),
        issuingBody = CommonValues.identifierIssuingBody1
    )
    val identifier2 = LegalEntityIdentifierResponse(
        value = CommonValues.identifierValue2,
        type = TypeKeyNameDto(
            technicalKey = CommonValues.identifierTypeTechnicalKey2,
            name = CommonValues.identifierTypeName2,
        ),
        issuingBody = CommonValues.identifierIssuingBody2

    )
    val identifier3 = LegalEntityIdentifierResponse(
        value = CommonValues.identifierValue3,
        type = TypeKeyNameDto(
            technicalKey = CommonValues.identifierTypeTechnicalKey3,
            name = CommonValues.identifierTypeName3,
        ),
        issuingBody = CommonValues.identifierIssuingBody3

    )
    val identifier4 = LegalEntityIdentifierResponse(
        value = CommonValues.identifierValue4,
        type = TypeKeyNameDto(
            technicalKey = CommonValues.identifierTypeTechnicalKey4,
            name = CommonValues.identifierTypeName4,
        ),
        issuingBody = CommonValues.identifierIssuingBody4

    )

    val legalForm1 = LegalFormResponse(
        technicalKey = CommonValues.legalFormTechnicalKey1,
        name = CommonValues.legalFormName1,
        abbreviation = CommonValues.legalFormAbbreviation1,
    )

    val legalForm2 = LegalFormResponse(
        technicalKey = CommonValues.legalFormTechnicalKey2,
        name = CommonValues.legalFormName2,
        abbreviation = CommonValues.legalFormAbbreviation2,
    )

    val leBusinessStatus1 = LegalEntityStateResponse(
        officialDenotation = CommonValues.businessStatusOfficialDenotation1,
        validFrom = CommonValues.businessStatusValidFrom1,
        validTo = CommonValues.businessStatusValidUntil1,
        type = TypeKeyNameDto(
            technicalKey = CommonValues.businessStateType1,
            name = CommonValues.businessStateType1.getTypeName(),
        )
    )

    val leBusinessStatus2 = LegalEntityStateResponse(
        officialDenotation = CommonValues.businessStatusOfficialDenotation2,
        validFrom = CommonValues.businessStatusValidFrom2,
        validTo = CommonValues.businessStatusValidUntil2,
        type = TypeKeyNameDto(
            technicalKey = CommonValues.businessStateType2,
            name = CommonValues.businessStateType2.getTypeName(),
        )
    )

    val classification1 = ClassificationResponse(
        value = CommonValues.classificationValue1,
        code = CommonValues.classificationCode1,
        type = CommonValues.classificationType.toDto()
    )

    val classification2 = ClassificationResponse(
        value = CommonValues.classificationValue2,
        code = CommonValues.classificationCode2,
        type = CommonValues.classificationType.toDto()
    )

    val classification3 = ClassificationResponse(
        value = CommonValues.classificationValue3,
        code = CommonValues.classificationCode3,
        type = CommonValues.classificationType.toDto()
    )

    val classification4 = ClassificationResponse(
        value = CommonValues.classificationValue4,
        code = CommonValues.classificationCode4,
        type = CommonValues.classificationType.toDto()
    )


    val country1 = TypeKeyNameDto(
        technicalKey = CommonValues.country1,
        name = CommonValues.country1.getName()
    )
    val country2 = TypeKeyNameDto(
        technicalKey = CommonValues.country2,
        name = CommonValues.country2.getName()
    )

    val geoCoordinate1 = GeoCoordinateDto(CommonValues.geoCoordinates1.first, CommonValues.geoCoordinates1.second)
    val geoCoordinate2 = GeoCoordinateDto(CommonValues.geoCoordinates2.first, CommonValues.geoCoordinates2.second)

    val address1 = PhysicalPostalAddressResponse(
        basePhysicalAddress = BasePhysicalAddressDto(
            industrialZone = CommonValues.industrialZone1,
            building = CommonValues.building1,
            floor = CommonValues.floor1,
            door = CommonValues.door1
        ),
        areaPart = AreaDistrictResponse(
            administrativeAreaLevel1 = CommonValues.adminAreaLevel1Region1,
            administrativeAreaLevel2 = CommonValues.county1,
            district = CommonValues.district1,
        ),
        street = StreetDto(CommonValues.street1, CommonValues.houseNumber1),
        baseAddress = BasePostalAddressResponse(
            geographicCoordinates = geoCoordinate1,
            country = country1,
            postalCode = CommonValues.postCode1,
            city = CommonValues.city1,
        )
    )

    val address2 = PhysicalPostalAddressResponse(
        basePhysicalAddress = BasePhysicalAddressDto(
            industrialZone = CommonValues.industrialZone2,
            building = CommonValues.building2,
            floor = CommonValues.floor2,
            door = CommonValues.door2
        ),
        areaPart = AreaDistrictResponse(
            administrativeAreaLevel1 = CommonValues.adminAreaLevel1Region2,
            administrativeAreaLevel2 = CommonValues.county2,
            district = CommonValues.district2,
        ),
        street = StreetDto(CommonValues.street2, CommonValues.houseNumber2),
        baseAddress = BasePostalAddressResponse(
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

    val legalEntityResponsePool1 = PoolLegalEntityResponse(
        legalName = CommonValues.name1,
        legalAddress = LogisticAddressResponse(
            bpna = CommonValues.bpnAddress1,
            physicalPostalAddress = address1,
            bpnLegalEntity = CommonValues.bpn1,
            bpnSite = "BPNS0000000001XY",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        ),
        legalEntity = LegalEntityResponse(
            bpnl = CommonValues.bpn1,
            identifiers = listOf(identifier1, identifier2),
            legalShortName = CommonValues.shortName1,
            legalForm = legalForm1,
            states = listOf(leBusinessStatus1),
            classifications = listOf(classification1, classification2),
            currentness = CommonValues.now,
            createdAt = CommonValues.now,
            updatedAt = CommonValues.now,
        )

    )

    val legalEntityResponsePool2 = PoolLegalEntityResponse(
        legalName = CommonValues.name3,
        legalAddress = LogisticAddressResponse(
            bpna = CommonValues.bpnAddress2,
            physicalPostalAddress = address2,
            bpnLegalEntity = CommonValues.bpn2,
            bpnSite = "BPNS0000000002XY",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        ),
        legalEntity = LegalEntityResponse(
            bpnl = CommonValues.bpn2,
            identifiers = listOf(identifier3, identifier4),
            legalShortName = CommonValues.shortName3,
            legalForm = legalForm2,
            states = listOf(leBusinessStatus2),
            classifications = listOf(classification3, classification4),
            currentness = CommonValues.now,
            createdAt = CommonValues.now,
            updatedAt = CommonValues.now,
        )
    )

    val legalEntityResponseGate1 = LegalEntityResponse(
        bpnl = CommonValues.bpn1,
        identifiers = listOf(identifier1, identifier2),
        legalShortName = CommonValues.shortName1,
        legalForm = legalForm1,
        states = listOf(leBusinessStatus1),
        classifications = listOf(classification1, classification2),
        currentness = CommonValues.now,
        createdAt = CommonValues.now,
        updatedAt = CommonValues.now,
    )

    val legalEntityResponseGate2 = LegalEntityResponse(
        bpnl = CommonValues.bpn2,
        identifiers = listOf(identifier3, identifier4),
        legalShortName = CommonValues.shortName3,
        legalForm = legalForm2,
        states = listOf(leBusinessStatus2),
        classifications = listOf(classification3, classification4),
        currentness = CommonValues.now,
        createdAt = CommonValues.now,
        updatedAt = CommonValues.now,
    )

    val legalEntityGateInputResponse1 = LegalEntityGateInputResponse(
        legalEntity = RequestValues.legalEntity1,
        legalAddress = RequestValues.address1,
        externalId = CommonValues.externalId1,
    )

    val legalEntityGateInputResponse2 = LegalEntityGateInputResponse(
        legalEntity = RequestValues.legalEntity2,
        legalAddress = RequestValues.address2,
        externalId = CommonValues.externalId2,
    )

    val legalEntityGateInputResponse3 = LegalEntityGateInputResponse(
        legalEntity = RequestValues.legalEntity3,
        legalAddress = RequestValues.address3,
        externalId = CommonValues.externalId3,
    )


    //Gate Output Legal Entities Response
    val legalEntityGateOutputResponse1 = LegalEntityGateOutputResponse(
        legalEntity = RequestValues.legalEntity1,
        externalId = CommonValues.externalId1,
        bpn = CommonValues.bpn1,
        legalAddress = RequestValues.address1
    )

    val legalEntityGateOutputResponse2 = LegalEntityGateOutputResponse(
        legalEntity = RequestValues.legalEntity2,
        externalId = CommonValues.externalId2,
        bpn = CommonValues.bpn2,
        legalAddress = RequestValues.address2
    )

    //Values without processStartedAt value
    val newLegalEntityGateInputResponse1 = LegalEntityGateInputResponse(
        legalEntity = RequestValues.legalEntity1,
        legalAddress = RequestValues.address1,
        externalId = CommonValues.externalId1,
    )
    val newLegalEntityGateInputResponse2 = LegalEntityGateInputResponse(
        legalEntity = RequestValues.legalEntity2,
        legalAddress = RequestValues.address2,
        externalId = CommonValues.externalId2,
    )

    val legalEntityGateOutput1 = LegalEntityGateOutput(
        legalEntity = legalEntityResponseGate1,
        legalNameParts = arrayOf(CommonValues.name1),
        externalId = CommonValues.externalId1,
        legalAddress = logisticAddress1,
    )

    val legalEntityGateOutput2 = LegalEntityGateOutput(
        legalEntity = legalEntityResponseGate2,
        legalNameParts = arrayOf(CommonValues.name3),
        externalId = CommonValues.externalId2,
        legalAddress = logisticAddress2,
    )

    val siteResponse1 = SiteResponse(
        bpns = CommonValues.bpnSite1,
        name = CommonValues.nameSite1,
        states = listOf(),
        bpnLegalEntity = CommonValues.bpn1,
        createdAt = CommonValues.now,
        updatedAt = CommonValues.now
    )
    val siteResponse2 = SiteResponse(
        bpns = CommonValues.bpnSite2,
        name = CommonValues.nameSite2,
        states = listOf(),
        bpnLegalEntity = CommonValues.bpn2,
        createdAt = CommonValues.now,
        updatedAt = CommonValues.now
    )

    val siteGateInputResponse1 = SiteGateInputResponse(
        site = RequestValues.site1,
        externalId = CommonValues.externalIdSite1,
        legalEntityExternalId = CommonValues.externalId1,
    )

    val siteGateInputResponse2 = SiteGateInputResponse(
        site = RequestValues.site2,
        externalId = CommonValues.externalIdSite2,
        legalEntityExternalId = CommonValues.externalId2,
    )

    val persistencesiteGateInputResponse1 = SiteGateInputResponse(
        site = RequestValues.site1,
        externalId = CommonValues.externalIdSite1,
        legalEntityExternalId = CommonValues.externalId1,
    )

    val persistenceSiteGateInputResponse2 = SiteGateInputResponse(
        site = RequestValues.site2,
        externalId = CommonValues.externalIdSite2,
        legalEntityExternalId = CommonValues.externalId2,
    )

    val persistencesiteGateOutputResponse1 = SiteGateOutputResponse(
        site = RequestValues.site1,
        externalId = CommonValues.externalIdSite1,
        legalEntityExternalId = CommonValues.externalId1,
        bpn = CommonValues.bpnSite1,
    )

    val persistencesiteGateOutputResponse2 = SiteGateOutputResponse(
        site = RequestValues.site2,
        externalId = CommonValues.externalIdSite2,
        legalEntityExternalId = CommonValues.externalId2,
        bpn = CommonValues.bpnSite2,
    )

    val siteGateOutput1 = SiteGateOutput(
        site = siteResponse1,
        mainAddress = logisticAddress1,
        externalId = CommonValues.externalIdSite1,
    )
    val siteGateOutput2 = SiteGateOutput(
        site = siteResponse2,
        mainAddress = logisticAddress2,
        externalId = CommonValues.externalIdSite2,
    )

    val addressGateInputResponse1 = AddressGateInputResponse(
        address = RequestValues.address1
            .copy(
                name = CommonValues.name1,
                identifiers = listOf(
                    AddressIdentifierDto(SaasValues.identifier1.value!!, SaasValues.identifier1.type?.technicalKey!!),
                    AddressIdentifierDto(SaasValues.identifier2.value!!, SaasValues.identifier2.type?.technicalKey!!)
                )
            ),
        externalId = CommonValues.externalIdAddress1,
        legalEntityExternalId = CommonValues.externalId1,
    )

    val addressGateInputResponse2 = AddressGateInputResponse(
        address = RequestValues.address2
            .copy(
                name = CommonValues.nameSite1,
                identifiers = listOf(
                    AddressIdentifierDto(SaasValues.identifier1.value!!, SaasValues.identifier1.type?.technicalKey!!),
                    AddressIdentifierDto(SaasValues.identifier2.value!!, SaasValues.identifier2.type?.technicalKey!!)
                )
            ),
        externalId = CommonValues.externalIdAddress2,
        siteExternalId = CommonValues.externalIdSite1,
    )

    val addressGateOutput1 = AddressGateOutput(
        address = logisticAddress1,
        externalId = CommonValues.externalIdAddress1,
    )
    val addressGateOutput2 = AddressGateOutput(
        address = logisticAddress2,
        externalId = CommonValues.externalIdAddress2,
    )

    val logisticAddressGateInputResponse1 = AddressGateInputResponse(
        address = RequestValues.logisticAddress1.copy(
            name = CommonValues.name1,
            identifiers = listOf(
                AddressIdentifierDto(SaasValues.identifier1.value!!, SaasValues.identifier1.type?.technicalKey!!)
            )
        ),
        externalId = CommonValues.externalIdAddress1,
        legalEntityExternalId = null,
    )

    val logisticAddressGateInputResponse2 = AddressGateInputResponse(
        address = RequestValues.logisticAddress2.copy(
            name = CommonValues.name2,
            identifiers = listOf(
                AddressIdentifierDto(SaasValues.identifier1.value!!, SaasValues.identifier1.type?.technicalKey!!)
            )
        ),
        externalId = CommonValues.externalIdAddress2,
        siteExternalId = null,
    )

    //Output Response Values
    val logisticAddressGateOutputResponse1 = AddressGateOutputResponse(
        address = RequestValues.logisticAddress1.copy(
            name = CommonValues.name1,
            identifiers = listOf(
                AddressIdentifierDto(SaasValues.identifier1.value!!, SaasValues.identifier1.type?.technicalKey!!)
            )
        ),
        externalId = CommonValues.externalIdAddress1,
        legalEntityExternalId = null,
        bpn = CommonValues.bpnAddress1
    )

    val logisticAddressGateOutputResponse2 = AddressGateOutputResponse(
        address = RequestValues.logisticAddress2.copy(
            name = CommonValues.name2,
            identifiers = listOf(
                AddressIdentifierDto(SaasValues.identifier1.value!!, SaasValues.identifier1.type?.technicalKey!!)
            )
        ),
        externalId = CommonValues.externalIdAddress2,
        siteExternalId = null,
        bpn = CommonValues.bpnAddress2
    )
}