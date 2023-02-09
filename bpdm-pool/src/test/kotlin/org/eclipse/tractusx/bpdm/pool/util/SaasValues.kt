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

    val country1 = CountrySaas(CommonValues.country1, CommonValues.country1.getName())
    val country2 = CountrySaas(CommonValues.country2, CommonValues.country2.getName())
    val country3 = CountrySaas(CommonValues.country3, CommonValues.country3.getName())

    val legalEntityName1 = NameSaas(CommonValues.name1)
    val legalEntityName2 = NameSaas(CommonValues.name2)
    val legalEntityName3 = NameSaas(CommonValues.name3)
    val legalEntityName4 = NameSaas(CommonValues.name4)
    val legalEntityName5 = NameSaas(CommonValues.name5)

    val siteName1 = NameSaas(CommonValues.siteName1)
    val siteName2 = NameSaas(CommonValues.siteName2)
    val siteName3 = NameSaas(CommonValues.siteName3)

    val identifierType1 = TypeKeyNameUrlSaas(CommonValues.identifierTypeTechnicalKey1, CommonValues.identifierTypeName1, CommonValues.identifierTypeUrl1)

    val issuingBody1 = TypeKeyNameUrlSaas(CommonValues.issuingBodyKey1, CommonValues.issuingBodyName1, CommonValues.issuingBodyUrl1)

    val identifierStatus1 = TypeKeyNameSaas(CommonValues.identifierStatusKey1, CommonValues.identifierStatusName1)

    val identifier1 = IdentifierSaas(identifierType1, CommonValues.identifierValue1, issuingBody1, identifierStatus1)
    val identifier2 = IdentifierSaas(identifierType1, CommonValues.identifierValue2, issuingBody1, identifierStatus1)
    val identifier3 = IdentifierSaas(identifierType1, CommonValues.identifierValue3, issuingBody1, identifierStatus1)

    val legalForm1 = LegalFormSaas(
        CommonValues.legalFormName1,
        CommonValues.legalFormUrl1,
        CommonValues.legalFormTechnicalKey1,
        CommonValues.legalFormAbbreviation1,
        language1
    )
    val legalForm2 = LegalFormSaas(
        CommonValues.legalFormName2,
        CommonValues.legalFormUrl2,
        CommonValues.legalFormTechnicalKey2,
        CommonValues.legalFormAbbreviation2,
        language2
    )
    val legalForm3 = LegalFormSaas(
        CommonValues.legalFormName3,
        CommonValues.legalFormUrl3,
        CommonValues.legalFormTechnicalKey3,
        CommonValues.legalFormAbbreviation3,
        language3
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

    val profile1 = PartnerProfileSaas(classifications = listOf(classification1, classification2))
    val profile2 = PartnerProfileSaas(classifications = listOf(classification3, classification4))
    val profile3 = PartnerProfileSaas(classifications = listOf(classification5))

    val addressId1 = "address-saas-1"
    val addressId2 = "address-saas-2"
    val addressId3 = "address-saas-3"

    val adminArea1 = AdministrativeAreaSaas(CommonValues.adminArea1)
    val adminArea2 = AdministrativeAreaSaas(CommonValues.adminArea2)
    val adminArea3 = AdministrativeAreaSaas(CommonValues.adminArea3)
    val adminArea4 = AdministrativeAreaSaas(CommonValues.adminArea4)
    val adminArea5 = AdministrativeAreaSaas(CommonValues.adminArea5)

    val postCode1 = PostCodeSaas(CommonValues.postCode1)
    val postCode2 = PostCodeSaas(CommonValues.postCode2)
    val postCode3 = PostCodeSaas(CommonValues.postCode3)
    val postCode4 = PostCodeSaas(CommonValues.postCode4)
    val postCode5 = PostCodeSaas(CommonValues.postCode5)

    val locality1 = LocalitySaas(value = CommonValues.locality1)
    val locality2 = LocalitySaas(value = CommonValues.locality2)
    val locality3 = LocalitySaas(value = CommonValues.locality3)
    val locality4 = LocalitySaas(value = CommonValues.locality4)
    val locality5 = LocalitySaas(value = CommonValues.locality5)

    val thoroughfare1 = ThoroughfareSaas(value = CommonValues.thoroughfare1)
    val thoroughfare2 = ThoroughfareSaas(value = CommonValues.thoroughfare2)
    val thoroughfare3 = ThoroughfareSaas(value = CommonValues.thoroughfare3)
    val thoroughfare4 = ThoroughfareSaas(value = CommonValues.thoroughfare4)
    val thoroughfare5 = ThoroughfareSaas(value = CommonValues.thoroughfare5)

    val premise1 = PremiseSaas(value = CommonValues.premise1)
    val premise2 = PremiseSaas(value = CommonValues.premise2)
    val premise3 = PremiseSaas(value = CommonValues.premise3)
    val premise4 = PremiseSaas(value = CommonValues.premise4)
    val premise5 = PremiseSaas(value = CommonValues.premise5)

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
        country = country1,
        administrativeAreas = listOf(adminArea1, adminArea2),
        postCodes = listOf(postCode1, postCode2),
        localities = listOf(locality1, locality2),
        thoroughfares = listOf(thoroughfare1, thoroughfare2),
        premises = listOf(premise1, premise2),
        postalDeliveryPoints = listOf(postalDeliveryPoint1, postalDeliveryPoint2)
    )

    val address2 = AddressSaas(
        id = addressId2,
        externalId = addressId2,
        saasId = addressId2,
        country = country2,
        administrativeAreas = listOf(adminArea3, adminArea4),
        postCodes = listOf(postCode3, postCode4),
        localities = listOf(locality3, locality4),
        thoroughfares = listOf(thoroughfare3, thoroughfare4),
        premises = listOf(premise3, premise4),
        postalDeliveryPoints = listOf(postalDeliveryPoint3, postalDeliveryPoint4)
    )

    val address3 = AddressSaas(
        id = addressId3,
        externalId = addressId3,
        saasId = addressId3,
        country = country3,
        administrativeAreas = listOf(adminArea5),
        postCodes = listOf(postCode5),
        localities = listOf(locality5),
        thoroughfares = listOf(thoroughfare5),
        premises = listOf(premise5),
        postalDeliveryPoints = listOf(postalDeliveryPoint5)
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
        profile = profile1,
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
        profile = profile2,
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
        profile = profile3,
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
}