package org.eclipse.tractusx.bpdm.gate.util

import com.neovisionaries.i18n.CurrencyCode
import org.eclipse.tractusx.bpdm.common.dto.response.*
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameUrlDto
import org.eclipse.tractusx.bpdm.common.model.BusinessPartnerType
import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityGateOutput

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

    val legalForm1 = LegalFormResponse(
        technicalKey = CommonValues.legalFormTechnicalKey1,
        name = CommonValues.legalFormName1,
        url = CommonValues.legalFormUrl1,
        mainAbbreviation = CommonValues.legalFormAbbreviation1,
        language = language1
    )

    val legalForm2 = LegalFormResponse(
        technicalKey = CommonValues.legalFormTechnicalKey2,
        name = CommonValues.legalFormName2,
        url = CommonValues.legalFormUrl2,
        mainAbbreviation = CommonValues.legalFormAbbreviation2,
        language = language2
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

    val legalEntityResponse1 = LegalEntityResponse(
        bpn = CommonValues.bpn1,
        identifiers = listOf(identifier1, identifier2),
        names = listOf(name1, name2),
        legalForm = legalForm1,
        status = businessStatus1,
        profileClassifications = listOf(classification1, classification2),
        types = listOf(businessPartnerTypeLegalEntity),
        bankAccounts = listOf(bankAccount1, bankAccount2)
    )

    val legalEntityResponse2 = LegalEntityResponse(
        bpn = CommonValues.bpn2,
        identifiers = listOf(identifier3, identifier4),
        names = listOf(name3, name4),
        legalForm = legalForm2,
        status = businessStatus2,
        profileClassifications = listOf(classification3, classification4),
        types = listOf(businessPartnerTypeLegalEntity),
        bankAccounts = listOf(bankAccount3, bankAccount4)
    )

    val legalEntityGateOutput1 = LegalEntityGateOutput(
        legalEntity = legalEntityResponse1,
        CommonValues.externalId1
    )

    val legalEntityGateOutput2 = LegalEntityGateOutput(
        legalEntity = legalEntityResponse2,
        CommonValues.externalId2
    )
}