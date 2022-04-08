package com.catenax.gpdm.component.cdq.service

import com.catenax.gpdm.dto.response.SyncResponse
import com.catenax.gpdm.entity.SyncType
import com.catenax.gpdm.service.SyncRecordService
import com.catenax.gpdm.service.toDto
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
    partnerImportPageService: PartnerImportPageService,
    entityManager: EntityManager
) {
    private val starter = ImportStarter(partnerImportPageService, entityManager, syncRecordService)

    /**
     * Import records synchronously and return a [SyncResponse] about the import result information
     */
    fun import(): SyncResponse{
       return importWithStarter{ a, b -> starter.import(a, b) }
    }

    /**
     * Import records asynchronously and return a [SyncResponse] with information about the started import
     */
    fun importAsync(): SyncResponse{
        return importWithStarter { a, b -> starter.importAsync(a, b) }
    }

    /**
     * Fetch a [SyncResponse] about the state of the current import
     */
    fun getImportStatus(): SyncResponse{
        return syncRecordService.getOrCreateRecord(SyncType.CDQ_IMPORT).toDto()
    }

    private fun importWithStarter(starterFunction: (OffsetDateTime, String?) -> Unit): SyncResponse{
        val syncRecord = syncRecordService.getOrCreateRecord(SyncType.CDQ_IMPORT)

        val startedAt = syncRecord.startedAt ?: SyncRecordService.syncStartTime
        val startAfter =  syncRecord.errorSave

        val response = syncRecordService.setSynchronizationStart(syncRecord).toDto()

        starterFunction(startedAt, startAfter)

        return response
    }

    /**
     * Private starter class to work with the main import service
     *
     * Needed for Async annotation to work
     */
    private open class ImportStarter(
        private val partnerImportPageService: PartnerImportPageService,
        private val entityManager: EntityManager,
        private val syncRecordService: SyncRecordService
    ){

        /**
         * Start paginated [import] asynchronously
         */
        @Async
        open fun importAsync(importFrom: OffsetDateTime, startAfterRecord: String?) {
            import(importFrom, startAfterRecord)
        }

        /**
         * Import CDQ partner records last modified after [importFrom] and with CDQ internal ID greater than [startAfterRecord]
         *
         * Data is imported in a paginated way. On an error during a page import the latest [startAfterRecord] ID is saved so that the import can later be resumed
         */
        fun import(importFrom: OffsetDateTime, startAfterRecord: String?){
            var startAfter: String? = startAfterRecord
            var importedCount = 0

            do{
                try{
                    val response = partnerImportPageService.import(importFrom, startAfter)
                    startAfter = response.nextStartAfter
                    importedCount += response.partners.size
                    val progress = importedCount / response.totalElements.toFloat()
                    syncRecordService.setProgress(syncRecordService.getOrCreateRecord(SyncType.CDQ_IMPORT), progress)

                }catch (exception: RuntimeException){
                    syncRecordService.setSynchronizationError(syncRecordService.getOrCreateRecord(SyncType.CDQ_IMPORT), exception.message!!, startAfter)
                    throw exception
                }

                //Clear session after each page import to improve JPA performance
                entityManager.clear()
            } while (startAfter != null)

            syncRecordService.setSynchronizationSuccess(syncRecordService.getOrCreateRecord(SyncType.CDQ_IMPORT))
        }



    }
}