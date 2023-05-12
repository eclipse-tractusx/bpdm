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

import com.neovisionaries.i18n.LanguageCode
import org.eclipse.tractusx.bpdm.common.dto.saas.*
import org.eclipse.tractusx.bpdm.common.model.*
import java.time.LocalDateTime

/**
 * Test values for SaaS DTOs
 * Numbered values should match with @see ResponseValues numbered values for easier testing
 */
object SaasValues {

    val partnerId1 = "saas-1"
    val partnerId2 = "saas-2"
    val partnerId3 = "saas-3"
    val partnerId4 = "saas-4"
    val partnerId5 = "saas-5"
    val partnerId6 = "saas-6"
    val partnerId7 = "saas-7"
    val partnerId8 = "saas-8"
    val partnerId9 = "saas-9"


    val datasource1 = "datasource-1"

    val createdTime1 = LocalDateTime.of(2020, 1, 1, 1, 1)

    val language1 = LanguageSaas(LanguageCode.en, LanguageCode.en.getName())
    val language2 = LanguageSaas(LanguageCode.de, LanguageCode.de.getName())
    val language3 = LanguageSaas(LanguageCode.zh, LanguageCode.zh.getName())

    val legalEntityName1 = NameSaas(CommonValues.name1)
    val legalEntityName2 = NameSaas(CommonValues.name2)
    val legalEntityName3 = NameSaas(CommonValues.name3)
    val legalEntityName4 = NameSaas(CommonValues.name4)
    val legalEntityName5 = NameSaas(CommonValues.name5)

    val siteName1 = NameSaas(CommonValues.siteName1)
    val siteName2 = NameSaas(CommonValues.siteName2)
    val siteName3 = NameSaas(CommonValues.siteName3)

    val identifierType1 = TypeKeyNameUrlSaas(CommonValues.identifierTypeTechnicalKey1, CommonValues.identifierTypeName1)
    val identifierType2 = TypeKeyNameUrlSaas(CommonValues.identifierTypeTechnicalKey2, CommonValues.identifierTypeName2)
    val identifierType3 = TypeKeyNameUrlSaas(CommonValues.identifierTypeTechnicalKey3, CommonValues.identifierTypeName3)

    val issuingBody1 = TypeKeyNameUrlSaas(name=CommonValues.issuingBody1)
    val issuingBody2 = TypeKeyNameUrlSaas(name=CommonValues.issuingBody2)
    val issuingBody3 = TypeKeyNameUrlSaas(name=CommonValues.issuingBody3)

    val identifierStatus1 = TypeKeyNameSaas("identifierstatuskey", "identifierstatusname")

    val identifier1 = IdentifierSaas(identifierType1, CommonValues.identifierValue1, issuingBody1, identifierStatus1)
    val identifier2 = IdentifierSaas(identifierType2, CommonValues.identifierValue2, issuingBody2, identifierStatus1)
    val identifier3 = IdentifierSaas(identifierType3, CommonValues.identifierValue3, issuingBody3, identifierStatus1)

    val legalForm1 = LegalFormSaas(
        technicalKey = CommonValues.legalFormTechnicalKey1,
        name = CommonValues.legalFormName1,
        mainAbbreviation = CommonValues.legalFormAbbreviation1,
    )
    val legalForm2 = LegalFormSaas(
        technicalKey = CommonValues.legalFormTechnicalKey2,
        name = CommonValues.legalFormName2,
        mainAbbreviation = CommonValues.legalFormAbbreviation2,
    )
    val legalForm3 = LegalFormSaas(
        technicalKey = CommonValues.legalFormTechnicalKey3,
        name = CommonValues.legalFormName3,
        mainAbbreviation = CommonValues.legalFormAbbreviation3,
    )

    val statusType1 = TypeKeyNameUrlSaas(CommonValues.statusType1.name)
    val statusType2 = TypeKeyNameUrlSaas(CommonValues.statusType2.name)
    val statusType3 = TypeKeyNameUrlSaas(CommonValues.statusType3.name)

    val status1 = BusinessPartnerStatusSaas(statusType1, CommonValues.statusDenotation1, CommonValues.statusValidFrom1)
    val status2 = BusinessPartnerStatusSaas(statusType2, CommonValues.statusDenotation2, CommonValues.statusValidFrom2)
    val status3 = BusinessPartnerStatusSaas(statusType3, CommonValues.statusDenotation3, CommonValues.statusValidFrom3)

    val classificationType = TypeKeyNameUrlSaas(CommonValues.classificationType.name)

