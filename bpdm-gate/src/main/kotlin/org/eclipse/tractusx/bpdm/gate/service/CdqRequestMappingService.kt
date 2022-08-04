package org.eclipse.tractusx.bpdm.gate.service

import com.neovisionaries.i18n.CountryCode
import com.neovisionaries.i18n.LanguageCode
import org.eclipse.tractusx.bpdm.common.dto.*
import org.eclipse.tractusx.bpdm.common.dto.cdq.*
import org.eclipse.tractusx.bpdm.common.model.AddressType
import org.eclipse.tractusx.bpdm.common.model.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.model.CharacterSet
import org.eclipse.tractusx.bpdm.gate.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.gate.config.CdqConfigProperties
import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityGateInput
import org.springframework.stereotype.Service

@Service
class CdqRequestMappingService(
    private val bpnConfigProperties: BpnConfigProperties,
    private val cdqConfigProperties: CdqConfigProperties
) {
    fun toCdqModel(legalEntity: LegalEntityGateInput): BusinessPartnerCdq {
        return toCdqModel(legalEntity.legalEntity, legalEntity.externalId)
    }

    private fun toCdqModel(legalEntity: LegalEntityDto, externalId: String): BusinessPartnerCdq {
        return BusinessPartnerCdq(
            externalId = externalId,
            dataSource = cdqConfigProperties.datasource,
            identifiers = toIdentifiersCdq(legalEntity.identifiers, legalEntity.bpn),
            names = legalEntity.names.map { it.toCdqModel() },
            legalForm = toLegalFormCdq(legalEntity.legalForm),
            status = legalEntity.status?.toCdqModel(),
            profile = toPartnerProfileCdq(legalEntity.profileClassifications),
            types = legalEntity.types.map { it.toCdqModel() },
            bankAccounts = legalEntity.bankAccounts.map { it.toCdqModel() },
            addresses = listOf(toCdqModel(legalEntity.legalAddress))
        )
    }

    private fun BankAccountDto.toCdqModel(): BankAccountCdq {
        return BankAccountCdq(
            internationalBankAccountIdentifier = internationalBankAccountIdentifier,
            internationalBankIdentifier = internationalBankIdentifier,
            nationalBankAccountIdentifier = nationalBankAccountIdentifier,
            nationalBankIdentifier = nationalBankIdentifier
        )
    }

    private fun BusinessPartnerType.toCdqModel(): TypeKeyNameUrlCdq {
        return TypeKeyNameUrlCdq(name)
    }

    private fun toPartnerProfileCdq(profileClassifications: Collection<ClassificationDto>): PartnerProfileCdq? {
        if (profileClassifications.isEmpty()) {
            return null
        }
        return PartnerProfileCdq(classifications = profileClassifications.map { it.toCdqModel() })
    }

    private fun ClassificationDto.toCdqModel(): ClassificationCdq {
        return ClassificationCdq(
            value = value,
            code = code,
            type = if (type != null) TypeKeyNameUrlCdq(type!!.name) else null
        )
    }

    private fun BusinessStatusDto.toCdqModel(): BusinessPartnerStatusCdq {
        return BusinessPartnerStatusCdq(
            type = TypeKeyNameUrlCdq(type.name),
            officialDenotation = officialDenotation,
            validFrom = validFrom,
            validUntil = validUntil
        )
    }

    private fun toLegalFormCdq(technicalKey: String?) = if (technicalKey != null) LegalFormCdq(technicalKey = technicalKey) else null

    private fun NameDto.toCdqModel(): NameCdq {
        return NameCdq(
            value = value,
            shortName = shortName,
            type = TypeKeyNameUrlCdq(type.name),
            language = toLanguageCdq(this.language)
        )
    }

    private fun IdentifierDto.toCdqModel(): IdentifierCdq {
        return IdentifierCdq(
            type = TypeKeyNameUrlCdq(type),
            value = value,
            issuingBody = TypeKeyNameUrlCdq(issuingBody),
            status = TypeKeyNameCdq(status)
        )
    }

    private fun toCdqModel(address: AddressDto): AddressCdq {
        return with(address) {
            AddressCdq(
                version = toCdqModel(version),
                careOf = toCareOfCdq(careOf),
                contexts = contexts.map { toContextCdq(it) },
                country = toCountryCdq(country),
                administrativeAreas = administrativeAreas.map { toCdqModel(it, version.language) },
                postCodes = postCodes.map { toCdqModel(it) },
                localities = localities.map { toCdqModel(it, version.language) },
                thoroughfares = thoroughfares.map { toCdqModel(it, version.language) },
                postalDeliveryPoints = postalDeliveryPoints.map { toCdqModel(it, version.language) },
                premises = premises.map { toCdqModel(it, version.language) },
                geographicCoordinates = toCdqModel(geographicCoordinates),
                types = toAddressTypesCdq(types)
            )
        }
    }

    private fun toCdqModel(version: AddressVersionDto): AddressVersionCdq? {
        val languageCdq = toLanguageCdq(version.language)
        val characterSetCdq = toCharacterSetCdq(version.characterSet)

        return if (languageCdq == null && characterSetCdq == null) null else AddressVersionCdq(languageCdq, characterSetCdq)
    }


    private fun toCareOfCdq(careOf: String?): WrappedValueCdq? =
        if (careOf != null) WrappedValueCdq(careOf) else null

    private fun toContextCdq(context: String): WrappedValueCdq =
        WrappedValueCdq(context)

    private fun toCdqModel(adminArea: AdministrativeAreaDto, languageCode: LanguageCode): AdministrativeAreaCdq =
        AdministrativeAreaCdq(adminArea.value, adminArea.shortName, toKeyNameUrlTypeCdq(adminArea.type), toLanguageCdq(languageCode))


    private fun toCdqModel(postcode: PostCodeDto): PostCodeCdq =
        PostCodeCdq(postcode.value, toKeyNameUrlTypeCdq(postcode.type))

    private fun toCdqModel(locality: LocalityDto, languageCode: LanguageCode): LocalityCdq =
        LocalityCdq(toKeyNameUrlTypeCdq(locality.type), locality.shortName, locality.value, toLanguageCdq(languageCode))

    private fun toCdqModel(thoroughfare: ThoroughfareDto, languageCode: LanguageCode): ThoroughfareCdq =
        ThoroughfareCdq(
            toKeyNameUrlTypeCdq(thoroughfare.type),
            thoroughfare.shortName,
            thoroughfare.number,
            thoroughfare.value,
            thoroughfare.name,
            thoroughfare.direction,
            toLanguageCdq(languageCode)
        )

    private fun toCdqModel(deliveryPoint: PostalDeliveryPointDto, languageCode: LanguageCode): PostalDeliveryPointCdq =
        PostalDeliveryPointCdq(
            toKeyNameUrlTypeCdq(deliveryPoint.type),
            deliveryPoint.shortName,
            deliveryPoint.number,
            deliveryPoint.value,
            toLanguageCdq(languageCode)
        )

    private fun toCdqModel(premise: PremiseDto, languageCode: LanguageCode): PremiseCdq =
        PremiseCdq(toKeyNameUrlTypeCdq(premise.type), premise.shortName, premise.number, premise.value, toLanguageCdq(languageCode))


    private fun toCdqModel(geoCoordinate: GeoCoordinateDto?): GeoCoordinatesCdq? =
        geoCoordinate?.let { GeoCoordinatesCdq(it.longitude, it.latitude) }

    private fun toIdentifiersCdq(identifiers: Collection<IdentifierDto>, bpn: String?): Collection<IdentifierCdq> {
        var identifiersCdq = identifiers.map { it.toCdqModel() }
        if (bpn != null) {
            identifiersCdq = identifiersCdq.plus(createBpnIdentifierCdq(bpn))
        }
        return identifiersCdq
    }

    private fun toAddressTypesCdq(types: Collection<AddressType>): Collection<TypeKeyNameUrlCdq> {
        val legalAddressTypes = if (!types.contains(AddressType.LEGAL)) types.plus(AddressType.LEGAL) else types
        return legalAddressTypes.map { toKeyNameUrlTypeCdq(it) }
    }


    private fun createBpnIdentifierCdq(bpn: String): IdentifierCdq {
        return IdentifierCdq(
            type = TypeKeyNameUrlCdq(bpnConfigProperties.id, bpnConfigProperties.name),
            value = bpn,
            issuingBody = TypeKeyNameUrlCdq(bpnConfigProperties.agencyKey, bpnConfigProperties.agencyName)
        )
    }

    private inline fun <reified T> toKeyNameTypeCdq(type: Enum<T>): TypeKeyNameCdq where T : Enum<T> =
        TypeKeyNameCdq(type.name, null)

    private inline fun <reified T> toKeyNameUrlTypeCdq(type: Enum<T>): TypeKeyNameUrlCdq where T : Enum<T> =
        TypeKeyNameUrlCdq(type.name, null)

    private fun toLanguageCdq(technicalKey: LanguageCode) =
        if (technicalKey != LanguageCode.undefined) LanguageCdq(technicalKey, null) else null

    private fun toCountryCdq(countryCode: CountryCode) =
        if (countryCode != CountryCode.UNDEFINED) CountryCdq(countryCode, null) else null

    private fun toCharacterSetCdq(characterSet: CharacterSet) =
        if (characterSet != CharacterSet.UNDEFINED) toKeyNameTypeCdq(characterSet) else null

}