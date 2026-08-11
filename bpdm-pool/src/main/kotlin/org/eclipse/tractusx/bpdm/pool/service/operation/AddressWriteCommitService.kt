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

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.model.ChangelogRecord
import org.eclipse.tractusx.bpdm.pool.model.PendingAddressWrite
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The single sink that persists staged logistic-address writes and emits their CREATE/UPDATE changelog. No-change
 * writes are neither saved nor logged but are still returned, preserving the caller's positional contract. Committing is
 * separate from staging so a caller can wire a still-unsaved cyclic parent graph and persist it last.
 */
@Service
class AddressWriteCommitService(
    private val logisticAddressRepository: LogisticAddressRepository,
    private val changelogCreateService: ChangelogCreateService
) {
    /**
     * Persists the staged addresses that changed, emits their CREATE or UPDATE changelog, and returns one result per
     * staged write.
     */
    @Transactional
    fun commit(staged: List<PendingAddressWrite>): List<UpsertResult<LogisticAddressDb>> {
        logisticAddressRepository.saveAll(staged.filter { it.upsertType != UpsertType.NoChange }.map { it.address })

        changelogCreateService.record(staged.mapNotNull {
            when (it.upsertType) {
                UpsertType.Created -> ChangelogRecord(it.address.bpn, ChangelogType.CREATE, BusinessPartnerType.ADDRESS)
                UpsertType.Updated -> ChangelogRecord(it.address.bpn, ChangelogType.UPDATE, BusinessPartnerType.ADDRESS)
                UpsertType.NoChange -> null
            }
        })

        return staged.map { UpsertResult(it.address, it.upsertType) }
    }
}