    val classification1 = ClassificationSaas(value = CommonValues.classification1, type = classificationType)
    val classification2 = ClassificationSaas(value = CommonValues.classification2, type = classificationType)
    val classification3 = ClassificationSaas(value = CommonValues.classification3, type = classificationType)
    val classification4 = ClassificationSaas(value = CommonValues.classification4, type = classificationType)
    val classification5 = ClassificationSaas(value = CommonValues.classification5, type = classificationType)

    val addressId1 = "address-saas-1"
    val addressId2 = "address-saas-2"
    val addressId3 = "address-saas-3"

    val locality1 = LocalitySaas(saasValue = CommonValues.locality1, saasType = SaasLocalityType.CITY, saasLanguage = language1)
    val locality2 = LocalitySaas(value = CommonValues.locality2)
    val locality3 = LocalitySaas(saasValue = CommonValues.locality3, saasType = SaasLocalityType.CITY, saasLanguage = language1)
    val locality4 = LocalitySaas(value = CommonValues.locality4)

    val city1 = LocalitySaas(saasValue = CommonValues.city1, saasType = SaasLocalityType.CITY, saasLanguage = language1)
    val city2 = LocalitySaas(saasValue = CommonValues.city2, saasType = SaasLocalityType.CITY, saasLanguage = language2)
    val city3 = LocalitySaas(saasValue = CommonValues.city3, saasType = SaasLocalityType.CITY, saasLanguage = language3)

    val postalDeliveryPoint1 = PostalDeliveryPointSaas(value = CommonValues.postalDeliveryPoint1)
    val postalDeliveryPoint2 = PostalDeliveryPointSaas(value = CommonValues.postalDeliveryPoint2)
    val postalDeliveryPoint3 = PostalDeliveryPointSaas(value = CommonValues.postalDeliveryPoint3)
    val postalDeliveryPoint4 = PostalDeliveryPointSaas(value = CommonValues.postalDeliveryPoint4)
    val postalDeliveryPoint5 = PostalDeliveryPointSaas(value = CommonValues.postalDeliveryPoint5)

    val parentRelationType = TypeKeyNameSaas("PARENT")

    val legalEntityType = TypeKeyNameUrlSaas("LEGAL_ENTITY")
    val siteType = TypeKeyNameUrlSaas("ORGANIZATIONAL_UNIT")
    val addressType = TypeKeyNameUrlSaas("BP_ADDRESS")


    val parentRelation = RelationSaas(
        type = parentRelationType,
        startNode = CommonValues.bpnL1,
        endNode = CommonValues.bpnS1,
        startNodeDataSource = datasource1,
        endNodeDataSource = datasource1
    )

    val address1 = AddressSaas(
        id = addressId1,
        externalId = addressId1,
        saasId = addressId1,
        country = CountrySaas(CommonValues.country1, CommonValues.country1.getName()),
        administrativeAreas = listOf(AdministrativeAreaSaas(CommonValues.adminArea1), AdministrativeAreaSaas(CommonValues.adminArea2)),
        postCodes = listOf(PostCodeSaas(CommonValues.postCode1), PostCodeSaas(CommonValues.postCode2)),
        localities = listOf(locality1, locality2),
        thoroughfares = listOf(ThoroughfareSaas(value = CommonValues.thoroughfare1), ThoroughfareSaas(value = CommonValues.thoroughfare2)),
        premises = listOf(PremiseSaas(value = CommonValues.premise1), PremiseSaas(value = CommonValues.premise2)),
        postalDeliveryPoints = listOf(postalDeliveryPoint1, postalDeliveryPoint2),
        types = listOf(SaasAddressType.LEGAL.toSaasTypeDto()),
    )

    val address2 = AddressSaas(
        id = addressId2,
        externalId = addressId2,
        saasId = addressId2,
        country = CountrySaas(CommonValues.country2, CommonValues.country2.getName()),
        administrativeAreas = listOf(AdministrativeAreaSaas(CommonValues.adminArea3), AdministrativeAreaSaas(CommonValues.adminArea4)),
        postCodes = listOf(PostCodeSaas(CommonValues.postCode3), PostCodeSaas(CommonValues.postCode4)),
        localities = listOf(locality3, locality4),
        thoroughfares = listOf(ThoroughfareSaas(value = CommonValues.thoroughfare3), ThoroughfareSaas(value = CommonValues.thoroughfare4)),
        premises = listOf(PremiseSaas(value = CommonValues.premise3), PremiseSaas(value = CommonValues.premise4)),
        postalDeliveryPoints = listOf(postalDeliveryPoint3, postalDeliveryPoint4),
        types = listOf(SaasAddressType.LEGAL.toSaasTypeDto()),
    )

