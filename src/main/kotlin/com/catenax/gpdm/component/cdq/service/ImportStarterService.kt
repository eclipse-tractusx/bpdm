package com.catenax.gpdm.component.cdq.service

import com.catenax.gpdm.dto.response.SyncResponse
import com.catenax.gpdm.entity.SyncType
import com.catenax.gpdm.service.SyncRecordService
import com.catenax.gpdm.service.toDto
import org.springframework.stereotype.Service

/**
 * Starts the partner import either blocking or non-blocking
 */
@Service
class ImportStarterService(
    private val syncRecordService: SyncRecordService,
    private val importService: PartnerImportService
) {

    /**
     * Import records synchronously and return a [SyncResponse] about the import result information
     */
    fun import(): SyncResponse {
        return startImport(true)
    }

    /**
     * Import records asynchronously and return a [SyncResponse] with information about the started import
     */
    fun importAsync(): SyncResponse {
        return startImport(false)
    }

    /**
     * Fetch a [SyncResponse] about the state of the current import
     */
    fun getImportStatus(): SyncResponse {
        return syncRecordService.getOrCreateRecord(SyncType.CDQ_IMPORT).toDto()
    }

    private fun startImport(inSync: Boolean): SyncResponse {
        val record = syncRecordService.getOrCreateRecord(SyncType.CDQ_IMPORT)

        val fromTime = record.startedAt ?: SyncRecordService.syncStartTime
        val saveState = record.errorSave

        val response = syncRecordService.setSynchronizationStart(record).toDto()

        if (inSync)
            importService.importPaginated(fromTime, saveState)
        else
            importService.importPaginatedAsync(fromTime, saveState)

        return response
    }

}