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

import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.dto.response.*
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameUrlDto
import org.eclipse.tractusx.bpdm.common.model.AddressType
import org.eclipse.tractusx.bpdm.common.service.toDto
import org.eclipse.tractusx.bpdm.gate.api.model.*

object ResponseValues {
    val language1 = TypeKeyNameDto(
        technicalKey = CommonValues.language1,
        name = CommonValues.language1.getName()
    )

    val language2 = TypeKeyNameDto(
        technicalKey = CommonValues.language2,
        name = CommonValues.language2.getName()
    )

    val identifier1 = IdentifierResponse(
        value = CommonValues.identifierValue1,
        type = TypeKeyNameDto(
            technicalKey = CommonValues.identifierTypeTechnicalKey1,
            name = CommonValues.identifierTypeName1,
        ),
        issuingBody = CommonValues.identifierIssuingBody1
    )
    val identifier2 = IdentifierResponse(
        value = CommonValues.identifierValue2,
        type = TypeKeyNameDto(
            technicalKey = CommonValues.identifierTypeTechnicalKey2,
            name = CommonValues.identifierTypeName2,
        ),
        issuingBody = CommonValues.identifierIssuingBody2

    )
    val identifier3 = IdentifierResponse(
        value = CommonValues.identifierValue3,
        type = TypeKeyNameDto(
            technicalKey = CommonValues.identifierTypeTechnicalKey3,
            name = CommonValues.identifierTypeName3,
        ),
        issuingBody = CommonValues.identifierIssuingBody3

    )
    val identifier4 = IdentifierResponse(
        value = CommonValues.identifierValue4,
        type = TypeKeyNameDto(
            technicalKey = CommonValues.identifierTypeTechnicalKey4,
            name = CommonValues.identifierTypeName4,
        ),
        issuingBody = CommonValues.identifierIssuingBody4

    )

    val name1 = NameResponse(
        value = CommonValues.name1,
        shortName = CommonValues.shortName1,
    )

    val name2 = NameResponse(
        value = CommonValues.name2,
        shortName = CommonValues.shortName2,
    )

    val name3 = NameResponse(
        value = CommonValues.name3,
        shortName = CommonValues.shortName3,
    )

