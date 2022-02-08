package com.catenax.gpdm.service

import com.catenax.gpdm.dto.GeoCoordinateDto
import com.catenax.gpdm.dto.response.*
import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.dto.response.type.TypeKeyNameUrlDto
import com.catenax.gpdm.dto.response.type.TypeNameUrlDto
import com.catenax.gpdm.entity.*
import com.neovisionaries.i18n.CountryCode
import com.neovisionaries.i18n.CurrencyCode
import com.neovisionaries.i18n.LanguageCode
import org.springframework.data.domain.Page


fun <S, T> Page<S>.toDto(dtoContent: Collection<T>) : PageResponse<T> {
    return PageResponse(this.totalElements, this.totalPages, this.number, this.numberOfElements, dtoContent)
}

fun <T: NamedUrlType> T.toDto(): TypeKeyNameUrlDto<T>{
    return TypeKeyNameUrlDto(this, getTypeName(), getUrl())
}

fun <T: NamedType> T.toDto(): TypeKeyNameDto<T>{
    return TypeKeyNameDto(this, getTypeName())
}

fun LanguageCode.toDto(): TypeKeyNameDto<LanguageCode> {
    return TypeKeyNameDto(this, getName())
}

fun CountryCode.toDto(): TypeKeyNameDto<CountryCode> {
    return TypeKeyNameDto(this, getName())
}

fun CurrencyCode.toDto(): TypeKeyNameDto<CurrencyCode> {
    return TypeKeyNameDto(this, getName())
}

fun BusinessPartner.toDto(): BusinessPartnerResponse {
    return toDto(
        identifiers.map { it.toDto() },
        names.map { it.toDto() },
        legalForm.toDto(),
        stati.maxWithOrNull(compareBy{it.validFrom})!!.toDto(),
        addresses.map { it.toDto() },
        classification.map { it.toDto() },
        types.map { it.toDto() },
        bankAccounts.map { it.toDto() },
        roles.map { it.toDto() },
        startNodeRelations.map { it.toDto() }.plus(endNodeRelations.map { it.toDto() })
    )
}

fun BusinessPartner.toDto(
    identifiers: Collection<IdentifierResponse>,
    names: Collection<NameResponse>,
    legalForm: LegalFormResponse,
    status: BusinessStatusResponse,
    addresses: Collection<AddressResponse>,
    classifications: Collection<ClassificationResponse>,
    types: Collection<TypeKeyNameUrlDto<BusinessPartnerType>>,
    bankAccounts: Collection<BankAccountResponse>,
    roles: Collection<TypeKeyNameDto<String>>,
    relations: Collection<RelationResponse>
): BusinessPartnerResponse {
    return BusinessPartnerResponse(bpn, identifiers, names, legalForm, status, addresses, classifications, types, bankAccounts, roles, relations)
}

fun Identifier.toDto(): IdentifierResponse {
    return toDto(type.toDto(), issuingBody.toDto(), status.toDto())
}

fun Identifier.toDto(type: TypeKeyNameUrlDto<String>, issuingBody: TypeKeyNameUrlDto<String>, status: TypeKeyNameDto<IdentifierStatus>): IdentifierResponse {
    return IdentifierResponse(value, type, issuingBody, status)
}

fun IdentifierType.toDto(): TypeKeyNameUrlDto<String> {
    return TypeKeyNameUrlDto(technicalKey, name, url)
}

fun IssuingBody.toDto(): TypeKeyNameUrlDto<String> {
    return TypeKeyNameUrlDto(technicalKey, name, url)
}

fun Name.toDto(): NameResponse {
    return NameResponse(value, shortName, type.toDto(), language.toDto())
}

fun LegalForm.toDto(): LegalFormResponse {
    return LegalFormResponse(technicalKey, name, url, mainAbbreviation, language.toDto(), categories.map { it.toDto() })
}

fun LegalFormCategory.toDto(): TypeNameUrlDto {
    return TypeNameUrlDto(name, url)
}

fun BusinessStatus.toDto(): BusinessStatusResponse {
    return BusinessStatusResponse(officialDenotation, validFrom, validUntil, type.toDto())
}

fun Role.toDto(): TypeKeyNameDto<String> {
    return TypeKeyNameDto(technicalKey, name)
}

fun Address.toDto(): AddressResponse {
    return AddressResponse(
        version.toDto(),
        careOf,
        contexts,
        country.toDto(),
        administrativeAreas.map { it.toDto() },
        postCodes.map { it.toDto() },
        localities.map { it.toDto() },
        thoroughfares.map { it.toDto() },
        premises.map { it.toDto() },
        postalDeliveryPoints.map { it.toDto() },
        geoCoordinates?.toDto(),
        types.map { it.toDto() }
    )
}



fun AdministrativeArea.toDto(): AdministrativeAreaResponse {
    return AdministrativeAreaResponse(value, shortName, fipsCode, type.toDto(), language.toDto())
}


fun PostCode.toDto(): PostCodeResponse {
    return PostCodeResponse(value, type.toDto())
}

fun Locality.toDto(): LocalityResponse {
    return LocalityResponse(value, shortName, localityType.toDto(), language.toDto())
}

fun Thoroughfare.toDto(): ThoroughfareResponse {
    return ThoroughfareResponse(value, name, shortName, number, direction, type.toDto(), language.toDto())
}

fun Premise.toDto(): PremiseResponse {
    return PremiseResponse(value, shortName, number, type.toDto(), language.toDto())
}

fun PostalDeliveryPoint.toDto(): PostalDeliveryPointResponse {
    return PostalDeliveryPointResponse(value, shortName, number, type.toDto(), language.toDto())
}

fun AddressVersion.toDto(): AddressVersionResponse {
    return AddressVersionResponse(characterSet.toDto(), language.toDto())
}

fun GeographicCoordinate.toDto(): GeoCoordinateDto {
    return GeoCoordinateDto(longitude, latitude, altitude)
}

fun Classification.toDto(): ClassificationResponse {
    return ClassificationResponse(value, code, type.toDto())
}

fun ClassificationType.toDto(): TypeNameUrlDto{
    return TypeNameUrlDto(name, url)
}

fun Relation.toDto(): RelationResponse {
    return RelationResponse(relationClass.toDto(), type.toDto(), startNode.bpn, endNode.bpn, startedAt, endedAt)
}

fun BankAccount.toDto(): BankAccountResponse {
    return BankAccountResponse(trustScores, currency.toDto(), internationalBankAccountIdentifier, internationalBankIdentifier,
        nationalBankAccountIdentifier, nationalBankIdentifier)
}

