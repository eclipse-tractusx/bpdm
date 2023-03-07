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

import org.eclipse.tractusx.bpdm.common.dto.saas.*
import org.eclipse.tractusx.bpdm.common.model.AddressType
import org.eclipse.tractusx.bpdm.common.service.SaasMappings
import java.time.LocalDateTime

/**
 * Test values for SaaS DTOs
 * Numbered values should match with RequestValues numbered values for easier testing
 */
object SaasValues {
    val testDatasource = "test-saas-datasource"

    val idTypeBpn = TypeKeyNameUrlSaas(SaasMappings.BPN_TECHNICAL_KEY, "Business Partner Number")
    val issuerBpn = TypeKeyNameUrlSaas("CATENAX", "Catena-X")

    val language1 = LanguageSaas(technicalKey = CommonValues.language1)
    val language2 = LanguageSaas(technicalKey = CommonValues.language2)

    val characterSet1 = TypeKeyNameSaas(CommonValues.characterSet1.name)
    val characterSet2 = TypeKeyNameSaas(CommonValues.characterSet2.name)

    val country1 = CountrySaas(shortName = CommonValues.country1)
    val country2 = CountrySaas(shortName = CommonValues.country2)

    val modificationTime1 = LocalDateTime.of(2020, 1, 1, 2, 1)
    val modificationTime2 = LocalDateTime.of(2020, 1, 1, 3, 1)

