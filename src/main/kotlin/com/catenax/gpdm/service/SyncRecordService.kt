package com.catenax.gpdm.service

import com.catenax.gpdm.entity.SyncRecord
import com.catenax.gpdm.entity.SyncStatus
import com.catenax.gpdm.entity.SyncType
import com.catenax.gpdm.exception.BpdmSyncConflictException
import com.catenax.gpdm.exception.BpdmSyncStateException
import com.catenax.gpdm.repository.SyncRecordRepository
import org.springframework.stereotype.Service
import java.time.*
import java.time.format.DateTimeFormatter

@Service
class SyncRecordService(
    private val syncRecordRepository: SyncRecordRepository
) {

    companion object {
        val syncStartTime = OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    }

    fun getOrCreateRecord(type: SyncType): SyncRecord{
        return syncRecordRepository.findByType(type)?: run {
            val newEntry = SyncRecord(
                type,
                SyncStatus.NOT_SYNCHED
            )
            syncRecordRepository.save(newEntry)
        }
    }

    fun setSynchronizationStart(record: SyncRecord): SyncRecord{
        if(record.status == SyncStatus.RUNNING)
            throw BpdmSyncConflictException(SyncType.CDQ_IMPORT)

        record.progress = if(record.status == SyncStatus.ERROR) record.progress else 0f
        record.startedAt = Instant.now().atOffset(ZoneOffset.UTC)
        record.status = SyncStatus.RUNNING
        record.errorDetails = null

        return syncRecordRepository.save(record)
    }

    fun setSynchronizationSuccess(record: SyncRecord): SyncRecord{
        if(record.status != SyncStatus.RUNNING)
            throw BpdmSyncStateException("Synchronization of type ${record.type} can't switch from state ${record.status} to ${SyncStatus.SUCCESS}.")

        record.finishedAt = Instant.now().atOffset(ZoneOffset.UTC)
        record.progress = 1f
        record.status = SyncStatus.SUCCESS
        record.errorDetails = null
        record.errorSave = null

        return syncRecordRepository.save(record)
    }

    fun setSynchronizationError(record: SyncRecord, errorMessage: String, saveState: String?): SyncRecord{
        record.finishedAt = Instant.now().atOffset(ZoneOffset.UTC)
        record.status = SyncStatus.ERROR
        record.errorDetails = errorMessage
        record.errorSave = saveState

        return syncRecordRepository.save(record)
    }

    fun setProgress(record: SyncRecord, progress: Float): SyncRecord{
        if(record.status != SyncStatus.RUNNING)
            throw BpdmSyncStateException("Synchronization of type ${record.type} can't change progress when not running.")

        record.progress = progress

        return syncRecordRepository.save(record)
    }




}