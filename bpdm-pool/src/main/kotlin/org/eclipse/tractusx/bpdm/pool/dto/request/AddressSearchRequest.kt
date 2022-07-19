package org.eclipse.tractusx.bpdm.pool.dto.request

import io.swagger.v3.oas.annotations.media.Schema

data class AddressSearchRequest(
    @Schema(description = "Filter by Business Partner Numbers of legal entities which are at that address")
    val legalEntities: Collection<String> = emptyList(),
    @Schema(description = "Filter by Business Partner Numbers of sites which are at that address")
    val sites: Collection<String> = emptyList()
)
