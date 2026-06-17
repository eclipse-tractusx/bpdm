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

import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.model.AddressContentParsed
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateRequest
import org.eclipse.tractusx.bpdm.pool.model.AddressUpserted
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.combine
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.springframework.stereotype.Service

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
    private val siteRepository: SiteRepository
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

    fun create(parsed: List<AddressCreateParsed>): List<UpsertResult<AddressUpserted>> =
        TODO("create: persist parsed addresses and map to results")
}
