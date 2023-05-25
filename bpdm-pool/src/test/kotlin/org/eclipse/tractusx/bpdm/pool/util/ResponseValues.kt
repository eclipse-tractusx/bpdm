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

import org.eclipse.tractusx.bpdm.common.dto.StreetDto
import org.eclipse.tractusx.bpdm.common.dto.response.*
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.common.service.toDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressPartnerCreateResponse
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityPartnerCreateResponse
import org.eclipse.tractusx.bpdm.pool.api.model.response.SitePartnerCreateResponse
import java.time.Instant
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

    private val country1 = TypeKeyNameDto(CommonValues.country1, CommonValues.country1.getName())
    private val country2 = TypeKeyNameDto(CommonValues.country2, CommonValues.country2.getName())
    private val country3 = TypeKeyNameDto(CommonValues.country3, CommonValues.country3.getName())

    private val identifier1 =
        LegalEntityIdentifierResponse(CommonValues.identifierValue1, RequestValues.identifierType1, CommonValues.issuingBody1)
    private val identifier2 =
        LegalEntityIdentifierResponse(CommonValues.identifierValue2, RequestValues.identifierType2, CommonValues.issuingBody2)
    private val identifier3 =
        LegalEntityIdentifierResponse(CommonValues.identifierValue3, RequestValues.identifierType3, CommonValues.issuingBody3)

    private val name1 = NameResponse(value = CommonValues.name1)
    private val name2 = NameResponse(value = CommonValues.name2)
    private val name3 = NameResponse(value = CommonValues.name3)
    private val name4 = NameResponse(value = CommonValues.name4)
    private val name5 = NameResponse(value = CommonValues.name5)

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

    private val statusType1 = TypeKeyNameDto(CommonValues.statusType1, CommonValues.statusType1.getTypeName())
    private val statusType2 = TypeKeyNameDto(CommonValues.statusType2, CommonValues.statusType2.getTypeName())
    private val statusType3 = TypeKeyNameDto(CommonValues.statusType3, CommonValues.statusType3.getTypeName())

    private val leStatus1 = LegalEntityStateResponse(CommonValues.statusDenotation1, CommonValues.statusValidFrom1, null, statusType1)
    private val leStatus2 = LegalEntityStateResponse(CommonValues.statusDenotation2, CommonValues.statusValidFrom2, null, statusType2)
    private val leStatus3 = LegalEntityStateResponse(CommonValues.statusDenotation3, CommonValues.statusValidFrom3, null, statusType3)

    private val siteStatus1 = SiteStateResponse(CommonValues.statusDenotation1, CommonValues.statusValidFrom1, null, CommonValues.statusType1.toDto())
    private val siteStatus2 = SiteStateResponse(CommonValues.statusDenotation2, CommonValues.statusValidFrom2, null, CommonValues.statusType2.toDto())
    private val siteStatus3 = SiteStateResponse(CommonValues.statusDenotation3, CommonValues.statusValidFrom3, null, CommonValues.statusType3.toDto())

    private val classificationType = TypeKeyNameDto(CommonValues.classificationType, CommonValues.classificationType.name)

    private val classification1 = ClassificationResponse(CommonValues.classification1, null, classificationType)
    private val classification2 = ClassificationResponse(CommonValues.classification2, null, classificationType)
    private val classification3 = ClassificationResponse(CommonValues.classification3, null, classificationType)
    private val classification4 = ClassificationResponse(CommonValues.classification4, null, classificationType)
    private val classification5 = ClassificationResponse(CommonValues.classification5, null, classificationType)

    private val address1 = PhysicalPostalAddressResponse(
        companyPostCode = CommonValues.postCode2,
        industrialZone = CommonValues.industrialZone1,
        building = CommonValues.building1,
        floor = CommonValues.floor1,
        door = CommonValues.door1,
        areaPart = AreaDistrictResponse(
            administrativeAreaLevel1 = CommonValues.adminAreaLevel1Region1,
            administrativeAreaLevel2 = CommonValues.county1,
            district = CommonValues.district1,
        ),
        street = StreetDto(CommonValues.street1, CommonValues.houseNumber1),
        baseAddress = BasePostalAddressResponse(
            geographicCoordinates = null,
            country = country1,
            postCode = CommonValues.postCode1,
            city = CommonValues.city1,
        )
    )

    private val address2 = PhysicalPostalAddressResponse(
        industrialZone = CommonValues.industrialZone2,
        building = CommonValues.building2,
        floor = CommonValues.floor2,
        door = CommonValues.door2,
        areaPart = AreaDistrictResponse(
            administrativeAreaLevel1 = CommonValues.adminAreaLevel1Region2,
            administrativeAreaLevel2 = CommonValues.county2,
            district = CommonValues.district2,
        ),
        street = StreetDto(CommonValues.street2, CommonValues.houseNumber2),
        baseAddress = BasePostalAddressResponse(
            geographicCoordinates = null,
            country = country2,
            postCode = CommonValues.postCode2,
            city = CommonValues.city2,
        )
    )

    private val address3 = PhysicalPostalAddressResponse(
        industrialZone = CommonValues.industrialZone3,
        building = CommonValues.building3,
        floor = CommonValues.floor3,
        door = CommonValues.door3,
        areaPart = AreaDistrictResponse(
            administrativeAreaLevel1 = CommonValues.adminAreaLevel1Region3,
            administrativeAreaLevel2 = CommonValues.county3,
            district = CommonValues.district3,
        ),
        street = StreetDto(CommonValues.street3, CommonValues.houseNumber3),
        baseAddress = BasePostalAddressResponse(
            geographicCoordinates = null,
            country = country3,
            postCode = CommonValues.postCode3,
            city = CommonValues.city3,
        )
    )

    val addressPartner1 = LogisticAddressResponse(
        bpn = CommonValues.bpnA1,
        physicalPostalAddress = address1,
        bpnLegalEntity = null,
        bpnSite = null,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    val addressPartner2 = LogisticAddressResponse(
        bpn = CommonValues.bpnA2,
        physicalPostalAddress = address2,
        bpnLegalEntity = null,
        bpnSite = null,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    val addressPartner3 = LogisticAddressResponse(
        bpn = CommonValues.bpnA3,
        physicalPostalAddress = address3,
        bpnLegalEntity = null,
        bpnSite = null,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    val addressPartnerCreate1 = AddressPartnerCreateResponse(
        address = addressPartner1,
        index = CommonValues.index1
    )

    val addressPartnerCreate2 = AddressPartnerCreateResponse(
        address = addressPartner2,
        index = CommonValues.index2
    )

    val addressPartnerCreate3 = AddressPartnerCreateResponse(
        address = addressPartner3,
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
        site = site1,
        mainAddress = addressPartner1.copy(
            bpnSite = site1.bpn,
            isMainAddress = true
        ),
        index = CommonValues.index1
    )

    val siteUpsert2 = SitePartnerCreateResponse(
        site = site2,
        mainAddress = addressPartner2.copy(
            bpnSite = site2.bpn,
            isMainAddress = true
        ),
        index = CommonValues.index2
    )

    val siteUpsert3 = SitePartnerCreateResponse(
        site = site3,
        mainAddress = addressPartner3.copy(
            bpnSite = site3.bpn,
            isMainAddress = true
        ),
        index = CommonValues.index3
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
        legalEntity = LegalEntityResponse(
            bpn = CommonValues.bpnL1,
            legalName = name1,
            identifiers = listOf(LegalEntityIdentifierResponse(CommonValues.identifierValue1, RequestValues.identifierType1, CommonValues.issuingBody1)),
            legalForm = legalForm1,
            states = listOf(leStatus1),
            classifications = listOf(classification1, classification2),
            currentness = SaasValues.createdTime1.toInstant(ZoneOffset.UTC),
            createdAt = CommonValues.now,
            updatedAt = CommonValues.now
        ),
        legalAddress = addressPartner1.copy(
            bpnLegalEntity = legalEntity1.bpn,
            isLegalAddress = true
        ),
        index = CommonValues.index1
    )

    val legalEntityUpsert2 = LegalEntityPartnerCreateResponse(
        legalEntity = LegalEntityResponse(
            bpn = CommonValues.bpnL2,
            legalName = name3,
            identifiers = listOf(LegalEntityIdentifierResponse(CommonValues.identifierValue2, RequestValues.identifierType2, CommonValues.issuingBody2)),
            legalForm = legalForm2,
            states = listOf(leStatus2),
            classifications = listOf(classification3, classification4),
            currentness = SaasValues.createdTime1.toInstant(ZoneOffset.UTC),
            createdAt = CommonValues.now,
            updatedAt = CommonValues.now
        ),
        legalAddress = addressPartner2.copy(
            bpnLegalEntity = legalEntity2.bpn,
            isLegalAddress = true
        ),
        index = CommonValues.index2
    )

    val legalEntityUpsert3 = LegalEntityPartnerCreateResponse(
        legalEntity = LegalEntityResponse(
            bpn = CommonValues.bpnL3,
            legalName = name5,
            identifiers = listOf(LegalEntityIdentifierResponse(CommonValues.identifierValue3, RequestValues.identifierType3, CommonValues.issuingBody3)),
            legalForm = legalForm3,
            states = listOf(leStatus3),
            classifications = listOf(classification5),
            currentness = SaasValues.createdTime1.toInstant(ZoneOffset.UTC),
            createdAt = CommonValues.now,
            updatedAt = CommonValues.now
        ),
        legalAddress = addressPartner3.copy(
            bpnLegalEntity = legalEntity3.bpn,
            isLegalAddress = true
        ),
        index = CommonValues.index3
    )


}