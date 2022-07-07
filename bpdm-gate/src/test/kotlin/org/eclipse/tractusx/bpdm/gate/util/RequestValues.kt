package org.eclipse.tractusx.bpdm.gate.util

import com.neovisionaries.i18n.CurrencyCode
import org.eclipse.tractusx.bpdm.common.dto.request.*
import org.eclipse.tractusx.bpdm.common.model.BusinessPartnerType
import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityRequest

object RequestValues {
    val identifier1 =
        IdentifierRequest(CommonValues.idValue1, CommonValues.idTypeTechnicalKey1, CommonValues.idIssuingBodyTechnicalKey1, CommonValues.idStatusTechnicalKey1)
    val identifier2 =
        IdentifierRequest(CommonValues.idValue2, CommonValues.idTypeTechnicalKey2, CommonValues.idIssuingBodyTechnicalKey2, CommonValues.idStatusTechnicalKey2)
    val identifier3 =
        IdentifierRequest(CommonValues.idValue3, CommonValues.idTypeTechnicalKey3, CommonValues.idIssuingBodyTechnicalKey3, CommonValues.idStatusTechnicalKey3)
    val identifier4 =
        IdentifierRequest(CommonValues.idValue4, CommonValues.idTypeTechnicalKey4, CommonValues.idIssuingBodyTechnicalKey4, CommonValues.idStatusTechnicalKey4)

    val name1 = NameRequest(value = CommonValues.name1, shortName = CommonValues.shortName1, type = CommonValues.nameType1, language = CommonValues.language1)
    val name2 = NameRequest(value = CommonValues.name2, shortName = CommonValues.shortName2, type = CommonValues.nameType1, language = CommonValues.language1)
    val name3 = NameRequest(value = CommonValues.name3, shortName = CommonValues.shortName3, type = CommonValues.nameType1, language = CommonValues.language1)
    val name4 = NameRequest(value = CommonValues.name4, shortName = CommonValues.shortName4, type = CommonValues.nameType1, language = CommonValues.language1)

    val businessStatusRequest1 = BusinessStatusRequest(
        officialDenotation = CommonValues.businessStatusOfficialDenotation1,
        validFrom = CommonValues.businessStatusValidFrom1,
        validUntil = CommonValues.businessStatusValidUntil1,
        type = CommonValues.businessStatusType1
    )

    val businessStatusRequest2 = BusinessStatusRequest(
        officialDenotation = CommonValues.businessStatusOfficialDenotation2,
        validFrom = CommonValues.businessStatusValidFrom2,
        validUntil = CommonValues.businessStatusValidUntil2,
        type = CommonValues.businessStatusType2
    )

    val classificationRequest1 = ClassificationRequest(
        value = CommonValues.classificationValue1,
        code = CommonValues.classificationCode1,
        type = CommonValues.classificationType
    )

    val classificationRequest2 = ClassificationRequest(
        value = CommonValues.classificationValue2,
        code = CommonValues.classificationCode2,
        type = CommonValues.classificationType
    )

    val classificationRequest3 = ClassificationRequest(
        value = CommonValues.classificationValue3,
        code = CommonValues.classificationCode3,
        type = CommonValues.classificationType
    )

    val classificationRequest4 = ClassificationRequest(
        value = CommonValues.classificationValue4,
        code = CommonValues.classificationCode4,
        type = CommonValues.classificationType
    )

    val bankAccountRequest1 = BankAccountRequest(
        trustScores = emptyList(),
        currency = CurrencyCode.UNDEFINED,
        internationalBankAccountIdentifier = CommonValues.internationalBankAccountIdentifier1,
        internationalBankIdentifier = CommonValues.internationalBankIdentifier0,
        nationalBankAccountIdentifier = CommonValues.nationalBankAccountIdentifier1,
        nationalBankIdentifier = CommonValues.nationalBankIdentifier0
    )

    val bankAccountRequest2 = BankAccountRequest(
        trustScores = emptyList(),
        currency = CurrencyCode.UNDEFINED,
        internationalBankAccountIdentifier = CommonValues.internationalBankAccountIdentifier2,
        internationalBankIdentifier = CommonValues.internationalBankIdentifier0,
        nationalBankAccountIdentifier = CommonValues.nationalBankAccountIdentifier2,
        nationalBankIdentifier = CommonValues.nationalBankIdentifier0
    )

    val bankAccountRequest3 = BankAccountRequest(
        trustScores = emptyList(),
        currency = CurrencyCode.UNDEFINED,
        internationalBankAccountIdentifier = CommonValues.internationalBankAccountIdentifier3,
        internationalBankIdentifier = CommonValues.internationalBankIdentifier0,
        nationalBankAccountIdentifier = CommonValues.nationalBankAccountIdentifier3,
        nationalBankIdentifier = CommonValues.nationalBankIdentifier0
    )

    val bankAccountRequest4 = BankAccountRequest(
        trustScores = emptyList(),
        currency = CurrencyCode.UNDEFINED,
        internationalBankAccountIdentifier = CommonValues.internationalBankAccountIdentifier4,
        internationalBankIdentifier = CommonValues.internationalBankIdentifier0,
        nationalBankAccountIdentifier = CommonValues.nationalBankAccountIdentifier4,
        nationalBankIdentifier = CommonValues.nationalBankIdentifier0
    )

    val legalEntity1 = LegalEntityRequest(
        externalId = CommonValues.externalId1,
        bpn = CommonValues.bpn1,
        identifiers = listOf(identifier1, identifier2),
        names = listOf(name1, name2),
        legalForm = CommonValues.legalFormTechnicalKey1,
        status = businessStatusRequest1,
        profileClassifications = listOf(classificationRequest1, classificationRequest2),
        types = listOf(BusinessPartnerType.LEGAL_ENTITY),
        bankAccounts = listOf(bankAccountRequest1, bankAccountRequest2)
    )

    val legalEntity2 = LegalEntityRequest(
        externalId = CommonValues.externalId2,
        bpn = CommonValues.bpn2,
        identifiers = listOf(identifier3, identifier4),
        names = listOf(name3, name4),
        legalForm = CommonValues.legalFormTechnicalKey2,
        status = businessStatusRequest2,
        profileClassifications = listOf(classificationRequest3, classificationRequest4),
        types = listOf(BusinessPartnerType.LEGAL_ENTITY),
        bankAccounts = listOf(bankAccountRequest3, bankAccountRequest4)
    )
}