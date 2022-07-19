package org.eclipse.tractusx.bpdm.pool.dto.request

import io.swagger.v3.oas.annotations.media.Schema

data class SiteSearchRequest(
    @Schema(description = "Filter sites that should belong to legal entities (specified by BPNL)")
    val legalEntities: Collection<String>
)
