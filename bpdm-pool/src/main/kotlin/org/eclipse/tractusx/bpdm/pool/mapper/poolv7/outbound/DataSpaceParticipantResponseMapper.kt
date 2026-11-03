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

import org.eclipse.tractusx.bpdm.pool.api.v7.model.participant.DataSpaceParticipantV7Dto
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.springframework.stereotype.Component

/**
 * Maps the results of the data space participant search to the v7 API participant DTOs.
 */
@Component
class DataSpaceParticipantResponseMapper {

    /**
     * Returns the data space participation the given legal entity holds.
     */
    fun toParticipant(legalEntity: LegalEntityDb): DataSpaceParticipantV7Dto =
        DataSpaceParticipantV7Dto(bpnL = legalEntity.bpn, isDataSpaceParticipant = legalEntity.isDataSpaceParticipant)
}
