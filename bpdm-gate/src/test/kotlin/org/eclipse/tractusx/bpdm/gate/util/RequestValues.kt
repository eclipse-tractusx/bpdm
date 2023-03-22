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
import org.eclipse.tractusx.bpdm.common.model.AddressType
import org.eclipse.tractusx.bpdm.gate.api.model.AddressGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.BusinessPartnerCandidateDto
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.SiteGateInputRequest

object RequestValues {
    val identifier1 =
        IdentifierDto(
            CommonValues.identifierValue1,
            CommonValues.identifierTypeTechnicalKey1,
            CommonValues.identifierIssuingBodyTechnicalKey1,
        )
    val identifier2 =
        IdentifierDto(
            CommonValues.identifierValue2,
            CommonValues.identifierTypeTechnicalKey2,
            CommonValues.identifierIssuingBodyTechnicalKey2,
        )
    val identifier3 =
        IdentifierDto(
            CommonValues.identifierValue3,
            CommonValues.identifierTypeTechnicalKey3,
            CommonValues.identifierIssuingBodyTechnicalKey3,
        )
    val identifier4 =
        IdentifierDto(
            CommonValues.identifierValue4,
            CommonValues.identifierTypeTechnicalKey4,
            CommonValues.identifierIssuingBodyTechnicalKey4,
        )

    val name1 = NameDto(value = CommonValues.name1, shortName = CommonValues.shortName1)
    val name2 = NameDto(value = CommonValues.name2, shortName = CommonValues.shortName2)
    val name3 = NameDto(value = CommonValues.name3, shortName = CommonValues.shortName3)
    val name4 = NameDto(value = CommonValues.name4, shortName = CommonValues.shortName4)

    val leBusinessStatus1 = LegalEntityStateDto(
        officialDenotation = CommonValues.businessStatusOfficialDenotation1,
        validFrom = CommonValues.businessStatusValidFrom1,
        validTo = CommonValues.businessStatusValidUntil1,
        type = CommonValues.businessStateType1
    )

    val leBusinessStatus2 = LegalEntityStateDto(
        officialDenotation = CommonValues.businessStatusOfficialDenotation2,
        validFrom = CommonValues.businessStatusValidFrom2,
        validTo = CommonValues.businessStatusValidUntil2,
        type = CommonValues.businessStateType2
    )

    val siteBusinessStatus1 = SiteStateDto(
        description = CommonValues.businessStatusOfficialDenotation1,
        validFrom = CommonValues.businessStatusValidFrom1,
        validTo = CommonValues.businessStatusValidUntil1,
        type = CommonValues.businessStateType1
    )

    val siteBusinessStatus2 = SiteStateDto(
        description = CommonValues.businessStatusOfficialDenotation2,
        validFrom = CommonValues.businessStatusValidFrom2,
        validTo = CommonValues.businessStatusValidUntil2,
        type = CommonValues.businessStateType2
    )

    val classification1 = ClassificationDto(
        value = CommonValues.classificationValue1,
        code = CommonValues.classificationCode1,
        type = CommonValues.classificationType
    )

    val classification2 = ClassificationDto(
        value = CommonValues.classificationValue2,
        code = CommonValues.classificationCode2,
        type = CommonValues.classificationType
    )

    val classification3 = ClassificationDto(
        value = CommonValues.classificationValue3,
        code = CommonValues.classificationCode3,
        type = CommonValues.classificationType
    )

    val classification4 = ClassificationDto(
        value = CommonValues.classificationValue4,
        code = CommonValues.classificationCode4,
        type = CommonValues.classificationType
    )

    val addressVersion1 = AddressVersionDto(CommonValues.characterSet1, CommonValues.language1)
    val addressVersion2 = AddressVersionDto(CommonValues.characterSet2, CommonValues.language2)

    val adminArea1 = AdministrativeAreaDto(value = CommonValues.adminArea1, type = CommonValues.adminAreaType1)
    val adminArea2 = AdministrativeAreaDto(value = CommonValues.adminArea2, type = CommonValues.adminAreaType2)

    val postCode1 = PostCodeDto(value = CommonValues.postCode1, type = CommonValues.postCodeType1)
    val postCode2 = PostCodeDto(value = CommonValues.postCode2, type = CommonValues.postCodeType2)

    val locality1 = LocalityDto(value = CommonValues.locality1, type = CommonValues.localityType1)
    val locality2 = LocalityDto(value = CommonValues.locality2, type = CommonValues.localityType2)

