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

package org.eclipse.tractusx.bpdm.gate.util

import org.eclipse.tractusx.bpdm.common.dto.cdq.*
import org.eclipse.tractusx.bpdm.common.model.AddressType
import org.eclipse.tractusx.bpdm.common.service.CdqMappings

/**
 * Test values for CDQ DTOs
 * Numbered values should match with RequestValues numbered values for easier testing
 */
object CdqValues {
    val idTypeBpn = TypeKeyNameUrlCdq(CdqMappings.BPN_TECHNICAL_KEY, "Business Partner Number")
    val issuerBpn = TypeKeyNameUrlCdq("CATENAX", "Catena-X")

    val language1 = LanguageCdq(technicalKey = CommonValues.language1)
    val language2 = LanguageCdq(technicalKey = CommonValues.language2)

    val characterSet1 = TypeKeyNameCdq(CommonValues.characterSet1.name)
    val characterSet2 = TypeKeyNameCdq(CommonValues.characterSet2.name)

    val country1 = CountryCdq(shortName = CommonValues.country1)
    val country2 = CountryCdq(shortName = CommonValues.country2)

    val identifier1 = IdentifierCdq(
        type = TypeKeyNameUrlCdq(technicalKey = CommonValues.identifierTypeTechnicalKey1),
        value = CommonValues.identifierValue1,
        issuingBody = TypeKeyNameUrlCdq(technicalKey = CommonValues.identifierIssuingBodyTechnicalKey1),
        status = TypeKeyNameCdq(technicalKey = CommonValues.identifierStatusTechnicalKey1)
    )

    val identifier1Response = identifier1.copy(
        type = identifier1.type!!.copy(
            name = CommonValues.identifierTypeName1,
            url = CommonValues.identifierTypeUrl1
        ),
        issuingBody = identifier1.issuingBody!!.copy(
            name = CommonValues.identifierIssuingBodyName1,
            url = CommonValues.identifierIssuingBodyUrl1
        ),
        status = identifier1.status!!.copy(
            name = CommonValues.identifierStatusName1
        )
    )

    val identifier2 = IdentifierCdq(
        type = TypeKeyNameUrlCdq(technicalKey = CommonValues.identifierTypeTechnicalKey2),
        value = CommonValues.identifierValue2,
        issuingBody = TypeKeyNameUrlCdq(technicalKey = CommonValues.identifierIssuingBodyTechnicalKey2),
        status = TypeKeyNameCdq(technicalKey = CommonValues.identifierStatusTechnicalKey2)
    )

    val identifier2Response = identifier2.copy(
        type = identifier2.type!!.copy(
            name = CommonValues.identifierTypeName2,
            url = CommonValues.identifierTypeUrl2
        ),
        issuingBody = identifier2.issuingBody!!.copy(
            name = CommonValues.identifierIssuingBodyName2,
            url = CommonValues.identifierIssuingBodyUrl2
        ),
        status = identifier2.status!!.copy(
            name = CommonValues.identifierStatusName2
        )
    )

    val identifier3 = IdentifierCdq(
        type = TypeKeyNameUrlCdq(technicalKey = CommonValues.identifierTypeTechnicalKey3),
        value = CommonValues.identifierValue3,
        issuingBody = TypeKeyNameUrlCdq(technicalKey = CommonValues.identifierIssuingBodyTechnicalKey3),
        status = TypeKeyNameCdq(technicalKey = CommonValues.identifierStatusTechnicalKey3)
    )

    val identifier3Response = identifier3.copy(
        type = identifier3.type!!.copy(
            name = CommonValues.identifierTypeName3,
            url = CommonValues.identifierTypeUrl3
        ),
        issuingBody = identifier3.issuingBody!!.copy(
            name = CommonValues.identifierIssuingBodyName3,
            url = CommonValues.identifierIssuingBodyUrl3
        ),
        status = identifier3.status!!.copy(
            name = CommonValues.identifierStatusName3
        )
    )

    val identifier4 = IdentifierCdq(
        type = TypeKeyNameUrlCdq(technicalKey = CommonValues.identifierTypeTechnicalKey4),
        value = CommonValues.identifierValue4,
        issuingBody = TypeKeyNameUrlCdq(technicalKey = CommonValues.identifierIssuingBodyTechnicalKey4),
        status = TypeKeyNameCdq(technicalKey = CommonValues.identifierStatusTechnicalKey4)
    )

