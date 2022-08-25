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

package org.eclipse.tractusx.bpdm.pool.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.entity.SyncRecord
import org.eclipse.tractusx.bpdm.pool.entity.SyncStatus
import org.eclipse.tractusx.bpdm.pool.entity.SyncType
import org.eclipse.tractusx.bpdm.pool.exception.BpdmSyncConflictException
import org.eclipse.tractusx.bpdm.pool.exception.BpdmSyncStateException
import org.eclipse.tractusx.bpdm.pool.repository.SyncRecordRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Uses transaction isolation level "serializable" in order to make sure that in case of parallel execution on different spring boot instances,
 * only one instance can get the sync record and make changes like setting it to "running" state at the same time.
 */
@Service
class SyncRecordService(
    private val syncRecordRepository: SyncRecordRepository
) {
    companion object {
        val syncStartTime = LocalDateTime.of(2000, 1, 1, 0, 0).toInstant(ZoneOffset.UTC)
    }

    private val logger = KotlinLogging.logger { }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    fun getOrCreateRecord(type: SyncType): SyncRecord {
        return syncRecordRepository.findByType(type) ?: run {
            logger.info { "Create new sync record entry for type $type" }
            val newEntry = SyncRecord(
                type,
                SyncStatus.NOT_SYNCED,
                syncStartTime
            )
            syncRecordRepository.save(newEntry)
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    fun setSynchronizationStart(type: SyncType): SyncRecord {
        val record = getOrCreateRecord(type)

        if (record.status == SyncStatus.RUNNING)
            throw BpdmSyncConflictException(SyncType.CDQ_IMPORT)

        logger.debug { "Set sync of type ${record.type} to status ${SyncStatus.RUNNING}" }

        record.errorDetails = null

        if (record.status != SyncStatus.ERROR) {
            record.fromTime = record.startedAt ?: syncStartTime
            record.errorDetails = null
            record.errorSave = null
            record.startedAt = Instant.now()
            record.finishedAt = null
            record.count = 0
            record.progress = 0f
        }
        record.status = SyncStatus.RUNNING

        return syncRecordRepository.save(record)
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    fun setSynchronizationSuccess(type: SyncType): SyncRecord {
        val record = getOrCreateRecord(type)
        if (record.status != SyncStatus.RUNNING)
            throw BpdmSyncStateException("Synchronization of type ${record.type} can't switch from state ${record.status} to ${SyncStatus.SUCCESS}.")

        logger.debug { "Set sync of type ${record.type} to status ${SyncStatus.SUCCESS}" }

        record.finishedAt = Instant.now()
        record.progress = 1f
        record.status = SyncStatus.SUCCESS
        record.errorDetails = null
        record.errorSave = null

        return syncRecordRepository.save(record)
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    fun setSynchronizationError(type: SyncType, errorMessage: String, saveState: String?): SyncRecord {
        val record = getOrCreateRecord(type)
        logger.debug { "Set sync of type ${record.type} to status ${SyncStatus.ERROR} with message $errorMessage" }

        record.finishedAt = Instant.now()
        record.status = SyncStatus.ERROR
        record.errorDetails = errorMessage.take(255)
        record.errorSave = saveState

        return syncRecordRepository.save(record)
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    fun setProgress(type: SyncType, count: Int, progress: Float): SyncRecord {
        val record = getOrCreateRecord(type)
        if (record.status != SyncStatus.RUNNING)
            throw BpdmSyncStateException("Synchronization of type ${record.type} can't change progress when not running.")

        logger.debug { "Update progress of sync type ${record.type} to $progress" }

        record.count = count
        record.progress = progress

        return syncRecordRepository.save(record)
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    fun reset(type: SyncType): SyncRecord {
        val record = getOrCreateRecord(type)
        logger.debug { "Reset sync status of type ${record.type}" }

        record.status = SyncStatus.NOT_SYNCED
        record.errorDetails = null
        record.errorSave = null
        record.startedAt = null
        record.finishedAt = null
        record.count = 0
        record.progress = 0f
        record.fromTime = syncStartTime

        return syncRecordRepository.save(record)
    }
}