package com.catenax.gpdm.dto.response


import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.dto.response.type.TypeKeyNameUrlDto
import com.catenax.gpdm.entity.BusinessPartnerType

data class BusinessPartnerResponse (
    val bpn: String,
    val identifiers: Collection<IdentifierResponse>,
    val names: Collection<NameResponse>,
    val legalForm: LegalFormResponse?,
    val status: BusinessStatusResponse?,
    val addresses: Collection<AddressResponse>,
    val profileClassifications:  Collection<ClassificationResponse>,
    val types: Collection<TypeKeyNameUrlDto<BusinessPartnerType>>,
    val bankAccounts: Collection<BankAccountResponse>,
    val roles: Collection<TypeKeyNameDto<String>>,
    val relations: Collection<RelationResponse>
)
