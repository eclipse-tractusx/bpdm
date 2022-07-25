package org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.dto.response.SyncResponse
import org.eclipse.tractusx.bpdm.pool.entity.SyncType
import org.eclipse.tractusx.bpdm.pool.service.SyncRecordService
import org.eclipse.tractusx.bpdm.pool.service.toDto
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.mapping.TypeMapping
import org.opensearch.client.opensearch.indices.CreateIndexRequest
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
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

    @EventListener(ContextRefreshedEvent::class)
    fun createOnInit() {
        val indexAlreadyExists = openSearchClient.indices().exists { it.index("business-partner") }.value()

        if (!indexAlreadyExists) {
            val indexFile = ResourceUtils.getFile("classpath:opensearch/index-mappings.json")
            val jsonpMapper = openSearchClient._transport().jsonpMapper()
            val jsonParser = jsonpMapper.jsonProvider().createParser(indexFile.inputStream())
            val createIndexRequest =
                CreateIndexRequest.Builder().index("business-partner").mappings(TypeMapping._DESERIALIZER.deserialize(jsonParser, jsonpMapper)).build()
            openSearchClient.indices().create(createIndexRequest)
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
}