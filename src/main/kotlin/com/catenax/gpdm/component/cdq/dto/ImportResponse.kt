package com.catenax.gpdm.component.cdq.dto

data class ImportResponse(
    val importedSize: Int,
    val partnerBpns: Collection<String>
)
