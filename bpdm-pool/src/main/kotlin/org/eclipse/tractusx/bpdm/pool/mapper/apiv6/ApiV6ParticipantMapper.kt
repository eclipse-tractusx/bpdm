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

package org.eclipse.tractusx.bpdm.pool.mapper.apiv6

import org.eclipse.tractusx.bpdm.pool.api.v6.model.participant.CxMembershipV6Dto
import org.eclipse.tractusx.bpdm.common.exception.BpdmMultipleNotFoundException
import org.eclipse.tractusx.bpdm.pool.exception.BpdmDuplicateRequestEntriesException
import org.eclipse.tractusx.bpdm.pool.model.output.participation.DataSpaceParticipantRecord
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.DataSpaceParticipantUpdateError
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.DataSpaceParticipantUpdateError.DuplicateParticipantEntry
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.DataSpaceParticipantUpdateError.LegalEntityNotFound
import org.springframework.stereotype.Component

private const val LEGAL_ENTITIES = "Legal Entities"

/**
 * Maps data space participation records and rejections to their API v6 Catena-X membership form.
 */
@Component
class ApiV6ParticipantMapper {

    /**
     * Returns the API DTO for [record].
     */
    fun toDto(record: DataSpaceParticipantRecord): CxMembershipV6Dto =
        CxMembershipV6Dto(bpnL = record.legalEntityBpn, isCatenaXMember = record.isDataSpaceParticipant)

    /**
     * Returns the exception the API reports a rejected participation update with, naming every legal entity the reported
     * error applies to because the update applies as a whole, and reporting a malformed request ahead of an unresolvable
     * legal entity.
     */
    fun toUpdateException(errors: List<DataSpaceParticipantUpdateError>): RuntimeException {
        val duplicated = mutableListOf<String>()
        val unresolvable = mutableListOf<String>()

        errors.forEach { error ->
            when (error) {
                is DuplicateParticipantEntry -> duplicated.add(error.legalEntityBpn)
                is LegalEntityNotFound -> unresolvable.add(error.legalEntityBpn)
            }
        }

        return if (duplicated.isNotEmpty())
            BpdmDuplicateRequestEntriesException(LEGAL_ENTITIES, duplicated.distinct())
        else
            BpdmMultipleNotFoundException(LEGAL_ENTITIES, unresolvable.distinct())
    }
}
