package com.catenax.gpdm.component.elastic.impl.service

import com.catenax.gpdm.component.elastic.impl.repository.BusinessPartnerDocRepository
import com.catenax.gpdm.dto.response.SyncResponse
import com.catenax.gpdm.entity.SyncType
import com.catenax.gpdm.service.SyncRecordService
import com.catenax.gpdm.service.toDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ElasticSyncStarterService(
    private val syncRecordService: SyncRecordService,
    private val businessPartnerDocRepository: BusinessPartnerDocRepository,
    private val elasticSyncService: ElasticSyncService
) {

    /**
     * Checks for changed records since the last export and exports those changes to Elasticsearch
     */
    fun export(): SyncResponse {
        return startExport(true)
    }

    /**
     * Non-blocking asynchronous variant of [export]
     */
    fun exportAsync(): SyncResponse {
        return startExport(false)
    }

    /**
     * Fetch a [SyncResponse] about the state of the latest export
     */
    fun getExportStatus(): SyncResponse {
        return syncRecordService.getOrCreateRecord(SyncType.ELASTIC).toDto()
    }

    /**
     * Clears the whole index and resets the time of the last update
     */
    @Transactional
    fun clearElastic() {
        businessPartnerDocRepository.deleteAll()
        syncRecordService.reset(syncRecordService.getOrCreateRecord(SyncType.ELASTIC))
    }

    /**
     *  Start export either asynchronously or synchronously depending on whether [inSync]
     */
    private fun startExport(inSync: Boolean): SyncResponse {
        val record = syncRecordService.getOrCreateRecord(SyncType.ELASTIC)

        val fromTime = record.startedAt ?: SyncRecordService.syncStartTime
        val saveState = record.errorSave

        val response = syncRecordService.setSynchronizationStart(record).toDto()

        if (inSync)
            elasticSyncService.exportPaginated(fromTime, saveState)
        else
            elasticSyncService.exportPaginatedAsync(fromTime, saveState)

        return response
    }


}