package org.eclipse.tractusx.bpdm.pool.component.cdq.dto

data class ImportResponse(
    val importedSize: Int,
    val partnerBpns: Collection<String>
)