    val address3 = AddressSaas(
        id = addressId3,
        externalId = addressId3,
        saasId = addressId3,
        country = CountrySaas(CommonValues.country3, CommonValues.country3.getName()),
        administrativeAreas = listOf(AdministrativeAreaSaas(CommonValues.adminArea5)),
        postCodes = listOf(PostCodeSaas(CommonValues.postCode5)),
        localities = listOf(LocalitySaas(saasValue = CommonValues.locality5, saasType = SaasLocalityType.CITY, saasLanguage = language1)),
        thoroughfares = listOf(ThoroughfareSaas(value = CommonValues.thoroughfare5)),
        premises = listOf(PremiseSaas(value = CommonValues.premise5)),
        postalDeliveryPoints = listOf(postalDeliveryPoint5),
        types = listOf(SaasAddressType.LEGAL.toSaasTypeDto()),
    )


    val legalEntity1 = BusinessPartnerSaas(
        id = partnerId1,
        createdAt = createdTime1,
        lastModifiedAt = createdTime1,
        externalId = partnerId1,
        dataSource = datasource1,
        names = listOf(legalEntityName1, legalEntityName2),
        identifiers = listOf(identifier1),
        legalForm = legalForm1,
        status = status1,
        profile = PartnerProfileSaas(classifications = listOf(classification1, classification2)),
        addresses = listOf(address1),
        types = listOf(legalEntityType)
    )

    val legalEntity2 = BusinessPartnerSaas(
        id = partnerId2,
        createdAt = createdTime1,
        lastModifiedAt = createdTime1,
        externalId = partnerId2,
        dataSource = datasource1,
        names = listOf(legalEntityName3, legalEntityName4),
        identifiers = listOf(identifier2),
        legalForm = legalForm2,
        status = status2,
        profile = PartnerProfileSaas(classifications = listOf(classification3, classification4)),
        addresses = listOf(address2),
        types = listOf(legalEntityType)
    )

    val legalEntity3 = BusinessPartnerSaas(
        id = partnerId3,
        createdAt = createdTime1,
        lastModifiedAt = createdTime1,
        externalId = partnerId3,
        dataSource = datasource1,
        names = listOf(legalEntityName5),
        identifiers = listOf(identifier3),
        legalForm = legalForm3,
        status = status3,
        profile =  PartnerProfileSaas(classifications = listOf(classification5)),
        addresses = listOf(address3),
        types = listOf(legalEntityType)
    )

    val site1 = BusinessPartnerSaas(
        id = partnerId4,
        createdAt = createdTime1,
        lastModifiedAt = createdTime1,
        externalId = partnerId4,
        dataSource = datasource1,
        names = listOf(siteName1),
        status = status1,
        addresses = listOf(address1),
        types = listOf(siteType),
        relations = listOf(
            RelationSaas(
                type = parentRelationType,
                startNode = CommonValues.bpnL1,
                endNode = CommonValues.bpnS1,
                startNodeDataSource = datasource1,
                endNodeDataSource = datasource1
            )
        )
    )

    val site2 = BusinessPartnerSaas(
        id = partnerId5,
        createdAt = createdTime1,
        lastModifiedAt = createdTime1,
        externalId = partnerId5,
        dataSource = datasource1,
        names = listOf(siteName2),
        status = status2,
        addresses = listOf(address2),
        types = listOf(siteType),
        relations = listOf(
            RelationSaas(
                type = parentRelationType,
                startNode = CommonValues.bpnL2,
                endNode = CommonValues.bpnS2,
                startNodeDataSource = datasource1,
                endNodeDataSource = datasource1
            )
        )
    )

    val site3 = BusinessPartnerSaas(
        id = partnerId6,
        createdAt = createdTime1,
        lastModifiedAt = createdTime1,
        externalId = partnerId6,
        dataSource = datasource1,
        names = listOf(siteName3),
        status = status3,
        addresses = listOf(address3),
        types = listOf(siteType),
        relations = listOf(
            RelationSaas(
                type = parentRelationType,
                startNode = CommonValues.bpnL3,
                endNode = CommonValues.bpnS3,
                startNodeDataSource = datasource1,
                endNodeDataSource = datasource1
            )
        )
    )


