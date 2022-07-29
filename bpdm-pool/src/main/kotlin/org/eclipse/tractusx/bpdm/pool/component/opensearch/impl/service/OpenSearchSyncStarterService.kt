package org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.BUSINESS_PARTNER_INDEX_NAME
import org.eclipse.tractusx.bpdm.pool.dto.response.SyncResponse
import org.eclipse.tractusx.bpdm.pool.entity.SyncType
import org.eclipse.tractusx.bpdm.pool.service.SyncRecordService
import org.eclipse.tractusx.bpdm.pool.service.toDto
import org.opensearch.client.RequestOptions
import org.opensearch.client.RestHighLevelClient
import org.opensearch.client.indices.CreateIndexRequest
import org.opensearch.client.indices.GetMappingsRequest
import org.opensearch.client.indices.GetMappingsResponse
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch.indices.DeleteIndexRequest
import org.opensearch.cluster.metadata.MappingMetadata
import org.opensearch.common.xcontent.XContentType
import org.springframework.context.ApplicationContextException
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.ResourceUtils
import java.nio.file.Files

@Service
class OpenSearchSyncStarterService(
    private val syncRecordService: SyncRecordService,
    private val openSearchSyncService: OpenSearchSyncService,
    private val openSearchClient: OpenSearchClient,
    private val restHighLevelClient: RestHighLevelClient
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

        deleteIndexIfExists(BUSINESS_PARTNER_INDEX_NAME)
        createIndex(BUSINESS_PARTNER_INDEX_NAME)

        syncRecordService.reset(syncRecordService.getOrCreateRecord(SyncType.OPENSEARCH))
    }

    /**
     * Checks whether the existing business partner index is up-to-date with the current index mappings and recreates the index if necessary
     *
     * @throws [ApplicationContextException] shutting down the application on exception
     */
    @EventListener(ContextRefreshedEvent::class)
    fun updateOnInit() {

        logger.info { "Checking whether OpenSearch index needs to be recreated..." }
        try {
            val indexAlreadyExists = openSearchClient.indices().exists { it.index(BUSINESS_PARTNER_INDEX_NAME) }.value()

            if (!indexAlreadyExists) {
                logger.info { "Create index as it does not exist yet" }
                createIndex(BUSINESS_PARTNER_INDEX_NAME)
            } else {
                val tempIndexName = "temp-business-partner"
                deleteIndexIfExists(tempIndexName)
                createIndex(tempIndexName)

                val existingMappingMetadata = getIndexMappings(BUSINESS_PARTNER_INDEX_NAME).sourceAsMap()
                val requiredMappingMetadata = getIndexMappings(tempIndexName).sourceAsMap()

                if (requiredMappingMetadata != existingMappingMetadata) {
                    clearOpenSearch()
                } else {
                    logger.info { "Index mappings still up-to-date" }
                }

                deleteIndexIfExists(tempIndexName)
            }
        } catch (e: Throwable) {
            // make sure Application exits when exception is thrown
            throw ApplicationContextException("Exception when updating OpenSearch index during initialization", e)
        }

    }

    private fun getIndexMappings(indexName: String): MappingMetadata {
        val request = GetMappingsRequest()
        request.indices(indexName)
        val getMappingResponse: GetMappingsResponse = restHighLevelClient.indices().getMapping(request, RequestOptions.DEFAULT)
        return getMappingResponse.mappings()[indexName]!!
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

    private fun deleteIndexIfExists(indexName: String) {
        val indexExists = openSearchClient.indices().exists { it.index(indexName) }.value()
        if (indexExists) {
            val deleteIndexRequest: DeleteIndexRequest = DeleteIndexRequest.Builder().index(indexName).build()
            openSearchClient.indices().delete(deleteIndexRequest)
        }
    }

    private fun createIndex(indexName: String) {
        val indexFile = ResourceUtils.getFile("classpath:opensearch/index-mappings.json")
        val indexJson = Files.readString(indexFile.toPath())

        val request = CreateIndexRequest(indexName)
        request.mapping(indexJson, XContentType.JSON)

        restHighLevelClient.indices().create(request, RequestOptions.DEFAULT)
    }
}