/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.component.saas.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.api.dto.ImportIdEntry
import org.eclipse.tractusx.bpdm.pool.api.dto.SyncType
import org.eclipse.tractusx.bpdm.pool.api.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.dto.response.ImportIdMappingResponse
import org.eclipse.tractusx.bpdm.pool.api.dto.response.SyncResponse
import org.eclipse.tractusx.bpdm.pool.repository.ImportEntryRepository
import org.eclipse.tractusx.bpdm.pool.service.SyncRecordService
import org.eclipse.tractusx.bpdm.pool.service.toDto
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

/**
 * Starts the partner import either blocking or non-blocking
 */
@Service
class ImportStarterService(
    private val syncRecordService: SyncRecordService,
    private val importService: PartnerImportService,
    private val importEntryRepository: ImportEntryRepository
) {

    private val logger = KotlinLogging.logger { }

    /**
     * Import records synchronously and return a [SyncResponse] about the import result information
     */
    @Scheduled(cron = "\${bpdm.saas.import-scheduler-cron-expr:-}", zone = "UTC")
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
        return syncRecordService.getOrCreateRecord(SyncType.SAAS_IMPORT).toDto()
    }

    /**
     * Filter import entries by the given [importIdentifiers]
     */
    fun getImportIdEntries(importIdentifiers: Collection<String>): ImportIdMappingResponse {
        val foundEntries = importEntryRepository.findByImportIdentifierIn(importIdentifiers).map { ImportIdEntry(it.importIdentifier, it.bpn) }
        val missingEntries = importIdentifiers.minus(foundEntries.map { it.importId }.toSet())

        return ImportIdMappingResponse(foundEntries, missingEntries)
    }

    /**
     * Paginate over import entries by [paginationRequest]
     */
    fun getImportIdEntries(paginationRequest: PaginationRequest): PageResponse<ImportIdEntry> {
        val entriesPage = importEntryRepository.findAll(PageRequest.of(paginationRequest.page, paginationRequest.size))
        return entriesPage.toDto(entriesPage.content.map { ImportIdEntry(it.importIdentifier, it.bpn) })
    }



    private fun startImport(inSync: Boolean): SyncResponse {
        val record = syncRecordService.setSynchronizationStart(SyncType.SAAS_IMPORT)
        logger.debug { "Initializing SaaS import starting with ID ${record.errorSave}' for modified records from '${record.fromTime}' with async: ${!inSync}" }

        if (inSync)
            importService.importPaginated(record.fromTime, record.errorSave)
        else
            importService.importPaginatedAsync(record.fromTime, record.errorSave)

        return record.toDto()
    }

}