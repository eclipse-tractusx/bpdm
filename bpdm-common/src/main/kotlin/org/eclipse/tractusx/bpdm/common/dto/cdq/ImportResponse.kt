package org.eclipse.tractusx.bpdm.common.dto.cdq

data class ImportResponse(
    val importedSize: Int,
    val partnerBpns: Collection<String>
)
