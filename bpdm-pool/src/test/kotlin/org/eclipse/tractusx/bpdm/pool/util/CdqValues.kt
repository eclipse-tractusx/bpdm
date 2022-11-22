/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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
import org.eclipse.tractusx.bpdm.common.dto.cdq.*
import java.time.LocalDateTime

/**
 * Test values for CDQ DTOs
 * Numbered values should match with @see ResponseValues numbered values for easier testing
 */
object CdqValues {

    val partnerId1 = "cdq-1"
    val partnerId2 = "cdq-2"
    val partnerId3 = "cdq-3"
    val partnerId4 = "cdq-4"
    val partnerId5 = "cdq-5"
    val partnerId6 = "cdq-6"
    val partnerId7 = "cdq-7"
    val partnerId8 = "cdq-8"
    val partnerId9 = "cdq-9"


    val datasource1 = "datasource-1"

    val createdTime1 = LocalDateTime.of(2020, 1, 1, 1, 1)

    val language1 = LanguageCdq(LanguageCode.en, LanguageCode.en.getName())
    val language2 = LanguageCdq(LanguageCode.de, LanguageCode.de.getName())
    val language3 = LanguageCdq(LanguageCode.zh, LanguageCode.zh.getName())

    val country1 = CountryCdq(CommonValues.country1, CommonValues.country1.getName())
    val country2 = CountryCdq(CommonValues.country2, CommonValues.country2.getName())
    val country3 = CountryCdq(CommonValues.country3, CommonValues.country3.getName())

    val legalEntityName1 = NameCdq(CommonValues.name1)
    val legalEntityName2 = NameCdq(CommonValues.name2)
    val legalEntityName3 = NameCdq(CommonValues.name3)
    val legalEntityName4 = NameCdq(CommonValues.name4)
    val legalEntityName5 = NameCdq(CommonValues.name5)

    val siteName1 = NameCdq(CommonValues.siteName1)
    val siteName2 = NameCdq(CommonValues.siteName2)
    val siteName3 = NameCdq(CommonValues.siteName3)

    val identifierType1 = TypeKeyNameUrlCdq(CommonValues.identifierTypeTechnicalKey1, CommonValues.identiferTypeName1, CommonValues.identifierTypeUrl1)

    val issuingBody1 = TypeKeyNameUrlCdq(CommonValues.issuingBodyKey1, CommonValues.issuingBodyName1, CommonValues.issuingBodyUrl1)

    val identifierStatus1 = TypeKeyNameCdq(CommonValues.identifierStatusKey1, CommonValues.identifierStatusName1)

    val identifier1 = IdentifierCdq(identifierType1, CommonValues.identifierValue1, issuingBody1, identifierStatus1)
    val identifier2 = IdentifierCdq(identifierType1, CommonValues.identifierValue2, issuingBody1, identifierStatus1)
    val identifier3 = IdentifierCdq(identifierType1, CommonValues.identifierValue3, issuingBody1, identifierStatus1)

    val legalForm1 = LegalFormCdq(
        CommonValues.legalFormName1,
        CommonValues.legalFormUrl1,
        CommonValues.legalFormTechnicalKey1,
        CommonValues.legalFormAbbreviation1,
        language1
    )
    val legalForm2 = LegalFormCdq(
        CommonValues.legalFormName2,
        CommonValues.legalFormUrl2,
        CommonValues.legalFormTechnicalKey2,
        CommonValues.legalFormAbbreviation2,
        language2
    )
    val legalForm3 = LegalFormCdq(
        CommonValues.legalFormName3,
        CommonValues.legalFormUrl3,
        CommonValues.legalFormTechnicalKey3,
        CommonValues.legalFormAbbreviation3,
        language3
    )

    val statusType1 = TypeKeyNameUrlCdq(CommonValues.statusType1.name)
    val statusType2 = TypeKeyNameUrlCdq(CommonValues.statusType2.name)
    val statusType3 = TypeKeyNameUrlCdq(CommonValues.statusType3.name)

    val status1 = BusinessPartnerStatusCdq(statusType1, CommonValues.statusDenotation1, CommonValues.statusValidFrom1)
    val status2 = BusinessPartnerStatusCdq(statusType2, CommonValues.statusDenotation2, CommonValues.statusValidFrom2)
    val status3 = BusinessPartnerStatusCdq(statusType3, CommonValues.statusDenotation3, CommonValues.statusValidFrom3)

    val classificationType = TypeKeyNameUrlCdq(CommonValues.classificationType.name)

    val classification1 = ClassificationCdq(value = CommonValues.classification1, type = classificationType)
    val classification2 = ClassificationCdq(value = CommonValues.classification2, type = classificationType)
    val classification3 = ClassificationCdq(value = CommonValues.classification3, type = classificationType)
    val classification4 = ClassificationCdq(value = CommonValues.classification4, type = classificationType)
    val classification5 = ClassificationCdq(value = CommonValues.classification5, type = classificationType)

