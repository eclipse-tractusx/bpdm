package org.eclipse.tractusx.bpdm.pool.component.cdq.dto

data class UpsertRequest(
    val datasource: String,
    val businessPartners: Collection<BusinessPartnerCdq>,
    val featuresOn: Collection<CdqFeatures> = emptyList(),
    val featuresOff: Collection<CdqFeatures> = emptyList()
)

enum class CdqFeatures(){
    UPSERT_BY_EXTERNAL_ID,
    API_ERROR_ON_FAILURES,
    LAB_USE_QUEUES,
    ENABLE_PRECURATION,
    TRANSFORM_RECORD,
    ENABLE_SETTINGS,
    ENABLE_ASYNC
}
