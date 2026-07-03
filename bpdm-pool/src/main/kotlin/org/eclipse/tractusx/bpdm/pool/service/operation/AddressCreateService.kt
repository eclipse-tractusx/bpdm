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
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.mapper.entity.AddressEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateParsed
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.eclipse.tractusx.bpdm.pool.service.BpnIssuingService
import org.eclipse.tractusx.bpdm.pool.service.PartnerChangelogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Creates logistic addresses from already-resolved parents — the single owner of the address-create *operation*. It
 * consumes an [AddressCreateParsed] command (content already validated, parents already resolved to entities) and
 * persists the address. Content validation and parent resolution are the parsers' job
 * ([org.eclipse.tractusx.bpdm.pool.service.parser.TypedParentAddressCreateParser] /
 * [org.eclipse.tractusx.bpdm.pool.service.parser.UntypedParentAddressCreateParser]); in-transaction creators whose
 * parent is not yet persisted build the command themselves and call [create] directly. Order-preserving positional
 * contract (see [org.eclipse.tractusx.bpdm.pool.model.ParseResult]).
 */
@Service
class AddressCreateService(
    private val bpnIssuingService: BpnIssuingService,
    private val logisticAddressRepository: LogisticAddressRepository,
    private val changelogService: PartnerChangelogService,
    private val addressEntityMapper: AddressEntityMapper
) {

    /**
     * Returns the persisted entities (within the caller's transaction) rather than a detached response model: the
     * write is a pure in-transaction collaborator, and turning entities into version-specific responses is the job of
     * the border/application service at the edge. No `UpsertType` here — a create always yields `Created`, unlike update
     * which can be a no-op.
     */
    @Transactional
    fun create(parsed: List<AddressCreateParsed>): List<LogisticAddressDb> {
        val bpns = bpnIssuingService.issueAddressBpns(parsed.size)
        // A freshly created address has no shared history yet, so its sharing-member count starts at zero.
        val entities = parsed.zip(bpns) { entry, bpn -> addressEntityMapper.toEntity(bpn, entry, numberOfSharingMembers = 0) }

        logisticAddressRepository.saveAll(entities)
        changelogService.createChangelogEntries(entities.map {
            ChangelogEntryCreateRequest(it.bpn, ChangelogType.CREATE, BusinessPartnerType.ADDRESS)
        })

        return entities
    }
}