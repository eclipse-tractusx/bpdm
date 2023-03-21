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

package org.eclipse.tractusx.bpdm.pool.util

import org.eclipse.tractusx.bpdm.common.dto.response.*
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameUrlDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressPartnerCreateResponse
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityPartnerCreateResponse
import org.eclipse.tractusx.bpdm.pool.api.model.response.SitePartnerCreateResponse
import java.time.ZoneOffset

/**
 * Test values for response DTOs
 * Numbered values should match with @see SaasValues numbered values for easier testing
 */
object ResponseValues {

    val language0 = TypeKeyNameDto(CommonValues.language0, CommonValues.language0.getName())
    val language1 = TypeKeyNameDto(CommonValues.language1, CommonValues.language1.getName())
    val language2 = TypeKeyNameDto(CommonValues.language2, CommonValues.language2.getName())
    val language3 = TypeKeyNameDto(CommonValues.language3, CommonValues.language3.getName())

    val characterSet1 = TypeKeyNameDto(CommonValues.characterSet1, CommonValues.characterSet1.getTypeName())

    val country1 = TypeKeyNameDto(CommonValues.country1, CommonValues.country1.getName())
    val country2 = TypeKeyNameDto(CommonValues.country2, CommonValues.country2.getName())
    val country3 = TypeKeyNameDto(CommonValues.country3, CommonValues.country3.getName())

    val identifier1 =
        IdentifierResponse(CommonValues.identifierValue1, RequestValues.identifierType1, CommonValues.issuingBody1)
    val identifier2 =
        IdentifierResponse(CommonValues.identifierValue2, RequestValues.identifierType2, CommonValues.issuingBody2)
    val identifier3 =
        IdentifierResponse(CommonValues.identifierValue3, RequestValues.identifierType3, CommonValues.issuingBody3)

    val name1 = NameResponse(value = CommonValues.name1)
    val name2 = NameResponse(value = CommonValues.name2)
    val name3 = NameResponse(value = CommonValues.name3)
    val name4 = NameResponse(value = CommonValues.name4)
    val name5 = NameResponse(value = CommonValues.name5)

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
    val legalForm3 = LegalFormResponse(
        technicalKey = CommonValues.legalFormTechnicalKey3,
        name = CommonValues.legalFormName3,
        abbreviation = CommonValues.legalFormAbbreviation3,
    )

    val statusType1 = TypeKeyNameDto(CommonValues.statusType1, CommonValues.statusType1.getTypeName())
    val statusType2 = TypeKeyNameDto(CommonValues.statusType2, CommonValues.statusType2.getTypeName())
    val statusType3 = TypeKeyNameDto(CommonValues.statusType3, CommonValues.statusType3.getTypeName())

    val leStatus1 = LegalEntityStatusResponse(CommonValues.statusDenotation1, CommonValues.statusValidFrom1, null, statusType1)
    val leStatus2 = LegalEntityStatusResponse(CommonValues.statusDenotation2, CommonValues.statusValidFrom2, null, statusType2)
    val leStatus3 = LegalEntityStatusResponse(CommonValues.statusDenotation3, CommonValues.statusValidFrom3, null, statusType3)

    val classificationType = TypeKeyNameDto(CommonValues.classificationType, CommonValues.classificationType.name)

    val classification1 = ClassificationResponse(CommonValues.classification1, null, classificationType)
    val classification2 = ClassificationResponse(CommonValues.classification2, null, classificationType)
    val classification3 = ClassificationResponse(CommonValues.classification3, null, classificationType)
    val classification4 = ClassificationResponse(CommonValues.classification4, null, classificationType)
    val classification5 = ClassificationResponse(CommonValues.classification5, null, classificationType)

    val adminAreaType1 = TypeKeyNameUrlDto(CommonValues.adminAreaType1, CommonValues.adminAreaType1.getTypeName(), CommonValues.adminAreaType1.getUrl())

    val adminArea1 = AdministrativeAreaResponse(value = CommonValues.adminArea1, type = adminAreaType1, language = language0)
    val adminArea2 = AdministrativeAreaResponse(value = CommonValues.adminArea2, type = adminAreaType1, language = language0)
    val adminArea3 = AdministrativeAreaResponse(value = CommonValues.adminArea3, type = adminAreaType1, language = language0)
    val adminArea4 = AdministrativeAreaResponse(value = CommonValues.adminArea4, type = adminAreaType1, language = language0)
    val adminArea5 = AdministrativeAreaResponse(value = CommonValues.adminArea5, type = adminAreaType1, language = language0)

    val postCodeType1 = TypeKeyNameUrlDto(CommonValues.postCodeType1, CommonValues.postCodeType1.getTypeName(), CommonValues.postCodeType1.getUrl())

