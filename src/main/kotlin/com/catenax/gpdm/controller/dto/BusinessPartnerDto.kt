package com.catenax.gpdm.controller.dto

data class BusinessPartnerDto (
    val bpn: String,
    val identifiers: Collection<IdentifierDto>,
    val names: Collection<NameDto>
)