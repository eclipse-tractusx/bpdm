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

package org.eclipse.tractusx.bpdm.orchestrator.service.parser

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.zipParseResults
import org.eclipse.tractusx.bpdm.orchestrator.entity.SharingMemberRecordDb
import org.eclipse.tractusx.bpdm.orchestrator.model.BusinessPartnerRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.error.GoldenRecordTaskCreateParseError
import org.eclipse.tractusx.bpdm.orchestrator.model.parsed.GoldenRecordTaskCreateParsed
import org.eclipse.tractusx.bpdm.orchestrator.model.request.GoldenRecordTaskCreateRequest
import org.eclipse.tractusx.bpdm.orchestrator.repository.SharingMemberRecordRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class GoldenRecordTaskCreateParser(
    private val sharingMemberRecordRepository: SharingMemberRecordRepository
) {

    fun parse(requests: List<GoldenRecordTaskCreateRequest>): List<ParseResult<GoldenRecordTaskCreateParsed, GoldenRecordTaskCreateParseError>> {
        val businessPartnerResults = requests.map { validateBusinessPartner(it.businessPartner) }
        val gateRecordResults = resolveGateRecords(requests.map { it.recordId })

        return zipParseResults(businessPartnerResults, gateRecordResults) { businessPartner, gateRecord ->
            GoldenRecordTaskCreateParsed(existingGateRecord = gateRecord, businessPartner = businessPartner)
        }
    }

    private fun validateBusinessPartner(businessPartner: BusinessPartnerRequest): ParseResult<BusinessPartnerRequest, GoldenRecordTaskCreateParseError> =
        if (businessPartner.additionalSites.isNotEmpty() && businessPartner.site == null)
            ParseResult.ofSingleFailure(GoldenRecordTaskCreateParseError.AdditionalSitesWithoutSite)
        else
            ParseResult.Success(businessPartner)

    private fun resolveGateRecords(recordIds: List<String?>): List<ParseResult<SharingMemberRecordDb?, GoldenRecordTaskCreateParseError>> {
        val parsedUuids = recordIds.map { recordId -> recordId?.let { toUuidOrNull(it) } }

        val recordsByPrivateId = parsedUuids.filterNotNull().toSet()
            .let { sharingMemberRecordRepository.findByPrivateIdIn(it) }
            .associateBy { it.privateId }

        return recordIds.zip(parsedUuids).map { (recordId, uuid) ->
            when {
                recordId == null -> ParseResult.Success(null)
                uuid == null -> ParseResult.ofSingleFailure(GoldenRecordTaskCreateParseError.RecordIdInvalid(recordId))
                else -> recordsByPrivateId[uuid]?.let { ParseResult.Success(it) }
                    ?: ParseResult.ofSingleFailure(GoldenRecordTaskCreateParseError.RecordNotFound(recordId))
            }
        }
    }

    private fun toUuidOrNull(uuidString: String): UUID? =
        try {
            UUID.fromString(uuidString)
        } catch (_: IllegalArgumentException) {
            null
        }
}
