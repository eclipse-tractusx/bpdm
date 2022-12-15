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

package org.eclipse.tractusx.bpdm.pool.component.cdq.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.entity.SyncType
import org.eclipse.tractusx.bpdm.pool.service.SyncRecordService
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.Instant
import jakarta.persistence.EntityManager

/**
 * Imports business partner entries from CDQ
 */
@Service
class PartnerImportService(
    private val syncRecordService: SyncRecordService,
    private val partnerImportPageService: PartnerImportPageService,
    private val entityManager: EntityManager
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Asynchronous version of [importPaginated]
     */
    @Async
    fun importPaginatedAsync(fromTime: Instant, saveState: String?) {
        importPaginated(fromTime, saveState)
    }

    /**
     * Import CDQ partner records last modified after [fromTime] and with CDQ internal ID greater than [saveState]
     *
     * Data is imported in a paginated way. On an error during a page import the latest [saveState] ID is saved so that the import can later be resumed
     */
    fun importPaginated(fromTime: Instant, saveState: String?) {
        logger.info { "Starting CDQ import starting with ID ${saveState}' for modified records from '$fromTime'" }

        var startAfter: String? = saveState
        var importedCount = 0

        do {
            try {
                val response = partnerImportPageService.import(fromTime, startAfter)
                startAfter = response.nextStartAfter
                val createdCount = response.legalEntities.created.size + response.sites.created.size + response.addresses.created.size
                val updatedCount = response.legalEntities.updated.size + response.sites.updated.size + response.addresses.updated.size
                importedCount += createdCount + updatedCount
                val progress = importedCount / response.totalElements.toFloat()
                syncRecordService.setProgress(SyncType.CDQ_IMPORT, importedCount, progress)
            } catch (exception: RuntimeException) {
                logger.error(exception) { "Exception encountered on CDQ import" }
                syncRecordService.setSynchronizationError(
                    SyncType.CDQ_IMPORT,
                    exception.message ?: "No Message",
                    startAfter
                )
                return
            }

            //Clear session after each page import to improve JPA performance
            entityManager.clear()
        } while (startAfter != null)

        syncRecordService.setSynchronizationSuccess(SyncType.CDQ_IMPORT)

        logger.info { "CDQ import finished successfully" }
    }
}