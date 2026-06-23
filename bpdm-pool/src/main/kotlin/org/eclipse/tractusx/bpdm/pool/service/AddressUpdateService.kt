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
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.mapper.AddressEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.AddressContentParsed
import org.eclipse.tractusx.bpdm.pool.model.AddressContentRequest
import org.eclipse.tractusx.bpdm.pool.model.AddressScriptVariantParsed
import org.eclipse.tractusx.bpdm.pool.model.AddressContentParseError
import org.eclipse.tractusx.bpdm.pool.model.AddressUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.LogisticAddressParsed
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.combine
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Updates existing logistic addresses given an already-resolved target. This is the lower, target-injected layer: it
 * validates address *content* and applies changes to a supplied managed entity, but it does not resolve the target by
 * BPN (that is [AdditionalAddressUpdateService]'s job). Update never re-parents. Callers that already hold the managed
 * target use this service directly. Order-preserving positional contract (see [ParseResult]).
 */
@Service
class AddressUpdateService(
    private val addressRequestParser: LogisticAddressRequestParser,
    private val duplicateValidator: AddressIdentifierDuplicateValidator,
    private val logisticAddressRepository: LogisticAddressRepository,
    private val changelogService: PartnerChangelogService,
    private val equivalenceMapper: BusinessPartnerEquivalenceMapper,
    private val addressEntityMapper: AddressEntityMapper
) {

    /**
     * Validates address content only (presence/format, metadata resolution, identifier duplicates). [ownerBpns] is the
     * BPN of each entry's update target, so an address may legitimately re-submit its own existing identifiers.
     */
    fun parseContent(contents: List<AddressContentRequest>, ownerBpns: List<String?>): List<ParseResult<AddressContentParsed, AddressContentParseError>> {
        val contentResults = addressRequestParser.parse(contents)
        val duplicateErrors = duplicateValidator.validate(contents, ownerBpns)
        return contentResults.mapIndexed { index, result -> result.combine(duplicateErrors[index]) { it } }
    }

    /**
     * Returns the updated entities (within the caller's transaction) rather than a detached response model: the write
     * is a pure in-transaction collaborator, and building version-specific responses is the job of the border/application
     * service at the edge.
     */
    @Transactional
    fun update(parsed: List<AddressUpdateParsed>): List<UpsertResult<LogisticAddressDb>> =
        parsed.map { update(it) }

    private fun update(parsed: AddressUpdateParsed): UpsertResult<LogisticAddressDb> {
        val target = parsed.target

        val before = equivalenceMapper.toEquivalenceDto(target)
        // The sharing-member count is Pool-maintained, not part of the update payload, so carry the current value forward.
        applyTo(target, parsed.address, parsed.scriptVariants, target.confidenceCriteria.numberOfSharingMembers)
        parsed.site?.run { target.additionalSites.add(parsed.site) }
        val after = equivalenceMapper.toEquivalenceDto(target)

        val upsertType = if (before != after) {
            logisticAddressRepository.save(target)
            changelogService.createChangelogEntries(listOf(ChangelogEntryCreateRequest(target.bpn, ChangelogType.UPDATE, BusinessPartnerType.ADDRESS)))
            UpsertType.Updated
        } else {
            UpsertType.NoChange
        }

        return UpsertResult(target, upsertType)
    }

    /**
     * Applies parsed address content onto a managed entity using the pure mapper sub-builders. Does not save or emit a
     * changelog — callers that own aggregate-level change detection (legal entity / site) reuse this and decide
     * persistence themselves.
     */
    fun applyTo(target: LogisticAddressDb, address: LogisticAddressParsed, scriptVariants: List<AddressScriptVariantParsed>, numberOfSharingMembers: Int) {
        target.name = address.name
        target.physicalPostalAddress = addressEntityMapper.toPhysical(address.physicalPostalAddress)
        target.alternativePostalAddress = address.alternativePostalAddress?.let { addressEntityMapper.toAlternative(it) }
        target.confidenceCriteria = addressEntityMapper.toConfidence(address.confidenceCriteria, numberOfSharingMembers)
        target.identifiers.replace(addressEntityMapper.toIdentifiers(address.identifiers, target))
        target.states.replace(addressEntityMapper.toStates(address.states, target))
        target.scriptVariants.replace(addressEntityMapper.toScriptVariants(scriptVariants))
    }

    /**
     * Executes [update] for the successfully parsed entries and weaves the results back into a positional list aligned
     * with the input; failures pass through unchanged. Generic in the error type so both this service and
     * [AdditionalAddressUpdateService] (whose errors are wider) can reuse it.
     */
    fun <E> parseAndUpdate(parseResults: List<ParseResult<AddressUpdateParsed, E>>): List<ParseResult<UpsertResult<LogisticAddressDb>, E>> {
        val updated = update(parseResults.filterIsInstance<ParseResult.Success<AddressUpdateParsed>>().map { it.parsed })

        val updatedIterator = updated.iterator()
        return parseResults.map { result ->
            when (result) {
                is ParseResult.Success -> ParseResult.Success(updatedIterator.next())
                is ParseResult.Failure -> result
            }
        }
    }
}