    val identifier4Response = identifier4.copy(
        type = identifier4.type!!.copy(
            name = CommonValues.identifierTypeName4,
            url = CommonValues.identifierTypeUrl4
        ),
        issuingBody = identifier4.issuingBody!!.copy(
            name = CommonValues.identifierIssuingBodyName4,
            url = CommonValues.identifierIssuingBodyUrl4
        ),
        status = identifier4.status!!.copy(
            name = CommonValues.identifierStatusName4
        )
    )

    val identifierBpn1 = IdentifierCdq(
        type = idTypeBpn,
        value = CommonValues.bpn1,
        issuingBody = issuerBpn
    )

    val identifierBpn2 = IdentifierCdq(
        type = idTypeBpn,
        value = CommonValues.bpn2,
        issuingBody = issuerBpn
    )

    val identifierBpnSite1 = IdentifierCdq(
        type = idTypeBpn,
        value = CommonValues.bpnSite1,
        issuingBody = issuerBpn
    )

    val identifierBpnSite2 = IdentifierCdq(
        type = idTypeBpn,
        value = CommonValues.bpnSite2,
        issuingBody = issuerBpn
    )

    val identifierBpnAddress1 = IdentifierCdq(
        type = idTypeBpn,
        value = CommonValues.bpnAddress1,
        issuingBody = issuerBpn
    )

    val identifierBpnAddress2 = IdentifierCdq(
        type = idTypeBpn,
        value = CommonValues.bpnAddress2,
        issuingBody = issuerBpn
    )

    val name1 = NameCdq(
        value = CommonValues.name1,
        shortName = CommonValues.shortName1,
        type = TypeKeyNameUrlCdq(technicalKey = CommonValues.nameType1.name),
        language = language1
    )

    val name2 = NameCdq(
        value = CommonValues.name2,
        shortName = CommonValues.shortName2,
        type = TypeKeyNameUrlCdq(technicalKey = CommonValues.nameType1.name),
        language = language1
    )

    val name3 = NameCdq(
        value = CommonValues.name3,
        shortName = CommonValues.shortName3,
        type = TypeKeyNameUrlCdq(technicalKey = CommonValues.nameType1.name),
        language = language1
    )

    val name4 = NameCdq(
        value = CommonValues.name4,
        shortName = CommonValues.shortName4,
        type = TypeKeyNameUrlCdq(technicalKey = CommonValues.nameType1.name),
        language = language1
    )

    val nameSite1 = NameCdq(value = CommonValues.nameSite1)
    val nameSite2 = NameCdq(value = CommonValues.nameSite2)

    val legalFormCategory1 = TypeNameUrlCdq(
        name = CommonValues.legalFormCategoryName1,
        url = CommonValues.legalFormCategoryUrl1
    )

    val legalFormCategory2 = TypeNameUrlCdq(
        name = CommonValues.legalFormCategoryName2,
        url = CommonValues.legalFormCategoryUrl2
    )

    val legalForm1 = LegalFormCdq(technicalKey = CommonValues.legalFormTechnicalKey1)

    val legalForm1Response = legalForm1.copy(
        name = CommonValues.legalFormName1,
        url = CommonValues.legalFormUrl1,
        mainAbbreviation = CommonValues.legalFormAbbreviation1,
        language = language1,
        categories = listOf(legalFormCategory1)
    )

    val legalForm2 = LegalFormCdq(technicalKey = CommonValues.legalFormTechnicalKey2)

    val legalForm2Response = legalForm2.copy(
        name = CommonValues.legalFormName2,
        url = CommonValues.legalFormUrl2,
        mainAbbreviation = CommonValues.legalFormAbbreviation2,
        language = language2,
        categories = listOf(legalFormCategory2)
    )

    val businessStatus1 = BusinessPartnerStatusCdq(
        type = TypeKeyNameUrlCdq(technicalKey = CommonValues.businessStatusType1.name),
        officialDenotation = CommonValues.businessStatusOfficialDenotation1,
        validFrom = CommonValues.businessStatusValidFrom1,
        validUntil = CommonValues.businessStatusValidUntil1
    )

