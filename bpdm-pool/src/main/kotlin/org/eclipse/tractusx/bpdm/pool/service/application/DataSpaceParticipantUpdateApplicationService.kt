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

package org.eclipse.tractusx.bpdm.pool.service.application

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.mapper.DbToOutputMapper
import org.eclipse.tractusx.bpdm.pool.model.output.participation.DataSpaceParticipantUpdateResult
import org.eclipse.tractusx.bpdm.pool.model.parsed.participation.DataSpaceParticipantUpdateError
import org.eclipse.tractusx.bpdm.pool.model.parsed.participation.DataSpaceParticipantUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.raw.participation.DataSpaceParticipantUpdateRaw
import org.eclipse.tractusx.bpdm.pool.service.operation.participation.DataSpaceParticipantUpdateService
import org.eclipse.tractusx.bpdm.pool.service.parser.participation.DataSpaceParticipantUpdateParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Sets the data space participation of legal entities.
 */
@Service
class DataSpaceParticipantUpdateApplicationService(
    private val dataSpaceParticipantUpdateParser: DataSpaceParticipantUpdateParser,
    private val dataSpaceParticipantUpdateService: DataSpaceParticipantUpdateService,
    private val dbToOutputMapper: DbToOutputMapper
) {

    /**
     * Sets the participation of every legal entity [requests] name and returns them as stored, or changes none of them
     * and returns every reason an entry is rejected.
     */
    @Transactional
    fun update(requests: List<DataSpaceParticipantUpdateRaw>): DataSpaceParticipantUpdateResult {
        val parseResults = dataSpaceParticipantUpdateParser.parse(requests)
        val errors = parseResults.filterIsInstance<ParseResult.Failure<DataSpaceParticipantUpdateError>>().flatMap { it.errors }
        if (errors.isNotEmpty()) return DataSpaceParticipantUpdateResult.Rejected(errors)

        val parsed = parseResults.filterIsInstance<ParseResult.Success<DataSpaceParticipantUpdateParsed>>().map { it.parsed }
        val updated = dataSpaceParticipantUpdateService.update(parsed)
        return DataSpaceParticipantUpdateResult.Updated(updated.map { dbToOutputMapper.toParticipantRecord(it.value) })
    }
}
