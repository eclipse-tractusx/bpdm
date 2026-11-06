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

package org.eclipse.tractusx.bpdm.pool.service.operation.address

import org.eclipse.tractusx.bpdm.pool.model.raw.changelog.ChangelogType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.logging.PersistedChangeLogger
import org.eclipse.tractusx.bpdm.common.logging.PersistedChangeType
import org.eclipse.tractusx.bpdm.common.model.DbTimestamp
import org.eclipse.tractusx.bpdm.pool.model.raw.businesspartner.BusinessPartnerChangeRaw
import org.eclipse.tractusx.bpdm.pool.model.parsed.businesspartner.UpsertResultParsed
import org.eclipse.tractusx.bpdm.pool.model.raw.businesspartner.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.model.raw.changelog.ChangelogEntryRaw
import org.eclipse.tractusx.bpdm.pool.model.parsed.address.PendingAddressWriteParsed
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.eclipse.tractusx.bpdm.pool.service.operation.changelog.ChangelogCreateService
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
    private val changelogCreateService: ChangelogCreateService,
    private val changeLogger: PersistedChangeLogger
) {

    /**
     * Persists the staged addresses that changed, emits their CREATE or UPDATE changelog, and returns one result per
     * staged write.
     */
    @Transactional
    fun commit(staged: List<PendingAddressWriteParsed>): List<UpsertResultParsed<LogisticAddressDb>> {
        staged.filter { it.upsertType == UpsertType.Updated }.forEach { it.address.updatedAt = DbTimestamp.now() }
        logisticAddressRepository.saveAll(staged.filter { it.upsertType != UpsertType.NoChange }.map { it.address })

        changelogCreateService.record(staged.mapNotNull {
            when (it.upsertType) {
                UpsertType.Created -> ChangelogEntryRaw(it.address.bpn, ChangelogType.CREATE, BusinessPartnerType.ADDRESS)
                UpsertType.Updated -> ChangelogEntryRaw(it.address.bpn, ChangelogType.UPDATE, BusinessPartnerType.ADDRESS)
                UpsertType.NoChange -> null
            }
        })

        staged.forEach { write ->
            val type = when (write.upsertType) {
                UpsertType.Created -> PersistedChangeType.Created
                UpsertType.Updated -> PersistedChangeType.Updated
                UpsertType.NoChange -> return@forEach
            }
            changeLogger.log(BusinessPartnerChangeRaw(type, BusinessPartnerChangeRaw.Kind.Address, write.address.bpn))
        }

        return staged.map { UpsertResultParsed(it.address, it.upsertType) }
    }
}
