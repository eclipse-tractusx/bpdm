package org.eclipse.tractusx.bpdm.gate.service

import com.neovisionaries.i18n.CountryCode
import com.neovisionaries.i18n.CurrencyCode
import com.neovisionaries.i18n.LanguageCode
import org.eclipse.tractusx.bpdm.common.dto.cdq.*
import org.eclipse.tractusx.bpdm.common.dto.response.*
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameUrlDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeNameUrlDto
import org.eclipse.tractusx.bpdm.common.model.HasDefaultValue
import org.eclipse.tractusx.bpdm.common.model.NamedType
import org.eclipse.tractusx.bpdm.common.model.NamedUrlType
import org.eclipse.tractusx.bpdm.common.service.CdqMappings
import org.eclipse.tractusx.bpdm.common.service.toDto
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
            relations = emptyList(),
            legalAddress = toDto(businessPartner.addresses.single())
        )
    }

    private fun toDto(address: AddressCdq): AddressResponse {
        return AddressResponse(
            version = toDto(address.version!!),
            careOf = address.careOf?.value,
            contexts = address.contexts.mapNotNull { it.value },
            country = toDto(address.country),
            administrativeAreas = address.administrativeAreas.map { toDto(it) },
            postCodes = address.postCodes.map { toDto(it) },
            localities = address.localities.map { toDto(it) },
            thoroughfares = address.thoroughfares.map { toDto(it) },
            premises = address.premises.map { toDto(it) },
            postalDeliveryPoints = address.postalDeliveryPoints.map { toDto(it) },
            geographicCoordinates = if (address.geographicCoordinates != null) CdqMappings.toDto(address.geographicCoordinates!!) else null,
            types = address.types.map { toDtoTyped(it) }
        )
    }

    private fun toDto(postalDeliveryPointCdq: PostalDeliveryPointCdq): PostalDeliveryPointResponse {
        return PostalDeliveryPointResponse(
            value = postalDeliveryPointCdq.value,
            shortName = postalDeliveryPointCdq.shortName,
            number = postalDeliveryPointCdq.number,
            type = toDtoTyped(postalDeliveryPointCdq.type),
            language = toDto(postalDeliveryPointCdq.language)
        )
    }

    private fun toDto(premiseCdq: PremiseCdq): PremiseResponse {
        return PremiseResponse(
            value = premiseCdq.value,
            shortName = premiseCdq.shortName,
            number = premiseCdq.number,
            type = toDtoTyped(premiseCdq.type),
            language = toDto(premiseCdq.language)
        )
    }

    private fun toDto(thoroughfareCdq: ThoroughfareCdq): ThoroughfareResponse {
        return ThoroughfareResponse(
            value = thoroughfareCdq.value ?: "",
            name = thoroughfareCdq.name,
            shortName = thoroughfareCdq.shortName,
            number = thoroughfareCdq.number,
            direction = thoroughfareCdq.direction,
            type = toDtoTyped(thoroughfareCdq.type),
            language = toDto(thoroughfareCdq.language)
        )
    }

    private fun toDto(localityCdq: LocalityCdq): LocalityResponse {
        return LocalityResponse(
            value = localityCdq.value,
            shortName = localityCdq.shortName,
            type = toDtoTyped(localityCdq.type),
            language = toDto(localityCdq.language)
        )
    }

    private fun toDto(postCodeCdq: PostCodeCdq): PostCodeResponse {
        return PostCodeResponse(
            value = postCodeCdq.value,
            type = toDtoTyped(postCodeCdq.type)
        )
    }

    private fun toDto(administrativeAreaCdq: AdministrativeAreaCdq): AdministrativeAreaResponse {
        return AdministrativeAreaResponse(
            value = administrativeAreaCdq.value,
            shortName = administrativeAreaCdq.shortName,
            null,
            type = toDtoTyped(administrativeAreaCdq.type),
            language = toDto(administrativeAreaCdq.language)
        )
    }

    private fun toDto(addressVersion: AddressVersionCdq): AddressVersionResponse {
        return AddressVersionResponse(
            characterSet = toDtoTyped(addressVersion.characterSet),
            language = toDto(addressVersion.language)
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
            type = toDtoTyped(name.type),
            language = toDto(name.language)
        )
    }

    private fun toDto(language: LanguageCdq?): TypeKeyNameDto<LanguageCode> {
        val languageCode = CdqMappings.toLanguageCode(language)
        return languageCode.toDto()
    }

    private fun toDto(country: CountryCdq?): TypeKeyNameDto<CountryCode> {
        val countryCode = CdqMappings.toCountryCode(country)
        return countryCode.toDto()
    }

    private inline fun <reified T> toDtoTyped(type: TypeKeyNameUrlCdq?): TypeKeyNameUrlDto<T> where T : Enum<T>, T : HasDefaultValue<T>, T : NamedUrlType {
        val enumValue = CdqMappings.toTypeOrDefault<T>(type)
        return enumValue.toDto()
    }

    private inline fun <reified T> toDtoTyped(type: TypeKeyNameCdq?): TypeKeyNameDto<T> where T : Enum<T>, T : HasDefaultValue<T>, T : NamedType {
        val enumValue = CdqMappings.toTypeOrDefault<T>(type)
        return enumValue.toDto()
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