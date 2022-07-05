package org.eclipse.tractusx.bpdm.common.dto.cdq

import java.time.LocalDateTime

data class BusinessPartnerCdq (
    val id: String? = null,
    val createdAt: LocalDateTime? = null,
    val lastModifiedAt: LocalDateTime? = null,
    val externalId: String? = null,
    val dataSource: String? = null,
    val disclosed: Boolean? = false,
    val updateMonitoring: Boolean? = false,
    val metadata: BusinessPartnerMetadataCdq? = null,
    val record: String? = null,
    val names: Collection<NameCdq> = emptyList(),
    val legalForm: LegalFormCdq? = null,
    val identifiers: Collection<IdentifierCdq> = emptyList(),
    val categories: Collection<TypeKeyNameUrlCdq> = emptyList(),
    val status: BusinessPartnerStatusCdq? = null,
    val profile: PartnerProfileCdq? = null,
    val relations: Collection<RelationCdq> = emptyList(),
    val types: Collection<TypeKeyNameUrlCdq> = emptyList(),
    val addresses: Collection<AddressCdq> = emptyList(),
    val bankAccounts: Collection<BankAccountCdq> = emptyList(),
)