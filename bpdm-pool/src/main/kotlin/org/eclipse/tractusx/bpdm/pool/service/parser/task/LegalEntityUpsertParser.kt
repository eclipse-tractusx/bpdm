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
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.model.error.*
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityContentWrite
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityUpdateContentWrite
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityContentParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityContentRequest
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityUpsertRequest
import org.eclipse.tractusx.bpdm.pool.model.request.UpsertIntent
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.LegalEntityContentParser
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.LegalEntityUpdateContentParser
import org.eclipse.tractusx.bpdm.pool.util.orFailure
import org.eclipse.tractusx.bpdm.pool.util.parsedOrRecord
import org.eclipse.tractusx.bpdm.pool.util.singleOrRecord
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Decides what a request asks to happen to its legal entity: create it, write over it, or leave it as it stands.
 */
@Service
class LegalEntityUpsertParser(
    private val legalEntityReferenceParser: LegalEntityReferenceParser,
    private val bpnReferenceParser: BpnReferenceParser,
    private val legalEntityContentParser: LegalEntityContentParser,
    private val updateContentParser: LegalEntityUpdateContentParser
) {

    /**
     * Reports what is to be written for this legal entity, or every reason it cannot be carried out.
     */
    @Transactional(readOnly = true)
    fun parse(
        request: LegalEntityUpsertRequest
    ): ParseResult<LegalEntityUpsertParsed, LegalEntityUpsertParseError> {
        val errors = mutableListOf<LegalEntityUpsertParseError>()
        return parseUpsert(request, errors).orFailure(errors)
    }

    private fun parseUpsert(
        request: LegalEntityUpsertRequest,
        errors: MutableList<LegalEntityUpsertParseError>
    ): LegalEntityUpsertParsed? {
        val legalEntityReference = bpnReferenceParser.parse(request.reference)
        val legalAddressReference = bpnReferenceParser.parse(request.legalAddress.reference)
        val existingLegalEntity = legalEntityReferenceParser.parse(request.reference).parsedOrRecord(errors)?.existingRecord

        if (existingLegalEntity != null && request.intent == UpsertIntent.WriteOnlyIfAbsent)
            return LegalEntityUpsertParsed.Unchanged(legalEntityReference, legalAddressReference, existingLegalEntity)

        val content = LegalEntityContentRequest(request.header, request.legalAddress.content)

        return when (existingLegalEntity) {
            null -> parseCreate(content, errors)?.let { LegalEntityUpsertParsed.Create(legalEntityReference, legalAddressReference, it) }
            else -> parseUpdate(content, existingLegalEntity, errors)
                ?.let { LegalEntityUpsertParsed.Update(legalEntityReference, legalAddressReference, it) }
        }
    }

    private fun parseCreate(
        content: LegalEntityContentRequest,
        errors: MutableList<LegalEntityUpsertParseError>
    ): LegalEntityContentParsed? =
        legalEntityContentParser.parse(listOf(LegalEntityContentWrite(content, existingLegalEntity = null)))
            .singleOrRecord(errors, ::toContentError)

    private fun parseUpdate(
        content: LegalEntityContentRequest,
        target: LegalEntityDb,
        errors: MutableList<LegalEntityUpsertParseError>
    ): LegalEntityUpdateParsed? =
        updateContentParser.parse(listOf(LegalEntityUpdateContentWrite(content, target)))
            .singleOrRecord(errors, ::toUpdateContentError)

    private fun toUpdateContentError(error: LegalEntityUpdateContentParseError): LegalEntityUpsertParseError =
        when (error) {
            is LegalEntityContentParseError -> toContentError(error)
            is MultipleUltimateOwnersInHierarchy -> MultipleUltimateOwners(error.conflictingBpnls)
            is AlternativeHeadquarterCannotOwnUltimately -> AlternativeHeadquarterCannotOwn(error.bpnl)
        }

    private fun toContentError(error: LegalEntityContentParseError): LegalEntityUpsertParseError =
        when (error) {
            is LegalEntityHeaderParseError -> LegalEntityContentInvalid(error)
            is AddressContentParseError -> LegalAddressContentInvalid(error)
        }
}
