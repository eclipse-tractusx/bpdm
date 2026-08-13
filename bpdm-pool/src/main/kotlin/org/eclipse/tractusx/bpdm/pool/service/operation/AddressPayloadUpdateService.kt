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
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.mapper.entity.AddressUpdateMapper
import org.eclipse.tractusx.bpdm.pool.model.update.AddressUpdate
import org.eclipse.tractusx.bpdm.pool.model.PendingAddressWrite
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressUpdateParsed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Applies a full parsed address-update payload — descriptive content plus an optional site assignment — to an
 * already-resolved address, replacing every field the payload covers.
 */
@Service
class AddressPayloadUpdateService(
    private val addressStagedUpdateService: AddressUpdateService,
    private val addressUpdateMapper: AddressUpdateMapper
) {

    /**
     * Applies the given payloads in full and reports for each address whether it actually changed.
     */
    @Transactional
    fun update(parsed: List<AddressUpdateParsed>): List<UpsertResult<LogisticAddressDb>> =
        commit(stageUpdate(parsed))

    /**
     * Applies the payloads in memory without persisting, so a caller can see which addresses changed before handing them
     * to [commit].
     */
    fun stageUpdate(parsed: List<AddressUpdateParsed>): List<PendingAddressWrite> =
        parsed.map { entry ->
            addressStagedUpdateService.stageUpdate(
                AddressUpdate(entry.target, addressUpdateMapper.toFullUpdate(entry.address, listOfNotNull(entry.site)))
            )
        }

    /**
     * Persists the staged addresses that changed and emits their UPDATE changelog.
     */
    @Transactional
    fun commit(staged: List<PendingAddressWrite>): List<UpsertResult<LogisticAddressDb>> =
        addressStagedUpdateService.commit(staged)
}
