package com.catenax.gpdm.dto.response

import com.catenax.gpdm.entity.SyncStatus
import com.catenax.gpdm.entity.SyncType
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.persistence.Column

data class SyncResponse (
    val type: SyncType,
    val status: SyncStatus,
    val progress: Float = 0f,
    val errorDetails: String? = null,
    val startedAt: OffsetDateTime? = null,
    val finishedAt: OffsetDateTime? = null
        )