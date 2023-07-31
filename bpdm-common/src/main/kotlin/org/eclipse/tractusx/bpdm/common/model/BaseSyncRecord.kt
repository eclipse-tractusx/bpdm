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

package org.eclipse.tractusx.bpdm.common.model

import java.time.Instant

/**
 * Info about most current sync run by type
 */
interface BaseSyncRecord<SYNC_TYPE> {
    /**
     * Discriminator to allow multiple sync records in the DB
     */
    var type: SYNC_TYPE

    /**
     * @see SyncStatus
     */
    var status: SyncStatus

    /**
     * Sync run considers only data after this instant
     */
    var fromTime: Instant

    /**
     * Progress from 0 ot 1
     */
    var progress: Float

    /**
     * Progress counter
     */
    var count: Int

    /**
     * Plain message for error status
     */
    var errorDetails: String?

    /**
     * Optional serialized state to allow resume after error status
     */
    var errorSave: String?

    /**
     * Instant this sync run was started
     */
    var startedAt: Instant?

    /**
     * Instant this sync run was finished
     */
    var finishedAt: Instant?
}
