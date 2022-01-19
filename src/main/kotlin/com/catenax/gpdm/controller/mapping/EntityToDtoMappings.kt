package com.catenax.gpdm.controller.mapping

import com.catenax.gpdm.controller.dto.*
import com.catenax.gpdm.entity.*
import org.springframework.data.domain.Page


fun <S, T> Page<S>.toDto(dtoContent: Collection<T>) : PageResponse<T>{
    return PageResponse(this.totalElements, this.totalPages, this.number, this.size, dtoContent)
}

fun BusinessPartner.toDto() : BusinessPartnerDto{
    return this.toDto(this.identifiers.map { it.toDto() },
        this.names.map { it.toDto() },
        this.legalForm.toDto())
}

fun BusinessPartner.toDto(identifiers: Collection<IdentifierDto>,
                          names: Collection<NameDto>,
                          legalForm: LegalFormDto) : BusinessPartnerDto{
    return BusinessPartnerDto(this.bpn, identifiers, names, legalForm)
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



fun BaseNamedEntity.toNamedDto(): BaseNamedDto{
    return BaseNamedDto(this.value, this.shortName, this.number)
}