    val profile1 = PartnerProfileCdq(classifications = listOf(classification1, classification2))
    val profile2 = PartnerProfileCdq(classifications = listOf(classification3, classification4))
    val profile3 = PartnerProfileCdq(classifications = listOf(classification5))

    val addressId1 = "address-cdq-1"
    val addressId2 = "address-cdq-2"
    val addressId3 = "address-cdq-3"

    val adminArea1 = AdministrativeAreaCdq(CommonValues.adminArea1)
    val adminArea2 = AdministrativeAreaCdq(CommonValues.adminArea2)
    val adminArea3 = AdministrativeAreaCdq(CommonValues.adminArea3)
    val adminArea4 = AdministrativeAreaCdq(CommonValues.adminArea4)
    val adminArea5 = AdministrativeAreaCdq(CommonValues.adminArea5)

    val postCode1 = PostCodeCdq(CommonValues.postCode1)
    val postCode2 = PostCodeCdq(CommonValues.postCode2)
    val postCode3 = PostCodeCdq(CommonValues.postCode3)
    val postCode4 = PostCodeCdq(CommonValues.postCode4)
    val postCode5 = PostCodeCdq(CommonValues.postCode5)

    val locality1 = LocalityCdq(value = CommonValues.locality1)
    val locality2 = LocalityCdq(value = CommonValues.locality2)
    val locality3 = LocalityCdq(value = CommonValues.locality3)
    val locality4 = LocalityCdq(value = CommonValues.locality4)
    val locality5 = LocalityCdq(value = CommonValues.locality5)

    val thoroughfare1 = ThoroughfareCdq(value = CommonValues.thoroughfare1)
    val thoroughfare2 = ThoroughfareCdq(value = CommonValues.thoroughfare2)
    val thoroughfare3 = ThoroughfareCdq(value = CommonValues.thoroughfare3)
    val thoroughfare4 = ThoroughfareCdq(value = CommonValues.thoroughfare4)
    val thoroughfare5 = ThoroughfareCdq(value = CommonValues.thoroughfare5)

    val premise1 = PremiseCdq(value = CommonValues.premise1)
    val premise2 = PremiseCdq(value = CommonValues.premise2)
    val premise3 = PremiseCdq(value = CommonValues.premise3)
    val premise4 = PremiseCdq(value = CommonValues.premise4)
    val premise5 = PremiseCdq(value = CommonValues.premise5)

    val postalDeliveryPoint1 = PostalDeliveryPointCdq(value = CommonValues.postalDeliveryPoint1)
    val postalDeliveryPoint2 = PostalDeliveryPointCdq(value = CommonValues.postalDeliveryPoint2)
    val postalDeliveryPoint3 = PostalDeliveryPointCdq(value = CommonValues.postalDeliveryPoint3)
    val postalDeliveryPoint4 = PostalDeliveryPointCdq(value = CommonValues.postalDeliveryPoint4)
    val postalDeliveryPoint5 = PostalDeliveryPointCdq(value = CommonValues.postalDeliveryPoint5)

    val parentRelationType = TypeKeyNameCdq("PARENT")

    val legalEntityType = TypeKeyNameUrlCdq("LEGAL_ENTITY")
    val siteType = TypeKeyNameUrlCdq("ORGANIZATIONAL_UNIT")
    val addressType = TypeKeyNameUrlCdq("BP_ADDRESS")

    val parentRelation = RelationCdq(
        type = parentRelationType,
        startNode = CommonValues.bpnL1,
        endNode = CommonValues.bpnS1,
        startNodeDataSource = datasource1,
        endNodeDataSource = datasource1
    )

    val address1 = AddressCdq(
        id = addressId1,
        externalId = addressId1,
        cdqId = addressId1,
        country = country1,
        administrativeAreas = listOf(adminArea1, adminArea2),
        postCodes = listOf(postCode1, postCode2),
        localities = listOf(locality1, locality2),
        thoroughfares = listOf(thoroughfare1, thoroughfare2),
        premises = listOf(premise1, premise2),
        postalDeliveryPoints = listOf(postalDeliveryPoint1, postalDeliveryPoint2)
    )

    val address2 = AddressCdq(
        id = addressId2,
        externalId = addressId2,
        cdqId = addressId2,
        country = country2,
        administrativeAreas = listOf(adminArea3, adminArea4),
        postCodes = listOf(postCode3, postCode4),
        localities = listOf(locality3, locality4),
        thoroughfares = listOf(thoroughfare3, thoroughfare4),
        premises = listOf(premise3, premise4),
        postalDeliveryPoints = listOf(postalDeliveryPoint3, postalDeliveryPoint4)
    )

    val address3 = AddressCdq(
        id = addressId3,
        externalId = addressId3,
        cdqId = addressId3,
        country = country3,
        administrativeAreas = listOf(adminArea5),
        postCodes = listOf(postCode5),
        localities = listOf(locality5),
        thoroughfares = listOf(thoroughfare5),
        premises = listOf(premise5),
        postalDeliveryPoints = listOf(postalDeliveryPoint5)
    )


