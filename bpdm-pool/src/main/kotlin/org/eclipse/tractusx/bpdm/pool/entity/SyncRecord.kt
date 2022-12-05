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

package org.eclipse.tractusx.bpdm.pool.entity

import java.time.Instant
import jakarta.persistence.*

@Entity
@Table(name = "sync_records")
class SyncRecord(
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, unique = true)
    var type: SyncType,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: SyncStatus,
    @Column(name = "from_time", nullable = false)
    var fromTime: Instant,
    @Column(name = "progress", nullable = false)
    var progress: Float = 0f,
    @Column(name = "count", nullable = false)
    var count: Int = 0,
    @Column(name = "status_details")
    var errorDetails: String? = null,
    @Column(name = "save_state")
    var errorSave: String? = null,
    @Column(name = "started_at")
    var startedAt: Instant? = null,
    @Column(name = "finished_at")
    var finishedAt: Instant? = null,
    ): BaseEntity()


enum class SyncType{
    OPENSEARCH,
    CDQ_IMPORT
}

enum class SyncStatus{
    NOT_SYNCED,
    RUNNING,
    SUCCESS,
    ERROR
}