    val businessStatus2 = BusinessPartnerStatusCdq(
        type = TypeKeyNameUrlCdq(technicalKey = CommonValues.businessStatusType2.name),
        officialDenotation = CommonValues.businessStatusOfficialDenotation2,
        validFrom = CommonValues.businessStatusValidFrom2,
        validUntil = CommonValues.businessStatusValidUntil2
    )

    val classification1 = ClassificationCdq(
        value = CommonValues.classificationValue1,
        code = CommonValues.classificationCode1,
        type = TypeKeyNameUrlCdq(technicalKey = CommonValues.classificationType.name)
    )

    val classification2 = ClassificationCdq(
        value = CommonValues.classificationValue2,
        code = CommonValues.classificationCode2,
        type = TypeKeyNameUrlCdq(technicalKey = CommonValues.classificationType.name)
    )

    val classification3 = ClassificationCdq(
        value = CommonValues.classificationValue3,
        code = CommonValues.classificationCode3,
        type = TypeKeyNameUrlCdq(technicalKey = CommonValues.classificationType.name)
    )

    val classification4 = ClassificationCdq(
        value = CommonValues.classificationValue4,
        code = CommonValues.classificationCode4,
        type = TypeKeyNameUrlCdq(technicalKey = CommonValues.classificationType.name)
    )

    val profile1 = PartnerProfileCdq(
        classifications = listOf(classification1, classification2)
    )

    val profile2 = PartnerProfileCdq(
        classifications = listOf(classification3, classification4)
    )

    val bankAccount1 = BankAccountCdq(
        internationalBankAccountIdentifier = CommonValues.internationalBankAccountIdentifier1,
        internationalBankIdentifier = CommonValues.internationalBankIdentifier0,
        nationalBankAccountIdentifier = CommonValues.nationalBankAccountIdentifier1,
        nationalBankIdentifier = CommonValues.nationalBankIdentifier0
    )

    val bankAccount2 = BankAccountCdq(
        internationalBankAccountIdentifier = CommonValues.internationalBankAccountIdentifier2,
        internationalBankIdentifier = CommonValues.internationalBankIdentifier0,
        nationalBankAccountIdentifier = CommonValues.nationalBankAccountIdentifier2,
        nationalBankIdentifier = CommonValues.nationalBankIdentifier0
    )

    val bankAccount3 = BankAccountCdq(
        internationalBankAccountIdentifier = CommonValues.internationalBankAccountIdentifier3,
        internationalBankIdentifier = CommonValues.internationalBankIdentifier0,
        nationalBankAccountIdentifier = CommonValues.nationalBankAccountIdentifier3,
        nationalBankIdentifier = CommonValues.nationalBankIdentifier0
    )

    val bankAccount4 = BankAccountCdq(
        internationalBankAccountIdentifier = CommonValues.internationalBankAccountIdentifier4,
        internationalBankIdentifier = CommonValues.internationalBankIdentifier0,
        nationalBankAccountIdentifier = CommonValues.nationalBankAccountIdentifier4,
        nationalBankIdentifier = CommonValues.nationalBankIdentifier0
    )

    val version1 = AddressVersionCdq(language1, characterSet1)
    val version2 = AddressVersionCdq(language2, characterSet2)

    val careOf1 = WrappedValueCdq(CommonValues.careOf1)
    val careOf2 = WrappedValueCdq(CommonValues.careOf2)

    val context1 = WrappedValueCdq(CommonValues.context1)
    val context2 = WrappedValueCdq(CommonValues.context2)

    val addressType1 = TypeKeyNameUrlCdq(technicalKey = AddressType.LEGAL.name)

    val adminAreaType1 = TypeKeyNameUrlCdq(CommonValues.adminAreaType1.name)
    val adminAreaType2 = TypeKeyNameUrlCdq(CommonValues.adminAreaType2.name)

    val adminArea1 = AdministrativeAreaCdq(CommonValues.adminArea1, type = adminAreaType1, language = language1)
    val adminArea2 = AdministrativeAreaCdq(CommonValues.adminArea2, type = adminAreaType2, language = language2)

    val postCodeType1 = TypeKeyNameUrlCdq(CommonValues.postCodeType1.name)
    val postCodeType2 = TypeKeyNameUrlCdq(CommonValues.postCodeType2.name)

    val postCode1 = PostCodeCdq(CommonValues.postCode1, postCodeType1)
    val postCode2 = PostCodeCdq(CommonValues.postCode2, postCodeType2)

