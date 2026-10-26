/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.bpdm.gate.service.operation.sync

import org.eclipse.tractusx.bpdm.common.service.BaseSyncRecordService
import org.eclipse.tractusx.bpdm.gate.entity.SyncTypeDb
import org.eclipse.tractusx.bpdm.gate.repository.SyncRecordRepository
import org.eclipse.tractusx.bpdm.gate.service.SyncRecordService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Tracks up to which time each synchronisation with another service has been processed.
 */
@Service
class SyncRecordOperationService(
    private val syncRecordRepository: SyncRecordRepository,
    private val syncRecordService: SyncRecordService
) {

    /**
     * Returns the time up to which [type] has been synchronised, or the initial time if it never was.
     */
    @Transactional(readOnly = true)
    fun getSyncedUntil(type: SyncTypeDb): Instant =
        syncRecordRepository.findByType(type)?.fromTime ?: BaseSyncRecordService.INITIAL_FROM_TIME

    /**
     * Records that [type] is synchronised up to [until], keeping the previous time if it is null.
     */
    @Transactional
    fun advance(type: SyncTypeDb, until: Instant?) {
        syncRecordService.updateRecord(syncRecordService.getOrCreateRecord(type), until)
    }
}
