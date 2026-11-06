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

import org.eclipse.tractusx.bpdm.pool.mapper.ParsedToDbMapper
import org.eclipse.tractusx.bpdm.pool.model.parsed.businesspartner.UpsertResultParsed
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.model.parsed.address.AddressChangeParsed
import org.eclipse.tractusx.bpdm.pool.model.raw.businesspartner.FieldUpdateRaw
import org.eclipse.tractusx.bpdm.pool.model.parsed.address.PendingAddressWriteParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.address.AddressUpdateParsed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Applies a full parsed address-update payload — descriptive content plus an optionally stated site membership — to an
 * already-resolved address, replacing every field the payload covers.
 */
@Service
class AddressPayloadUpdateService(
    private val addressStagedUpdateService: AddressUpdateService,
    private val parsedToDbMapper: ParsedToDbMapper
) {

    /**
     * Applies the given payloads in full and reports for each address whether it actually changed.
     */
    @Transactional
    fun update(parsed: List<AddressUpdateParsed>): List<UpsertResultParsed<LogisticAddressDb>> =
        commit(stageUpdate(parsed))

    /**
     * Applies the payloads in memory without persisting, so a caller can see which addresses changed before handing them
     * to [commit].
     */
    fun stageUpdate(parsed: List<AddressUpdateParsed>): List<PendingAddressWriteParsed> =
        parsed.map { entry ->
            addressStagedUpdateService.stageUpdate(
                AddressChangeParsed(entry.target, parsedToDbMapper.toAddressFullChange(entry.address, entry.sites.toFieldUpdate()))
            )
        }

    /**
     * Persists the staged addresses that changed and emits their UPDATE changelog.
     */
    @Transactional
    fun commit(staged: List<PendingAddressWriteParsed>): List<UpsertResultParsed<LogisticAddressDb>> =
        addressStagedUpdateService.commit(staged)

    private fun List<SiteDb>?.toFieldUpdate(): FieldUpdateRaw<List<SiteDb>> =
        this?.let { FieldUpdateRaw.Set(it) } ?: FieldUpdateRaw.NoOp
}
