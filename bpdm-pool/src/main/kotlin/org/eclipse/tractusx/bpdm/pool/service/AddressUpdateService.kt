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

import org.eclipse.tractusx.bpdm.common.util.mapSelectedBatch
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.mapper.entity.AddressEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.AddressContentParsed
import org.eclipse.tractusx.bpdm.pool.model.AddressContentRequest
import org.eclipse.tractusx.bpdm.pool.model.AddressScriptVariantParsed
import org.eclipse.tractusx.bpdm.pool.model.AddressContentParseError
import org.eclipse.tractusx.bpdm.pool.model.AddressContentUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.AddressSiteAssignment
import org.eclipse.tractusx.bpdm.pool.model.AddressUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.LogisticAddressParsed
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
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
    private val addressSiteAssignmentService: AddressSiteAssignmentService,
    private val addressContentUpdateService: AddressContentUpdateService
) {

    fun parseContent(contents: List<AddressContentRequest>, ownerBpns: List<String?>): List<ParseResult<AddressContentParsed, AddressContentParseError>> {
        return addressContentUpdateService.parse(contents, ownerBpns)
    }

    @Transactional
    fun update(parsed: List<AddressUpdateParsed>): List<UpsertResult<LogisticAddressDb>> {
        val assignResults = parsed.mapSelectedBatch(
                select = { entry -> entry.site?.let { AddressSiteAssignment(entry.target, it) } },
                default = false,
                transform = { assignments -> addressSiteAssignmentService.assign(assignments).map { it.upsertType == UpsertType.Updated } },
            )

        val contentUpdateRequests =  parsed.zip(assignResults) { entry, assignResult ->
            AddressContentUpdateParsed(entry.target, entry.address, entry.scriptVariants, !assignResult)
        }
        val contentUpdateResults = addressContentUpdateService.update(contentUpdateRequests)

        return contentUpdateResults.zip(assignResults) { contentResult, siteUpdated ->
            val changed = contentResult.upsertType == UpsertType.Updated || siteUpdated
            contentResult.copy(upsertType = if (changed) UpsertType.Updated else UpsertType.NoChange)
        }
    }
}
