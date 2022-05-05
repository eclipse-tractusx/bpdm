package com.catenax.gpdm.component.cdq.service

import com.catenax.gpdm.component.cdq.config.CdqIdentifierConfigProperties
import com.catenax.gpdm.component.cdq.dto.*
import com.catenax.gpdm.dto.GeoCoordinateDto
import com.catenax.gpdm.dto.request.*
import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.dto.response.type.TypeKeyNameUrlDto
import com.catenax.gpdm.dto.response.type.TypeNameUrlDto
import com.catenax.gpdm.entity.BusinessPartnerType
import com.catenax.gpdm.entity.ClassificationType
import com.catenax.gpdm.entity.HasDefaultValue
import com.neovisionaries.i18n.CountryCode
import com.neovisionaries.i18n.CurrencyCode
import com.neovisionaries.i18n.LanguageCode
import org.springframework.stereotype.Service

@Service
class CdqRequestMappingService(
    val cdqIdentifierConfigProperties: CdqIdentifierConfigProperties
) {
    private fun toReference(type: TypeKeyNameUrlCdq?): String{
        return type!!.technicalKey!!
    }

    private fun toOptionalReference(type: TypeKeyNameUrlCdq?): String?{
        return type?.technicalKey
    }

    private fun toOptionalReference(type: TypeKeyNameCdq?): String?{
        return type?.technicalKey
    }

    private fun toOptionalReference(legalForm: LegalFormCdq?): String?{
        return legalForm?.technicalKey
    }

    private inline fun <reified T> toType(type: TypeKeyNameUrlCdq): T where T: Enum<T>{
        return enumValueOf(type.technicalKey!!)
    }

    private inline fun <reified T> toTypeOrDefault(type: TypeKeyNameUrlCdq?): T where T: Enum<T>, T: HasDefaultValue<T>{
        return technicalKeyToType(type?.technicalKey)
    }

    private inline fun <reified T> toTypeOrDefault(type: TypeKeyNameCdq?): T where T: Enum<T>, T: HasDefaultValue<T>{
       return technicalKeyToType(type?.technicalKey)
    }

    private inline fun <reified T> technicalKeyToType(technicalKey: String?): T where T: Enum<T>, T: HasDefaultValue<T>{
        val allValues =   enumValues<T>()
        val foundValue = if(technicalKey != null) allValues.map { it.name }.find { technicalKey == it } else null
        return if(foundValue != null) enumValueOf(foundValue) else allValues.first().getDefault()
    }

    private fun  toLanguageCode(language: LanguageCdq?): LanguageCode{
        return language?.technicalKey ?: LanguageCode.undefined
    }

    private fun  toCountryCode(country: CountryCdq?): CountryCode{
        return country?.shortName ?: CountryCode.UNDEFINED
    }

    fun toRequest(partner: BusinessPartnerCdq): BusinessPartnerRequest {
        return BusinessPartnerRequest(
            partner.identifiers.find { it.type?.technicalKey == "BPN" }?.value,
            partner.identifiers.map { toRequest(it) }.plus(toCdqIdentifierRequest(partner.id)),
            partner.names.map { toRequest(it) },
            toOptionalReference(partner.legalForm),
            if(partner.status != null) toRequest(partner.status) else null,
            partner.addresses.map { toRequest(it) },
            toRequest(partner.profile),
            partner.types.map{ toTypeOrDefault<BusinessPartnerType>(it) }.toSet(),
            partner.bankAccounts.map { toRequest(it) }
        )
    }

    fun toRequest(identifier: IdentifierCdq): IdentifierRequest {
        return IdentifierRequest(identifier.value,
            toReference(identifier.type),
            toOptionalReference(identifier.issuingBody),
            toOptionalReference(identifier.status))
    }

    fun toCdqIdentifierRequest(idValue: String): IdentifierRequest {
        return IdentifierRequest(idValue,
            cdqIdentifierConfigProperties.typeKey,
            cdqIdentifierConfigProperties.issuerKey,
            cdqIdentifierConfigProperties.statusImportedKey)
    }

    fun toRequest(idType: TypeKeyNameUrlCdq): TypeKeyNameUrlDto<String>{
        return TypeKeyNameUrlDto(idType.technicalKey!!, idType.name ?: "", idType.url)
    }

    fun toRequest(idStatus: TypeKeyNameCdq): TypeKeyNameDto<String>{
        return TypeKeyNameDto(idStatus.technicalKey!!, idStatus.name!!)
    }

    fun toRequest(name: NameCdq): NameRequest {
        return NameRequest(name.value,
            name.shortName,
            toTypeOrDefault(name.type),
            toLanguageCode(name.language)
        )
    }

    fun toRequest(legalForm: LegalFormCdq, partner: BusinessPartnerCdq): LegalFormRequest{
        return LegalFormRequest(
            legalForm.technicalKey,
            legalForm.name,
            legalForm.url,
            legalForm.mainAbbreviation,
            toLanguageCode(legalForm.language),
            partner.categories.map { toCategoryRequest(it) }
        )
    }

    fun toCategoryRequest(category: TypeKeyNameUrlCdq): TypeNameUrlDto{
        return TypeNameUrlDto(category.name!!, category.url)
    }

    fun toRequest(status: BusinessPartnerStatusCdq): BusinessStatusRequest {
        return BusinessStatusRequest(status.officialDenotation,
            status.validFrom,
            status.validUntil,
            toType(status.type))
    }

    fun toRequest(address: AddressCdq): AddressRequest {
        return AddressRequest(
            toRequest(address.version),
            address.careOf?.value,
            address.contexts.mapNotNull { it.value },
            toCountryCode(address.country),
            address.administrativeAreas.map { toRequest(it) },
            address.postCodes.map { toRequest(it) },
            address.localities.map { toRequest(it) },
            address.thoroughfares.map { toRequest(it) },
            address.premises.map { toRequest(it) },
            address.postalDeliveryPoints.map { toRequest(it) },
            if(address.geographicCoordinates != null) toRequest(address.geographicCoordinates) else null,
            address.types.map { toTypeOrDefault(it) }
        )
    }

    fun toRequest(version: AddressVersionCdq?): AddressVersionRequest {
        return AddressVersionRequest(toTypeOrDefault(version?.characterSet), toLanguageCode(version?.language))
    }

    fun toRequest(area: AdministrativeAreaCdq): AdministrativeAreaRequest {
        return AdministrativeAreaRequest(area.value,
            area.shortName,
            null,
            toTypeOrDefault(area.type))
    }

    fun toRequest(postcode: PostCodeCdq): PostCodeRequest {
        return PostCodeRequest(postcode.value, toTypeOrDefault(postcode.type))
    }

    fun toRequest(locality: LocalityCdq): LocalityRequest {
        return LocalityRequest(locality.value, locality.shortName, toTypeOrDefault(locality.type))
    }

    fun toRequest(thoroughfare: ThoroughfareCdq): ThoroughfareRequest {
        return ThoroughfareRequest(
            thoroughfare.value ?: "",
            thoroughfare.name,
            thoroughfare.shortName,
            thoroughfare.number,
            thoroughfare.direction,
            toTypeOrDefault(thoroughfare.type)
        )
    }

    fun toRequest(premise: PremiseCdq): PremiseRequest {
        return PremiseRequest(premise.value,
            premise.shortName,
            premise.number,
            toTypeOrDefault(premise.type))
    }

    fun toRequest(deliveryPoint: PostalDeliveryPointCdq): PostalDeliveryPointRequest {
        return PostalDeliveryPointRequest(deliveryPoint.value,
            deliveryPoint.shortName,
            deliveryPoint.number,
            toTypeOrDefault(deliveryPoint.type))
    }

    fun toRequest(geoCoords: GeoCoordinatesCdq): GeoCoordinateDto{
        return GeoCoordinateDto(geoCoords.longitude, geoCoords.latitude, 0.0f)
    }

    fun toRequest(profile: PartnerProfileCdq?): Collection<ClassificationRequest>{
        return profile?.classifications?.map { toRequest(it) } ?: emptyList()
    }

    fun toRequest(classification: ClassificationCdq): ClassificationRequest {
        return ClassificationRequest(classification.value, classification.code, toType<ClassificationType>(classification.type))
    }

    fun toRequest(account: BankAccountCdq): BankAccountRequest {
        return BankAccountRequest(
            emptyList(),
            CurrencyCode.UNDEFINED,
            account.internationalBankAccountIdentifier,
            account.internationalBankIdentifier,
            account.nationalBankAccountIdentifier,
            account.nationalBankIdentifier)
    }

}