package org.eclipse.tractusx.bpdm.common.dto.cdq

data class UpsertResponse(
    val failures: Collection<UpsertFailure> = emptyList(),
    val featuresOn: Collection<String>,
    val numberOfAccepted: Int,
    val numberOfFailed: Int
)
