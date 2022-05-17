package com.catenax.gpdm.service

import com.catenax.gpdm.entity.SyncRecord
import com.catenax.gpdm.entity.SyncStatus
import com.catenax.gpdm.entity.SyncType
import com.catenax.gpdm.exception.BpdmSyncConflictException
import com.catenax.gpdm.exception.BpdmSyncStateException
import com.catenax.gpdm.repository.SyncRecordRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class SyncRecordService(
    private val syncRecordRepository: SyncRecordRepository
) {

    companion object {
        val syncStartTime = LocalDateTime.of(2000, 1, 1, 0, 0).toInstant(ZoneOffset.UTC)
    }

    fun getOrCreateRecord(type: SyncType): SyncRecord {
        return syncRecordRepository.findByType(type) ?: run {
            val newEntry = SyncRecord(
                type,
                SyncStatus.NOT_SYNCED
            )
            syncRecordRepository.save(newEntry)
        }
    }

    fun setSynchronizationStart(record: SyncRecord): SyncRecord {
        if (record.status == SyncStatus.RUNNING)
            throw BpdmSyncConflictException(SyncType.CDQ_IMPORT)

        record.progress = if (record.status == SyncStatus.ERROR) record.progress else 0f
        record.count = if (record.status == SyncStatus.ERROR) record.count else 0
        record.startedAt = Instant.now()
        record.finishedAt = null
        record.status = SyncStatus.RUNNING
        record.errorDetails = null


        return syncRecordRepository.save(record)
    }

    fun setSynchronizationSuccess(record: SyncRecord): SyncRecord{
        if(record.status != SyncStatus.RUNNING)
            throw BpdmSyncStateException("Synchronization of type ${record.type} can't switch from state ${record.status} to ${SyncStatus.SUCCESS}.")

        record.finishedAt = Instant.now()
        record.progress = 1f
        record.status = SyncStatus.SUCCESS
        record.errorDetails = null
        record.errorSave = null

        return syncRecordRepository.save(record)
    }

    fun setSynchronizationError(record: SyncRecord, errorMessage: String, saveState: String?): SyncRecord {
        record.finishedAt = Instant.now()
        record.status = SyncStatus.ERROR
        record.errorDetails = errorMessage.take(255)
        record.errorSave = saveState

        return syncRecordRepository.save(record)
    }

    fun setProgress(record: SyncRecord, count: Int, progress: Float): SyncRecord {
        if (record.status != SyncStatus.RUNNING)
            throw BpdmSyncStateException("Synchronization of type ${record.type} can't change progress when not running.")

        record.count = count
        record.progress = progress

        return syncRecordRepository.save(record)
    }

    fun reset(record: SyncRecord): SyncRecord {
        record.status = SyncStatus.NOT_SYNCED
        record.errorDetails = null
        record.errorSave = null
        record.startedAt = null
        record.finishedAt = null
        record.count = 0
        record.progress = 0f

        return syncRecordRepository.save(record)
    }

}