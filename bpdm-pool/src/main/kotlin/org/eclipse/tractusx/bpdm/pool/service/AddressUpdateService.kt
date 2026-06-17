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
import org.eclipse.tractusx.bpdm.pool.mapper.AddressUpsertedMapper
import org.eclipse.tractusx.bpdm.pool.model.AddressContentParsed
import org.eclipse.tractusx.bpdm.pool.model.AddressUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.AddressUpdateParseError
import org.eclipse.tractusx.bpdm.pool.model.AddressUpdateRequest
import org.eclipse.tractusx.bpdm.pool.model.AddressUpserted
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.combine
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Updates existing logistic addresses in two explicit phases: [parse] validates loose requests and resolves the update
 * target entity (never re-parents); [update] applies changes to already-parsed addresses. Both honour the order-preserving
 * positional list contract (see [ParseResult]).
 */
@Service
class AddressUpdateService(
    private val addressRequestParser: LogisticAddressRequestParser,
    private val duplicateValidator: AddressIdentifierDuplicateValidator,
    private val logisticAddressRepository: LogisticAddressRepository,
    private val changelogService: PartnerChangelogService,
    private val equivalenceMapper: BusinessPartnerEquivalenceMapper,
    private val addressEntityMapper: AddressEntityMapper,
    private val addressUpsertedMapper: AddressUpsertedMapper
) {

    fun parse(requests: List<AddressUpdateRequest>): List<ParseResult<AddressUpdateParsed, AddressUpdateParseError>> {
        val contents = requests.map { it.content }

        val contentResults = addressRequestParser.parse(contents)
        // An address may legitimately re-submit its own existing identifiers, so its own BPN is excluded from duplicates.
        val duplicateErrors = duplicateValidator.validate(contents, ownerBpns = requests.map { it.addressBpn })

        val targetsByBpn = logisticAddressRepository
            .findDistinctByBpnIn(requests.map { it.addressBpn }.toSet())
            .associateBy { it.bpn }

        return requests.mapIndexed { index, request ->
            val resolutionErrors = mutableListOf<AddressUpdateParseError>()

            val target = targetsByBpn[request.addressBpn]
                ?: run { resolutionErrors.add(AddressUpdateParseError.UnresolvableTarget(request.addressBpn)); null }

            val contentResult: ParseResult<AddressContentParsed, AddressUpdateParseError> = contentResults[index]
            contentResult.combine(resolutionErrors + duplicateErrors[index]) { content ->
                // Reached only when there are no errors, so the target above is resolved.
                AddressUpdateParsed(target!!, content.address, content.scriptVariants)
            }
        }
    }

    @Transactional
    fun update(parsed: List<AddressUpdateParsed>): List<UpsertResult<AddressUpserted>> =
        parsed.map { update(it) }

    private fun update(parsed: AddressUpdateParsed): UpsertResult<AddressUpserted> {
        val target = parsed.target

        val before = equivalenceMapper.toEquivalenceDto(target)
        applyChanges(target, parsed)
        val after = equivalenceMapper.toEquivalenceDto(target)

        val upsertType = if (before != after) {
            logisticAddressRepository.save(target)
            changelogService.createChangelogEntries(listOf(ChangelogEntryCreateRequest(target.bpn, ChangelogType.UPDATE, BusinessPartnerType.ADDRESS)))
            UpsertType.Updated
        } else {
            UpsertType.NoChange
        }

        return UpsertResult(addressUpsertedMapper.toUpserted(target), upsertType)
    }

    private fun applyChanges(target: LogisticAddressDb, parsed: AddressUpdateParsed) {
        val address = parsed.address

        target.name = address.name
        target.physicalPostalAddress = addressEntityMapper.toPhysical(address.physicalPostalAddress)
        target.alternativePostalAddress = address.alternativePostalAddress?.let { addressEntityMapper.toAlternative(it) }
        // The sharing-member count is Pool-maintained, not part of the update payload, so carry the current value forward.
        target.confidenceCriteria = addressEntityMapper.toConfidence(address.confidenceCriteria, target.confidenceCriteria.numberOfSharingMembers)
        target.identifiers.replace(addressEntityMapper.toIdentifiers(address.identifiers, target))
        target.states.replace(addressEntityMapper.toStates(address.states, target))
        target.scriptVariants.replace(addressEntityMapper.toScriptVariants(parsed.scriptVariants))
    }
}
