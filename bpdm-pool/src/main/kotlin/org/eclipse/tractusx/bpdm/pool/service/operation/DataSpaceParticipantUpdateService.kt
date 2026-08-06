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

package org.eclipse.tractusx.bpdm.pool.service.operation

import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.model.parsed.DataSpaceParticipantUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.update.AddressContentUpdate
import org.eclipse.tractusx.bpdm.pool.model.update.FieldUpdate
import org.eclipse.tractusx.bpdm.pool.model.update.LegalEntityHeaderUpdate
import org.eclipse.tractusx.bpdm.pool.model.update.LegalEntityUpdate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Sets the data space participation of legal entities, leaving every other part of them alone.
 */
@Service
class DataSpaceParticipantUpdateService(
    private val legalEntityUpdateService: LegalEntityUpdateService
) {

    /**
     * Applies the given participations and reports for each legal entity whether it actually changed.
     */
    @Transactional
    fun update(parsed: List<DataSpaceParticipantUpdateParsed>): List<UpsertResult<LegalEntityDb>> =
        legalEntityUpdateService.update(
            parsed.map {
                LegalEntityUpdate(
                    legalEntity = it.target,
                    header = LegalEntityHeaderUpdate.NoOp.copy(isDataSpaceParticipant = FieldUpdate.Set(it.isDataSpaceParticipant)),
                    legalAddress = AddressContentUpdate.NoOp
                )
            }
        )
}
