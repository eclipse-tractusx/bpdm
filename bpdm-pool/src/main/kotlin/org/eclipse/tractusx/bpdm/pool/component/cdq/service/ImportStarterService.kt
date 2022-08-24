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
import org.eclipse.tractusx.bpdm.pool.dto.response.SyncResponse
import org.eclipse.tractusx.bpdm.pool.entity.SyncType
import org.eclipse.tractusx.bpdm.pool.service.SyncRecordService
import org.eclipse.tractusx.bpdm.pool.service.toDto
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

/**
 * Starts the partner import either blocking or non-blocking
 */
@Service
class ImportStarterService(
    private val syncRecordService: SyncRecordService,
    private val importService: PartnerImportService
) {

    private val logger = KotlinLogging.logger { }

    /**
     * Import records synchronously and return a [SyncResponse] about the import result information
     */
    @Scheduled(cron = "\${bpdm.cdq.import-scheduler-cron-expr:-}", zone = "UTC")
    fun import(): SyncResponse {
        return startImport(true)
    }

    /**
     * Import records asynchronously and return a [SyncResponse] with information about the started import
     */
    fun importAsync(): SyncResponse {
        return startImport(false)
    }

    /**
     * Fetch a [SyncResponse] about the state of the current import
     */
    fun getImportStatus(): SyncResponse {
        return syncRecordService.getOrCreateRecord(SyncType.CDQ_IMPORT).toDto()
    }

    private fun startImport(inSync: Boolean): SyncResponse {
        val (record, previousStartedAt) = syncRecordService.setSynchronizationStart(SyncType.CDQ_IMPORT)

        val fromTime = previousStartedAt ?: SyncRecordService.syncStartTime
        val saveState = record.errorSave

        val response = record.toDto()

        logger.debug { "Initializing CDQ import starting with ID ${record.errorSave}' for modified records from '$fromTime' with async: ${!inSync}" }

        if (inSync)
            importService.importPaginated(fromTime, saveState)
        else
            importService.importPaginatedAsync(fromTime, saveState)

        return response
    }

}