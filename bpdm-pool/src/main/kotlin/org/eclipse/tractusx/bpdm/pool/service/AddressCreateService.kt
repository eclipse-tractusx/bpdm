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
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateRequest
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.combine
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Creates logistic addresses in two explicit phases so callers can route validation failures themselves:
 * [parse] validates loose requests and resolves parents to entities; [create] persists already-parsed addresses.
 * Both honour the order-preserving positional list contract (see [ParseResult]).
 */
@Service
class AddressCreateService(
    private val addressRequestParser: LogisticAddressRequestParser,
    private val duplicateValidator: AddressIdentifierDuplicateValidator,
    private val legalEntityRepository: LegalEntityRepository,
    private val siteRepository: SiteRepository,
    private val bpnIssuingService: BpnIssuingService,
    private val logisticAddressRepository: LogisticAddressRepository,
    private val changelogService: PartnerChangelogService,
    private val addressEntityMapper: AddressEntityMapper
) {

    fun parse(requests: List<AddressCreateRequest>): List<ParseResult<AddressCreateParsed, AddressCreateParseError>> {
        val contents = requests.map { it.content }

        val contentResults = addressRequestParser.parse(contents)
        // Created addresses have no own identity yet, so none of their identifiers can be self-duplicates.
        val duplicateErrors = duplicateValidator.validate(contents, ownerBpns = requests.map { null })

        val legalEntitiesByBpn = legalEntityRepository
            .findDistinctByBpnIn(requests.map { it.legalEntityBpn }.toSet())
            .associateBy { it.bpn }
        val sitesByBpn = siteRepository
            .findDistinctByBpnIn(requests.mapNotNull { it.siteBpn }.toSet())
            .associateBy { it.bpn }

        return requests.mapIndexed { index, request ->
            val resolutionErrors = mutableListOf<AddressCreateParseError>()

            val legalEntity = legalEntitiesByBpn[request.legalEntityBpn]
                ?: run { resolutionErrors.add(AddressCreateParseError.UnresolvableLegalEntity(request.legalEntityBpn)); null }
            val site = request.siteBpn?.let { siteBpn ->
                sitesByBpn[siteBpn] ?: run { resolutionErrors.add(AddressCreateParseError.UnresolvableSite(siteBpn)); null }
            }

            val contentResult: ParseResult<AddressContentParsed, AddressCreateParseError> = contentResults[index]
            contentResult.combine(resolutionErrors + duplicateErrors[index]) { content ->
                // Reached only when there are no errors, so the legal entity above is resolved.
                AddressCreateParsed(legalEntity!!, site, content.address, content.scriptVariants)
            }
        }
    }

    /**
     * Convenience for callers that route per-entry failures: [parse] then [create] the successful entries, returning
     * one result per request (positional, see [ParseResult]) where each is either the persisted entity or the parse
     * errors for that entry. `@Transactional` so resolution and persistence share one persistence context.
     */
    @Transactional
    fun parseAndCreate(requests: List<AddressCreateRequest>): List<ParseResult<LogisticAddressDb, AddressCreateParseError>> {
        val parseResults = parse(requests)
        val created = create(parseResults.filterIsInstance<ParseResult.Success<AddressCreateParsed>>().map { it.parsed })

        val createdIterator = created.iterator()
        return parseResults.map { result ->
            when (result) {
                is ParseResult.Success -> ParseResult.Success(createdIterator.next())
                is ParseResult.Failure -> result
            }
        }
    }

    /**
     * Returns the persisted entities (within the caller's transaction) rather than a detached response model: the
     * write is a pure in-transaction collaborator, and turning entities into version-specific responses is the job of
     * the border/application service at the edge. See the address service layering rationale. No `UpsertType` here —
     * a create always yields `Created`, unlike update which can be a no-op.
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
