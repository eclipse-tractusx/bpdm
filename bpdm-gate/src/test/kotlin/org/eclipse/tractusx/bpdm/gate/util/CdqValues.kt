package org.eclipse.tractusx.bpdm.gate.util

import com.neovisionaries.i18n.LanguageCode
import org.eclipse.tractusx.bpdm.common.dto.cdq.*
import org.eclipse.tractusx.bpdm.common.model.AddressType
import org.eclipse.tractusx.bpdm.common.model.BusinessPartnerType

/**
 * Test values for CDQ DTOs
 * Numbered values should match with RequestValues numbered values for easier testing
 */
object CdqValues {
    val idTypeBpn = TypeKeyNameUrlCdq("BPN", "Business Partner Number")
    val issuerBpn = TypeKeyNameUrlCdq("CATENAX", "Catena-X")

    val identifier1 = IdentifierCdq(
        type = TypeKeyNameUrlCdq(technicalKey = CommonValues.idTypeTechnicalKey1),
        value = CommonValues.idValue1,
        issuingBody = TypeKeyNameUrlCdq(technicalKey = CommonValues.idIssuingBodyTechnicalKey1),
        status = TypeKeyNameCdq(technicalKey = CommonValues.idStatusTechnicalKey1)
    )

    val identifier2 = IdentifierCdq(
        type = TypeKeyNameUrlCdq(technicalKey = CommonValues.idTypeTechnicalKey2),
        value = CommonValues.idValue2,
        issuingBody = TypeKeyNameUrlCdq(technicalKey = CommonValues.idIssuingBodyTechnicalKey2),
        status = TypeKeyNameCdq(technicalKey = CommonValues.idStatusTechnicalKey2)
    )

    val identifier3 = IdentifierCdq(
        type = TypeKeyNameUrlCdq(technicalKey = CommonValues.idTypeTechnicalKey3),
        value = CommonValues.idValue3,
        issuingBody = TypeKeyNameUrlCdq(technicalKey = CommonValues.idIssuingBodyTechnicalKey3),
        status = TypeKeyNameCdq(technicalKey = CommonValues.idStatusTechnicalKey3)
    )

    val identifier4 = IdentifierCdq(
        type = TypeKeyNameUrlCdq(technicalKey = CommonValues.idTypeTechnicalKey4),
        value = CommonValues.idValue4,
        issuingBody = TypeKeyNameUrlCdq(technicalKey = CommonValues.idIssuingBodyTechnicalKey4),
        status = TypeKeyNameCdq(technicalKey = CommonValues.idStatusTechnicalKey4)
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

    val language1 = LanguageCdq(
        technicalKey = LanguageCode.en,
        name = null
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

    val legalForm1 = LegalFormCdq(technicalKey = CommonValues.legalFormTechnicalKey1)

    val legalForm2 = LegalFormCdq(technicalKey = CommonValues.legalFormTechnicalKey2)

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

    val legalAddress1 = AddressCdq(types = listOf(TypeKeyNameUrlCdq(AddressType.LEGAL.name)))

    val legalEntity1 = BusinessPartnerCdq(
        externalId = CommonValues.externalId1,
        identifiers = listOf(identifier1, identifier2, identifierBpn1),
        names = listOf(name1, name2),
        legalForm = legalForm1,
        status = businessStatus1,
        profile = profile1,
        types = listOf(TypeKeyNameUrlCdq(technicalKey = BusinessPartnerType.LEGAL_ENTITY.name)),
        bankAccounts = listOf(bankAccount1, bankAccount2),
        addresses = listOf(legalAddress1)
    )

    val legalEntity2 = BusinessPartnerCdq(
        externalId = CommonValues.externalId2,
        identifiers = listOf(identifier3, identifier4, identifierBpn2),
        names = listOf(name3, name4),
        legalForm = legalForm2,
        status = businessStatus2,
        profile = profile2,
        types = listOf(TypeKeyNameUrlCdq(technicalKey = BusinessPartnerType.LEGAL_ENTITY.name)),
        bankAccounts = listOf(bankAccount3, bankAccount4),
        addresses = listOf(legalAddress1)
    )
}