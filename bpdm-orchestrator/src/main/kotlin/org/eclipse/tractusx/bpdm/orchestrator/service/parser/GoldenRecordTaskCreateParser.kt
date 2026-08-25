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

import org.eclipse.tractusx.bpdm.orchestrator.model.GoldenRecordTaskCreateParseError
import org.eclipse.tractusx.bpdm.orchestrator.model.GoldenRecordTaskCreateParsed
import org.eclipse.tractusx.bpdm.orchestrator.model.GoldenRecordTaskCreateRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.ParseResult
import org.eclipse.tractusx.bpdm.orchestrator.repository.SharingMemberRecordRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GoldenRecordTaskCreateParser(
    private val sharingMemberRecordRepository: SharingMemberRecordRepository
) {
    fun parse(
        requests: List<GoldenRecordTaskCreateRequest>
    ): List<ParseResult<GoldenRecordTaskCreateParsed, GoldenRecordTaskCreateParseError>> {
        val parsedRecordIds = requests.map { request ->
            request.recordId?.let { recordId ->
                try {
                    UUID.fromString(recordId)
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
        }
        val existingRecords = sharingMemberRecordRepository.findByPrivateIdIn(parsedRecordIds.filterNotNull().toSet())
            .associateBy { it.privateId }

        return requests.zip(parsedRecordIds).map { (request, recordId) ->
            val requestPath = "requests[${request.index}]"
            val errors = mutableListOf<GoldenRecordTaskCreateParseError>()

            if (request.recordId != null && recordId == null) {
                errors.add(GoldenRecordTaskCreateParseError.InvalidRecordId("$requestPath.recordId", request.recordId))
            }

            val resolvedRecord = if (recordId != null) existingRecords[recordId] else null
            if (recordId != null && resolvedRecord == null) {
                errors.add(GoldenRecordTaskCreateParseError.RecordNotFound("$requestPath.recordId", request.recordId!!))
            }

            if (request.businessPartner.additionalSites.isNotEmpty() && request.businessPartner.site == null) {
                errors.add(GoldenRecordTaskCreateParseError.AdditionalSitesWithoutMainSite("$requestPath.businessPartner"))
            }

            if (errors.isEmpty()) {
                ParseResult.Success(
                    GoldenRecordTaskCreateParsed(
                        mode = request.mode,
                        gateRecord = resolvedRecord,
                        businessPartner = request.businessPartner
                    )
                )
            } else {
                ParseResult.Failure(errors)
            }
        }
    }
}
