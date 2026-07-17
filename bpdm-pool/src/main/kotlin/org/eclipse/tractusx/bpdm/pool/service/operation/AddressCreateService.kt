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

package org.eclipse.tractusx.bpdm.pool.service.operation

import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.mapper.entity.AddressEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.PendingAddressWrite
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressCreateParsed
import org.eclipse.tractusx.bpdm.pool.service.BpnIssuingService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The single authority for creating logistic addresses: issues address BPNs, builds the entities, persists them and
 * emits their CREATE changelog. Returns managed entities in the caller's transaction, not response models.
 *
 * [create] does this in one call. A creator wiring a freshly created address into a not-yet-persisted parent instead
 * uses [stageCreate] (build unsaved) and [commit] (persist + changelog), wiring the graph in between.
 */
@Service
class AddressCreateService(
    private val bpnIssuingService: BpnIssuingService,
    private val addressEntityMapper: AddressEntityMapper,
    private val logisticAddressWriteCommitService: LogisticAddressWriteCommitService
) {

    @Transactional
    fun create(parsed: List<AddressCreateParsed>): List<LogisticAddressDb> =
        commit(stageCreate(parsed)).map { it.value }

    fun stageCreate(parsed: List<AddressCreateParsed>): List<PendingAddressWrite> {
        val bpns = bpnIssuingService.issueAddressBpns(parsed.size)
        // A freshly created address starts with zero sharing members.
        return parsed.zip(bpns) { entry, bpn ->
            PendingAddressWrite(addressEntityMapper.toEntity(bpn, entry, numberOfSharingMembers = 0), UpsertType.Created)
        }
    }

    @Transactional
    fun commit(staged: List<PendingAddressWrite>): List<UpsertResult<LogisticAddressDb>> =
        logisticAddressWriteCommitService.commit(staged)
}