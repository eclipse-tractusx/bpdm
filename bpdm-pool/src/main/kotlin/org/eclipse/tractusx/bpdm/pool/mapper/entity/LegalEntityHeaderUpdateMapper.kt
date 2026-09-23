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

import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityHeaderParsed
import org.eclipse.tractusx.bpdm.pool.model.update.FieldUpdate
import org.eclipse.tractusx.bpdm.pool.model.update.LegalEntityHeaderUpdate
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Builds the header change that fully replaces a legal entity's header with the given parsed content.
 *
 * Every field the payload states is set; the derived ultimate-owner BPNL is left alone, because ownership recalculation
 * maintains it rather than the payload. The caller supplies [currentness], an impure "now", so that this mapper stays
 * pure.
 */
@Component
class LegalEntityHeaderUpdateMapper {

    fun toFullUpdate(header: LegalEntityHeaderParsed, currentness: Instant) = LegalEntityHeaderUpdate(
        legalName = FieldUpdate.Set(header.legalName),
        legalShortName = FieldUpdate.Set(header.legalShortName),
        legalForm = FieldUpdate.Set(header.legalForm),
        confidenceCriteria = FieldUpdate.Set(header.confidenceCriteria),
        isDataSpaceParticipant = FieldUpdate.Set(header.isDataSpaceParticipant),
        ownershipUltimate = FieldUpdate.Set(header.ownershipUltimate),
        ultimateOwnerBpnl = FieldUpdate.NoOp,
        currentness = FieldUpdate.Set(currentness),
        identifiers = FieldUpdate.Set(header.identifiers),
        states = FieldUpdate.Set(header.states),
        scriptVariants = FieldUpdate.Set(header.scriptVariants)
    )
}
