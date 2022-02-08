package com.catenax.gpdm.dto.request

import com.catenax.gpdm.entity.BusinessPartnerType

data class BusinessPartnerRequest (
    val identifiers: Collection<IdentifierRequest> = emptyList(),
    val names: Collection<NameRequest>,
    val legalForm: String,
    val status: BusinessStatusRequest,
    val addresses: Collection<AddressRequest> = emptyList(),
    val profileClassifications: Collection<ClassificationRequest> = emptyList(),
    val types: Collection<BusinessPartnerType> = emptyList(),
    val bankAccounts: Collection<BankAccountRequest> = emptyList()
        )