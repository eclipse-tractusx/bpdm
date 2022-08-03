package org.eclipse.tractusx.bpdm.gate.service

import com.neovisionaries.i18n.CurrencyCode
import com.neovisionaries.i18n.LanguageCode
import org.eclipse.tractusx.bpdm.common.dto.cdq.*
import org.eclipse.tractusx.bpdm.common.dto.response.*
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameUrlDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeNameUrlDto
import org.eclipse.tractusx.bpdm.common.model.HasDefaultValue
import org.eclipse.tractusx.bpdm.common.service.CdqMappings
import org.eclipse.tractusx.bpdm.gate.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityGateOutput
import org.springframework.stereotype.Service

@Service
class OutputCdqMappingService(
    private val bpnConfigProperties: BpnConfigProperties,
) {

    fun toOutput(businessPartner: BusinessPartnerCdq): LegalEntityGateOutput {
        return LegalEntityGateOutput(
            externalId = businessPartner.externalId!!,
            legalEntity = toDto(businessPartner)
        )
    }

    private fun toDto(businessPartner: BusinessPartnerCdq): LegalEntityResponse {
        return LegalEntityResponse(
            bpn = businessPartner.identifiers.find { it.type?.technicalKey == bpnConfigProperties.id }!!.value,
            identifiers = businessPartner.identifiers.filter { it.type?.technicalKey != bpnConfigProperties.id }.map { toDto(it) },
            names = businessPartner.names.map { toDto(it) },
            legalForm = if (businessPartner.legalForm != null) toDto(businessPartner.legalForm!!) else null,
            status = if (businessPartner.status != null) toDto(businessPartner.status!!) else null,
            profileClassifications = businessPartner.profile?.classifications?.map { toDto(it) } ?: emptyList(),
            types = businessPartner.types.map { toDtoTyped(it) },
            bankAccounts = businessPartner.bankAccounts.map { toDto(it) },
            roles = emptyList(),
            relations = businessPartner.relations.map { toDto(it) }
        )
    }

    private fun toDto(relation: RelationCdq): RelationResponse {
        return RelationResponse(
            relationClass = toDtoTyped(relation.relationClass),
            type = toDtoTyped(relation.type),
            startNode = relation.startNode,
            endNode = relation.endNode,
            startedAt = relation.startedAt,
            endedAt = relation.endedAt
        )
    }

    private fun toDto(bankAccount: BankAccountCdq): BankAccountResponse {
        return BankAccountResponse(
            trustScores = emptyList(),
            currency = TypeKeyNameDto(technicalKey = CurrencyCode.UNDEFINED, name = CurrencyCode.UNDEFINED.name),
            internationalBankAccountIdentifier = bankAccount.internationalBankAccountIdentifier,
            internationalBankIdentifier = bankAccount.internationalBankIdentifier,
            nationalBankAccountIdentifier = bankAccount.nationalBankAccountIdentifier,
            nationalBankIdentifier = bankAccount.nationalBankIdentifier
        )
    }

    private fun toDto(classification: ClassificationCdq): ClassificationResponse {
        return ClassificationResponse(
            value = classification.value,
            code = classification.code,
            type = if (classification.type != null && classification.type!!.name != null) TypeNameUrlDto(
                classification.type!!.name!!,
                classification.type!!.url
            ) else null
        )
    }

    private fun toDto(status: BusinessPartnerStatusCdq): BusinessStatusResponse {
        return BusinessStatusResponse(
            officialDenotation = status.officialDenotation,
            validFrom = status.validFrom,
            validUntil = status.validUntil,
            type = toDtoTyped(status.type)
        )
    }

    private fun toDto(legalForm: LegalFormCdq): LegalFormResponse {
        return LegalFormResponse(
            technicalKey = legalForm.technicalKey,
            name = legalForm.name!!,
            url = legalForm.url,
            mainAbbreviation = legalForm.mainAbbreviation,
            language = toDto(legalForm.language)
        )
    }

    private fun toDto(name: NameCdq): NameResponse {
        return NameResponse(
            value = name.value,
            shortName = name.shortName,
            type = toDtoTyped(name.type!!),
            language = toDto(name.language)
        )
    }

    private fun toDto(language: LanguageCdq?): TypeKeyNameDto<LanguageCode> {
        return TypeKeyNameDto(
            technicalKey = CdqMappings.toLanguageCode(language),
            name = language!!.name!!
        )
    }

    private inline fun <reified T> toDtoTyped(type: TypeKeyNameUrlCdq): TypeKeyNameUrlDto<T> where T : Enum<T>, T : HasDefaultValue<T> {
        return TypeKeyNameUrlDto(CdqMappings.toTypeOrDefault(type), type.name!!, type.url)
    }

    private inline fun <reified T> toDtoTyped(type: TypeKeyNameCdq): TypeKeyNameDto<T> where T : Enum<T>, T : HasDefaultValue<T> {
        return TypeKeyNameDto(CdqMappings.toTypeOrDefault(type), type.name!!)
    }

    private fun toDto(identifierCdq: IdentifierCdq): IdentifierResponse {
        return IdentifierResponse(
            value = identifierCdq.value,
            type = toDto(identifierCdq.type!!),
            issuingBody = toDto(identifierCdq.issuingBody!!),
            status = toDto(identifierCdq.status!!)
        )
    }

    private fun toDto(type: TypeKeyNameUrlCdq): TypeKeyNameUrlDto<String> {
        return TypeKeyNameUrlDto(type.technicalKey!!, type.name!!, type.url)
    }

    private fun toDto(type: TypeKeyNameCdq): TypeKeyNameDto<String> {
        return TypeKeyNameDto(type.technicalKey!!, type.name!!)
    }
}