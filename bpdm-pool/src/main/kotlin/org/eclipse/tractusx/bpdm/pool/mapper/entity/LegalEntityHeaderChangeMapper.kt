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

import org.eclipse.tractusx.bpdm.pool.model.parsed.legalentity.LegalEntityHeaderParsed
import org.eclipse.tractusx.bpdm.pool.model.raw.businesspartner.FieldUpdateRaw
import org.eclipse.tractusx.bpdm.pool.model.parsed.legalentity.LegalEntityHeaderChangeParsed
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Builds a [LegalEntityHeaderChangeParsed] that fully replaces a legal entity's header from parsed content. Every payload
 * field is [FieldUpdateRaw.Set]; the derived [LegalEntityHeaderChangeParsed.ultimateOwnerBpnl] is left [FieldUpdateRaw.NoOp] because
 * it is maintained by ownership recalculation, not the payload. [currentness] is supplied by the caller (an impure
 * "now") to keep this mapper pure.
 */
@Component
class LegalEntityHeaderChangeMapper {

    fun toFullUpdate(header: LegalEntityHeaderParsed, currentness: Instant) = LegalEntityHeaderChangeParsed(
        legalName = FieldUpdateRaw.Set(header.legalName),
        legalShortName = FieldUpdateRaw.Set(header.legalShortName),
        legalForm = FieldUpdateRaw.Set(header.legalForm),
        confidenceCriteria = FieldUpdateRaw.Set(header.confidenceCriteria),
        isDataSpaceParticipant = FieldUpdateRaw.Set(header.isDataSpaceParticipant),
        // A payload that does not state the ownership flag must not clear it (V6 can never state it).
        ownershipUltimate = header.ownershipUltimate?.let { FieldUpdateRaw.Set(it) } ?: FieldUpdateRaw.NoOp,
        ultimateOwnerBpnl = FieldUpdateRaw.NoOp,
        currentness = FieldUpdateRaw.Set(currentness),
        identifiers = FieldUpdateRaw.Set(header.identifiers),
        states = FieldUpdateRaw.Set(header.states),
        scriptVariants = FieldUpdateRaw.Set(header.scriptVariants)
    )
}
