package com.catenax.gpdm.service

import com.catenax.gpdm.entity.SyncRecord
import com.catenax.gpdm.entity.SyncStatus
import com.catenax.gpdm.entity.SyncType
import com.catenax.gpdm.exception.BpdmSyncConflictException
import com.catenax.gpdm.exception.BpdmSyncStateException
import com.catenax.gpdm.repository.SyncRecordRepository
import mu.KotlinLogging
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

    private val logger = KotlinLogging.logger { }

    fun getOrCreateRecord(type: SyncType): SyncRecord {
        return syncRecordRepository.findByType(type) ?: run {
            logger.info { "Create new sync record entry for type $type" }
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

        logger.debug { "Set sync of type ${record.type} to status ${SyncStatus.RUNNING}" }

        record.progress = if (record.status == SyncStatus.ERROR) record.progress else 0f
        record.count = if (record.status == SyncStatus.ERROR) record.count else 0
        record.startedAt = Instant.now()
        record.finishedAt = null
        record.status = SyncStatus.RUNNING
        record.errorDetails = null


        return syncRecordRepository.save(record)
    }

    fun setSynchronizationSuccess(record: SyncRecord): SyncRecord {
        if (record.status != SyncStatus.RUNNING)
            throw BpdmSyncStateException("Synchronization of type ${record.type} can't switch from state ${record.status} to ${SyncStatus.SUCCESS}.")

        logger.debug { "Set sync of type ${record.type} to status ${SyncStatus.SUCCESS}" }

        record.finishedAt = Instant.now()
        record.progress = 1f
        record.status = SyncStatus.SUCCESS
        record.errorDetails = null
        record.errorSave = null

        return syncRecordRepository.save(record)
    }

    fun setSynchronizationError(record: SyncRecord, errorMessage: String, saveState: String?): SyncRecord {
        logger.debug { "Set sync of type ${record.type} to status ${SyncStatus.ERROR} with message $errorMessage" }

        record.finishedAt = Instant.now()
        record.status = SyncStatus.ERROR
        record.errorDetails = errorMessage.take(255)
        record.errorSave = saveState

        return syncRecordRepository.save(record)
    }

    fun setProgress(record: SyncRecord, count: Int, progress: Float): SyncRecord {
        if (record.status != SyncStatus.RUNNING)
            throw BpdmSyncStateException("Synchronization of type ${record.type} can't change progress when not running.")

        logger.debug { "Update progress of sync type ${record.type} to $progress" }

        record.count = count
        record.progress = progress

        return syncRecordRepository.save(record)
    }

    fun reset(record: SyncRecord): SyncRecord {
        logger.debug { "Reset sync status of type ${record.type}" }

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