package com.catenax.gpdm.controller.mapping

import com.catenax.gpdm.controller.dto.*
import com.catenax.gpdm.entity.BaseNamedEntity
import com.catenax.gpdm.entity.BusinessPartner
import com.catenax.gpdm.entity.Identifier
import com.catenax.gpdm.entity.Registration
import org.springframework.data.domain.Page


fun <S, T> Page<S>.toDto(dtoContent: Collection<T>) : PageResponse<T>{
    return PageResponse(this.totalElements, this.totalPages, this.number, this.size, dtoContent)
}

fun BusinessPartner.toDto() : BusinessPartnerDto{
    return this.toDto(this.identifiers.map { it.toDto() })
}

fun BusinessPartner.toDto(identifiers: Collection<IdentifierDto>) : BusinessPartnerDto{
    return BusinessPartnerDto(this.bpn, identifiers)
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

fun BaseNamedEntity.toNamedDto(): BaseNamedDto{
    return BaseNamedDto(this.value, this.shortName, this.number)
}
