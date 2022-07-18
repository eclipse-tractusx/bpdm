package org.eclipse.tractusx.bpdm.pool.component.cdq.service

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.*
import org.eclipse.tractusx.bpdm.common.dto.cdq.*
import org.eclipse.tractusx.bpdm.common.model.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.model.HasDefaultValue
import org.eclipse.tractusx.bpdm.common.service.CdqMappings
import org.eclipse.tractusx.bpdm.pool.component.cdq.config.CdqIdentifierConfigProperties
import org.eclipse.tractusx.bpdm.pool.dto.request.BusinessPartnerRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.LegalFormRequest
import org.eclipse.tractusx.bpdm.pool.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.pool.dto.response.type.TypeKeyNameUrlDto
import org.eclipse.tractusx.bpdm.pool.dto.response.type.TypeNameUrlDto
import org.springframework.stereotype.Service

@Service
class CdqRequestMappingService(
    val cdqIdentifierConfigProperties: CdqIdentifierConfigProperties
) {

    private inline fun <reified T> toTypeOrDefault(type: TypeKeyNameCdq?): T where T : Enum<T>, T : HasDefaultValue<T> {
        return CdqMappings.technicalKeyToType(type?.technicalKey)
    }

    private fun toCountryCode(country: CountryCdq?): CountryCode {
        return country?.shortName ?: CountryCode.UNDEFINED
    }

    fun toRequest(partner: BusinessPartnerCdq): BusinessPartnerRequest {
        return BusinessPartnerRequest(
            partner.identifiers.find { it.type?.technicalKey == "BPN" }?.value,
            partner.identifiers.map { CdqMappings.toDto(it) }.plus(toCdqIdentifierRequest(partner.id!!)),
            partner.names.map { CdqMappings.toDto(it) },
            CdqMappings.toOptionalReference(partner.legalForm),
            if (partner.status != null) CdqMappings.toDto(partner.status!!) else null,
            partner.addresses.map { toRequest(it) },
            listOf(),
            CdqMappings.toDto(partner.profile),
            partner.types.map { CdqMappings.toTypeOrDefault<BusinessPartnerType>(it) }.toSet(),
            partner.bankAccounts.map { CdqMappings.toDto(it) }
        )
    }

    fun toCdqIdentifierRequest(idValue: String): IdentifierDto {
        return IdentifierDto(
            idValue,
            cdqIdentifierConfigProperties.typeKey,
            cdqIdentifierConfigProperties.issuerKey,
            cdqIdentifierConfigProperties.statusImportedKey
        )
    }

    fun toRequest(idType: TypeKeyNameUrlCdq): TypeKeyNameUrlDto<String> {
        return TypeKeyNameUrlDto(idType.technicalKey!!, idType.name ?: "", idType.url)
    }

    fun toRequest(idStatus: TypeKeyNameCdq): TypeKeyNameDto<String> {
        return TypeKeyNameDto(idStatus.technicalKey!!, idStatus.name!!)
    }

    fun toRequest(legalForm: LegalFormCdq, partner: BusinessPartnerCdq): LegalFormRequest {
        return LegalFormRequest(
            legalForm.technicalKey,
            legalForm.name!!,
            legalForm.url,
            legalForm.mainAbbreviation,
            CdqMappings.toLanguageCode(legalForm.language),
            partner.categories.map { toCategoryRequest(it) }
        )
    }

    fun toCategoryRequest(category: TypeKeyNameUrlCdq): TypeNameUrlDto {
        return TypeNameUrlDto(category.name!!, category.url)
    }

    fun toRequest(address: AddressCdq): AddressRequest {
        return AddressRequest(
            null,
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
            if (address.geographicCoordinates != null) toRequest(address.geographicCoordinates!!) else null,
            address.types.map { CdqMappings.toTypeOrDefault(it) }
        )
    }

    fun toRequest(version: AddressVersionCdq?): AddressVersionRequest {
        return AddressVersionRequest(toTypeOrDefault(version?.characterSet), CdqMappings.toLanguageCode(version?.language))
    }

    fun toRequest(area: AdministrativeAreaCdq): AdministrativeAreaRequest {
        return AdministrativeAreaRequest(
            area.value,
            area.shortName,
            null,
            CdqMappings.toTypeOrDefault(area.type)
        )
    }

    fun toRequest(postcode: PostCodeCdq): PostCodeRequest {
        return PostCodeRequest(postcode.value, CdqMappings.toTypeOrDefault(postcode.type))
    }

    fun toRequest(locality: LocalityCdq): LocalityRequest {
        return LocalityRequest(locality.value, locality.shortName, CdqMappings.toTypeOrDefault(locality.type))
    }

    fun toRequest(thoroughfare: ThoroughfareCdq): ThoroughfareRequest {
        return ThoroughfareRequest(
            thoroughfare.value ?: "",
            thoroughfare.name,
            thoroughfare.shortName,
            thoroughfare.number,
            thoroughfare.direction,
            CdqMappings.toTypeOrDefault(thoroughfare.type)
        )
    }

    fun toRequest(premise: PremiseCdq): PremiseRequest {
        return PremiseRequest(
            premise.value,
            premise.shortName,
            premise.number,
            CdqMappings.toTypeOrDefault(premise.type)
        )
    }

    fun toRequest(deliveryPoint: PostalDeliveryPointCdq): PostalDeliveryPointRequest {
        return PostalDeliveryPointRequest(
            deliveryPoint.value,
            deliveryPoint.shortName,
            deliveryPoint.number,
            CdqMappings.toTypeOrDefault(deliveryPoint.type)
        )
    }

    fun toRequest(geoCoords: GeoCoordinatesCdq): GeoCoordinateDto {
        return GeoCoordinateDto(geoCoords.longitude, geoCoords.latitude, 0.0f)
    }

}