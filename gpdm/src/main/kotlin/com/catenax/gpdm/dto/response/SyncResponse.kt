package com.catenax.gpdm.dto.response

import com.catenax.gpdm.entity.SyncStatus
import com.catenax.gpdm.entity.SyncType
import java.time.Instant

data class SyncResponse(
    val type: SyncType,
    val status: SyncStatus,
    val count: Int = 0,
    val progress: Float = 0f,
    val errorDetails: String? = null,
    val startedAt: Instant? = null,
    val finishedAt: Instant? = null
)