    val addressPartner1 = BusinessPartnerSaas(
        id = partnerId7,
        createdAt = createdTime1,
        lastModifiedAt = createdTime1,
        externalId = partnerId7,
        dataSource = datasource1,
        addresses = listOf(address1),
        types = listOf(addressType),
        relations = listOf(
            RelationSaas(
                type = parentRelationType,
                startNode = CommonValues.bpnL1,
                endNode = CommonValues.bpnA1,
                startNodeDataSource = datasource1,
                endNodeDataSource = datasource1
            )
        )
    )

    val addressPartner2 = BusinessPartnerSaas(
        id = partnerId8,
        createdAt = createdTime1,
        lastModifiedAt = createdTime1,
        externalId = partnerId8,
        dataSource = datasource1,
        addresses = listOf(address2),
        types = listOf(addressType),
        relations = listOf(
            RelationSaas(
                type = parentRelationType,
                startNode = CommonValues.bpnL2,
                endNode = CommonValues.bpnA2,
                startNodeDataSource = datasource1,
                endNodeDataSource = datasource1
            )
        )
    )

    val addressPartner3 = BusinessPartnerSaas(
        id = partnerId9,
        createdAt = createdTime1,
        lastModifiedAt = createdTime1,
        externalId = partnerId9,
        dataSource = datasource1,
        addresses = listOf(address3),
        types = listOf(addressType),
        relations = listOf(
            RelationSaas(
                type = parentRelationType,
                startNode = CommonValues.bpnL3,
                endNode = CommonValues.bpnA3,
                startNodeDataSource = datasource1,
                endNodeDataSource = datasource1
            )
        )
    )

    private val thoroughfareZone1 = ThoroughfareSaas(name = CommonValues.industrialZone1, saasType = SaasThoroughfareType.INDUSTRIAL_ZONE, language = language1)
    private val thoroughfareStreet1 = ThoroughfareSaas(name = CommonValues.street1, direction = null
        , number = CommonValues.houseNumber1, saasType = SaasThoroughfareType.STREET, language = language1
    )
    private val thoroughfareZone2 = ThoroughfareSaas(name = CommonValues.industrialZone2, saasType = SaasThoroughfareType.INDUSTRIAL_ZONE, language = language2)
    private val thoroughfareStreet2 = ThoroughfareSaas(name = CommonValues.street2, direction = null
        , number = CommonValues.houseNumber2, saasType = SaasThoroughfareType.STREET, language = language2
    )
    private val thoroughfareZone3 = ThoroughfareSaas(name = CommonValues.industrialZone3, saasType = SaasThoroughfareType.INDUSTRIAL_ZONE, language = language3)
    private val thoroughfareStreet3 = ThoroughfareSaas(name = CommonValues.street3, direction = null
        , number = CommonValues.houseNumber3, saasType = SaasThoroughfareType.STREET, language = language3
    )


    val addressPartnerSaas1 = BusinessPartnerSaas(
        id = partnerId7,
        createdAt = createdTime1,
        lastModifiedAt = createdTime1,
        externalId = partnerId7,
        dataSource = datasource1,
        addresses = listOf( AddressSaas(
            id = addressId1,
            externalId = addressId1,
            saasId = addressId1,
            country = CountrySaas(CommonValues.country1, CommonValues.country1.getName()),
            administrativeAreas = listOf(
                AdministrativeAreaSaas(CommonValues.adminArea1, SaasAdministrativeAreaType.REGION, language1),
                AdministrativeAreaSaas(CommonValues.county1, SaasAdministrativeAreaType.COUNTY, language1)
            ),
            postCodes = listOf(PostCodeSaas(CommonValues.postCode1, SaasPostCodeType.REGULAR)),
            localities = listOf(
                city1, LocalitySaas(CommonValues.districtLevel1_1, SaasLocalityType.DISTRICT, language1),
                LocalitySaas(CommonValues.districtLevel2_1, SaasLocalityType.QUARTER, language1)
            ),
            thoroughfares = listOf(thoroughfareZone1, thoroughfareStreet1),
            premises = listOf(
                PremiseSaas(CommonValues.building1, SaasPremiseType.BUILDING, language1),
                PremiseSaas(CommonValues.door1, SaasPremiseType.ROOM, language1),
                PremiseSaas(CommonValues.floor1, SaasPremiseType.LEVEL, language1)
            ),
            postalDeliveryPoints = listOf(
                PostalDeliveryPointSaas(value = CommonValues.postalDeliveryPoint1), PostalDeliveryPointSaas(value = CommonValues.postalDeliveryPoint2)
            ),
            types = listOf(SaasAddressType.LEGAL.toSaasTypeDto()),
        )),
        types = listOf(addressType),
        relations = listOf(
            RelationSaas(
                type = parentRelationType,
                startNode = CommonValues.bpnL1,
                endNode = CommonValues.bpnA1,
                startNodeDataSource = datasource1,
                endNodeDataSource = datasource1
            )
        )
    )