    val postCode1 = PostCodeResponse(CommonValues.postCode1, postCodeType1)
    val postCode2 = PostCodeResponse(CommonValues.postCode2, postCodeType1)
    val postCode3 = PostCodeResponse(CommonValues.postCode3, postCodeType1)
    val postCode4 = PostCodeResponse(CommonValues.postCode4, postCodeType1)
    val postCode5 = PostCodeResponse(CommonValues.postCode5, postCodeType1)

    val localityType1 = TypeKeyNameUrlDto(CommonValues.localityType1, CommonValues.localityType1.getTypeName(), CommonValues.localityType1.getUrl())

    val locality1 = LocalityResponse(CommonValues.locality1, null, localityType1, language0)
    val locality2 = LocalityResponse(CommonValues.locality2, null, localityType1, language0)
    val locality3 = LocalityResponse(CommonValues.locality3, null, localityType1, language0)
    val locality4 = LocalityResponse(CommonValues.locality4, null, localityType1, language0)
    val locality5 = LocalityResponse(CommonValues.locality5, null, localityType1, language0)

    val thoroughfareType1 =
        TypeKeyNameUrlDto(CommonValues.thoroughfareType1, CommonValues.thoroughfareType1.getTypeName(), CommonValues.thoroughfareType1.getUrl())

    val thoroughfare1 = ThoroughfareResponse(value = CommonValues.thoroughfare1, type = thoroughfareType1, language = language0)
    val thoroughfare2 = ThoroughfareResponse(value = CommonValues.thoroughfare2, type = thoroughfareType1, language = language0)
    val thoroughfare3 = ThoroughfareResponse(value = CommonValues.thoroughfare3, type = thoroughfareType1, language = language0)
    val thoroughfare4 = ThoroughfareResponse(value = CommonValues.thoroughfare4, type = thoroughfareType1, language = language0)
    val thoroughfare5 = ThoroughfareResponse(value = CommonValues.thoroughfare5, type = thoroughfareType1, language = language0)

    val premiseType1 = TypeKeyNameUrlDto(CommonValues.premiseType1, CommonValues.premiseType1.getTypeName(), CommonValues.premiseType1.getUrl())

    val premise1 = PremiseResponse(value = CommonValues.premise1, type = premiseType1, language = language0)
    val premise2 = PremiseResponse(value = CommonValues.premise2, type = premiseType1, language = language0)
    val premise3 = PremiseResponse(value = CommonValues.premise3, type = premiseType1, language = language0)
    val premise4 = PremiseResponse(value = CommonValues.premise4, type = premiseType1, language = language0)
    val premise5 = PremiseResponse(value = CommonValues.premise5, type = premiseType1, language = language0)

    val postalDeliveryPointType1 = TypeKeyNameUrlDto(
        CommonValues.postalDeliveryPointType1,
        CommonValues.postalDeliveryPointType1.getTypeName(),
        CommonValues.postalDeliveryPointType1.getUrl()
    )

    val postalDeliveryPoint1 =
        PostalDeliveryPointResponse(value = CommonValues.postalDeliveryPoint1, type = postalDeliveryPointType1, language = language0)
    val postalDeliveryPoint2 =
        PostalDeliveryPointResponse(value = CommonValues.postalDeliveryPoint2, type = postalDeliveryPointType1, language = language0)
    val postalDeliveryPoint3 =
        PostalDeliveryPointResponse(value = CommonValues.postalDeliveryPoint3, type = postalDeliveryPointType1, language = language0)
    val postalDeliveryPoint4 =
        PostalDeliveryPointResponse(value = CommonValues.postalDeliveryPoint4, type = postalDeliveryPointType1, language = language0)
    val postalDeliveryPoint5 =
        PostalDeliveryPointResponse(value = CommonValues.postalDeliveryPoint5, type = postalDeliveryPointType1, language = language0)

    val version1 = AddressVersionResponse(characterSet1, language0)

    val address1 = AddressResponse(
        version = version1,
        country = country1,
        administrativeAreas = listOf(adminArea1, adminArea2),
        postCodes = listOf(postCode1, postCode2),
        localities = listOf(locality1, locality2),
        thoroughfares = listOf(thoroughfare1, thoroughfare2),
        premises = listOf(premise1, premise2),
        postalDeliveryPoints = listOf(postalDeliveryPoint1, postalDeliveryPoint2)
    )

    val address2 = AddressResponse(
        version = version1,
        country = country2,
        administrativeAreas = listOf(adminArea3, adminArea4),
        postCodes = listOf(postCode3, postCode4),
        localities = listOf(locality3, locality4),
        thoroughfares = listOf(thoroughfare3, thoroughfare4),
        premises = listOf(premise3, premise4),
        postalDeliveryPoints = listOf(postalDeliveryPoint3, postalDeliveryPoint4)
    )

