package org.eclipse.tractusx.bpdm.gate.util

import com.neovisionaries.i18n.CurrencyCode
import org.eclipse.tractusx.bpdm.common.dto.*
import org.eclipse.tractusx.bpdm.common.model.AddressType
import org.eclipse.tractusx.bpdm.common.model.BusinessPartnerType
import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityGateInput

object RequestValues {
    val identifier1 =
        IdentifierDto(CommonValues.idValue1, CommonValues.idTypeTechnicalKey1, CommonValues.idIssuingBodyTechnicalKey1, CommonValues.idStatusTechnicalKey1)
    val identifier2 =
        IdentifierDto(CommonValues.idValue2, CommonValues.idTypeTechnicalKey2, CommonValues.idIssuingBodyTechnicalKey2, CommonValues.idStatusTechnicalKey2)
    val identifier3 =
        IdentifierDto(CommonValues.idValue3, CommonValues.idTypeTechnicalKey3, CommonValues.idIssuingBodyTechnicalKey3, CommonValues.idStatusTechnicalKey3)
    val identifier4 =
        IdentifierDto(CommonValues.idValue4, CommonValues.idTypeTechnicalKey4, CommonValues.idIssuingBodyTechnicalKey4, CommonValues.idStatusTechnicalKey4)

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

    val legalAddress1 = AddressDto(types = listOf(AddressType.LEGAL))


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
        legalAddress = legalAddress1
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