    val localityType1 = TypeKeyNameUrlCdq(CommonValues.localityType1.name)
    val localityType2 = TypeKeyNameUrlCdq(CommonValues.localityType2.name)

    val locality1 = LocalityCdq(value = CommonValues.locality1, type = localityType1, language = language1)
    val locality2 = LocalityCdq(value = CommonValues.locality2, type = localityType2, language = language2)

    val thoroughfareType1 = TypeKeyNameUrlCdq(CommonValues.thoroughfareType1.name)
    val thoroughfareType2 = TypeKeyNameUrlCdq(CommonValues.thoroughfareType2.name)

    val thoroughfare1 = ThoroughfareCdq(value = CommonValues.thoroughfare1, type = thoroughfareType1, language = language1)
    val thoroughfare2 = ThoroughfareCdq(value = CommonValues.thoroughfare2, type = thoroughfareType2, language = language2)

    val premiseType1 = TypeKeyNameUrlCdq(CommonValues.premiseType1.name)
    val premiseType2 = TypeKeyNameUrlCdq(CommonValues.premiseType2.name)

    val premise1 = PremiseCdq(value = CommonValues.premise1, type = premiseType1, language = language1)
    val premise2 = PremiseCdq(value = CommonValues.premise2, type = premiseType2, language = language2)

    val postalDeliveryPointType1 = TypeKeyNameUrlCdq(CommonValues.postalDeliveryPointType1.name)
    val postalDeliveryPointType2 = TypeKeyNameUrlCdq(CommonValues.postalDeliveryPointType2.name)

    val postalDeliveryPoint1 = PostalDeliveryPointCdq(value = CommonValues.postalDeliveryPoint1, type = postalDeliveryPointType1, language = language1)
    val postalDeliveryPoint2 = PostalDeliveryPointCdq(value = CommonValues.postalDeliveryPoint2, type = postalDeliveryPointType2, language = language2)

    val geoCoordinate1 = GeoCoordinatesCdq(CommonValues.geoCoordinates1.first, CommonValues.geoCoordinates1.second)
    val geoCoordinate2 = GeoCoordinatesCdq(CommonValues.geoCoordinates2.first, CommonValues.geoCoordinates2.second)

    val address1 = AddressCdq(
        version = version1,
        country = country1,
        careOf = careOf1,
        contexts = listOf(context1),
        administrativeAreas = listOf(adminArea1),
        postCodes = listOf(postCode1),
        localities = listOf(locality1),
        thoroughfares = listOf(thoroughfare1),
        premises = listOf(premise1),
        postalDeliveryPoints = listOf(postalDeliveryPoint1),
        types = listOf(addressType1),
        geographicCoordinates = geoCoordinate1
    )

    val address2 = AddressCdq(
        country = country2,
        version = version2,
        careOf = careOf2,
        contexts = listOf(context2),
        administrativeAreas = listOf(adminArea2),
        postCodes = listOf(postCode2),
        localities = listOf(locality2),
        thoroughfares = listOf(thoroughfare2),
        premises = listOf(premise2),
        postalDeliveryPoints = listOf(postalDeliveryPoint2),
        types = listOf(addressType1),
        geographicCoordinates = geoCoordinate2
    )

    val legalEntity1 = BusinessPartnerCdq(
        externalId = CommonValues.externalId1,
        identifiers = listOf(identifier1, identifier2, identifierBpn1),
        names = listOf(name1, name2),
        legalForm = legalForm1,
        status = businessStatus1,
        profile = profile1,
        types = listOf(TypeKeyNameUrlCdq(technicalKey = BusinessPartnerTypeCdq.LEGAL_ENTITY.name)),
        bankAccounts = listOf(bankAccount1, bankAccount2),
        addresses = listOf(address1),
        dataSource = "test-cdq-datasource-legal-entity"
    )

    val legalEntity2 = BusinessPartnerCdq(
        externalId = CommonValues.externalId2,
        identifiers = listOf(identifier3, identifier4, identifierBpn2),
        names = listOf(name3, name4),
        legalForm = legalForm2,
        status = businessStatus2,
        profile = profile2,
        types = listOf(TypeKeyNameUrlCdq(technicalKey = BusinessPartnerTypeCdq.LEGAL_ENTITY.name)),
        bankAccounts = listOf(bankAccount3, bankAccount4),
        addresses = listOf(address2),
        dataSource = "test-cdq-datasource-legal-entity"
    )

