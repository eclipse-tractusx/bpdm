package org.eclipse.tractusx.bpdm.pool.component.cdq.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.dto.response.SyncResponse
import org.eclipse.tractusx.bpdm.pool.entity.SyncType
import org.eclipse.tractusx.bpdm.pool.service.SyncRecordService
import org.eclipse.tractusx.bpdm.pool.service.toDto
import org.springframework.stereotype.Service

/**
 * Starts the partner import either blocking or non-blocking
 */
@Service
class ImportStarterService(
    private val syncRecordService: SyncRecordService,
    private val importService: PartnerImportService
) {

    private val logger = KotlinLogging.logger { }

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

        logger.debug { "Initializing CDQ import starting with ID ${record.errorSave}' for modified records from '$fromTime' with async: ${!inSync}" }

        if (inSync)
            importService.importPaginated(fromTime, saveState)
        else
            importService.importPaginatedAsync(fromTime, saveState)

        return response
    }

}