    val address3 = AddressResponse(
        version = version1,
        country = country3,
        administrativeAreas = listOf(adminArea5),
        postCodes = listOf(postCode5),
        localities = listOf(locality5),
        thoroughfares = listOf(thoroughfare5),
        premises = listOf(premise5),
        postalDeliveryPoints = listOf(postalDeliveryPoint5)
    )

    val addressPartner1 = AddressPartnerResponse(
        bpn = CommonValues.bpnA1,
        properties = address1
    )

    val addressPartner2 = AddressPartnerResponse(
        bpn = CommonValues.bpnA2,
        properties = address2
    )

    val addressPartner3 = AddressPartnerResponse(
        bpn = CommonValues.bpnA3,
        properties = address3
    )

    val addressPartnerCreate1 = AddressPartnerCreateResponse(
        bpn = addressPartner1.bpn,
        properties = addressPartner1.properties,
        index = CommonValues.index1
    )

    val addressPartnerCreate2 = AddressPartnerCreateResponse(
        bpn = addressPartner2.bpn,
        properties = addressPartner2.properties,
        index = CommonValues.index2
    )

    val addressPartnerCreate3 = AddressPartnerCreateResponse(
        bpn = addressPartner3.bpn,
        properties = addressPartner3.properties,
        index = CommonValues.index3
    )

    val site1 = SiteResponse(
        bpn = CommonValues.bpnS1,
        name = CommonValues.siteName1,
        status = listOf(),
        bpnLegalEntity = CommonValues.bpnL1,
        createdAt = CommonValues.now,
        updatedAt = CommonValues.now
    )

    val site2 = SiteResponse(
        bpn = CommonValues.bpnS2,
        name = CommonValues.siteName2,
        status = listOf(),
        bpnLegalEntity = CommonValues.bpnL2,
        createdAt = CommonValues.now,
        updatedAt = CommonValues.now
    )

    val site3 = SiteResponse(
        bpn = CommonValues.bpnS3,
        name = CommonValues.siteName3,
        status = listOf(),
        bpnLegalEntity = CommonValues.bpnL3,
        createdAt = CommonValues.now,
        updatedAt = CommonValues.now
    )

    val siteUpsert1 = SitePartnerCreateResponse(
        site1.bpn,
        site1.name,
        address1,
        CommonValues.index1
    )

    val siteUpsert2 = SitePartnerCreateResponse(
        site2.bpn,
        site2.name,
        address2,
        CommonValues.index2
    )

    val siteUpsert3 = SitePartnerCreateResponse(
        site3.bpn,
        site3.name,
        address3,
        CommonValues.index3
    )


    val legalEntity1 = LegalEntityResponse(
        bpn = CommonValues.bpnL1,
        legalName = name1,
        identifiers = listOf(identifier1),
        legalForm = legalForm1,
        status = listOf(leStatus1),
        classifications = listOf(classification1, classification2),
        currentness = SaasValues.createdTime1.toInstant(ZoneOffset.UTC),
        createdAt = CommonValues.now,
        updatedAt = CommonValues.now
    )

    val legalEntity2 = LegalEntityResponse(
        bpn = CommonValues.bpnL2,
        legalName = name3,
        identifiers = listOf(identifier2),
        legalForm = legalForm2,
        status = listOf(leStatus2),
        classifications = listOf(classification3, classification4),
        currentness = SaasValues.createdTime1.toInstant(ZoneOffset.UTC),
        createdAt = CommonValues.now,
        updatedAt = CommonValues.now
    )

    val legalEntity3 = LegalEntityResponse(
        bpn = CommonValues.bpnL3,
        legalName = name5,
        identifiers = listOf(identifier3),
        legalForm = legalForm3,
        status = listOf(leStatus3),
        classifications = listOf(classification5),
        currentness = SaasValues.createdTime1.toInstant(ZoneOffset.UTC),
        createdAt = CommonValues.now,
        updatedAt = CommonValues.now
    )

    val legalEntityUpsert1 = LegalEntityPartnerCreateResponse(
        legalEntity = legalEntity1,
        legalAddress = address1,
        index = CommonValues.index1
    )

    val legalEntityUpsert2 = LegalEntityPartnerCreateResponse(
        legalEntity = legalEntity2,
        legalAddress = address2,
        index = CommonValues.index2
    )

    val legalEntityUpsert3 = LegalEntityPartnerCreateResponse(
        legalEntity = legalEntity3,
        legalAddress = address3,
        index = CommonValues.index3
    )


}