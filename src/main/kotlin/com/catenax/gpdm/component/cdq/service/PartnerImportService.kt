package com.catenax.gpdm.component.cdq.service

import com.catenax.gpdm.entity.SyncType
import com.catenax.gpdm.service.SyncRecordService
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import javax.persistence.EntityManager

/**
 * Imports business partner entries from CDQ
 */
@Service
class PartnerImportService(
    private val syncRecordService: SyncRecordService,
    private val partnerImportPageService: PartnerImportPageService,
    private val entityManager: EntityManager
) {
    /**
     * Asynchronous version of [importPaginated]
     */
    @Async
    fun importPaginatedAsync(fromTime: OffsetDateTime, saveState: String?) {
        importPaginated(fromTime, saveState)
    }

    /**
     * Import CDQ partner records last modified after [fromTime] and with CDQ internal ID greater than [saveState]
     *
     * Data is imported in a paginated way. On an error during a page import the latest [saveState] ID is saved so that the import can later be resumed
     */
    fun importPaginated(fromTime: OffsetDateTime, saveState: String?) {
        var startAfter: String? = saveState
        var importedCount = 0

        do {
            try {
                val response = partnerImportPageService.import(fromTime, startAfter)
                startAfter = response.nextStartAfter
                importedCount += response.partners.size
                val progress = importedCount / response.totalElements.toFloat()
                syncRecordService.setProgress(syncRecordService.getOrCreateRecord(SyncType.CDQ_IMPORT), importedCount, progress)

            } catch (exception: RuntimeException) {
                syncRecordService.setSynchronizationError(syncRecordService.getOrCreateRecord(SyncType.CDQ_IMPORT), exception.message!!, startAfter)
                throw exception
            }

            //Clear session after each page import to improve JPA performance
            entityManager.clear()
        } while (startAfter != null)

        syncRecordService.setSynchronizationSuccess(syncRecordService.getOrCreateRecord(SyncType.CDQ_IMPORT))
    }
}