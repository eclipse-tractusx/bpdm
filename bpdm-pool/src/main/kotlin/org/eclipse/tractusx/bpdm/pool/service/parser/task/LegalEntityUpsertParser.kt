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
import org.eclipse.tractusx.bpdm.pool.model.error.*
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityUpsertPlan
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityContentRequest
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityCreateRequest
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityUpdateRequest
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityUpsertRequest
import org.eclipse.tractusx.bpdm.pool.model.request.UpsertIntent
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.LegalEntityCreateParser
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.LegalEntityUpdateParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Decides what a request asks to happen to its legal entity: resolve the reference, then hand the content to the
 * parser that owns that operation, or to none where nothing needs writing.
 */
@Service
class LegalEntityUpsertParser(
    private val legalEntityReferenceParser: LegalEntityReferenceParser,
    private val referenceResolutionParser: BpnReferenceResolutionParser,
    private val legalEntityCreateParser: LegalEntityCreateParser,
    private val legalEntityUpdateParser: LegalEntityUpdateParser
) {

    /**
     * Reports the plan for this legal entity, or every reason it cannot be carried out.
     */
    @Transactional(readOnly = true)
    fun parse(
        request: LegalEntityUpsertRequest
    ): ParseResult<LegalEntityUpsertPlan, LegalEntityUpsertParseError> {
        val errors = mutableListOf<LegalEntityUpsertParseError>()
        return parsePlan(request, errors).orFailure(errors)
    }

    private fun parsePlan(
        request: LegalEntityUpsertRequest,
        errors: MutableList<LegalEntityUpsertParseError>
    ): LegalEntityUpsertPlan? {
        val legalAddressReference = referenceResolutionParser.parse(request.legalAddress.reference)
        val resolvedLegalEntity = when (val result = legalEntityReferenceParser.parse(request.reference)) {
            is ParseResult.Failure -> { errors += result.errors; return null }
            is ParseResult.Success -> result.parsed
        }
        val legalEntityReference = resolvedLegalEntity.reference
        val existingLegalEntity = resolvedLegalEntity.existingRecord

        if (existingLegalEntity != null && request.intent == UpsertIntent.WriteOnlyIfAbsent)
            return LegalEntityUpsertPlan.Unchanged(legalEntityReference, legalAddressReference, existingLegalEntity)

        if (existingLegalEntity == null) {
            val created = legalEntityCreateParser
                .parseWithoutScriptVariantCoverage(listOf(LegalEntityCreateRequest(toContentRequest(request))))
                .singleOrRecord(errors, ::toCreateError) ?: return null
            return LegalEntityUpsertPlan.Create(legalEntityReference, legalAddressReference, created.content)
        }

        val updated = legalEntityUpdateParser
            .parseWithoutScriptVariantCoverage(listOf(LegalEntityUpdateRequest(existingLegalEntity.bpn, toContentRequest(request))))
            .singleOrRecord(errors, ::toUpdateError) ?: return null

        return LegalEntityUpsertPlan.Update(legalEntityReference, legalAddressReference, existingLegalEntity, updated.content)
    }

    private fun toContentRequest(request: LegalEntityUpsertRequest) =
        LegalEntityContentRequest(header = request.header, legalAddress = request.legalAddress.content)

    private fun toCreateError(error: LegalEntityCreateEntryParseError): LegalEntityUpsertParseError =
        when (error) {
            is LegalEntityHeaderParseError -> LegalEntityContentInvalid(error)
            is AddressContentParseError -> LegalAddressContentInvalid(error)
        }

    private fun toUpdateError(error: LegalEntityUpdateEntryParseError): LegalEntityUpsertParseError =
        when (error) {
            is LegalEntityHeaderParseError -> LegalEntityContentInvalid(error)
            is AddressContentParseError -> LegalAddressContentInvalid(error)
            is MultipleUltimateOwnersInHierarchy -> MultipleUltimateOwners(error.conflictingBpnls)
            is AlternativeHeadquarterCannotOwnUltimately -> AlternativeHeadquarterCannotOwn(error.bpnl)
            // The existing legal entity was resolved before this parser was called.
            is UnresolvableLegalEntity -> error("Unexpected unresolvable legal entity ${error.bpn}")
        }
}
