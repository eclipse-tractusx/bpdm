package org.eclipse.tractusx.bpdm.gate.util

import com.neovisionaries.i18n.CurrencyCode
import org.eclipse.tractusx.bpdm.common.dto.*
import org.eclipse.tractusx.bpdm.common.model.AddressType
import org.eclipse.tractusx.bpdm.common.model.BusinessPartnerType
import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityGateInput

object RequestValues {
    val identifier1 =
        IdentifierDto(
            CommonValues.identifierValue1,
            CommonValues.identifierTypeTechnicalKey1,
            CommonValues.identifierIssuingBodyTechnicalKey1,
            CommonValues.identifierStatusTechnicalKey1
        )
    val identifier2 =
        IdentifierDto(
            CommonValues.identifierValue2,
            CommonValues.identifierTypeTechnicalKey2,
            CommonValues.identifierIssuingBodyTechnicalKey2,
            CommonValues.identifierStatusTechnicalKey2
        )
    val identifier3 =
        IdentifierDto(
            CommonValues.identifierValue3,
            CommonValues.identifierTypeTechnicalKey3,
            CommonValues.identifierIssuingBodyTechnicalKey3,
            CommonValues.identifierStatusTechnicalKey3
        )
    val identifier4 =
        IdentifierDto(
            CommonValues.identifierValue4,
            CommonValues.identifierTypeTechnicalKey4,
            CommonValues.identifierIssuingBodyTechnicalKey4,
            CommonValues.identifierStatusTechnicalKey4
        )

    val name1 = NameDto(value = CommonValues.name1, shortName = CommonValues.shortName1, type = CommonValues.nameType1, language = CommonValues.language1)
    val name2 = NameDto(value = CommonValues.name2, shortName = CommonValues.shortName2, type = CommonValues.nameType1, language = CommonValues.language1)
    val name3 = NameDto(value = CommonValues.name3, shortName = CommonValues.shortName3, type = CommonValues.nameType1, language = CommonValues.language1)
    val name4 = NameDto(value = CommonValues.name4, shortName = CommonValues.shortName4, type = CommonValues.nameType1, language = CommonValues.language1)

    val businessStatus1 = BusinessStatusDto(
        officialDenotation = CommonValues.businessStatusOfficialDenotation1,
        validFrom = CommonValues.businessStatusValidFrom1,
        validUntil = CommonValues.businessStatusValidUntil1,
        type = CommonValues.businessStatusType1
    )

    val businessStatus2 = BusinessStatusDto(
        officialDenotation = CommonValues.businessStatusOfficialDenotation2,
        validFrom = CommonValues.businessStatusValidFrom2,
        validUntil = CommonValues.businessStatusValidUntil2,
        type = CommonValues.businessStatusType2
    )

    val classification1 = ClassificationDto(
        value = CommonValues.classificationValue1,
        code = CommonValues.classificationCode1,
        type = CommonValues.classificationType
    )

    val classification2 = ClassificationDto(
        value = CommonValues.classificationValue2,
        code = CommonValues.classificationCode2,
        type = CommonValues.classificationType
    )

    val classification3 = ClassificationDto(
        value = CommonValues.classificationValue3,
        code = CommonValues.classificationCode3,
        type = CommonValues.classificationType
    )

    val classification4 = ClassificationDto(
        value = CommonValues.classificationValue4,
        code = CommonValues.classificationCode4,
        type = CommonValues.classificationType
    )

    val bankAccount1 = BankAccountDto(
        trustScores = emptyList(),
        currency = CurrencyCode.UNDEFINED,
        internationalBankAccountIdentifier = CommonValues.internationalBankAccountIdentifier1,
        internationalBankIdentifier = CommonValues.internationalBankIdentifier0,
        nationalBankAccountIdentifier = CommonValues.nationalBankAccountIdentifier1,
        nationalBankIdentifier = CommonValues.nationalBankIdentifier0
    )

    val bankAccount2 = BankAccountDto(
        trustScores = emptyList(),
        currency = CurrencyCode.UNDEFINED,
        internationalBankAccountIdentifier = CommonValues.internationalBankAccountIdentifier2,
        internationalBankIdentifier = CommonValues.internationalBankIdentifier0,
        nationalBankAccountIdentifier = CommonValues.nationalBankAccountIdentifier2,
        nationalBankIdentifier = CommonValues.nationalBankIdentifier0
    )

    val bankAccount3 = BankAccountDto(
        trustScores = emptyList(),
        currency = CurrencyCode.UNDEFINED,
        internationalBankAccountIdentifier = CommonValues.internationalBankAccountIdentifier3,
        internationalBankIdentifier = CommonValues.internationalBankIdentifier0,
        nationalBankAccountIdentifier = CommonValues.nationalBankAccountIdentifier3,
        nationalBankIdentifier = CommonValues.nationalBankIdentifier0
    )