    val identifier1 = IdentifierSaas(
        type = TypeKeyNameUrlSaas(technicalKey = CommonValues.identifierTypeTechnicalKey1),
        value = CommonValues.identifierValue1,
        issuingBody = TypeKeyNameUrlSaas(technicalKey = CommonValues.identifierIssuingBodyTechnicalKey1),
        status = TypeKeyNameSaas(technicalKey = CommonValues.identifierStatusTechnicalKey1)
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

    val identifier2 = IdentifierSaas(
        type = TypeKeyNameUrlSaas(technicalKey = CommonValues.identifierTypeTechnicalKey2),
        value = CommonValues.identifierValue2,
        issuingBody = TypeKeyNameUrlSaas(technicalKey = CommonValues.identifierIssuingBodyTechnicalKey2),
        status = TypeKeyNameSaas(technicalKey = CommonValues.identifierStatusTechnicalKey2)
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

    val identifier3 = IdentifierSaas(
        type = TypeKeyNameUrlSaas(technicalKey = CommonValues.identifierTypeTechnicalKey3),
        value = CommonValues.identifierValue3,
        issuingBody = TypeKeyNameUrlSaas(technicalKey = CommonValues.identifierIssuingBodyTechnicalKey3),
        status = TypeKeyNameSaas(technicalKey = CommonValues.identifierStatusTechnicalKey3)
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

    val identifier4 = IdentifierSaas(
        type = TypeKeyNameUrlSaas(technicalKey = CommonValues.identifierTypeTechnicalKey4),
        value = CommonValues.identifierValue4,
        issuingBody = TypeKeyNameUrlSaas(technicalKey = CommonValues.identifierIssuingBodyTechnicalKey4),
        status = TypeKeyNameSaas(technicalKey = CommonValues.identifierStatusTechnicalKey4)
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

    val identifierBpn1 = IdentifierSaas(
        type = idTypeBpn,
        value = CommonValues.bpn1,
        issuingBody = issuerBpn
    )

    val identifierBpn2 = IdentifierSaas(
        type = idTypeBpn,
        value = CommonValues.bpn2,
        issuingBody = issuerBpn
    )

    val identifierBpn3 = IdentifierSaas(
        type = idTypeBpn,
        value = CommonValues.bpn3,
        issuingBody = issuerBpn
    )

    val identifierBpnSite1 = IdentifierSaas(
        type = idTypeBpn,
        value = CommonValues.bpnSite1,
        issuingBody = issuerBpn
    )

    val identifierBpnSite2 = IdentifierSaas(
        type = idTypeBpn,
        value = CommonValues.bpnSite2,
        issuingBody = issuerBpn
    )

    val identifierBpnSite3 = IdentifierSaas(
        type = idTypeBpn,
        value = CommonValues.bpnSite3,
        issuingBody = issuerBpn
    )

    val identifierBpnAddress1 = IdentifierSaas(
        type = idTypeBpn,
        value = CommonValues.bpnAddress1,
        issuingBody = issuerBpn
    )

    val identifierBpnAddress2 = IdentifierSaas(
        type = idTypeBpn,
        value = CommonValues.bpnAddress2,
        issuingBody = issuerBpn
    )

    val identifierBpnAddress3 = IdentifierSaas(
        type = idTypeBpn,
        value = CommonValues.bpnAddress3,
        issuingBody = issuerBpn
    )

    val name1 = NameSaas(
        value = CommonValues.name1,
        shortName = CommonValues.shortName1,
        type = TypeKeyNameUrlSaas(technicalKey = CommonValues.nameType1.name),
        language = language1
    )

    val name2 = NameSaas(
        value = CommonValues.name2,
        shortName = CommonValues.shortName2,
        type = TypeKeyNameUrlSaas(technicalKey = CommonValues.nameType1.name),
        language = language1
    )

    val name3 = NameSaas(
        value = CommonValues.name3,
        shortName = CommonValues.shortName3,
        type = TypeKeyNameUrlSaas(technicalKey = CommonValues.nameType1.name),
        language = language1
    )

    val name4 = NameSaas(
        value = CommonValues.name4,
        shortName = CommonValues.shortName4,
        type = TypeKeyNameUrlSaas(technicalKey = CommonValues.nameType1.name),
        language = language1
    )

    val nameSite1 = NameSaas(value = CommonValues.nameSite1)
    val nameSite2 = NameSaas(value = CommonValues.nameSite2)

    val legalFormCategory1 = TypeNameUrlSaas(
        name = CommonValues.legalFormCategoryName1,
        url = CommonValues.legalFormCategoryUrl1
    )

    val legalFormCategory2 = TypeNameUrlSaas(
        name = CommonValues.legalFormCategoryName2,
        url = CommonValues.legalFormCategoryUrl2
    )

    val legalForm1 = LegalFormSaas(technicalKey = CommonValues.legalFormTechnicalKey1)

    val legalForm1Response = legalForm1.copy(
        name = CommonValues.legalFormName1,
        url = CommonValues.legalFormUrl1,
        mainAbbreviation = CommonValues.legalFormAbbreviation1,
        language = language1,
        categories = listOf(legalFormCategory1)
    )

    val legalForm2 = LegalFormSaas(technicalKey = CommonValues.legalFormTechnicalKey2)

    val legalForm2Response = legalForm2.copy(
        name = CommonValues.legalFormName2,
        url = CommonValues.legalFormUrl2,
        mainAbbreviation = CommonValues.legalFormAbbreviation2,
        language = language2,
        categories = listOf(legalFormCategory2)
    )

    val businessStatus1 = BusinessPartnerStatusSaas(
        type = TypeKeyNameUrlSaas(technicalKey = CommonValues.businessStatusType1.name),
        officialDenotation = CommonValues.businessStatusOfficialDenotation1,
        validFrom = CommonValues.businessStatusValidFrom1,
        validUntil = CommonValues.businessStatusValidUntil1
    )

    val businessStatus2 = BusinessPartnerStatusSaas(
        type = TypeKeyNameUrlSaas(technicalKey = CommonValues.businessStatusType2.name),
        officialDenotation = CommonValues.businessStatusOfficialDenotation2,
        validFrom = CommonValues.businessStatusValidFrom2,
        validUntil = CommonValues.businessStatusValidUntil2
    )

    val classification1 = ClassificationSaas(
        value = CommonValues.classificationValue1,
        code = CommonValues.classificationCode1,
        type = TypeKeyNameUrlSaas(technicalKey = CommonValues.classificationType.name)
    )

    val classification2 = ClassificationSaas(
        value = CommonValues.classificationValue2,
        code = CommonValues.classificationCode2,
        type = TypeKeyNameUrlSaas(technicalKey = CommonValues.classificationType.name)
    )

    val classification3 = ClassificationSaas(
        value = CommonValues.classificationValue3,
        code = CommonValues.classificationCode3,
        type = TypeKeyNameUrlSaas(technicalKey = CommonValues.classificationType.name)
    )

    val classification4 = ClassificationSaas(
        value = CommonValues.classificationValue4,
        code = CommonValues.classificationCode4,
        type = TypeKeyNameUrlSaas(technicalKey = CommonValues.classificationType.name)
    )

    val profile1 = PartnerProfileSaas(
        classifications = listOf(classification1, classification2)
    )

    val profile2 = PartnerProfileSaas(
        classifications = listOf(classification3, classification4)
    )

    val bankAccount1 = BankAccountSaas(
        internationalBankAccountIdentifier = CommonValues.internationalBankAccountIdentifier1,
        internationalBankIdentifier = CommonValues.internationalBankIdentifier0,
        nationalBankAccountIdentifier = CommonValues.nationalBankAccountIdentifier1,
        nationalBankIdentifier = CommonValues.nationalBankIdentifier0
    )

    val bankAccount2 = BankAccountSaas(
        internationalBankAccountIdentifier = CommonValues.internationalBankAccountIdentifier2,
        internationalBankIdentifier = CommonValues.internationalBankIdentifier0,
        nationalBankAccountIdentifier = CommonValues.nationalBankAccountIdentifier2,
        nationalBankIdentifier = CommonValues.nationalBankIdentifier0
    )

    val bankAccount3 = BankAccountSaas(
        internationalBankAccountIdentifier = CommonValues.internationalBankAccountIdentifier3,
        internationalBankIdentifier = CommonValues.internationalBankIdentifier0,
        nationalBankAccountIdentifier = CommonValues.nationalBankAccountIdentifier3,
        nationalBankIdentifier = CommonValues.nationalBankIdentifier0
    )

    val bankAccount4 = BankAccountSaas(
        internationalBankAccountIdentifier = CommonValues.internationalBankAccountIdentifier4,
        internationalBankIdentifier = CommonValues.internationalBankIdentifier0,
        nationalBankAccountIdentifier = CommonValues.nationalBankAccountIdentifier4,
        nationalBankIdentifier = CommonValues.nationalBankIdentifier0
    )

    val version1 = AddressVersionSaas(language1, characterSet1)
    val version2 = AddressVersionSaas(language2, characterSet2)

    val careOf1 = WrappedValueSaas(CommonValues.careOf1)
    val careOf2 = WrappedValueSaas(CommonValues.careOf2)

    val context1 = WrappedValueSaas(CommonValues.context1)
    val context2 = WrappedValueSaas(CommonValues.context2)

    val addressType1 = TypeKeyNameUrlSaas(technicalKey = AddressType.HEADQUARTER.name)

    val adminAreaType1 = TypeKeyNameUrlSaas(CommonValues.adminAreaType1.name)
    val adminAreaType2 = TypeKeyNameUrlSaas(CommonValues.adminAreaType2.name)

    val adminArea1 = AdministrativeAreaSaas(CommonValues.adminArea1, type = adminAreaType1, language = language1)
    val adminArea2 = AdministrativeAreaSaas(CommonValues.adminArea2, type = adminAreaType2, language = language2)

    val postCodeType1 = TypeKeyNameUrlSaas(CommonValues.postCodeType1.name)
    val postCodeType2 = TypeKeyNameUrlSaas(CommonValues.postCodeType2.name)

    val postCode1 = PostCodeSaas(CommonValues.postCode1, postCodeType1)
    val postCode2 = PostCodeSaas(CommonValues.postCode2, postCodeType2)

    val localityType1 = TypeKeyNameUrlSaas(CommonValues.localityType1.name)
    val localityType2 = TypeKeyNameUrlSaas(CommonValues.localityType2.name)

    val locality1 = LocalitySaas(value = CommonValues.locality1, type = localityType1, language = language1)
    val locality2 = LocalitySaas(value = CommonValues.locality2, type = localityType2, language = language2)

    val thoroughfareType1 = TypeKeyNameUrlSaas(CommonValues.thoroughfareType1.name)
    val thoroughfareType2 = TypeKeyNameUrlSaas(CommonValues.thoroughfareType2.name)

    val thoroughfare1 = ThoroughfareSaas(value = CommonValues.thoroughfare1, type = thoroughfareType1, language = language1)
    val thoroughfare2 = ThoroughfareSaas(value = CommonValues.thoroughfare2, type = thoroughfareType2, language = language2)

    val premiseType1 = TypeKeyNameUrlSaas(CommonValues.premiseType1.name)
    val premiseType2 = TypeKeyNameUrlSaas(CommonValues.premiseType2.name)

    val premise1 = PremiseSaas(value = CommonValues.premise1, type = premiseType1, language = language1)
    val premise2 = PremiseSaas(value = CommonValues.premise2, type = premiseType2, language = language2)

    val postalDeliveryPointType1 = TypeKeyNameUrlSaas(CommonValues.postalDeliveryPointType1.name)
    val postalDeliveryPointType2 = TypeKeyNameUrlSaas(CommonValues.postalDeliveryPointType2.name)

    val postalDeliveryPoint1 = PostalDeliveryPointSaas(value = CommonValues.postalDeliveryPoint1, type = postalDeliveryPointType1, language = language1)
    val postalDeliveryPoint2 = PostalDeliveryPointSaas(value = CommonValues.postalDeliveryPoint2, type = postalDeliveryPointType2, language = language2)

    val geoCoordinate1 = GeoCoordinatesSaas(CommonValues.geoCoordinates1.first, CommonValues.geoCoordinates1.second)
    val geoCoordinate2 = GeoCoordinatesSaas(CommonValues.geoCoordinates2.first, CommonValues.geoCoordinates2.second)

    val address1 = AddressSaas(
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

    val address2 = AddressSaas(
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

    val legalEntityResponse1 = BusinessPartnerSaas(
        externalId = CommonValues.externalId1,
        identifiers = listOf(identifier1, identifier2, identifierBpn1),
        names = listOf(name1, name2),
        legalForm = legalForm1,
        status = businessStatus1,
        profile = profile1,
        types = listOf(TypeKeyNameUrlSaas(technicalKey = BusinessPartnerTypeSaas.LEGAL_ENTITY.name)),
        bankAccounts = listOf(bankAccount1, bankAccount2),
        addresses = listOf(address1),
        dataSource = testDatasource,
        lastModifiedAt = modificationTime1,
    )

    // identical but without lastModifiedAt
    val legalEntityRequest1 = legalEntityResponse1.copy(
        lastModifiedAt = null,
    )

    val legalEntityResponse2 = BusinessPartnerSaas(
        externalId = CommonValues.externalId2,
        identifiers = listOf(identifier3, identifier4, identifierBpn2),
        names = listOf(name3, name4),
        legalForm = legalForm2,
        status = businessStatus2,
        profile = profile2,
        types = listOf(TypeKeyNameUrlSaas(technicalKey = BusinessPartnerTypeSaas.LEGAL_ENTITY.name)),
        bankAccounts = listOf(bankAccount3, bankAccount4),
        addresses = listOf(address2),
        dataSource = testDatasource,
        lastModifiedAt = modificationTime2,
    )

    // identical but without lastModifiedAt
    val legalEntityRequest2 = legalEntityResponse2.copy(
        lastModifiedAt = null,
    )

    val siteBusinessPartner1 = BusinessPartnerSaas(
        externalId = CommonValues.externalIdSite1,
        identifiers = listOf(identifierBpnSite1, identifier1, identifier2), // identifiers copied from legal entity
        names = listOf(nameSite1),
        addresses = listOf(address1),
        dataSource = testDatasource,
        types = listOf(TypeKeyNameUrlSaas(BusinessPartnerTypeSaas.ORGANIZATIONAL_UNIT.name)),
        lastModifiedAt = modificationTime1,
        metadata = BusinessPartnerMetadataSaas(
            sharingStatus = SharingStatusSaas(SharingStatusType.SHARED_WITH_CONFIDENT_MATCH, "OK")
        ),
    )
    val siteBusinessPartnerRequest1 = siteBusinessPartner1.copy(
        lastModifiedAt = null,
        metadata = null,
    )

    val siteBusinessPartner2 = BusinessPartnerSaas(
        externalId = CommonValues.externalIdSite2,
        identifiers = listOf(identifierBpnSite2, identifier3, identifier4), // identifiers copied from legal entity
        names = listOf(nameSite2),
        addresses = listOf(address2),
        dataSource = testDatasource,
        types = listOf(TypeKeyNameUrlSaas(BusinessPartnerTypeSaas.ORGANIZATIONAL_UNIT.name)),
        lastModifiedAt = modificationTime2,
        metadata = BusinessPartnerMetadataSaas(
            sharingStatus = SharingStatusSaas(SharingStatusType.SHARED_WITH_NO_MATCH, "OK")
        ),
    )
    val siteBusinessPartnerRequest2 = siteBusinessPartner2.copy(
        lastModifiedAt = null,
        metadata = null,
    )

    val siteNotInPoolResponse = siteBusinessPartner2.copy(
        externalId = CommonValues.externalId3,
        identifiers = listOf(identifier3Response, identifier4Response, identifierBpnSite3),
        metadata = BusinessPartnerMetadataSaas(
            sharingStatus = SharingStatusSaas(SharingStatusType.SHARED_WITH_NO_MATCH, "OK")
        ),
    )

    val siteSharingErrorResponse = siteBusinessPartner2.copy(
        externalId = CommonValues.externalId4,
        identifiers = listOf(),
        metadata = BusinessPartnerMetadataSaas(
            sharingStatus = SharingStatusSaas(SharingStatusType.ERRONEOUS_RECORD, "Error message")
        ),
    )

    val sitePendingResponse = siteBusinessPartner2.copy(
        externalId = CommonValues.externalId5,
        identifiers = listOf(),
        lastModifiedAt = LocalDateTime.now().minusMinutes(1),
        metadata = BusinessPartnerMetadataSaas(
            sharingStatus = SharingStatusSaas(SharingStatusType.SHARED_BY_REVIEW, "OK")
        ),
    )

    val relationType = TypeKeyNameSaas(technicalKey = "PARENT")

    val relationSite1ToLegalEntity = RelationSaas(
        startNode = legalEntityRequest1.externalId!!,
        startNodeDataSource = testDatasource,
        endNode = siteBusinessPartner1.externalId!!,
        endNodeDataSource = testDatasource,
        type = relationType
    )

    val relationSite2ToLegalEntity = RelationSaas(
        startNode = legalEntityRequest2.externalId!!,
        startNodeDataSource = testDatasource,
        endNode = siteBusinessPartner2.externalId!!,
        endNodeDataSource = testDatasource,
        type = relationType
    )

    val siteBusinessPartnerWithRelations1 = siteBusinessPartner1.copy(
        relations = listOf(relationSite1ToLegalEntity)
    )

    val siteBusinessPartnerWithRelations2 = siteBusinessPartner2.copy(
        relations = listOf(relationSite2ToLegalEntity)
    )

    val addressBusinessPartner1 = BusinessPartnerSaas(
        externalId = CommonValues.externalIdAddress1,
        names = legalEntityRequest1.names,
        identifiers = listOf(identifierBpnAddress1, identifier1, identifier2), // identifiers copied from legal entity
        addresses = listOf(address1),
        dataSource = testDatasource,
        types = listOf(TypeKeyNameUrlSaas(BusinessPartnerTypeSaas.BP_ADDRESS.name)),
        lastModifiedAt = modificationTime1,
        metadata = BusinessPartnerMetadataSaas(
            sharingStatus = SharingStatusSaas(SharingStatusType.SHARED_WITH_CONFIDENT_MATCH, "OK")
        ),
    )

    val addressBusinessPartnerRequest1 = addressBusinessPartner1.copy(
        lastModifiedAt = null,
        metadata = null,
    )

    val addressBusinessPartner2 = BusinessPartnerSaas(
        externalId = CommonValues.externalIdAddress2,
        names = siteBusinessPartner1.names,
        identifiers = listOf(identifierBpnAddress2, identifier1, identifier2), // identifiers copied from site
        addresses = listOf(address2),
        dataSource = testDatasource,
        types = listOf(TypeKeyNameUrlSaas(BusinessPartnerTypeSaas.BP_ADDRESS.name)),
        lastModifiedAt = modificationTime2,
        metadata = BusinessPartnerMetadataSaas(
            sharingStatus = SharingStatusSaas(SharingStatusType.SHARED_WITH_NO_MATCH, "OK")
        ),
    )

    val addressBusinessPartnerRequest2 = addressBusinessPartner2.copy(
        lastModifiedAt = null,
        metadata = null,
    )

    val addressNotInPoolResponse = addressBusinessPartner2.copy(
        externalId = CommonValues.externalId3,
        identifiers = listOf(identifierBpnAddress3, identifier1, identifier2), // identifiers copied from site
        metadata = BusinessPartnerMetadataSaas(
            sharingStatus = SharingStatusSaas(SharingStatusType.SHARED_WITH_NO_MATCH, "OK")
        ),
    )

    val addressSharingErrorResponse = addressBusinessPartner2.copy(
        externalId = CommonValues.externalId4,
        identifiers = listOf(),
        metadata = BusinessPartnerMetadataSaas(
            sharingStatus = SharingStatusSaas(SharingStatusType.ERRONEOUS_RECORD, "Error message")
        ),
    )

    val addressPendingResponse = addressBusinessPartner2.copy(
        externalId = CommonValues.externalId5,
        identifiers = listOf(),
        lastModifiedAt = LocalDateTime.now().minusMinutes(1),
        metadata = BusinessPartnerMetadataSaas(
            sharingStatus = SharingStatusSaas(SharingStatusType.SHARED_BY_REVIEW, "OK")
        ),
    )

    val relationAddress1ToLegalEntity = RelationSaas(
        startNode = legalEntityRequest1.externalId!!,
        startNodeDataSource = testDatasource,
        endNode = addressBusinessPartner1.externalId!!,
        endNodeDataSource = testDatasource,
        type = relationType
    )

    val relationAddress2ToSite = RelationSaas(
        startNode = siteBusinessPartner1.externalId!!,
        startNodeDataSource = testDatasource,
        endNode = addressBusinessPartner2.externalId!!,
        endNodeDataSource = testDatasource,
        type = relationType
    )

    val addressBusinessPartnerWithRelations1 = addressBusinessPartner1.copy(
        relations = listOf(relationAddress1ToLegalEntity)
    )

    val addressBusinessPartnerWithRelations2 = addressBusinessPartner2.copy(
        relations = listOf(relationAddress2ToSite)
    )

    val legalEntityAugmented1 = legalEntityRequest1.copy(
        identifiers = listOf(identifier1Response, identifier2Response, identifierBpn1),
        legalForm = legalForm1Response,
        lastModifiedAt = modificationTime1,
        metadata = BusinessPartnerMetadataSaas(
            sharingStatus = SharingStatusSaas(SharingStatusType.SHARED_WITH_CONFIDENT_MATCH, "OK")
        ),
    )

    val legalEntityAugmented2 = legalEntityRequest2.copy(
        identifiers = listOf(identifier3Response, identifier4Response, identifierBpn2),
        legalForm = legalForm2Response,
        lastModifiedAt = modificationTime2,
        metadata = BusinessPartnerMetadataSaas(
            sharingStatus = SharingStatusSaas(SharingStatusType.SHARED_WITH_NO_MATCH, "OK")
        ),
    )

    val legalEntityAugmentedNotInPoolResponse = legalEntityAugmented2.copy(
        externalId = CommonValues.externalId3,
        identifiers = listOf(identifier3Response, identifier4Response, identifierBpn3),
        legalForm = legalForm2Response,
        lastModifiedAt = modificationTime2,
        metadata = BusinessPartnerMetadataSaas(
            sharingStatus = SharingStatusSaas(SharingStatusType.SHARED_WITH_NO_MATCH, "OK")
        ),
    )

    val legalEntityAugmentedSharingErrorResponse = legalEntityRequest2.copy(
        externalId = CommonValues.externalId4,
        identifiers = listOf(),
        legalForm = legalForm2Response,
        lastModifiedAt = modificationTime2,
        metadata = BusinessPartnerMetadataSaas(
            sharingStatus = SharingStatusSaas(SharingStatusType.ERRONEOUS_RECORD, "Error message")
        ),
    )

    val legalEntityAugmentedPendingResponse = legalEntityRequest2.copy(
        externalId = CommonValues.externalId5,
        identifiers = listOf(),
        legalForm = legalForm2Response,
        lastModifiedAt = LocalDateTime.now().minusMinutes(1),
        metadata = BusinessPartnerMetadataSaas(
            sharingStatus = SharingStatusSaas(SharingStatusType.SHARED_BY_REVIEW, "OK")
        ),
    )
}