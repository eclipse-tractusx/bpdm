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

package org.eclipse.tractusx.bpdm.gate.entity

import jakarta.persistence.*
import org.eclipse.tractusx.bpdm.common.model.BaseEntity
import org.eclipse.tractusx.bpdm.common.model.BaseSyncRecord
import org.eclipse.tractusx.bpdm.common.model.SyncStatus
import java.time.Instant

@Entity
@Table(name = "sync_records")
class SyncRecordDb(
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, unique = true)
    override var type: SyncTypeDb,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    override var status: SyncStatus,

    @Column(name = "from_time", nullable = false)
    override var fromTime: Instant,

    @Column(name = "progress", nullable = false)
    override var progress: Float = 0f,

    @Column(name = "count", nullable = false)
    override var count: Int = 0,

    @Column(name = "status_details")
    override var errorDetails: String? = null,

    @Column(name = "save_state")
    override var errorSave: String? = null,

    @Column(name = "started_at")
    override var startedAt: Instant? = null,

    @Column(name = "finished_at")
    override var finishedAt: Instant? = null,

    ) : BaseEntity(), BaseSyncRecord<SyncTypeDb>
