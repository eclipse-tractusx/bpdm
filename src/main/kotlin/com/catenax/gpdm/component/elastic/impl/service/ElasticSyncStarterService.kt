package com.catenax.gpdm.component.elastic.impl.service

import com.catenax.gpdm.component.elastic.impl.doc.BusinessPartnerDoc
import com.catenax.gpdm.dto.response.SyncResponse
import com.catenax.gpdm.entity.SyncType
import com.catenax.gpdm.exception.BpdmElasticIndexException
import com.catenax.gpdm.service.SyncRecordService
import com.catenax.gpdm.service.toDto
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.ApplicationContextException
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ElasticSyncStarterService(
    private val syncRecordService: SyncRecordService,
    private val elasticSyncService: ElasticSyncService,
    private val operations: ElasticsearchOperations,
    private val objectMapper: ObjectMapper
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
    @Throws(BpdmElasticIndexException::class)
    fun clearElastic(): Boolean {
        val indexOperations = operations.indexOps(BusinessPartnerDoc::class.java)
        val settings = indexOperations.createSettings()
        val mappings = indexOperations.createMapping()

        indexOperations.delete()

        if (!indexOperations.create(settings, mappings))
            throw BpdmElasticIndexException("Could not recreate business partner index")

        syncRecordService.reset(syncRecordService.getOrCreateRecord(SyncType.ELASTIC))

        return true
    }


    /**
     * Checks whether the existing business partner index is up-to-date with the current Entity version and recreates the index if necessary
     */
    @EventListener(ContextRefreshedEvent::class)
    @Throws(ApplicationContextException::class)
    fun updateOnInit() {
        try {
            val existingIndexOps = operations.indexOps(BusinessPartnerDoc::class.java)
            val settings = existingIndexOps.createSettings()
            val mappings = existingIndexOps.createMapping()

            val tempIndexOps = operations.indexOps(IndexCoordinates.of("temp-business-partner"))
            tempIndexOps.delete()

            if (!tempIndexOps.create(settings, mappings))
                throw BpdmElasticIndexException("Could not create temporary business partner index")

            val actualTempTree = objectMapper.valueToTree<JsonNode>(tempIndexOps.mapping)
            val actualExistingTree = objectMapper.valueToTree<JsonNode>(existingIndexOps.mapping)

            if (!actualTempTree.equals(actualExistingTree)) {
                clearElastic()
            }

            tempIndexOps.delete()
        } catch (e: Throwable) {
            //make sure Application exits when exception is thrown
            throw ApplicationContextException("Exception when updating Elasticsearch index during initialization", e)
        }

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