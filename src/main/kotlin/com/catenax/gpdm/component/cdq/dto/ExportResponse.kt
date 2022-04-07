package com.catenax.gpdm.component.cdq.dto

data class ExportResponse(
    val exportedSize: Int,
    val partnerBpns: Collection<String>
)
