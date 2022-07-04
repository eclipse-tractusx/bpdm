package org.eclipse.tractusx.bpdm.pool.component.elastic.impl.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.component.elastic.impl.doc.BusinessPartnerDoc
import org.eclipse.tractusx.bpdm.pool.dto.response.SyncResponse
import org.eclipse.tractusx.bpdm.pool.entity.SyncType
import org.eclipse.tractusx.bpdm.pool.exception.BpdmElasticIndexException
import org.eclipse.tractusx.bpdm.pool.service.SyncRecordService
import org.eclipse.tractusx.bpdm.pool.service.toDto
import org.springframework.context.ApplicationContextException
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.IndexOperations
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
    private val logger = KotlinLogging.logger { }

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
     *
     * @throws [BpdmElasticIndexException]
     */
    @Transactional
    fun clearElastic() {
        logger.info { "Recreating the Elasticsearch index" }

        val index = operations.indexOps(BusinessPartnerDoc::class.java)
        index.delete()
        createIndex(index)

        syncRecordService.reset(syncRecordService.getOrCreateRecord(SyncType.ELASTIC))
    }


    /**
     * Checks whether the existing business partner index is up-to-date with the current Entity version and recreates the index if necessary
     *
     * @throws [ApplicationContextException] shutting down the application on exception
     */
    @EventListener(ContextRefreshedEvent::class)
    fun updateOnInit() {

        logger.info { "Checking whether Elasticsearch index needs to be recreated..." }
        try {
            val currentIndex = operations.indexOps(BusinessPartnerDoc::class.java)

            if (!currentIndex.exists()) {
                logger.info { "Create index as it does not exist yet" }
                createIndex(currentIndex)
            } else {
                val tempIndex = operations.indexOps(IndexCoordinates.of("temp-business-partner"))

                tempIndex.delete()
                createIndex(tempIndex, currentIndex)

                val actualTempTree = objectMapper.valueToTree<JsonNode>(tempIndex.mapping)
                val actualExistingTree = objectMapper.valueToTree<JsonNode>(currentIndex.mapping)

                if (!actualTempTree.equals(actualExistingTree)) {
                    clearElastic()
                } else {
                    logger.info { "Index still up-to-date" }
                }

                tempIndex.delete()
            }
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

        logger.debug { "Initializing Elasticsearch export with records after '$fromTime' from page '${record.errorSave}' and asynchronously: ${!inSync}" }

        if (inSync)
            elasticSyncService.exportPaginated(fromTime, saveState)
        else
            elasticSyncService.exportPaginatedAsync(fromTime, saveState)

        return response
    }

    private fun createIndex(index: IndexOperations) {
        createIndex(index, index)
    }

    private fun createIndex(indexToCreate: IndexOperations, indexToTakeConfigFrom: IndexOperations) {
        val settings = indexToTakeConfigFrom.createSettings()
        val mappings = indexToTakeConfigFrom.createMapping()

        if (!indexToCreate.create(settings, mappings))
            throw BpdmElasticIndexException("Could not recreate business partner index")
    }

}