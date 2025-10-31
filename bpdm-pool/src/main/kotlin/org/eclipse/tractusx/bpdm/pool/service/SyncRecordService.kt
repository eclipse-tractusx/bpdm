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

package org.eclipse.tractusx.bpdm.pool.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.service.BaseSyncRecordService
import org.eclipse.tractusx.bpdm.pool.api.model.SyncType
import org.eclipse.tractusx.bpdm.pool.entity.SyncRecordDb
import org.eclipse.tractusx.bpdm.pool.repository.SyncRecordRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class SyncRecordService(
    private val syncRecordRepository: SyncRecordRepository
) : BaseSyncRecordService<SyncType, SyncRecordDb>() {

    override val logger = KotlinLogging.logger { }

    override fun newSyncRecord(type: SyncType, initialFromTime: Instant) =
        SyncRecordDb(
            type = type,
            fromTime = initialFromTime
        )

    override fun save(record: SyncRecordDb) =
        syncRecordRepository.save(record)

    override fun findByType(type: SyncType) =
        syncRecordRepository.findByType(type)
}
