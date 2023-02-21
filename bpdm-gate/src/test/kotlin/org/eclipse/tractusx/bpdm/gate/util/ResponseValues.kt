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

import com.neovisionaries.i18n.CurrencyCode
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.dto.response.*
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameUrlDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeNameUrlDto
import org.eclipse.tractusx.bpdm.common.model.AddressType
import org.eclipse.tractusx.bpdm.common.model.BusinessPartnerType
import org.eclipse.tractusx.bpdm.gate.dto.*
import java.time.Instant

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
        type = TypeKeyNameUrlDto(
            technicalKey = CommonValues.identifierTypeTechnicalKey1,
            name = CommonValues.identifierTypeName1,
            url = CommonValues.identifierTypeUrl1
        ),
        issuingBody = TypeKeyNameUrlDto(
            technicalKey = CommonValues.identifierIssuingBodyTechnicalKey1,
            name = CommonValues.identifierIssuingBodyName1,
            url = CommonValues.identifierIssuingBodyUrl1
        ),
        status = TypeKeyNameDto(
            technicalKey = CommonValues.identifierStatusTechnicalKey1,
            name = CommonValues.identifierStatusName1
        )
    )
    val identifier2 = IdentifierResponse(
        value = CommonValues.identifierValue2,
        type = TypeKeyNameUrlDto(
            technicalKey = CommonValues.identifierTypeTechnicalKey2,
            name = CommonValues.identifierTypeName2,
            url = CommonValues.identifierTypeUrl2
        ),
        issuingBody = TypeKeyNameUrlDto(
            technicalKey = CommonValues.identifierIssuingBodyTechnicalKey2,
            name = CommonValues.identifierIssuingBodyName2,
            url = CommonValues.identifierIssuingBodyUrl2
        ),
        status = TypeKeyNameDto(
            technicalKey = CommonValues.identifierStatusTechnicalKey2,
            name = CommonValues.identifierStatusName2
        )
    )
    val identifier3 = IdentifierResponse(
        value = CommonValues.identifierValue3,
        type = TypeKeyNameUrlDto(
            technicalKey = CommonValues.identifierTypeTechnicalKey3,
            name = CommonValues.identifierTypeName3,
            url = CommonValues.identifierTypeUrl3
        ),
        issuingBody = TypeKeyNameUrlDto(
            technicalKey = CommonValues.identifierIssuingBodyTechnicalKey3,
            name = CommonValues.identifierIssuingBodyName3,
            url = CommonValues.identifierIssuingBodyUrl3
        ),
        status = TypeKeyNameDto(
            technicalKey = CommonValues.identifierStatusTechnicalKey3,
            name = CommonValues.identifierStatusName3
        )
    )
    val identifier4 = IdentifierResponse(
        value = CommonValues.identifierValue4,
        type = TypeKeyNameUrlDto(
            technicalKey = CommonValues.identifierTypeTechnicalKey4,
            name = CommonValues.identifierTypeName4,
            url = CommonValues.identifierTypeUrl4
        ),
        issuingBody = TypeKeyNameUrlDto(
            technicalKey = CommonValues.identifierIssuingBodyTechnicalKey4,
            name = CommonValues.identifierIssuingBodyName4,
            url = CommonValues.identifierIssuingBodyUrl4
        ),
        status = TypeKeyNameDto(
            technicalKey = CommonValues.identifierStatusTechnicalKey4,
            name = CommonValues.identifierStatusName4
        )
    )

    val name1 = NameResponse(
        value = CommonValues.name1,
        shortName = CommonValues.shortName1,
        type = TypeKeyNameUrlDto(
            technicalKey = CommonValues.nameType1,
            name = CommonValues.nameType1.getTypeName(),
            url = CommonValues.nameType1.getUrl()
        ),
        language = language1
    )

    val name2 = NameResponse(
        value = CommonValues.name2,
        shortName = CommonValues.shortName2,
        type = TypeKeyNameUrlDto(
            technicalKey = CommonValues.nameType1,
            name = CommonValues.nameType1.getTypeName(),
            url = CommonValues.nameType1.getUrl()
        ),
        language = language1
    )

    val name3 = NameResponse(
        value = CommonValues.name3,
        shortName = CommonValues.shortName3,
        type = TypeKeyNameUrlDto(
            technicalKey = CommonValues.nameType1,
            name = CommonValues.nameType1.getTypeName(),
            url = CommonValues.nameType1.getUrl()
        ),
        language = language1
    )

    val name4 = NameResponse(
        value = CommonValues.name4,
        shortName = CommonValues.shortName4,
        type = TypeKeyNameUrlDto(
            technicalKey = CommonValues.nameType1,
            name = CommonValues.nameType1.getTypeName(),
            url = CommonValues.nameType1.getUrl()
        ),
        language = language1
    )

    val legalFormCategory1 = TypeNameUrlDto(CommonValues.legalFormCategoryName1, CommonValues.legalFormCategoryUrl1)
    val legalFormCategory2 = TypeNameUrlDto(CommonValues.legalFormCategoryName2, CommonValues.legalFormCategoryUrl2)

    val legalForm1 = LegalFormResponse(
        technicalKey = CommonValues.legalFormTechnicalKey1,
        name = CommonValues.legalFormName1,
        url = CommonValues.legalFormUrl1,
        mainAbbreviation = CommonValues.legalFormAbbreviation1,
        language = language1,
        categories = listOf(legalFormCategory1)
    )

    val legalForm2 = LegalFormResponse(
        technicalKey = CommonValues.legalFormTechnicalKey2,
        name = CommonValues.legalFormName2,
        url = CommonValues.legalFormUrl2,
        mainAbbreviation = CommonValues.legalFormAbbreviation2,
        language = language2,
        categories = listOf(legalFormCategory2)
    )

    val businessStatus1 = BusinessStatusResponse(
        officialDenotation = CommonValues.businessStatusOfficialDenotation1,
        validFrom = CommonValues.businessStatusValidFrom1,
        validUntil = CommonValues.businessStatusValidUntil1,
        type = TypeKeyNameUrlDto(
            technicalKey = CommonValues.businessStatusType1,
            name = CommonValues.businessStatusType1.getTypeName(),
            url = CommonValues.businessStatusType1.getUrl()
        )
    )

    val businessStatus2 = BusinessStatusResponse(
        officialDenotation = CommonValues.businessStatusOfficialDenotation2,
        validFrom = CommonValues.businessStatusValidFrom2,
        validUntil = CommonValues.businessStatusValidUntil2,
        type = TypeKeyNameUrlDto(
            technicalKey = CommonValues.businessStatusType2,
            name = CommonValues.businessStatusType2.getTypeName(),
            url = CommonValues.businessStatusType2.getUrl()
        )
    )

    val classification1 = ClassificationResponse(
        value = CommonValues.classificationValue1,
        code = CommonValues.classificationCode1
    )

    val classification2 = ClassificationResponse(
        value = CommonValues.classificationValue2,
        code = CommonValues.classificationCode2
    )

    val classification3 = ClassificationResponse(
        value = CommonValues.classificationValue3,
        code = CommonValues.classificationCode3
    )

    val classification4 = ClassificationResponse(
        value = CommonValues.classificationValue4,
        code = CommonValues.classificationCode4
    )

    val businessPartnerTypeLegalEntity = TypeKeyNameUrlDto(
        technicalKey = BusinessPartnerType.LEGAL_ENTITY,
        name = BusinessPartnerType.LEGAL_ENTITY.getTypeName(),
        url = BusinessPartnerType.LEGAL_ENTITY.getUrl()
    )

    val bankAccount1 = BankAccountResponse(
        trustScores = emptyList(),
        currency = TypeKeyNameDto(technicalKey = CurrencyCode.UNDEFINED, name = CurrencyCode.UNDEFINED.name),
        internationalBankAccountIdentifier = CommonValues.internationalBankAccountIdentifier1,
        internationalBankIdentifier = CommonValues.internationalBankIdentifier0,
        nationalBankAccountIdentifier = CommonValues.nationalBankAccountIdentifier1,
        nationalBankIdentifier = CommonValues.nationalBankIdentifier0
    )

    val bankAccount2 = BankAccountResponse(
        trustScores = emptyList(),
        currency = TypeKeyNameDto(technicalKey = CurrencyCode.UNDEFINED, name = CurrencyCode.UNDEFINED.name),
        internationalBankAccountIdentifier = CommonValues.internationalBankAccountIdentifier2,
        internationalBankIdentifier = CommonValues.internationalBankIdentifier0,
        nationalBankAccountIdentifier = CommonValues.nationalBankAccountIdentifier2,
        nationalBankIdentifier = CommonValues.nationalBankIdentifier0
    )

    val bankAccount3 = BankAccountResponse(
        trustScores = emptyList(),
        currency = TypeKeyNameDto(technicalKey = CurrencyCode.UNDEFINED, name = CurrencyCode.UNDEFINED.name),
        internationalBankAccountIdentifier = CommonValues.internationalBankAccountIdentifier3,
        internationalBankIdentifier = CommonValues.internationalBankIdentifier0,
        nationalBankAccountIdentifier = CommonValues.nationalBankAccountIdentifier3,
        nationalBankIdentifier = CommonValues.nationalBankIdentifier0
    )

    val bankAccount4 = BankAccountResponse(
        trustScores = emptyList(),
        currency = TypeKeyNameDto(technicalKey = CurrencyCode.UNDEFINED, name = CurrencyCode.UNDEFINED.name),
        internationalBankAccountIdentifier = CommonValues.internationalBankAccountIdentifier4,
        internationalBankIdentifier = CommonValues.internationalBankIdentifier0,
        nationalBankAccountIdentifier = CommonValues.nationalBankAccountIdentifier4,
        nationalBankIdentifier = CommonValues.nationalBankIdentifier0
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
        identifiers = listOf(identifier1, identifier2),
        names = listOf(name1, name2),
        legalForm = legalForm1,
        status = businessStatus1,
        profileClassifications = listOf(classification1, classification2),
        types = listOf(businessPartnerTypeLegalEntity),
        bankAccounts = listOf(bankAccount1, bankAccount2)
    )

    val legalEntityResponse2 = LegalEntityResponse(
        identifiers = listOf(identifier3, identifier4),
        names = listOf(name3, name4),
        legalForm = legalForm2,
        status = businessStatus2,
        profileClassifications = listOf(classification3, classification4),
        types = listOf(businessPartnerTypeLegalEntity),
        bankAccounts = listOf(bankAccount3, bankAccount4)
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
        bpn = CommonValues.bpn1,
        legalEntity = legalEntityResponse1,
        externalId = CommonValues.externalId1,
        legalAddress = address1
    )

    val legalEntityGateOutput2 = LegalEntityGateOutput(
        bpn = CommonValues.bpn2,
        legalEntity = legalEntityResponse2,
        externalId = CommonValues.externalId2,
        legalAddress = address2
    )

    val legalEntityPartnerResponse1 = LegalEntityPartnerResponse(
        bpn = CommonValues.bpn1,
        properties = legalEntityResponse1,
        currentness = Instant.ofEpochMilli(0)
    )
    val legalEntityPartnerResponse2 = LegalEntityPartnerResponse(
        bpn = CommonValues.bpn2,
        properties = legalEntityResponse2,
        currentness = Instant.ofEpochMilli(0)
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
        name = CommonValues.nameSite1
    )
    val siteResponse2 = SiteResponse(
        name = CommonValues.nameSite2
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
        bpn = CommonValues.bpnSite1,
        legalEntityBpn = CommonValues.bpn1
    )
    val siteGateOutput2 = SiteGateOutput(
        site = siteResponse2,
        mainAddress = address2,
        externalId = CommonValues.externalIdSite2,
        bpn = CommonValues.bpnSite2,
        legalEntityBpn = CommonValues.bpn2
    )

    val sitePartnerResponse1 = SitePartnerResponse(bpn = CommonValues.bpnSite1, name = CommonValues.nameSite1)
    val sitePartnerResponse2 = SitePartnerResponse(bpn = CommonValues.bpnSite2, name = CommonValues.nameSite2)

    val sitePartnerSearchResponse1 = SitePartnerSearchResponse(site = sitePartnerResponse1, bpnLegalEntity = CommonValues.bpn1)
    val sitePartnerSearchResponse2 = SitePartnerSearchResponse(site = sitePartnerResponse2, bpnLegalEntity = CommonValues.bpn2)

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