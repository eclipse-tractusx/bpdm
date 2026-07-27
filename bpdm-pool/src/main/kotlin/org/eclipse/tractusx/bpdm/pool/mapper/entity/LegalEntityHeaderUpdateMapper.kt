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

package org.eclipse.tractusx.bpdm.pool.mapper.entity

import org.eclipse.tractusx.bpdm.pool.model.update.FieldUpdate
import org.eclipse.tractusx.bpdm.pool.model.update.LegalEntityHeaderUpdate
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityHeaderParsed
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Builds a [LegalEntityHeaderUpdate] that fully replaces a legal entity's header from parsed content. Every payload
 * field is [FieldUpdate.Set]; the derived [LegalEntityHeaderUpdate.ultimateOwnerBpnl] is left [FieldUpdate.NoOp] because
 * it is maintained by ownership recalculation, not the payload. [currentness] is supplied by the caller (an impure
 * "now") to keep this mapper pure.
 */
@Component
class LegalEntityHeaderUpdateMapper {

    fun toFullUpdate(header: LegalEntityHeaderParsed, currentness: Instant) = LegalEntityHeaderUpdate(
        legalName = FieldUpdate.Set(header.legalName),
        legalShortName = FieldUpdate.Set(header.legalShortName),
        legalForm = FieldUpdate.Set(header.legalForm),
        confidenceCriteria = FieldUpdate.Set(header.confidenceCriteria),
        isCatenaXMemberData = FieldUpdate.Set(header.isParticipantData),
        // A payload that does not state the ownership flag must not clear it (V6 can never state it).
        ownershipUltimate = header.ownershipUltimate?.let { FieldUpdate.Set(it) } ?: FieldUpdate.NoOp,
        ultimateOwnerBpnl = FieldUpdate.NoOp,
        currentness = FieldUpdate.Set(currentness),
        identifiers = FieldUpdate.Set(header.identifiers),
        states = FieldUpdate.Set(header.states),
        scriptVariants = FieldUpdate.Set(header.scriptVariants)
    )
}
