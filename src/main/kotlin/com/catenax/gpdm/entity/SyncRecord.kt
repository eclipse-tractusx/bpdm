package com.catenax.gpdm.entity

import java.time.OffsetDateTime
import javax.persistence.*

@Entity
@Table(name = "sync_records")
class SyncRecord(
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, unique = true)
    var type: SyncType,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: SyncStatus,
    @Column(name = "progress", nullable = false)
    var progress: Float = 0f,
    @Column(name = "status_details")
    var errorDetails: String? = null,
    @Column(name = "save_state")
    var errorSave: String? = null,
    @Column(name = "started_at")
    var startedAt: OffsetDateTime? = null,
    @Column(name = "finished_at")
    var finishedAt: OffsetDateTime? = null,

    ): BaseEntity()


enum class SyncType{
    ELASTIC,
    CDQ_IMPORT,
    CDQ_EXPORT
}

enum class SyncStatus{
    NOT_SYNCHED,
    RUNNING,
    SUCCESS,
    ERROR
}