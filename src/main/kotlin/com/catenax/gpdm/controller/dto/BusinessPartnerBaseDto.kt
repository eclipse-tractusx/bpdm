package com.catenax.gpdm.controller.dto

import com.catenax.gpdm.entity.BusinessPartnerStatus
import com.catenax.gpdm.entity.BusinessPartnerTypes
import com.fasterxml.jackson.annotation.JsonProperty

data class BusinessPartnerBaseDto (
    var identifiers: Collection<IdentifierDto>,
    var names: Collection<NameDto>,
    var legalForm: LegalFormDto,
    var status: BusinessPartnerStatus?,
    var addresses: Collection<AddressDto>,
    var profile: ProfileDto?,
    var types: Collection<BusinessPartnerTypes>,
    var bankAccounts: Collection<BankAccountDto>,
    var roles: Collection<String>
)