    val addressPartnerSaas2 = BusinessPartnerSaas(
        id = partnerId8,
        createdAt = createdTime1,
        lastModifiedAt = createdTime1,
        externalId = partnerId8,
        dataSource = datasource1,
        addresses = listOf(AddressSaas(
            id = addressId2,
            externalId = addressId2,
            saasId = addressId2,
            country = CountrySaas(CommonValues.country2, CommonValues.country2.getName()),
            administrativeAreas = listOf(
                AdministrativeAreaSaas(CommonValues.adminArea2, SaasAdministrativeAreaType.REGION, language2),
                AdministrativeAreaSaas(CommonValues.county2, SaasAdministrativeAreaType.COUNTY, language2)
            ),
            postCodes = listOf(PostCodeSaas(CommonValues.postCode2, SaasPostCodeType.REGULAR)),
            localities = listOf(
                city2,
                LocalitySaas(CommonValues.districtLevel1_2, SaasLocalityType.DISTRICT, language1),
                LocalitySaas(CommonValues.districtLevel2_2, SaasLocalityType.QUARTER, language1)
            ),
            thoroughfares = listOf(thoroughfareZone2, thoroughfareStreet2),
            premises = listOf(
                PremiseSaas(CommonValues.building2, SaasPremiseType.BUILDING, language2),
                PremiseSaas(CommonValues.door2, SaasPremiseType.ROOM, language2),
                PremiseSaas(CommonValues.floor2, SaasPremiseType.LEVEL, language2)
            ),
            postalDeliveryPoints = listOf(
                PostalDeliveryPointSaas(value = CommonValues.postalDeliveryPoint3), PostalDeliveryPointSaas(value = CommonValues.postalDeliveryPoint4)
            ),
            types = listOf(SaasAddressType.LEGAL.toSaasTypeDto()),
        )),
        types = listOf(addressType),
        relations = listOf(
            RelationSaas(
                type = parentRelationType,
                startNode = CommonValues.bpnL2,
                endNode = CommonValues.bpnA2,
                startNodeDataSource = datasource1,
                endNodeDataSource = datasource1
            )
        )
    )

    val addressPartnerSaas3 = BusinessPartnerSaas(
        id = partnerId9,
        createdAt = createdTime1,
        lastModifiedAt = createdTime1,
        externalId = partnerId9,
        dataSource = datasource1,
        addresses = listOf(AddressSaas(
            id = addressId3,
            externalId = addressId3,
            saasId = addressId3,
            country = CountrySaas(CommonValues.country3, CommonValues.country3.getName()),
            administrativeAreas = listOf(
                AdministrativeAreaSaas(CommonValues.adminArea3, SaasAdministrativeAreaType.REGION, language3),
                AdministrativeAreaSaas(CommonValues.county3, SaasAdministrativeAreaType.COUNTY, language3)
            ),
            postCodes = listOf(PostCodeSaas(CommonValues.postCode3, SaasPostCodeType.REGULAR)),
            localities = listOf(
                city3,
                LocalitySaas(CommonValues.districtLevel1_3, SaasLocalityType.DISTRICT, language1),
                LocalitySaas(CommonValues.districtLevel2_3, SaasLocalityType.QUARTER, language1)
            ),
            thoroughfares = listOf(thoroughfareZone3, thoroughfareStreet3),
            premises = listOf(
                PremiseSaas(CommonValues.building3, SaasPremiseType.BUILDING, language2),
                PremiseSaas(CommonValues.door3, SaasPremiseType.ROOM, language2),
                PremiseSaas(CommonValues.floor3, SaasPremiseType.LEVEL, language2)
            ),
            postalDeliveryPoints = listOf(PostalDeliveryPointSaas(value = CommonValues.postalDeliveryPoint5)),
            types = listOf(SaasAddressType.LEGAL.toSaasTypeDto()),
        )),
        types = listOf(addressType),
        relations = listOf(
            RelationSaas(
                type = parentRelationType,
                startNode = CommonValues.bpnL3,
                endNode = CommonValues.bpnA3,
                startNodeDataSource = datasource1,
                endNodeDataSource = datasource1
            )
        )
    )
}