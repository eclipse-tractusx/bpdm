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

package org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound

import org.eclipse.tractusx.bpdm.common.exception.BpdmMultipleNotFoundException
import org.eclipse.tractusx.bpdm.pool.exception.BpdmDuplicateRequestEntriesException
import org.eclipse.tractusx.bpdm.pool.model.error.DataSpaceParticipantUpdateParseError
import org.eclipse.tractusx.bpdm.pool.model.error.DataSpaceParticipantUpdateParseError.DuplicateParticipantEntry
import org.eclipse.tractusx.bpdm.pool.model.error.UnresolvableLegalEntity
import org.springframework.stereotype.Component

private const val LEGAL_ENTITIES = "Legal Entities"

/**
 * Maps the participation update parser's sealed parse errors to the errors the participation endpoints report them with.
 */
@Component
class DataSpaceParticipantParseErrorMapper {

    /**
     * Returns the exception reporting a failed participation update parse, naming every legal entity the reported error
     * applies to because the update applies as a whole rather than per entry, and reporting a malformed request ahead of
     * an unresolvable legal entity.
     */
    fun toUpdateException(errors: List<DataSpaceParticipantUpdateParseError>): RuntimeException {
        val duplicated = mutableListOf<String>()
        val unresolvable = mutableListOf<String>()

        errors.forEach { error ->
            when (error) {
                is DuplicateParticipantEntry -> duplicated.add(error.legalEntityBpn)
                is UnresolvableLegalEntity -> unresolvable.add(error.bpn)
            }
        }

        return if (duplicated.isNotEmpty())
            BpdmDuplicateRequestEntriesException(LEGAL_ENTITIES, duplicated.distinct())
        else
            BpdmMultipleNotFoundException(LEGAL_ENTITIES, unresolvable.distinct())
    }
}
