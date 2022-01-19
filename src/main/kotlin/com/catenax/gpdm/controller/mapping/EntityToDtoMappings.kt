package com.catenax.gpdm.controller.mapping

import com.catenax.gpdm.controller.dto.*
import com.catenax.gpdm.entity.*
import com.neovisionaries.i18n.CountryCode
import org.springframework.data.domain.Page


fun <S, T> Page<S>.toDto(dtoContent: Collection<T>) : PageResponse<T>{
    return PageResponse(this.totalElements, this.totalPages, this.number, this.size, dtoContent)
}

fun BusinessPartner.toDto() : BusinessPartnerDto{
    return this.toDto(
        identifiers.map { it.toDto() },
        names.map { it.toDto() },
        legalForm.toDto(),
        addresses.map { it.toDto() },
        if(classification.isNotEmpty()) ProfileDto(classification.map { it.toDto() }) else null,
        startNodeRelations.map { it.toDto() } + endNodeRelations.map { it.toDto() },
        bankAccounts.map { it.toDto() }
    )
}

fun BusinessPartner.toDto(identifiers: Collection<IdentifierDto>,
                          names: Collection<NameDto>,
                          legalForm: LegalFormDto,
                          addresses: Collection<AddressDto>,
                          profile: ProfileDto?,
                          relations: Collection<RelationDto>,
                          bankAccounts: Collection<BankAccountDto>
) : BusinessPartnerDto{
    return BusinessPartnerDto(
        bpn,
        identifiers,
        names,
        legalForm,
        status,
        addresses,
        profile,
        relations,
        types,
        bankAccounts
    )
}

fun Identifier.toDto(): IdentifierDto{
    return this.toDto(this.toNamedDto(), this.registration?.toDto())
}

fun Identifier.toDto(nameComponent: BaseNamedDto, registration: RegistrationDto?): IdentifierDto{
    return IdentifierDto(nameComponent, this.type, registration)
}

fun Registration.toDto(): RegistrationDto {
    return this.toDto(this.issuingAgency.toNamedDto())
}
fun Registration.toDto(issuingAgency: BaseNamedDto): RegistrationDto{
    return RegistrationDto(this.hardeningGrade, issuingAgency, this.status, this.initialRegistration, this.lastUpdate)
}

fun Name.toDto(): NameDto{
    return this.toDto(this.toNamedDto())
}

fun Name.toDto(nameComponent: BaseNamedDto): NameDto{
    return NameDto(nameComponent, this.type)
}

fun LegalForm.toDto(): LegalFormDto{
    return this.toDto(this.toNamedDto())
}

fun LegalForm.toDto(nameComponent: BaseNamedDto): LegalFormDto{
    return LegalFormDto(nameComponent, this.type)
}

fun Address.toDto(): AddressDto{
    return this.toDto(
        identifiers.map { it.toDto() },
        careOf?.toNamedDto(),
        administrativeAreas.map { it.toDto() },
        postCodes.map { it.toDto() },
        localities.map { it.toDto() },
        thoroughfares.map { it.toDto() },
        premises.map { it.toDto() },
        postalDeliveryPoints.map { it.toDto() },
        versions.map { it.toDto() }
    )
}

fun Address.toDto(
    identifiers: Collection<IdentifierDto>,
    careOf: BaseNamedDto?,
    administrativeAreas: Collection<AdministrativeAreaDto>,
    postCodes: Collection<PostCodeDto>,
    localities: Collection<LocalityDto>,
    thoroughfares: Collection<ThoroughfareDto>,
    premises: Collection<PremiseDto>,
    postalDeliveryPoints: Collection<PostalDeliveryPointDto>,
    versions: Collection<AddressVersionDto>
): AddressDto{
    return AddressDto(identifiers, careOf, country, administrativeAreas, postCodes, localities,
        thoroughfares, premises, postalDeliveryPoints, type, versions)
}

fun AdministrativeArea.toDto(): AdministrativeAreaDto{
    return AdministrativeAreaDto(this.name, this.codes.map { it.toDto() }, this.type)
}

fun AdministrativeArea.toDto(codes: Collection<AdministrativeAreaCodeDto>): AdministrativeAreaDto{
    return AdministrativeAreaDto(this.name, codes, this.type)
}

fun AdministrativeAreaCode.toDto(): AdministrativeAreaCodeDto{
    return AdministrativeAreaCodeDto(this.value, this.type)
}

fun PostCode.toDto(): PostCodeDto{
    return toDto(toNamedDto())
}

fun PostCode.toDto(nameComponent: BaseNamedDto): PostCodeDto{
    return PostCodeDto(nameComponent, this.type)
}

fun Locality.toDto(): LocalityDto{
    return toDto(toNamedDto())
}

fun Locality.toDto(nameComponent: BaseNamedDto): LocalityDto{
    return LocalityDto(nameComponent, this.localityType)
}

fun Thoroughfare.toDto(): ThoroughfareDto{
    return toDto(toNamedDto())
}

fun Thoroughfare.toDto(nameComponent: BaseNamedDto): ThoroughfareDto{
    return ThoroughfareDto(nameComponent, this.type)
}

fun Premise.toDto(): PremiseDto{
    return toDto(toNamedDto())
}

fun Premise.toDto(nameComponent: BaseNamedDto): PremiseDto{
    return PremiseDto(nameComponent, this.type)
}

fun PostalDeliveryPoint.toDto(): PostalDeliveryPointDto{
    return toDto(toNamedDto())
}

fun PostalDeliveryPoint.toDto(nameComponent: BaseNamedDto): PostalDeliveryPointDto{
    return PostalDeliveryPointDto(nameComponent, this.type)
}

fun AddressVersion.toDto(): AddressVersionDto{
    return AddressVersionDto(this.characterSet, this.languageCode)
}

fun Classification.toDto(): ClassificationDto{
    return toDto(toNamedDto())
}

fun Classification.toDto(nameComponent: BaseNamedDto): ClassificationDto{
    return ClassificationDto(nameComponent, this.type)
}

fun Relation.toDto(): RelationDto{
    return RelationDto(relationClass, type, startNode.bpn, endNode.bpn, startedAt, endedAt)
}

fun BankAccount.toDto(): BankAccountDto{
    return BankAccountDto(trustScores, currency, internationalBankAccountIdentifier, internationalBankIdentifier,
        nationalBankAccountIdentifier, nationalBankIdentifier)
}

fun BaseNamedEntity.toNamedDto(): BaseNamedDto{
    return BaseNamedDto(this.value, this.shortName, this.number)
}
