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
import org.eclipse.tractusx.bpdm.pool.model.BpnReferenceAllocation
import org.eclipse.tractusx.bpdm.pool.model.error.*
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressUpsertPlan
import org.eclipse.tractusx.bpdm.pool.model.request.AddressUpdateRequest
import org.eclipse.tractusx.bpdm.pool.model.request.AddressUpsertRequest
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressUpdateParser
import org.eclipse.tractusx.bpdm.pool.service.parser.address.TypedParentAddressCreateParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Decides what a request asks to happen to its additional address: resolve the reference, then hand the content to
 * the parser that owns that operation.
 *
 * An additional address is written whenever it is stated, so unlike the other partners it has no unchanged case.
 */
@Service
class AdditionalAddressUpsertParser(
    private val addressReferenceParser: AddressReferenceParser,
    private val typedParentAddressCreateParser: TypedParentAddressCreateParser,
    private val addressUpdateParser: AddressUpdateParser
) {

    /**
     * Reports the plan for this additional address, or every reason it cannot be carried out.
     */
    @Transactional(readOnly = true)
    fun parse(
        request: AddressUpsertRequest,
        bpnReferences: BpnReferenceAllocation
    ): ParseResult<AddressUpsertPlan, GoldenRecordUpsertParseError> {
        val errors = mutableListOf<GoldenRecordUpsertParseError>()
        return parsePlan(request, bpnReferences, errors).orFailure(errors)
    }

    private fun parsePlan(
        request: AddressUpsertRequest,
        bpnReferences: BpnReferenceAllocation,
        errors: MutableList<GoldenRecordUpsertParseError>
    ): AddressUpsertPlan? {
        val resolved = when (
            val result = addressReferenceParser.parse(request.reference, bpnReferences, ::AdditionalAddressNotFound)
        ) {
            is ParseResult.Failure -> { errors += result.errors; return null }
            is ParseResult.Success -> result.parsed
        }
        val target = resolved.target

        if (target == null) {
            val content = typedParentAddressCreateParser
                .parseContent(listOf(request.content))
                .singleOrRecord(errors, ::toCreateError) ?: return null
            return AddressUpsertPlan.Create(resolved.reference, content)
        }

        val updated = addressUpdateParser
            .parse(listOf(AddressUpdateRequest(target.bpn, siteBpns = null, content = request.content)))
            .singleOrRecord(errors, ::toUpdateError) ?: return null

        return AddressUpsertPlan.Update(resolved.reference, target, updated.address)
    }

    private fun toCreateError(error: AddressCreateParseError): GoldenRecordUpsertParseError =
        when (error) {
            is AddressContentParseError -> AdditionalAddressContentInvalid(error)
            is UnresolvableLegalEntity -> LegalEntityNotFound(error.bpn)
            is UnresolvableSite -> SiteNotFound(error.bpn)
            // Parents are this request's own, so neither a mismatched nor an untyped parent can reach here.
            is SiteNotInAddressLegalEntity -> error("Unexpected parent mismatch for site ${error.siteBpn}")
            is InvalidParentBpn -> error("Unexpected untyped parent ${error.bpn}")
        }

    private fun toUpdateError(error: AddressUpdateParseError): GoldenRecordUpsertParseError =
        when (error) {
            is AddressContentParseError -> AdditionalAddressContentInvalid(error)
            is ScriptVariantCoverageParseError -> ScriptVariantCoverageLost(error)
            // The target was resolved first, and membership is stated once for the record, not by this update.
            is UnresolvableAddress -> error("Unexpected unresolvable address ${error.bpn}")
            is UnresolvableSite -> error("Unexpected unresolvable site ${error.bpn}")
            is SiteMainAddressOmitted -> error("Unexpected omitted main address site ${error.siteBpn}")
            is SiteNotInAddressLegalEntity -> error("Unexpected parent mismatch for site ${error.siteBpn}")
        }
}
