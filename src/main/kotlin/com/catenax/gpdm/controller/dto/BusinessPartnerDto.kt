package com.catenax.gpdm.controller.dto

import com.catenax.gpdm.entity.BusinessPartnerStatus

data class BusinessPartnerDto (
    val bpn: String,
    val identifiers: Collection<IdentifierDto>,
    val names: Collection<NameDto>,
    val legalForm: LegalFormDto,
    val status: BusinessPartnerStatus?
)