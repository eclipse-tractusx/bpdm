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
import org.eclipse.tractusx.bpdm.common.model.parsedOrNull
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityState
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.BpnReferenceParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.GoldenRecordTaskError
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.GoldenRecordTaskReferencesParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.LegalEntityUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.raw.legalentity.LegalEntityCreateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.legalentity.LegalEntityUpdateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.LegalEntityUpsertHeaderRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.LegalEntityUpsertRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UpsertIntent
import org.eclipse.tractusx.bpdm.pool.model.raw.legalentity.LegalEntityContentRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.legalentity.LegalEntityHeaderRaw
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.LegalEntityBpnParser
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.LegalEntityCreateParser
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.LegalEntityUpdateParser
import org.springframework.stereotype.Service

/**
 * Decides whether a golden record task creates its legal entity, writes over it, or leaves it as it stands.
 */
@Service
class LegalEntityUpsertParser(
    private val legalEntityBpnParser: LegalEntityBpnParser,
    private val legalEntityCreateParser: LegalEntityCreateParser,
    private val legalEntityUpdateParser: LegalEntityUpdateParser
) {

    /**
     * Reports what the task writes for [legalEntity], or every problem that stops it.
     */
    fun parse(
        legalEntity: LegalEntityUpsertRaw,
        references: GoldenRecordTaskReferencesParsed
    ): ParseResult<LegalEntityUpsertParsed, GoldenRecordTaskError> {
        val errors = mutableListOf<GoldenRecordTaskError>()
        return parseResultOf(parseUpsert(legalEntity, references, errors), errors)
    }

    private fun parseUpsert(
        legalEntity: LegalEntityUpsertRaw,
        references: GoldenRecordTaskReferencesParsed,
        errors: MutableList<GoldenRecordTaskError>
    ): LegalEntityUpsertParsed? {
        val reference = references.resolve(legalEntity.reference)
        val legalAddressReference = references.resolve(legalEntity.legalAddress.reference)
        val existingBpn = (reference as? BpnReferenceParsed.Existing)?.bpn
        val existingLegalEntity = existingBpn?.let { legalEntityBpnParser.parse(listOf(it)).single().parsedOrNull() }

        if (existingBpn != null && legalEntity.intent == UpsertIntent.WriteOnlyIfAbsent) {
            if (existingLegalEntity == null) errors += GoldenRecordTaskError.UnchangedLegalEntityNotFound(existingBpn)
            return existingLegalEntity?.let { LegalEntityUpsertParsed.Unchanged(reference, legalAddressReference, it) }
        }

        val isDataSpaceParticipant = legalEntity.header.isDataSpaceParticipant ?: existingLegalEntity?.isDataSpaceParticipant ?: false
        val header = toHeaderRequest(legalEntity.header, isDataSpaceParticipant, errors) ?: return null
        val content = LegalEntityContentRaw(header, legalEntity.legalAddress.content)

        return if (existingBpn == null)
            legalEntityCreateParser.parse(listOf(LegalEntityCreateRaw(content))).single()
                .orRecord(errors) { GoldenRecordTaskError.LegalEntityCreateInvalid(it) }
                ?.let { LegalEntityUpsertParsed.Create(reference, legalAddressReference, it) }
        else
            legalEntityUpdateParser.parse(listOf(LegalEntityUpdateRaw(existingBpn, content))).single()
                .orRecord(errors) { GoldenRecordTaskError.LegalEntityUpdateInvalid(it) }
                ?.let { LegalEntityUpsertParsed.Update(reference, legalAddressReference, it) }
    }

    private fun toHeaderRequest(header: LegalEntityUpsertHeaderRaw, isDataSpaceParticipant: Boolean, errors: MutableList<GoldenRecordTaskError>): LegalEntityHeaderRaw? {
        if (header.states.any { it.type == null }) {
            errors += GoldenRecordTaskError.StateTypeMissing
            return null
        }

        return LegalEntityHeaderRaw(
            legalName = header.legalName,
            legalShortName = header.legalShortName,
            legalForm = header.legalForm,
            identifiers = header.identifiers,
            states = header.states.map { LegalEntityState(it.validFrom, it.validTo, it.type!!) },
            confidenceCriteria = header.confidenceCriteria,
            isDataSpaceParticipant = isDataSpaceParticipant,
            ownershipUltimate = header.ownershipUltimate,
            scriptVariants = header.scriptVariants
        )
    }
}
