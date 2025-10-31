/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.common.service

import mu.KLogger
import org.eclipse.tractusx.bpdm.common.model.BaseSyncRecord
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * This services manages data about the most current sync run for each SYNC_TYPE.
 *
 * How to use:
 * - At the start of a sync run you should call #setSynchronizationStart(type).
 *  The returned record contains the automatically updated timestamp after which new data should be considered (#fromTime).
 *  If the last sync has failed #errorSave might contain state info to allow resuming near the failure position.
 *  If another sync for the same type is currently running a BpdmSyncConflictException is thrown.
 * - If the sync run was successful you should call #setSynchronizationSuccess(type).
 * - If the sync run has failed you should call #setSynchronizationError(type, errorMessage, saveState).
 *
 * Uses transaction isolation level "serializable" in order to make sure that in case of parallel execution on different spring boot instances,
 * only one instance can get the sync record and make changes like setting it to "running" state at the same time.
 */
abstract class BaseSyncRecordService<SYNC_TYPE : Enum<*>, SYNC_RECORD : BaseSyncRecord<SYNC_TYPE>> {
    companion object {
        val INITIAL_FROM_TIME: Instant = LocalDateTime.of(2000, 1, 1, 0, 0).toInstant(ZoneOffset.UTC)
    }

    protected abstract val logger: KLogger

    protected abstract fun newSyncRecord(type: SYNC_TYPE, initialFromTime: Instant): SYNC_RECORD

    protected abstract fun save(record: SYNC_RECORD): SYNC_RECORD

    protected abstract fun findByType(type: SYNC_TYPE): SYNC_RECORD?

    open fun getOrCreateRecord(type: SYNC_TYPE): SYNC_RECORD {
        return findByType(type) ?: run {
            logger.info { "Create new sync record entry for type $type" }
            val newEntry = newSyncRecord(type, INITIAL_FROM_TIME)
            save(newEntry)
        }
    }

    open fun updateRecord(syncRecord: SYNC_RECORD, newFromTime: Instant?): SYNC_RECORD {
        val hasChanges = newFromTime != syncRecord.fromTime
        if (hasChanges) {
            logger.debug { "Update from time for sync entry for type ${syncRecord.type}" }
            syncRecord.fromTime = newFromTime ?: syncRecord.fromTime
            save(syncRecord)
        }

        return syncRecord
    }
}