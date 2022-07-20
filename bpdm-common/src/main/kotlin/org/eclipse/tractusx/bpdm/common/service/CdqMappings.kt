package org.eclipse.tractusx.bpdm.common.service

import com.neovisionaries.i18n.CurrencyCode
import com.neovisionaries.i18n.LanguageCode
import org.eclipse.tractusx.bpdm.common.dto.*
import org.eclipse.tractusx.bpdm.common.dto.cdq.*
import org.eclipse.tractusx.bpdm.common.model.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.model.ClassificationType
import org.eclipse.tractusx.bpdm.common.model.HasDefaultValue

object CdqMappings {
    private fun toReference(type: TypeKeyNameUrlCdq?): String {
        return type!!.technicalKey!!
    }

    private fun toOptionalReference(type: TypeKeyNameUrlCdq?): String? {
        return type?.technicalKey
    }

    private fun toOptionalReference(type: TypeKeyNameCdq?): String? {
        return type?.technicalKey
    }

    fun toOptionalReference(legalForm: LegalFormCdq?): String? {
        return legalForm?.technicalKey
    }

    private inline fun <reified T> toType(type: TypeKeyNameUrlCdq): T where T : Enum<T> {
        return enumValueOf(type.technicalKey!!)
    }

    inline fun <reified T> toTypeOrDefault(type: TypeKeyNameUrlCdq?): T where T : Enum<T>, T : HasDefaultValue<T> {
        return technicalKeyToType(type?.technicalKey)
    }

    inline fun <reified T> technicalKeyToType(technicalKey: String?): T where T : Enum<T>, T : HasDefaultValue<T> {
        val allValues = enumValues<T>()
        val foundValue = if (technicalKey != null) allValues.map { it.name }.find { technicalKey == it } else null
        return if (foundValue != null) enumValueOf(foundValue) else allValues.first().getDefault()
    }

    fun toLanguageCode(language: LanguageCdq?): LanguageCode {
        return language?.technicalKey ?: LanguageCode.undefined
    }

    fun BusinessPartnerCdq.toDto(): LegalEntityWithReferencesDto {
        return LegalEntityWithReferencesDto(
            externalId = externalId!!,
            legalEntity = LegalEntityDto(
                bpn = identifiers.find { it.type?.technicalKey == "BPN" }?.value,
                identifiers = identifiers.filter { it.type?.technicalKey != "BPN" }.map { toDto(it) },
                names = names.map { toDto(it) },
                legalForm = toOptionalReference(legalForm),
                status = if (status != null) toDto(status) else null,
                profileClassifications = toDto(profile),
                types = types.map { toTypeOrDefault<BusinessPartnerType>(it) }.toSet(),
                bankAccounts = bankAccounts.map { toDto(it) }
            )
        )
    }

    fun toDto(identifier: IdentifierCdq): IdentifierDto {
        return IdentifierDto(
            identifier.value,
            toReference(identifier.type),
            toOptionalReference(identifier.issuingBody),
            toOptionalReference(identifier.status)
        )
    }

    fun toDto(name: NameCdq): NameDto {
        return NameDto(
            name.value,
            name.shortName,
            toTypeOrDefault(name.type),
            toLanguageCode(name.language)
        )
    }

    fun toDto(status: BusinessPartnerStatusCdq): BusinessStatusDto {
        return BusinessStatusDto(
            status.officialDenotation,
            status.validFrom,
            status.validUntil,
            toType(status.type)
        )
    }

    fun toDto(profile: PartnerProfileCdq?): Collection<ClassificationDto> {
        return profile?.classifications?.map { toDto(it) } ?: emptyList()
    }

    fun toDto(classification: ClassificationCdq): ClassificationDto {
        return ClassificationDto(classification.value, classification.code, toType<ClassificationType>(classification.type!!))
    }

    fun toDto(account: BankAccountCdq): BankAccountDto {
        return BankAccountDto(
            emptyList(),
            CurrencyCode.UNDEFINED,
            account.internationalBankAccountIdentifier,
            account.internationalBankIdentifier,
            account.nationalBankAccountIdentifier,
            account.nationalBankIdentifier
        )
    }
}