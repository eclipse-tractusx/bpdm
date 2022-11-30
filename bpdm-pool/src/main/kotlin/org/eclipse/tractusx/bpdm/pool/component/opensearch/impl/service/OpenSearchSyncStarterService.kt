/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.ADDRESS_PARTNER_INDEX_NAME
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.LEGAL_ENTITIES_INDEX_NAME
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.MAPPINGS_FILE_PATH_ADDRESSES
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.MAPPINGS_FILE_PATH_LEGAL_ENTITIES
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
import org.springframework.core.io.ResourceLoader
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OpenSearchSyncStarterService(
    private val syncRecordService: SyncRecordService,
    private val openSearchSyncService: OpenSearchSyncService,
    private val openSearchClient: OpenSearchClient,
    private val restHighLevelClient: RestHighLevelClient,
    private val resourceLoader: ResourceLoader
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Checks for changed records since the last export and exports those changes to OpenSearch
     */
    @Scheduled(cron = "\${bpdm.opensearch.export-scheduler-cron-expr:-}", zone = "UTC")
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
        logger.info { "Recreating the OpenSearch indexes" }

        deleteIndexesIfExists(LEGAL_ENTITIES_INDEX_NAME, ADDRESS_PARTNER_INDEX_NAME)
        createIndexes(
            IndexDefinition(LEGAL_ENTITIES_INDEX_NAME, MAPPINGS_FILE_PATH_LEGAL_ENTITIES),
            IndexDefinition(ADDRESS_PARTNER_INDEX_NAME, MAPPINGS_FILE_PATH_ADDRESSES)
        )

        syncRecordService.reset(SyncType.OPENSEARCH)
    }

    /**
     * Checks whether the existing indexes are up-to-date with the current index mappings and recreates the indexes if necessary
     *
     * @throws [ApplicationContextException] shutting down the application on exception
     */
    @EventListener(ContextRefreshedEvent::class)
    fun updateOnInit() {
        logger.info { "Checking whether OpenSearch indexes need to be recreated..." }
        try {
            var recreateIndexes = false

            recreateIndexes = updateOnInit(IndexDefinition(LEGAL_ENTITIES_INDEX_NAME, MAPPINGS_FILE_PATH_LEGAL_ENTITIES)) || recreateIndexes
            recreateIndexes = updateOnInit(IndexDefinition(ADDRESS_PARTNER_INDEX_NAME, MAPPINGS_FILE_PATH_ADDRESSES)) || recreateIndexes

            if (recreateIndexes) {
                clearOpenSearch()
            } else {
                logger.info { "Index mappings still up-to-date" }
            }
        } catch (e: Throwable) {
            // make sure Application exits when exception is thrown
            throw ApplicationContextException("Exception when updating OpenSearch indexes during initialization", e)
        }
    }

    /**
     * @return true if index mapping changed, false otherwise
     */
    private fun updateOnInit(indexDefinition: IndexDefinition): Boolean {
        val indexAlreadyExists = openSearchClient.indices().exists { it.index(indexDefinition.indexName) }.value()

        return if (!indexAlreadyExists) {
            true
        } else {
            val tempIndexName = "temp-${indexDefinition.indexName}"
            deleteIndexIfExists(tempIndexName)
            createIndex(IndexDefinition(tempIndexName, indexDefinition.mappingsFilePath))

            val existingMappingMetadata = getIndexMappings(indexDefinition.indexName).sourceAsMap()
            val requiredMappingMetadata = getIndexMappings(tempIndexName).sourceAsMap()

            deleteIndexIfExists(tempIndexName)

            requiredMappingMetadata != existingMappingMetadata
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
        val record = syncRecordService.setSynchronizationStart(SyncType.OPENSEARCH)
        val response = record.toDto()

        logger.debug { "Initializing OpenSearch export with records after '${record.fromTime}' from page '${record.errorSave}' and asynchronously: ${!inSync}" }

        if (inSync)
            openSearchSyncService.exportPaginated(record.fromTime, record.errorSave)
        else
            openSearchSyncService.exportPaginatedAsync(record.fromTime, record.errorSave)

        return response
    }

    private fun deleteIndexesIfExists(vararg indexNames: String) {
        for (indexName in indexNames) {
            deleteIndexIfExists(indexName)
        }
    }

    private fun deleteIndexIfExists(indexName: String) {
        val indexExists = openSearchClient.indices().exists { it.index(indexName) }.value()
        if (indexExists) {
            val deleteIndexRequest: DeleteIndexRequest = DeleteIndexRequest.Builder().index(indexName).build()
            openSearchClient.indices().delete(deleteIndexRequest)
        }
    }

    private fun createIndexes(vararg indexDefinitions: IndexDefinition) {
        for (indexDefinition in indexDefinitions) {
            createIndex(indexDefinition)
        }
    }

    private fun createIndex(indexDefinition: IndexDefinition) {
        val indexMappings = String(resourceLoader.getResource("classpath:${indexDefinition.mappingsFilePath}").inputStream.readAllBytes())
        val request = CreateIndexRequest(indexDefinition.indexName)
        request.mapping(indexMappings, XContentType.JSON)

        restHighLevelClient.indices().create(request, RequestOptions.DEFAULT)
    }
}

private data class IndexDefinition(val indexName: String, val mappingsFilePath: String)