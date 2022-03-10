package com.catenax.gpdm.component.cdq.dto

import java.time.LocalDateTime

data class BusinessPartnerCdq (
    val id: String,
    val createdAt: LocalDateTime,
    val lastModifiedAt: LocalDateTime,
    val externalId: String?,
    val dataSource: String,
    val disclosed: Boolean = false,
    val updateMonitoring: Boolean = false,
    val metadata: BusinessPartnerMetadataCdq?,
    val record: String?,
    val names: Collection<NameCdq> = emptyList(),
    val legalForm: LegalFormCdq?,
    val identifiers: Collection<IdentifierCdq> = emptyList(),
    val categories: Collection<TypeKeyNameUrlCdq> = emptyList(),
    val status: BusinessPartnerStatusCdq?,
    val profile: PartnerProfileCdq?,
    val relations: Collection<RelationCdq> = emptyList(),
    val types: Collection<TypeKeyNameUrlCdq> = emptyList(),
    val addresses: Collection<AddressCdq> = emptyList(),
    val bankAccounts: Collection<BankAccountCdq> = emptyList(),
    )