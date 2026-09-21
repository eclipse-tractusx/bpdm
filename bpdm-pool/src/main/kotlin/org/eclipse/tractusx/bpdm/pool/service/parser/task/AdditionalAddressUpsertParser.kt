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

package org.eclipse.tractusx.bpdm.pool.service.parser.task

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.error.AdditionalAddressContentInvalid
import org.eclipse.tractusx.bpdm.pool.model.error.AdditionalAddressNotFound
import org.eclipse.tractusx.bpdm.pool.model.error.AdditionalAddressUpsertParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.request.AddressUpsertRequest
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressContentParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Decides what a request asks to happen to its additional address: create it or write over it.
 *
 * An additional address is written whenever it is stated, so unlike the other partners it has no unchanged case.
 */
@Service
class AdditionalAddressUpsertParser(
    private val addressReferenceParser: AddressReferenceParser,
    private val referenceResolutionParser: BpnReferenceResolutionParser,
    private val addressContentParser: AddressContentParser
) {

    /**
     * Reports what is to be written for this additional address, or every reason it cannot be carried out.
     */
    @Transactional(readOnly = true)
    fun parse(
        request: AddressUpsertRequest
    ): ParseResult<AddressUpsertParsed, AdditionalAddressUpsertParseError> {
        val errors = mutableListOf<AdditionalAddressUpsertParseError>()
        return parseUpsert(request, errors).orFailure(errors)
    }

    private fun parseUpsert(
        request: AddressUpsertRequest,
        errors: MutableList<AdditionalAddressUpsertParseError>
    ): AddressUpsertParsed? {
        val addressReference = referenceResolutionParser.parse(request.reference)
        val existingAddress = addressReferenceParser
            .parse(request.reference, ::AdditionalAddressNotFound)
            .parsedOrRecord(errors)?.existingRecord

        val content = addressContentParser
            .parse(listOf(request.content), listOf(existingAddress?.bpn))
            .singleOrRecord(errors, ::AdditionalAddressContentInvalid) ?: return null

        return if (existingAddress == null)
            AddressUpsertParsed.Create(addressReference, content)
        else
            AddressUpsertParsed.Update(addressReference, existingAddress, content)
    }
}