    val bankAccount4 = BankAccountDto(
        trustScores = emptyList(),
        currency = CurrencyCode.UNDEFINED,
        internationalBankAccountIdentifier = CommonValues.internationalBankAccountIdentifier4,
        internationalBankIdentifier = CommonValues.internationalBankIdentifier0,
        nationalBankAccountIdentifier = CommonValues.nationalBankAccountIdentifier4,
        nationalBankIdentifier = CommonValues.nationalBankIdentifier0
    )

    val addressVersion1 = AddressVersionDto(CommonValues.characterSet1, CommonValues.language1)
    val addressVersion2 = AddressVersionDto(CommonValues.characterSet2, CommonValues.language2)

    val adminArea1 = AdministrativeAreaDto(value = CommonValues.adminArea1, type = CommonValues.adminAreaType1)
    val adminArea2 = AdministrativeAreaDto(value = CommonValues.adminArea2, type = CommonValues.adminAreaType2)

    val postCode1 = PostCodeDto(value = CommonValues.postCode1, type = CommonValues.postCodeType1)
    val postCode2 = PostCodeDto(value = CommonValues.postCode2, type = CommonValues.postCodeType2)

    val locality1 = LocalityDto(value = CommonValues.locality1, type = CommonValues.localityType1)
    val locality2 = LocalityDto(value = CommonValues.locality2, type = CommonValues.localityType2)

    val thoroughfare1 = ThoroughfareDto(value = CommonValues.thoroughfare1, type = CommonValues.thoroughfareType1)
    val thoroughfare2 = ThoroughfareDto(value = CommonValues.thoroughfare2, type = CommonValues.thoroughfareType2)

    val premise1 = PremiseDto(value = CommonValues.premise1, type = CommonValues.premiseType1)
    val premise2 = PremiseDto(value = CommonValues.premise2, type = CommonValues.premiseType2)

    val postalDeliveryPoint1 = PostalDeliveryPointDto(value = CommonValues.postalDeliveryPoint1, type = CommonValues.postalDeliveryPointType1)
    val postalDeliveryPoint2 = PostalDeliveryPointDto(value = CommonValues.postalDeliveryPoint2, type = CommonValues.postalDeliveryPointType2)

    val geoCoordinate1 = GeoCoordinateDto(CommonValues.geoCoordinates1.first, CommonValues.geoCoordinates1.second, CommonValues.geoCoordinates1.third)
    val geoCoordinate2 = GeoCoordinateDto(CommonValues.geoCoordinates2.first, CommonValues.geoCoordinates2.second, CommonValues.geoCoordinates2.third)

    val legalAddress1 = AddressDto(
        addressVersion1,
        CommonValues.careOf1,
        listOf(CommonValues.context1),
        CommonValues.country1,
        listOf(adminArea1),
        listOf(postCode1),
        listOf(locality1),
        listOf(thoroughfare1),
        listOf(premise1),
        listOf(postalDeliveryPoint1),
        geoCoordinate1,
        listOf(AddressType.LEGAL)
    )

    val legalAddress2 = AddressDto(
        addressVersion2,
        CommonValues.careOf2,
        listOf(CommonValues.context2),
        CommonValues.country2,
        listOf(adminArea2),
        listOf(postCode2),
        listOf(locality2),
        listOf(thoroughfare2),
        listOf(premise2),
        listOf(postalDeliveryPoint2),
        geoCoordinate2,
        listOf(AddressType.LEGAL)
    )


    val legalEntity1 = LegalEntityDto(
        bpn = CommonValues.bpn1,
        identifiers = listOf(identifier1, identifier2),
        names = listOf(name1, name2),
        legalForm = CommonValues.legalFormTechnicalKey1,
        status = businessStatus1,
        profileClassifications = listOf(classification1, classification2),
        types = listOf(BusinessPartnerType.LEGAL_ENTITY),
        bankAccounts = listOf(bankAccount1, bankAccount2),
        legalAddress = legalAddress1
    )

    val legalEntity2 = LegalEntityDto(
        bpn = CommonValues.bpn2,
        identifiers = listOf(identifier3, identifier4),
        names = listOf(name3, name4),
        legalForm = CommonValues.legalFormTechnicalKey2,
        status = businessStatus2,
        profileClassifications = listOf(classification3, classification4),
        types = listOf(BusinessPartnerType.LEGAL_ENTITY),
        bankAccounts = listOf(bankAccount3, bankAccount4),
        legalAddress = legalAddress2
    )

    val legalEntityGateInput1 = LegalEntityGateInput(
        externalId = CommonValues.externalId1,
        legalEntity = legalEntity1
    )

    val legalEntityGateInput2 = LegalEntityGateInput(
        externalId = CommonValues.externalId2,
        legalEntity = legalEntity2
    )
}