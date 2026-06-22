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

package org.eclipse.tractusx.bpdm.pool.service

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.mapper.AddressEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.AddressContentParsed
import org.eclipse.tractusx.bpdm.pool.model.AddressContentRequest
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.AddressContentParseError
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.combine
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Creates logistic addresses from already-resolved parents. This is the lower, parent-injected layer: it validates
 * address *content* and persists the address, but it does not resolve any parent (that is [AdditionalAddressCreateService]'s
 * job). Callers that already hold the parent entity — including in-transaction creators whose parent is not yet
 * persisted — use this service directly. All methods honour the order-preserving positional list contract (see
 * [ParseResult]).
 */
@Service
class AddressCreateService(
    private val addressRequestParser: LogisticAddressRequestParser,
    private val duplicateValidator: AddressIdentifierDuplicateValidator,
    private val bpnIssuingService: BpnIssuingService,
    private val logisticAddressRepository: LogisticAddressRepository,
    private val changelogService: PartnerChangelogService,
    private val addressEntityMapper: AddressEntityMapper
) {

    /**
     * Validates address content only (presence/format, metadata resolution, identifier duplicates). Created addresses
     * have no identity yet, so none of their identifiers can be self-duplicates (owner BPN is null).
     */
    fun parseContent(contents: List<AddressContentRequest>): List<ParseResult<AddressContentParsed, AddressContentParseError>> {
        val contentResults = addressRequestParser.parse(contents)
        val duplicateErrors = duplicateValidator.validate(contents, ownerBpns = contents.map { null })
        return contentResults.mapIndexed { index, result -> result.combine(duplicateErrors[index]) { it } }
    }

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

    /**
     * Executes [create] for the successfully parsed entries and weaves the persisted entities back into a positional
     * list aligned with the input; failures pass through unchanged (via [ParseResult]'s `out T` covariance). Generic in
     * the error type so both this service and [AdditionalAddressCreateService] (whose errors are wider) can reuse it.
     */
    fun <E> parseAndCreate(parseResults: List<ParseResult<AddressCreateParsed, E>>): List<ParseResult<LogisticAddressDb, E>> {
        val created = create(parseResults.filterIsInstance<ParseResult.Success<AddressCreateParsed>>().map { it.parsed })

        val createdIterator = created.iterator()
        return parseResults.map { result ->
            when (result) {
                is ParseResult.Success -> ParseResult.Success(createdIterator.next())
                is ParseResult.Failure -> result
            }
        }
    }
}
