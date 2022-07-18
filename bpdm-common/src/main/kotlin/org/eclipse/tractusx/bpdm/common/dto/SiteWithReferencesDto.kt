package org.eclipse.tractusx.bpdm.common.dto

data class SiteWithReferencesDto(
    val site: SiteDto,
    val legalEntityExternalId: String?,
)