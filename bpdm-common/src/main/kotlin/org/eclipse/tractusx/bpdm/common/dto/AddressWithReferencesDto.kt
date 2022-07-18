package org.eclipse.tractusx.bpdm.common.dto

data class AddressWithReferencesDto(
    val address: AddressDto,
    val legalEntityExternalId: String?,
    val siteExternalId: String?
)