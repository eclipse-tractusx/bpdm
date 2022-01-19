package com.catenax.gpdm.controller.dto

import com.catenax.gpdm.entity.BusinessPartnerStatus
import com.catenax.gpdm.entity.BusinessPartnerTypes

data class BusinessPartnerDto (
    val bpn: String,
    val identifiers: Collection<IdentifierDto>,
    val names: Collection<NameDto>,
    val legalForm: LegalFormDto,
    val status: BusinessPartnerStatus?,
    val addresses: Collection<AddressDto>,
    val profile: ProfileDto?,
    val relations: Collection<RelationDto>,
    val types: Collection<BusinessPartnerTypes>,
    val bankAccounts: Collection<BankAccountDto>,
    val roles: Collection<String>
)