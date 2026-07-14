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
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.model.PendingAddressWrite
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.eclipse.tractusx.bpdm.pool.service.PartnerChangelogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The single sink for staged logistic-address writes: both [LogisticAddressStagedCreateService] and
 * [LogisticAddressStagedUpdateService] produce [PendingAddressWrite]s and hand them here to be persisted. Keeping the
 * save and changelog in one place lets callers that own a cyclic parent relationship (legal entity ⇄ legal address,
 * site ⇄ main address) stage the create/update, wire the still-unsaved object graph in memory, and commit last — the
 * cyclic insert resolves at flush via the nullable `logistic_address.legal_entity_id` back-FK together with
 * `order_inserts`.
 */
@Service
class LogisticAddressWriteCommitService(
    private val logisticAddressRepository: LogisticAddressRepository,
    private val changelogService: PartnerChangelogService
) {
    /**
     * Persists the created/updated addresses and emits their CREATE/UPDATE changelog. NoChange entries are neither saved
     * nor logged; all entries are returned so the caller keeps the positional contract.
     */
    @Transactional
    fun commit(staged: List<PendingAddressWrite>): List<UpsertResult<LogisticAddressDb>> {
        logisticAddressRepository.saveAll(staged.filter { it.upsertType != UpsertType.NoChange }.map { it.address })

        changelogService.createChangelogEntries(staged.mapNotNull {
            when (it.upsertType) {
                UpsertType.Created -> ChangelogEntryCreateRequest(it.address.bpn, ChangelogType.CREATE, BusinessPartnerType.ADDRESS)
                UpsertType.Updated -> ChangelogEntryCreateRequest(it.address.bpn, ChangelogType.UPDATE, BusinessPartnerType.ADDRESS)
                UpsertType.NoChange -> null
            }
        })

        return staged.map { UpsertResult(it.address, it.upsertType) }
    }
}
