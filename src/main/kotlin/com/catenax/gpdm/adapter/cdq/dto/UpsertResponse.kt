package com.catenax.gpdm.adapter.cdq.dto

data class UpsertResponse(
    val failures: Collection<UpsertFailure> = emptyList(),
    val featuresOn: Collection<String>,
    val numberOfAccepted: Int,
    val numberOfFailed: Int
)
