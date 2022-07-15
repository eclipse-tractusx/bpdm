package org.eclipse.tractusx.bpdm.gate.util

import com.neovisionaries.i18n.LanguageCode
import org.eclipse.tractusx.bpdm.common.model.BusinessStatusType
import org.eclipse.tractusx.bpdm.common.model.ClassificationType
import org.eclipse.tractusx.bpdm.common.model.NameType
import java.time.LocalDateTime

/**
 * Contains simple test values used to create more complex test values such as DTOs
 */
object CommonValues {
    val externalId1 = "external-1"
    val externalId2 = "external-2"

    val bpn1 = "BPNL0000000000XY"
    val bpn2 = "BPNL0000000001XZ"

    val name1 = "Business Partner Name"
    val name2 = "Company ABC AG"
    val name3 = "Another Organisation Corp"
    val name4 = "Catena Test Name"

    val idValue1 = "DE123456789"
    val idTypeTechnicalKey1 = "VAT_DE"
    val idIssuingBodyTechnicalKey1 = "issuing body 1"
    val idStatusTechnicalKey1 = "ACTIVE"

    val idValue2 = "US123456789"
    val idTypeTechnicalKey2 = "VAT_US"
    val idIssuingBodyTechnicalKey2 = "issuing body 2"
    val idStatusTechnicalKey2 = "EXPIRED"

    val idValue3 = "FR123456789"
    val idTypeTechnicalKey3 = "VAT_FR"
    val idIssuingBodyTechnicalKey3 = "issuing body 3"
    val idStatusTechnicalKey3 = "PENDING"

    val idValue4 = "NL123456789"
    val idTypeTechnicalKey4 = "VAT_NL"
    val idIssuingBodyTechnicalKey4 = "issuing body 4"
    val idStatusTechnicalKey4 = "EXPIRED"

    val nameType1 = NameType.OTHER
    val language1 = LanguageCode.en

    val shortName1 = "short1"
    val shortName2 = "short2"
    val shortName3 = "short3"
    val shortName4 = "short4"

    val legalFormTechnicalKey1 = "GMBH"
    val legalFormTechnicalKey2 = "LLC"

    val businessStatusOfficialDenotation1 = "Active"
    val businessStatusOfficialDenotation2 = "Insolvent"

    val businessStatusValidFrom1 = LocalDateTime.of(2020, 1, 1, 0, 0)
    val businessStatusValidFrom2 = LocalDateTime.of(2019, 1, 1, 0, 0)

    val businessStatusValidUntil1 = LocalDateTime.of(2021, 1, 1, 0, 0)
    val businessStatusValidUntil2 = LocalDateTime.of(2022, 1, 1, 0, 0)

    val businessStatusType1 = BusinessStatusType.ACTIVE
    val businessStatusType2 = BusinessStatusType.INSOLVENCY

    val classificationType = ClassificationType.NACE

    val classificationValue1 = "Sale of motor vehicles"
    val classificationValue2 = "Data processing, hosting and related activities"
    val classificationValue3 = "Other information service activities"
    val classificationValue4 = "Financial and insurance activities"

    val classificationCode1 = "code1"
    val classificationCode2 = "code2"
    val classificationCode3 = "code3"
    val classificationCode4 = "code4"

    val internationalBankIdentifier0 = "11111111"
    val nationalBankIdentifier0 = "22222222"

    val internationalBankAccountIdentifier1 = "DE0000000000000000001"
    val nationalBankAccountIdentifier1 = "000000001"

    val internationalBankAccountIdentifier2 = "DE0000000000000000002"
    val nationalBankAccountIdentifier2 = "000000002"

    val internationalBankAccountIdentifier3 = "DE0000000000000000001"
    val nationalBankAccountIdentifier3 = "000000001"

    val internationalBankAccountIdentifier4 = "DE0000000000000000002"
    val nationalBankAccountIdentifier4 = "000000002"
}