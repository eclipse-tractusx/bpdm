package com.catenax.gpdm.adapter.cdq.dto

data class SharingStatusCdq(
    val status: SharingStatusType,
    val description: String?
)

enum class SharingStatusType(){
    UNDER_CONSIDERATION,
    UNDISCLOSED_RECORD,
    MISSING_INFORMATION_FOR_LINKAGE,
    ERRONEOUS_INFORMATION_FOR_LINKAGE,
    ERRONEOUS_RECORD,
    NATURAL_PERSON,
    PENDING_LINKAGE_DECISION,
    SHARED_WITH_NO_MATCH,
    SHARED_WITH_CONFIDENT_MATCH,
    SHARED_WITH_NO_MATCH_BY_REVIEW,
    SHARED_BY_REVIEW,
    PROCESS_ISSUE,
    VALID_OOS
}
