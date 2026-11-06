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

package org.eclipse.tractusx.bpdm.pool.service.parser.participation

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.GoldenRecordCountedUpdateError
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.GoldenRecordCountedUpdateError.SharingMemberRecordNotFound
import org.eclipse.tractusx.bpdm.pool.model.parsed.participation.GoldenRecordCountedUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.raw.participation.SharingMemberRecordRaw
import org.eclipse.tractusx.bpdm.pool.repository.SharingMemberRecordRepository
import org.springframework.stereotype.Service

/**
 * Resolves the sharing member records whose golden record counted flag the Orchestrator reports.
 */
@Service
class GoldenRecordCountedUpdateParser(
    private val sharingMemberRecordRepository: SharingMemberRecordRepository
) {

    /**
     * Reports for each of [records] either the stored sharing member record with the flag to set on it, or that the
     * Pool does not know the record.
     */
    fun parse(records: List<SharingMemberRecordRaw>): List<ParseResult<GoldenRecordCountedUpdateParsed, GoldenRecordCountedUpdateError>> {
        val storedRecords = sharingMemberRecordRepository.findByRecordIdIn(records.map { it.recordId }.toSet()).associateBy { it.recordId }

        return records.map { record ->
            storedRecords[record.recordId]
                ?.let { ParseResult.Success(GoldenRecordCountedUpdateParsed(it, record.isGoldenRecordCounted)) }
                ?: ParseResult.ofSingleFailure(SharingMemberRecordNotFound(record.recordId))
        }
    }
}
