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

import org.eclipse.tractusx.bpdm.common.dto.NameRegioncodeDto
import org.eclipse.tractusx.bpdm.common.dto.StreetDto
import org.eclipse.tractusx.bpdm.common.dto.response.*
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.common.service.toDto
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

    val country1 = TypeKeyNameDto(CommonValues.country1, CommonValues.country1.getName())
    val country2 = TypeKeyNameDto(CommonValues.country2, CommonValues.country2.getName())
    val country3 = TypeKeyNameDto(CommonValues.country3, CommonValues.country3.getName())

    val identifier1 =
        LegalEntityIdentifierResponse(CommonValues.identifierValue1, RequestValues.identifierType1, CommonValues.issuingBody1)
    val identifier2 =
        LegalEntityIdentifierResponse(CommonValues.identifierValue2, RequestValues.identifierType2, CommonValues.issuingBody2)
    val identifier3 =
        LegalEntityIdentifierResponse(CommonValues.identifierValue3, RequestValues.identifierType3, CommonValues.issuingBody3)

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

    val leStatus1 = LegalEntityStateResponse(CommonValues.statusDenotation1, CommonValues.statusValidFrom1, null, statusType1)
    val leStatus2 = LegalEntityStateResponse(CommonValues.statusDenotation2, CommonValues.statusValidFrom2, null, statusType2)
    val leStatus3 = LegalEntityStateResponse(CommonValues.statusDenotation3, CommonValues.statusValidFrom3, null, statusType3)

    val siteStatus1 = SiteStateResponse(CommonValues.statusDenotation1, CommonValues.statusValidFrom1, null, CommonValues.statusType1.toDto())
    val siteStatus2 = SiteStateResponse(CommonValues.statusDenotation2, CommonValues.statusValidFrom2, null, CommonValues.statusType2.toDto())
    val siteStatus3 = SiteStateResponse(CommonValues.statusDenotation3, CommonValues.statusValidFrom3, null, CommonValues.statusType3.toDto())

    val classificationType = TypeKeyNameDto(CommonValues.classificationType, CommonValues.classificationType.name)

    val classification1 = ClassificationResponse(CommonValues.classification1, null, classificationType)
    val classification2 = ClassificationResponse(CommonValues.classification2, null, classificationType)
    val classification3 = ClassificationResponse(CommonValues.classification3, null, classificationType)
    val classification4 = ClassificationResponse(CommonValues.classification4, null, classificationType)
    val classification5 = ClassificationResponse(CommonValues.classification5, null, classificationType)

    val address1 = BasePostalAddressResponse(
        geographicCoordinates = null,
        country = country1,
        administrativeAreaLevel1 = NameRegioncodeDto( CommonValues.adminAreaLevel1RegionCode_1,CommonValues.adminAreaLevel1Name_1),
        administrativeAreaLevel2 = CommonValues.county1,
        postCode = CommonValues.postCode1,
        city = CommonValues.city1,
        districtLevel1 = CommonValues.districtLevel1_1,
        districtLevel2 = CommonValues.districtLevel2_1,
        street = StreetDto(CommonValues.street1, CommonValues.houseNumber1),
        physicalAddress = PhysicalPostalAddressResponse(industrialZone = CommonValues.industrialZone1, building = CommonValues.building1, floor = CommonValues.floor1, door = CommonValues.door1),
    )
    val address2 = BasePostalAddressResponse(
        geographicCoordinates = null,
        country = country2,
        administrativeAreaLevel1 = NameRegioncodeDto( CommonValues.adminAreaLevel1RegionCode_2,CommonValues.adminAreaLevel1Name_2),
        administrativeAreaLevel2 = CommonValues.county2,
        postCode = CommonValues.postCode2,
        city = CommonValues.city2,
        districtLevel1 = CommonValues.districtLevel1_2,
        districtLevel2 = CommonValues.districtLevel2_2,
        street = StreetDto(CommonValues.street2, CommonValues.houseNumber2),
        physicalAddress = PhysicalPostalAddressResponse(industrialZone = CommonValues.industrialZone2, building = CommonValues.building2, floor = CommonValues.floor2, door = CommonValues.door2),
    )

    val address3 = BasePostalAddressResponse(
        geographicCoordinates = null,
        country = country3,
        administrativeAreaLevel1 = NameRegioncodeDto( CommonValues.adminAreaLevel1RegionCode_3,CommonValues.adminAreaLevel1Name_3),
        administrativeAreaLevel2 = CommonValues.county3,
        postCode = CommonValues.postCode3,
        city = CommonValues.city3,
        districtLevel1 = CommonValues.districtLevel1_3,
        districtLevel2 = CommonValues.districtLevel2_3,
        street = StreetDto(CommonValues.street3, CommonValues.houseNumber3),
        physicalAddress = PhysicalPostalAddressResponse(industrialZone = CommonValues.industrialZone2, building = CommonValues.building2, floor = CommonValues.floor2, door = CommonValues.door2),
    )

    val addressPartner1 = LogisticAddressResponse(
        bpn = CommonValues.bpnA1,
        postalAddress = address1
    )

    val addressPartner2 = LogisticAddressResponse(
        bpn = CommonValues.bpnA2,
        postalAddress = address2
    )

    val addressPartner3 = LogisticAddressResponse(
        bpn = CommonValues.bpnA3,
        postalAddress = address3
    )

    val addressPartnerCreate1 = AddressPartnerCreateResponse(
        bpn = addressPartner1.bpn,
        address = addressPartner1.postalAddress,
        index = CommonValues.index1
    )

    val addressPartnerCreate2 = AddressPartnerCreateResponse(
        bpn = addressPartner2.bpn,
        address = addressPartner2.postalAddress,
        index = CommonValues.index2
    )

    val addressPartnerCreate3 = AddressPartnerCreateResponse(
        bpn = addressPartner3.bpn,
        address = addressPartner3.postalAddress,
        index = CommonValues.index3
    )

    val site1 = SiteResponse(
        bpn = CommonValues.bpnS1,
        name = CommonValues.siteName1,
        states = listOf(siteStatus1),
        bpnLegalEntity = CommonValues.bpnL1,
        createdAt = CommonValues.now,
        updatedAt = CommonValues.now
    )

    val site2 = SiteResponse(
        bpn = CommonValues.bpnS2,
        name = CommonValues.siteName2,
        states = listOf(siteStatus2),
        bpnLegalEntity = CommonValues.bpnL2,
        createdAt = CommonValues.now,
        updatedAt = CommonValues.now
    )

    val site3 = SiteResponse(
        bpn = CommonValues.bpnS3,
        name = CommonValues.siteName3,
        states = listOf(siteStatus3),
        bpnLegalEntity = CommonValues.bpnL3,
        createdAt = CommonValues.now,
        updatedAt = CommonValues.now
    )

    val siteUpsert1 = SitePartnerCreateResponse(
        site1.bpn,
        site1.name,
        addressPartner1,
        CommonValues.index1
    )

    val siteUpsert2 = SitePartnerCreateResponse(
        site2.bpn,
        site2.name,
        addressPartner2,
        CommonValues.index2
    )

    val siteUpsert3 = SitePartnerCreateResponse(
        site3.bpn,
        site3.name,
        addressPartner3,
        CommonValues.index3
    )


    val legalEntity1 = LegalEntityResponse(
        bpn = CommonValues.bpnL1,
        legalName = name1,
        identifiers = listOf(identifier1),
        legalForm = legalForm1,
        states = listOf(leStatus1),
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
        states = listOf(leStatus2),
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
        states = listOf(leStatus3),
        classifications = listOf(classification5),
        currentness = SaasValues.createdTime1.toInstant(ZoneOffset.UTC),
        createdAt = CommonValues.now,
        updatedAt = CommonValues.now
    )

    val legalEntityUpsert1 = LegalEntityPartnerCreateResponse(
        legalEntity = legalEntity1,
        legalAddress = addressPartner1,
        index = CommonValues.index1
    )

    val legalEntityUpsert2 = LegalEntityPartnerCreateResponse(
        legalEntity = legalEntity2,
        legalAddress = addressPartner2,
        index = CommonValues.index2
    )

    val legalEntityUpsert3 = LegalEntityPartnerCreateResponse(
        legalEntity = legalEntity3,
        legalAddress = addressPartner3,
        index = CommonValues.index3
    )


}