    val name4 = NameResponse(
        value = CommonValues.name4,
        shortName = CommonValues.shortName4,
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

    val characterSet1 = TypeKeyNameDto(
        technicalKey = CommonValues.characterSet1,
        name = CommonValues.characterSet1.getTypeName()
    )
    val characterSet2 = TypeKeyNameDto(
        technicalKey = CommonValues.characterSet2,
        name = CommonValues.characterSet2.getTypeName()
    )

    val addressVersion1 = AddressVersionResponse(characterSet1, language1)
    val addressVersion2 = AddressVersionResponse(characterSet2, language2)

    val country1 = TypeKeyNameDto(
        technicalKey = CommonValues.country1,
        name = CommonValues.country1.getName()
    )
    val country2 = TypeKeyNameDto(
        technicalKey = CommonValues.country2,
        name = CommonValues.country2.getName()
    )

    val adminAreaType1 = TypeKeyNameUrlDto(
        technicalKey = CommonValues.adminAreaType1,
        name = CommonValues.adminAreaType1.getTypeName(),
        url = CommonValues.adminAreaType1.getUrl()
    )
    val adminAreaType2 = TypeKeyNameUrlDto(
        technicalKey = CommonValues.adminAreaType2,
        name = CommonValues.adminAreaType2.getTypeName(),
        url = CommonValues.adminAreaType2.getUrl()
    )

    val adminArea1 = AdministrativeAreaResponse(
        value = CommonValues.adminArea1,
        type = adminAreaType1,
        language = language1
    )
    val adminArea2 = AdministrativeAreaResponse(
        value = CommonValues.adminArea2,
        type = adminAreaType2,
        language = language2
    )

    val postCodeType1 = TypeKeyNameUrlDto(
        technicalKey = CommonValues.postCodeType1,
        name = CommonValues.postCodeType1.getTypeName(),
        url = CommonValues.postCodeType1.getUrl()
    )
    val postCodeType2 = TypeKeyNameUrlDto(
        technicalKey = CommonValues.postCodeType2,
        name = CommonValues.postCodeType2.getTypeName(),
        url = CommonValues.postCodeType2.getUrl()
    )

    val postCode1 = PostCodeResponse(
        value = CommonValues.postCode1,
        type = postCodeType1
    )
    val postCode2 = PostCodeResponse(
        value = CommonValues.postCode2,
        type = postCodeType2
    )

    val localityType1 = TypeKeyNameUrlDto(
        technicalKey = CommonValues.localityType1,
        name = CommonValues.localityType1.getTypeName(),
        url = CommonValues.localityType1.getUrl()
    )
    val localityType2 = TypeKeyNameUrlDto(
        technicalKey = CommonValues.localityType2,
        name = CommonValues.localityType2.getTypeName(),
        url = CommonValues.localityType2.getUrl()
    )

    val locality1 = LocalityResponse(
        value = CommonValues.locality1,
        type = localityType1,
        language = language1
    )
    val locality2 = LocalityResponse(
        value = CommonValues.locality2,
        type = localityType2,
        language = language2
    )

    val thoroughfareType1 = TypeKeyNameUrlDto(
        technicalKey = CommonValues.thoroughfareType1,
        name = CommonValues.thoroughfareType1.getTypeName(),
        url = CommonValues.thoroughfareType1.getUrl()
    )
    val thoroughfareType2 = TypeKeyNameUrlDto(
        technicalKey = CommonValues.thoroughfareType2,
        name = CommonValues.thoroughfareType2.getTypeName(),
        url = CommonValues.thoroughfareType2.getUrl()
    )

    val thoroughfare1 = ThoroughfareResponse(
        value = CommonValues.thoroughfare1,
        type = thoroughfareType1,
        language = language1
    )
    val thoroughfare2 = ThoroughfareResponse(
        value = CommonValues.thoroughfare2,
        type = thoroughfareType2,
        language = language2
    )

    val premiseType1 = TypeKeyNameUrlDto(
        technicalKey = CommonValues.premiseType1,
        name = CommonValues.premiseType1.getTypeName(),
        url = CommonValues.premiseType1.getUrl()
    )
    val premiseType2 = TypeKeyNameUrlDto(
        technicalKey = CommonValues.premiseType2,
        name = CommonValues.premiseType2.getTypeName(),
        url = CommonValues.premiseType2.getUrl()
    )

    val premise1 = PremiseResponse(
        value = CommonValues.premise1,
        type = premiseType1,
        language = language1
    )
    val premise2 = PremiseResponse(
        value = CommonValues.premise2,
        type = premiseType2,
        language = language2
    )

    val postalDeliveryPointType1 = TypeKeyNameUrlDto(
        technicalKey = CommonValues.postalDeliveryPointType1,
        name = CommonValues.postalDeliveryPointType1.getTypeName(),
        url = CommonValues.postalDeliveryPointType1.getUrl()
    )
    val postalDeliveryPointType2 = TypeKeyNameUrlDto(
        technicalKey = CommonValues.postalDeliveryPointType2,
        name = CommonValues.postalDeliveryPointType2.getTypeName(),
        url = CommonValues.postalDeliveryPointType2.getUrl()
    )

    val postalDeliveryPoint1 = PostalDeliveryPointResponse(
        value = CommonValues.postalDeliveryPoint1,
        type = postalDeliveryPointType1,
        language = language1
    )
    val postalDeliveryPoint2 = PostalDeliveryPointResponse(
        value = CommonValues.postalDeliveryPoint2,
        type = postalDeliveryPointType2,
        language = language2
    )

    val geoCoordinate1 = GeoCoordinateDto(CommonValues.geoCoordinates1.first, CommonValues.geoCoordinates1.second)
    val geoCoordinate2 = GeoCoordinateDto(CommonValues.geoCoordinates2.first, CommonValues.geoCoordinates2.second)

    val addressType1 = TypeKeyNameUrlDto(
        technicalKey = AddressType.HEADQUARTER,
        name = AddressType.HEADQUARTER.getTypeName(),
        url = AddressType.HEADQUARTER.getUrl()
    )

    val address1 = AddressResponse(
        version = addressVersion1,
        careOf = CommonValues.careOf1,
        contexts = listOf(CommonValues.context1),
        country = country1,
        administrativeAreas = listOf(adminArea1),
        postCodes = listOf(postCode1),
        localities = listOf(locality1),
        thoroughfares = listOf(thoroughfare1),
        premises = listOf(premise1),
        postalDeliveryPoints = listOf(postalDeliveryPoint1),
        geographicCoordinates = geoCoordinate1,
        types = listOf(addressType1)
    )

    val address2 = AddressResponse(
        version = addressVersion2,
        careOf = CommonValues.careOf2,
        contexts = listOf(CommonValues.context2),
        country = country2,
        administrativeAreas = listOf(adminArea2),
        postCodes = listOf(postCode2),
        localities = listOf(locality2),
        thoroughfares = listOf(thoroughfare2),
        premises = listOf(premise2),
        postalDeliveryPoints = listOf(postalDeliveryPoint2),
        geographicCoordinates = geoCoordinate2,
        types = listOf(addressType1)
    )

    val legalEntityResponse1 = LegalEntityResponse(
        bpn = CommonValues.bpn1,
        identifiers = listOf(identifier1, identifier2),
        legalName = name1,
        legalForm = legalForm1,
        states = listOf(leBusinessStatus1),
        classifications = listOf(classification1, classification2),
        currentness = CommonValues.now,
        createdAt = CommonValues.now,
        updatedAt = CommonValues.now
    )

    val legalEntityResponse2 = LegalEntityResponse(
        bpn = CommonValues.bpn2,
        identifiers = listOf(identifier3, identifier4),
        legalName = name3,
        legalForm = legalForm2,
        states = listOf(leBusinessStatus2),
        classifications = listOf(classification3, classification4),
        currentness = CommonValues.now,
        createdAt = CommonValues.now,
        updatedAt = CommonValues.now
    )

    val legalEntityGateInputResponse1 = LegalEntityGateInputResponse(
        legalEntity = RequestValues.legalEntity1,
        externalId = CommonValues.externalId1,
        bpn = CommonValues.bpn1,
        processStartedAt = SaasValues.modificationTime1,
    )

    val legalEntityGateInputResponse2 = LegalEntityGateInputResponse(
        legalEntity = RequestValues.legalEntity2,
        externalId = CommonValues.externalId2,
        bpn = CommonValues.bpn2,
        processStartedAt = SaasValues.modificationTime2,
    )

    val legalEntityGateOutput1 = LegalEntityGateOutput(
        legalEntity = legalEntityResponse1,
        legalAddress = address1,
        externalId = CommonValues.externalId1
    )

    val legalEntityGateOutput2 = LegalEntityGateOutput(
        legalEntity = legalEntityResponse2,
        legalAddress = address2,
        externalId = CommonValues.externalId2
    )

    val legalAddressSearchResponse1 = LegalAddressSearchResponse(
        legalEntity = CommonValues.bpn1,
        legalAddress = address1
    )
    val legalAddressSearchResponse2 = LegalAddressSearchResponse(
        legalEntity = CommonValues.bpn2,
        legalAddress = address2
    )

    val siteResponse1 = SiteResponse(
        bpn = CommonValues.bpnSite1,
        name = CommonValues.nameSite1,
        states = listOf(),
        bpnLegalEntity = CommonValues.bpn1,
        createdAt = CommonValues.now,
        updatedAt = CommonValues.now
    )
    val siteResponse2 = SiteResponse(
        bpn = CommonValues.bpnSite2,
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
        bpn = CommonValues.bpnSite1,
        processStartedAt = SaasValues.modificationTime1,
    )

    val siteGateInputResponse2 = SiteGateInputResponse(
        site = RequestValues.site2,
        externalId = CommonValues.externalIdSite2,
        legalEntityExternalId = CommonValues.externalId2,
        bpn = CommonValues.bpnSite2,
        processStartedAt = SaasValues.modificationTime2,
    )

    val mainAddressSearchResponse1 = MainAddressSearchResponse(site = CommonValues.bpnSite1, mainAddress = address1)
    val mainAddressSearchResponse2 = MainAddressSearchResponse(site = CommonValues.bpnSite2, mainAddress = address2)

    val siteGateOutput1 = SiteGateOutput(
        site = siteResponse1,
        mainAddress = address1,
        externalId = CommonValues.externalIdSite1,
    )
    val siteGateOutput2 = SiteGateOutput(
        site = siteResponse2,
        mainAddress = address2,
        externalId = CommonValues.externalIdSite2,
    )

    val addressGateInputResponse1 = AddressGateInputResponse(
        address = RequestValues.address1,
        externalId = CommonValues.externalIdAddress1,
        legalEntityExternalId = CommonValues.externalId1,
        bpn = CommonValues.bpnAddress1,
        processStartedAt = SaasValues.modificationTime1,
    )
    val addressGateInputResponse2 = AddressGateInputResponse(
        address = RequestValues.address2,
        externalId = CommonValues.externalIdAddress2,
        siteExternalId = CommonValues.externalIdSite1,
        bpn = CommonValues.bpnAddress2,
        processStartedAt = SaasValues.modificationTime2,
    )

    val addressGateOutput1 = AddressGateOutput(
        bpn = CommonValues.bpnAddress1,
        address = address1,
        externalId = CommonValues.externalIdAddress1,
        legalEntityBpn = CommonValues.bpn1
    )
    val addressGateOutput2 = AddressGateOutput(
        bpn = CommonValues.bpnAddress2,
        address = address2,
        externalId = CommonValues.externalIdAddress2,
        legalEntityBpn = CommonValues.bpn2
    )

    val addressPartnerResponse1 = AddressPartnerResponse(bpn = CommonValues.bpnAddress1, properties = address1)
    val addressPartnerResponse2 = AddressPartnerResponse(bpn = CommonValues.bpnAddress2, properties = address2)

    val addressPartnerSearchResponse1 = AddressPartnerSearchResponse(address = addressPartnerResponse1, bpnLegalEntity = CommonValues.bpn1)
    val addressPartnerSearchResponse2 = AddressPartnerSearchResponse(address = addressPartnerResponse2, bpnLegalEntity = CommonValues.bpn2)
}