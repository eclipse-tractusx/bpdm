/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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
import org.eclipse.tractusx.bpdm.common.model.orRecord
import org.eclipse.tractusx.bpdm.common.model.parseResultOf
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.AdditionalAddressUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.BpnReferenceParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.GoldenRecordTaskError
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.GoldenRecordTaskReferencesParsed
import org.eclipse.tractusx.bpdm.pool.model.raw.address.AddressUpdateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.AddressUpsertRaw
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressContentParser
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressUpdateParser
import org.springframework.stereotype.Service

/**
 * Decides whether a golden record task creates its additional address or writes over it.
 */
@Service
class AdditionalAddressUpsertParser(
    private val addressContentParser: AddressContentParser,
    private val addressUpdateParser: AddressUpdateParser
) {

    /**
     * Reports what the task writes for [additionalAddress], or every problem that stops it.
     */
    fun parse(
        additionalAddress: AddressUpsertRaw,
        references: GoldenRecordTaskReferencesParsed
    ): ParseResult<AdditionalAddressUpsertParsed, GoldenRecordTaskError> {
        val errors = mutableListOf<GoldenRecordTaskError>()

        val parsed = when (val reference = references.resolve(additionalAddress.reference)) {
            is BpnReferenceParsed.Existing ->
                addressUpdateParser.parse(listOf(AddressUpdateRaw(addressBpn = reference.bpn, siteBpns = null, content = additionalAddress.content))).single()
                    .orRecord(errors) { GoldenRecordTaskError.AdditionalAddressUpdateInvalid(it) }
                    ?.let { AdditionalAddressUpsertParsed.Update(reference, it) }
            else ->
                addressContentParser.parse(listOf(additionalAddress.content), listOf(null)).single()
                    .orRecord(errors) { GoldenRecordTaskError.AdditionalAddressCreateInvalid(it) }
                    ?.let { AdditionalAddressUpsertParsed.Create(reference, it) }
        }

        return parseResultOf(parsed, errors)
    }
}
