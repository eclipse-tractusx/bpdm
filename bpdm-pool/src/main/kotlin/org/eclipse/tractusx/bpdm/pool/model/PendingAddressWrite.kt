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

package org.eclipse.tractusx.bpdm.pool.model

import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb

/**
 * A logistic address with a staged, not-yet-persisted write (create or update) and the resulting change state. Produced
 * by [org.eclipse.tractusx.bpdm.pool.service.operation.AddressCreateService]'s `stageCreate` /
 * [org.eclipse.tractusx.bpdm.pool.service.operation.AddressUpdateService]'s `stageUpdate` and consumed by
 * [org.eclipse.tractusx.bpdm.pool.service.operation.LogisticAddressWriteCommitService]'s `commit`, which is the single
 * place an address is saved and changelogged. It is handed to callers so they can wire the (still-unsaved) address into
 * a cyclic parent — legal entity or site — before it is persisted.
 */
data class PendingAddressWrite(
    val address: LogisticAddressDb,
    val upsertType: UpsertType
)
