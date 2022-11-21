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
import org.eclipse.tractusx.bpdm.pool.config.OpenSearchConfigProperties
import org.eclipse.tractusx.bpdm.pool.entity.PartnerChangelogEntry
import org.eclipse.tractusx.bpdm.pool.entity.SyncType
import org.eclipse.tractusx.bpdm.pool.service.SyncRecordService
import org.springframework.data.domain.Page
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.Instant
import javax.persistence.EntityManager

/**
 * Provides functionality for managing the OpenSearch index
 */
@Service
class OpenSearchSyncService(
    val openSearchSyncPageService: OpenSearchSyncPageService,
    val configProperties: OpenSearchConfigProperties,
    val entityManager: EntityManager,
    private val syncRecordService: SyncRecordService
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Asynchronous version of [exportPaginated]
     */
    @Async
    fun exportPaginatedAsync(fromTime: Instant, saveState: String?) {
        exportPaginated(fromTime, saveState)
    }

    /**
     * Export new changes of the business partner records to the OpenSearch index
     *
     * A new change is discovered by comparing the updated timestamp of the business partner record with the time of the last export
     */
    fun exportPaginated(fromTime: Instant, saveState: String?) {
        var page = saveState?.toIntOrNull() ?: 0
        var docsPage: Page<PartnerChangelogEntry>

        logger.info { "Start OpenSearch export from time '$fromTime' and page '$page'" }

        do {
            try {
                docsPage = openSearchSyncPageService.exportPartnersToOpenSearch(fromTime, page, configProperties.exportPageSize)
                page++
                val record = syncRecordService.getOrCreateRecord(SyncType.OPENSEARCH)
                val newCount = record.count + docsPage.content.size
                syncRecordService.setProgress(SyncType.OPENSEARCH, newCount, newCount.toFloat() / docsPage.totalElements)

                //Clear session after each page import to improve JPA performance
                entityManager.clear()

            } catch (exception: RuntimeException) {
                logger.error(exception) { "Exception encountered on OpenSearch export" }
                syncRecordService.setSynchronizationError(SyncType.OPENSEARCH, exception.message!!, page.toString())
                return
            }
        } while (docsPage.totalPages > page)

        syncRecordService.setSynchronizationSuccess(SyncType.OPENSEARCH)

        logger.info { "Finished OpenSearch export successfully" }
    }
}