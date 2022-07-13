package org.eclipse.tractusx.bpdm.common.dto.cdq

data class FetchRequest(
    val datasource: String,
    val externalId: String,
    val featuresOn: Collection<CdqFeatures> = emptyList(),
    val featuresOff: Collection<CdqFeatures> = emptyList()
) {
    enum class CdqFeatures {
        FETCH_RECORD,
        FETCH_RELATIONS
    }
}