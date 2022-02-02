package com.catenax.gpdm.service

import com.catenax.gpdm.controller.dto.*
import com.catenax.gpdm.entity.*
import org.springframework.data.domain.Page


fun <S, T> Page<S>.toDto(dtoContent: Collection<T>) : PageResponse<T>{
    return PageResponse(this.totalElements, this.totalPages, this.number, this.size, dtoContent)
}

fun BusinessPartner.toDto() : BusinessPartnerDto{
    return this.toDto(
        toBaseDto(),
        startNodeRelations.map { it.toDto() } + endNodeRelations.map { it.toDto() }
    )
}

fun BusinessPartner.toDto(baseDto: BusinessPartnerBaseDto,
                          relations: Collection<RelationDto>
) : BusinessPartnerDto{
    return BusinessPartnerDto(
        bpn,
        baseDto,
        relations
    )
}


fun BusinessPartner.toBaseDto(): BusinessPartnerBaseDto{
    return toBaseDto(
        identifiers.map { it.toDto() },
        names.map { it.toDto() },
        legalForm.toDto(),
        addresses.map { it.toDto() },
        if(classification.isNotEmpty()) ProfileDto(classification.map { it.toDto() }) else null,
        bankAccounts.map { it.toDto() }
    )
}

fun BusinessPartner.toBaseDto(
    identifiers: Collection<IdentifierDto>,
    names: Collection<NameDto>,
    legalForm: LegalFormDto,
    addresses: Collection<AddressDto>,
    profile: ProfileDto?,
    bankAccounts: Collection<BankAccountDto>
): BusinessPartnerBaseDto{
    return BusinessPartnerBaseDto(identifiers, names, legalForm, status, addresses, profile, types, bankAccounts, roles)
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
    careOf: BaseNamedDto?,
    administrativeAreas: Collection<AdministrativeAreaDto>,
    postCodes: Collection<PostCodeDto>,
    localities: Collection<LocalityDto>,
    thoroughfares: Collection<ThoroughfareDto>,
    premises: Collection<PremiseDto>,
    postalDeliveryPoints: Collection<PostalDeliveryPointDto>,
    versions: Collection<AddressVersionDto>
): AddressDto{
    return AddressDto(careOf, country, administrativeAreas, postCodes, localities,
        thoroughfares, premises, postalDeliveryPoints, type, versions)
}

fun AdministrativeArea.toDto(): AdministrativeAreaDto{
    return toDto(codes.map { it.toDto() })
}

fun AdministrativeArea.toDto(codes: Collection<AdministrativeAreaCodeDto>): AdministrativeAreaDto{
    return AdministrativeAreaDto(this.value, codes, this.type)
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