    val thoroughfare1 = ThoroughfareDto(value = CommonValues.thoroughfare1, type = CommonValues.thoroughfareType1)
    val thoroughfare2 = ThoroughfareDto(value = CommonValues.thoroughfare2, type = CommonValues.thoroughfareType2)

    val premise1 = PremiseDto(value = CommonValues.premise1, type = CommonValues.premiseType1)
    val premise2 = PremiseDto(value = CommonValues.premise2, type = CommonValues.premiseType2)

    val postalDeliveryPoint1 = PostalDeliveryPointDto(value = CommonValues.postalDeliveryPoint1, type = CommonValues.postalDeliveryPointType1)
    val postalDeliveryPoint2 = PostalDeliveryPointDto(value = CommonValues.postalDeliveryPoint2, type = CommonValues.postalDeliveryPointType2)

    val geoCoordinate1 = GeoCoordinateDto(CommonValues.geoCoordinates1.first, CommonValues.geoCoordinates1.second)
    val geoCoordinate2 = GeoCoordinateDto(CommonValues.geoCoordinates2.first, CommonValues.geoCoordinates2.second)

    val address1 = AddressDto(
        addressVersion1,
        CommonValues.careOf1,
        listOf(CommonValues.context1),
        CommonValues.country1,
        listOf(adminArea1),
        listOf(postCode1),
        listOf(locality1),
        listOf(thoroughfare1),
        listOf(premise1),
        listOf(postalDeliveryPoint1),
        geoCoordinate1,
        listOf(AddressType.HEADQUARTER)
    )

    val address2 = AddressDto(
        addressVersion2,
        CommonValues.careOf2,
        listOf(CommonValues.context2),
        CommonValues.country2,
        listOf(adminArea2),
        listOf(postCode2),
        listOf(locality2),
        listOf(thoroughfare2),
        listOf(premise2),
        listOf(postalDeliveryPoint2),
        geoCoordinate2,
        listOf(AddressType.HEADQUARTER)
    )


    val legalEntity1 = LegalEntityDto(
        identifiers = listOf(identifier1, identifier2),
        legalName = name1,
        legalForm = CommonValues.legalFormTechnicalKey1,
        states = listOf(leBusinessStatus1),
        classifications = listOf(classification1, classification2),
        legalAddress = address1
    )

    val legalEntity2 = LegalEntityDto(
        identifiers = listOf(identifier3, identifier4),
        legalName = name3,
        legalForm = CommonValues.legalFormTechnicalKey2,
        states = listOf(leBusinessStatus2),
        classifications = listOf(classification3, classification4),
        legalAddress = address2
    )

    val legalEntityGateInputRequest1 = LegalEntityGateInputRequest(
        legalEntity = legalEntity1,
        externalId = CommonValues.externalId1,
        bpn = CommonValues.bpn1
    )

    val legalEntityGateInputRequest2 = LegalEntityGateInputRequest(
        legalEntity = legalEntity2,
        externalId = CommonValues.externalId2,
        bpn = CommonValues.bpn2
    )

    val site1 = SiteDto(
        name = CommonValues.nameSite1,
        states = listOf(siteBusinessStatus1),
        mainAddress = address1
    )

    val site2 = SiteDto(
        name = CommonValues.nameSite2,
        states = listOf(siteBusinessStatus2),
        mainAddress = address2
    )

    val siteGateInputRequest1 = SiteGateInputRequest(
        site = site1,
        externalId = CommonValues.externalIdSite1,
        legalEntityExternalId = CommonValues.externalId1,
        bpn = CommonValues.bpnSite1
    )

    val siteGateInputRequest2 = SiteGateInputRequest(
        site = site2,
        externalId = CommonValues.externalIdSite2,
        legalEntityExternalId = CommonValues.externalId2,
        bpn = CommonValues.bpnSite2
    )

    val addressGateInputRequest1 = AddressGateInputRequest(
        address = address1,
        externalId = CommonValues.externalIdAddress1,
        legalEntityExternalId = CommonValues.externalId1,
        bpn = CommonValues.bpnAddress1
    )
    val addressGateInputRequest2 = AddressGateInputRequest(
        address = address2,
        externalId = CommonValues.externalIdAddress2,
        siteExternalId = CommonValues.externalIdSite1,
        bpn = CommonValues.bpnAddress2
    )

    val candidate1 = BusinessPartnerCandidateDto(
        identifiers = listOf(identifier1),
        names = listOf(name1),
        address = address1
    )
}