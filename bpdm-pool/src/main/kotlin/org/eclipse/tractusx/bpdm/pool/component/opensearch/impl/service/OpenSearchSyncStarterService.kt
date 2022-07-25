package org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.BUSINESS_PARTNER_INDEX_NAME
import org.eclipse.tractusx.bpdm.pool.dto.response.SyncResponse
import org.eclipse.tractusx.bpdm.pool.entity.SyncType
import org.eclipse.tractusx.bpdm.pool.service.SyncRecordService
import org.eclipse.tractusx.bpdm.pool.service.toDto
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.mapping.TypeMapping
import org.opensearch.client.opensearch.indices.CreateIndexRequest
import org.opensearch.client.opensearch.indices.DeleteIndexRequest
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.ResourceUtils

@Service
class OpenSearchSyncStarterService(
    private val syncRecordService: SyncRecordService,
    private val openSearchSyncService: OpenSearchSyncService,
    private val openSearchClient: OpenSearchClient
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Checks for changed records since the last export and exports those changes to OpenSearch
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
        return syncRecordService.getOrCreateRecord(SyncType.OPENSEARCH).toDto()
    }

    /**
     * Clears the whole index and resets the time of the last update
     */
    @Transactional
    fun clearOpenSearch() {
        logger.info { "Recreating the OpenSearch index" }

        deleteIndex()
        createIndex()

        syncRecordService.reset(syncRecordService.getOrCreateRecord(SyncType.OPENSEARCH))
    }

    @EventListener(ContextRefreshedEvent::class)
    fun createOnInit() {
        val indexAlreadyExists = openSearchClient.indices().exists { it.index(BUSINESS_PARTNER_INDEX_NAME) }.value()

        if (!indexAlreadyExists) {
            createIndex()
        }
    }

    /**
     *  Start export either asynchronously or synchronously depending on whether [inSync]
     */
    private fun startExport(inSync: Boolean): SyncResponse {
        val record = syncRecordService.getOrCreateRecord(SyncType.OPENSEARCH)

        val fromTime = record.startedAt ?: SyncRecordService.syncStartTime
        val saveState = record.errorSave

        val response = syncRecordService.setSynchronizationStart(record).toDto()

        logger.debug { "Initializing OpenSearch export with records after '$fromTime' from page '${record.errorSave}' and asynchronously: ${!inSync}" }

        if (inSync)
            openSearchSyncService.exportPaginated(fromTime, saveState)
        else
            openSearchSyncService.exportPaginatedAsync(fromTime, saveState)

        return response
    }

    private fun deleteIndex() {
        val deleteIndexRequest: DeleteIndexRequest = DeleteIndexRequest.Builder().index(BUSINESS_PARTNER_INDEX_NAME).build()
        openSearchClient.indices().delete(deleteIndexRequest)
    }

    private fun createIndex() {
        val indexFile = ResourceUtils.getFile("classpath:opensearch/index-mappings.json")
        val jsonpMapper = openSearchClient._transport().jsonpMapper()
        val jsonParser = jsonpMapper.jsonProvider().createParser(indexFile.inputStream())
        val createIndexRequest =
            CreateIndexRequest.Builder().index(BUSINESS_PARTNER_INDEX_NAME).mappings(TypeMapping._DESERIALIZER.deserialize(jsonParser, jsonpMapper)).build()
        openSearchClient.indices().create(createIndexRequest)
    }
}