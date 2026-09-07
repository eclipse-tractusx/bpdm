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
import org.eclipse.tractusx.bpdm.orchestrator.model.error.RelationsGoldenRecordTaskCreateParseError
import org.eclipse.tractusx.bpdm.orchestrator.model.parsed.RelationsGoldenRecordTaskCreateParsed
import org.eclipse.tractusx.bpdm.orchestrator.model.request.RelationsGoldenRecordTaskCreateRequest
import org.eclipse.tractusx.bpdm.orchestrator.service.SharingMemberRecordResolutionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class RelationsGoldenRecordTaskCreateParser(
    private val sharingMemberRecordResolutionService: SharingMemberRecordResolutionService
) {

    fun parse(requests: List<RelationsGoldenRecordTaskCreateRequest>): List<ParseResult<RelationsGoldenRecordTaskCreateParsed, RelationsGoldenRecordTaskCreateParseError>> {
        val businessPartnerRelationsResults = requests.map { ParseResult.Success(it.businessPartnerRelations) }
        val gateRecordResults = sharingMemberRecordResolutionService.resolveGateRecords(
            requests.map { it.recordId },
            { RelationsGoldenRecordTaskCreateParseError.RecordIdInvalid(it) },
            { RelationsGoldenRecordTaskCreateParseError.RecordNotFound(it) }
        )

        return zipParseResults(businessPartnerRelationsResults, gateRecordResults) { businessPartnerRelations, gateRecord ->
            RelationsGoldenRecordTaskCreateParsed(
                existingGateRecord = gateRecord,
                businessPartnerRelations = businessPartnerRelations
            )
        }
    }
}
