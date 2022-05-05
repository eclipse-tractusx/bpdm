package com.catenax.gpdm.dto.elastic

data class ExportResponse(
    val exportedSize: Int,
    val exportedBpns: Collection<String>
)
