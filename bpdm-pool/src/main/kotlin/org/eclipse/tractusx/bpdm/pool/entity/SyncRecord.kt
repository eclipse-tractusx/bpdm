package org.eclipse.tractusx.bpdm.pool.entity

import java.time.Instant
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
    @Column(name = "count", nullable = false)
    var count: Int = 0,
    @Column(name = "status_details")
    var errorDetails: String? = null,
    @Column(name = "save_state")
    var errorSave: String? = null,
    @Column(name = "started_at")
    var startedAt: Instant? = null,
    @Column(name = "finished_at")
    var finishedAt: Instant? = null,

    ): BaseEntity()


enum class SyncType{
    OPENSEARCH,
    CDQ_IMPORT
}

enum class SyncStatus{
    NOT_SYNCED,
    RUNNING,
    SUCCESS,
    ERROR
}