    val legalEntity1 = BusinessPartnerCdq(
        id = partnerId1,
        createdAt = createdTime1,
        lastModifiedAt = createdTime1,
        externalId = partnerId1,
        dataSource = datasource1,
        names = listOf(legalEntityName1, legalEntityName2),
        identifiers = listOf(identifier1),
        legalForm = legalForm1,
        status = status1,
        profile = profile1,
        addresses = listOf(address1),
        types = listOf(legalEntityType)
    )

    val legalEntity2 = BusinessPartnerCdq(
        id = partnerId2,
        createdAt = createdTime1,
        lastModifiedAt = createdTime1,
        externalId = partnerId2,
        dataSource = datasource1,
        names = listOf(legalEntityName3, legalEntityName4),
        identifiers = listOf(identifier2),
        legalForm = legalForm2,
        status = status2,
        profile = profile2,
        addresses = listOf(address2),
        types = listOf(legalEntityType)
    )

    val legalEntity3 = BusinessPartnerCdq(
        id = partnerId3,
        createdAt = createdTime1,
        lastModifiedAt = createdTime1,
        externalId = partnerId3,
        dataSource = datasource1,
        names = listOf(legalEntityName5),
        identifiers = listOf(identifier3),
        legalForm = legalForm3,
        status = status3,
        profile = profile3,
        addresses = listOf(address3),
        types = listOf(legalEntityType)
    )

    val site1 = BusinessPartnerCdq(
        id = partnerId4,
        createdAt = createdTime1,
        lastModifiedAt = createdTime1,
        externalId = partnerId4,
        dataSource = datasource1,
        names = listOf(siteName1),
        addresses = listOf(address1),
        types = listOf(siteType),
        relations = listOf(
            RelationCdq(
                type = parentRelationType,
                startNode = CommonValues.bpnL1,
                endNode = CommonValues.bpnS1,
                startNodeDataSource = datasource1,
                endNodeDataSource = datasource1
            )
        )
    )

    val site2 = BusinessPartnerCdq(
        id = partnerId5,
        createdAt = createdTime1,
        lastModifiedAt = createdTime1,
        externalId = partnerId5,
        dataSource = datasource1,
        names = listOf(siteName2),
        addresses = listOf(address2),
        types = listOf(siteType),
        relations = listOf(
            RelationCdq(
                type = parentRelationType,
                startNode = CommonValues.bpnL2,
                endNode = CommonValues.bpnS2,
                startNodeDataSource = datasource1,
                endNodeDataSource = datasource1
            )
        )
    )

    val site3 = BusinessPartnerCdq(
        id = partnerId6,
        createdAt = createdTime1,
        lastModifiedAt = createdTime1,
        externalId = partnerId6,
        dataSource = datasource1,
        names = listOf(siteName3),
        addresses = listOf(address3),
        types = listOf(siteType),
        relations = listOf(
            RelationCdq(
                type = parentRelationType,
                startNode = CommonValues.bpnL3,
                endNode = CommonValues.bpnS3,
                startNodeDataSource = datasource1,
                endNodeDataSource = datasource1
            )
        )
    )

    val addressPartner1 = BusinessPartnerCdq(
        id = partnerId7,
        createdAt = createdTime1,
        lastModifiedAt = createdTime1,
        externalId = partnerId7,
        dataSource = datasource1,
        addresses = listOf(address1),
        types = listOf(addressType),
        relations = listOf(
            RelationCdq(
                type = parentRelationType,
                startNode = CommonValues.bpnL1,
                endNode = CommonValues.bpnA1,
                startNodeDataSource = datasource1,
                endNodeDataSource = datasource1
            )
        )
    )

    val addressPartner2 = BusinessPartnerCdq(
        id = partnerId8,
        createdAt = createdTime1,
        lastModifiedAt = createdTime1,
        externalId = partnerId8,
        dataSource = datasource1,
        addresses = listOf(address2),
        types = listOf(addressType),
        relations = listOf(
            RelationCdq(
                type = parentRelationType,
                startNode = CommonValues.bpnL2,
                endNode = CommonValues.bpnA2,
                startNodeDataSource = datasource1,
                endNodeDataSource = datasource1
            )
        )
    )

    val addressPartner3 = BusinessPartnerCdq(
        id = partnerId9,
        createdAt = createdTime1,
        lastModifiedAt = createdTime1,
        externalId = partnerId9,
        dataSource = datasource1,
        addresses = listOf(address3),
        types = listOf(addressType),
        relations = listOf(
            RelationCdq(
                type = parentRelationType,
                startNode = CommonValues.bpnL3,
                endNode = CommonValues.bpnA3,
                startNodeDataSource = datasource1,
                endNodeDataSource = datasource1
            )
        )
    )
}