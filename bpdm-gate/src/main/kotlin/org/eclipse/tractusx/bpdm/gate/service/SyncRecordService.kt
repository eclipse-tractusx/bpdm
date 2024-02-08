/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.gate.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.model.SyncStatus
import org.eclipse.tractusx.bpdm.common.service.BaseSyncRecordService
import org.eclipse.tractusx.bpdm.gate.entity.SyncRecord
import org.eclipse.tractusx.bpdm.gate.entity.SyncType
import org.eclipse.tractusx.bpdm.gate.repository.SyncRecordRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class SyncRecordService(
    private val syncRecordRepository: SyncRecordRepository
) : BaseSyncRecordService<SyncType, SyncRecord>() {

    override val logger = KotlinLogging.logger { }

    override fun newSyncRecord(type: SyncType, initialFromTime: Instant) =
        SyncRecord(
            type = type,
            status = SyncStatus.NOT_SYNCED,
            fromTime = initialFromTime
        )

    override fun save(record: SyncRecord) =
        syncRecordRepository.save(record)

    override fun findByType(type: SyncType) =
        syncRecordRepository.findByType(type)

}