    val siteBusinessPartner1 = BusinessPartnerCdq(
        externalId = CommonValues.externalIdSite1,
        identifiers = listOf(identifierBpnSite1, identifier1, identifier2), // identifiers copied from legal entity
        names = listOf(nameSite1),
        addresses = listOf(address1),
        dataSource = "test-cdq-datasource-site",
        types = listOf(TypeKeyNameUrlCdq(BusinessPartnerTypeCdq.ORGANIZATIONAL_UNIT.name))
    )
    val siteBusinessPartner2 = BusinessPartnerCdq(
        externalId = CommonValues.externalIdSite2,
        identifiers = listOf(identifierBpnSite2, identifier3, identifier4), // identifiers copied from legal entity
        names = listOf(nameSite2),
        addresses = listOf(address2),
        dataSource = "test-cdq-datasource-site",
        types = listOf(TypeKeyNameUrlCdq(BusinessPartnerTypeCdq.ORGANIZATIONAL_UNIT.name))
    )

    val relationType = TypeKeyNameCdq(technicalKey = "PARENT")

    val relationSite1ToLegalEntity = RelationCdq(
        startNode = legalEntity1.externalId!!,
        startNodeDataSource = "test-cdq-datasource-legal-entity",
        endNode = siteBusinessPartner1.externalId!!,
        endNodeDataSource = "test-cdq-datasource-site",
        type = relationType
    )

    val relationSite2ToLegalEntity = RelationCdq(
        startNode = legalEntity2.externalId!!,
        startNodeDataSource = "test-cdq-datasource-legal-entity",
        endNode = siteBusinessPartner2.externalId!!,
        endNodeDataSource = "test-cdq-datasource-site",
        type = relationType
    )

    val siteBusinessPartnerWithRelations1 = siteBusinessPartner1.copy(
        relations = listOf(relationSite1ToLegalEntity)
    )

    val siteBusinessPartnerWithRelations2 = siteBusinessPartner2.copy(
        relations = listOf(relationSite2ToLegalEntity)
    )

    val addressBusinessPartner1 = BusinessPartnerCdq(
        externalId = CommonValues.externalIdAddress1,
        identifiers = listOf(identifierBpnAddress1, identifier1, identifier2), // identifiers copied from legal entity
        addresses = listOf(address1),
        dataSource = "test-cdq-datasource-address",
        types = listOf(TypeKeyNameUrlCdq(BusinessPartnerTypeCdq.BP_ADDRESS.name))
    )

    val addressBusinessPartner2 = BusinessPartnerCdq(
        externalId = CommonValues.externalIdAddress2,
        identifiers = listOf(identifierBpnAddress2, identifier1, identifier2), // identifiers copied from site
        addresses = listOf(address2),
        dataSource = "test-cdq-datasource-address",
        types = listOf(TypeKeyNameUrlCdq(BusinessPartnerTypeCdq.BP_ADDRESS.name))
    )

    val relationAddress1ToLegalEntity = RelationCdq(
        startNode = legalEntity1.externalId!!,
        startNodeDataSource = "test-cdq-datasource-legal-entity",
        endNode = addressBusinessPartner1.externalId!!,
        endNodeDataSource = "test-cdq-datasource-address",
        type = relationType
    )

    val relationAddress2ToSite = RelationCdq(
        startNode = siteBusinessPartner1.externalId!!,
        startNodeDataSource = "test-cdq-datasource-site",
        endNode = addressBusinessPartner2.externalId!!,
        endNodeDataSource = "test-cdq-datasource-address",
        type = relationType
    )

    val addressBusinessPartnerWithRelations1 = addressBusinessPartner1.copy(
        relations = listOf(relationAddress1ToLegalEntity)
    )

    val addressBusinessPartnerWithRelations2 = addressBusinessPartner2.copy(
        relations = listOf(relationAddress2ToSite)
    )

    val legalEntity1Response = legalEntity1.copy(
        identifiers = listOf(identifier1Response, identifier2Response, identifierBpn1),
        legalForm = legalForm1Response
    )

    val legalEntity2Response = legalEntity2.copy(
        identifiers = listOf(identifier3Response, identifier4Response, identifierBpn2),
        legalForm = legalForm2Response
    )
}