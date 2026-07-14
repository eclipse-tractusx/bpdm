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
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.PendingAddressWrite
import org.eclipse.tractusx.bpdm.pool.service.BpnIssuingService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Stages logistic-address creates: issues the BPNs and builds the (unsaved) entities from already-resolved commands,
 * always [UpsertType.Created]. No persistence and no changelog here — [commit] (via [LogisticAddressWriteCommitService])
 * does that after the caller has wired the returned entities into their (possibly still-unsaved) cyclic parents. This is
 * the create-side counterpart of [LogisticAddressStagedUpdateService]. Order-preserving positional contract (see
 * [org.eclipse.tractusx.bpdm.pool.model.ParseResult]).
 */
@Service
class LogisticAddressStagedCreateService(
    private val bpnIssuingService: BpnIssuingService,
    private val addressEntityMapper: AddressEntityMapper,
    private val logisticAddressWriteCommitService: LogisticAddressWriteCommitService
) {
    /**
     * Issues the BPNs and builds the (unsaved) address entities from already-resolved commands. The caller wires the
     * returned entities into their parents before calling [commit].
     */
    fun stageCreate(parsed: List<AddressCreateParsed>): List<PendingAddressWrite> {
        val bpns = bpnIssuingService.issueAddressBpns(parsed.size)
        // A freshly created address has no shared history yet, so its sharing-member count starts at zero.
        return parsed.zip(bpns) { entry, bpn ->
            PendingAddressWrite(addressEntityMapper.toEntity(bpn, entry, numberOfSharingMembers = 0), UpsertType.Created)
        }
    }

    @Transactional
    fun commit(staged: List<PendingAddressWrite>): List<UpsertResult<LogisticAddressDb>> =
        logisticAddressWriteCommitService.commit(staged)
}
