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

package org.eclipse.tractusx.bpdm.pool.model.parsed.legalentity

import org.eclipse.tractusx.bpdm.pool.model.raw.businesspartner.FieldUpdateRaw
import org.eclipse.tractusx.bpdm.pool.entity.LegalFormDb
import org.eclipse.tractusx.bpdm.pool.model.raw.legalentity.LegalEntityStateRaw
import org.eclipse.tractusx.bpdm.pool.model.parsed.businesspartner.ConfidenceCriteriaParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.legalentity.LegalEntityHeaderChangeParsed.Companion.NoOp
import java.time.Instant

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
/**
 * A partial update to a legal entity's header: each field is a [FieldUpdateRaw] carrying domain values, applied by
 * `LegalEntityUpdateService`. No default values — a full replace must address every field. [ultimateOwnerBpnl] is derived
 * from [ownershipUltimate] plus the ownership relations, so a payload replace leaves it [FieldUpdateRaw.NoOp] and
 * `UltimateOwnerRecalculationService` sets only it.
 *
 * Build a targeted update by copying from [NoOp], for example
 * `LegalEntityHeaderChangeParsed.NoOp.copy(ultimateOwnerBpnl = FieldUpdateRaw.Set(bpnl))`.
 */
data class LegalEntityHeaderChangeParsed(
    val legalName: FieldUpdateRaw<String>,
    val legalShortName: FieldUpdateRaw<String?>,
    val legalForm: FieldUpdateRaw<LegalFormDb?>,
    val confidenceCriteria: FieldUpdateRaw<ConfidenceCriteriaParsed>,
    val isDataSpaceParticipant: FieldUpdateRaw<Boolean>,
    val ownershipUltimate: FieldUpdateRaw<Boolean>,
    val ultimateOwnerBpnl: FieldUpdateRaw<String?>,
    val currentness: FieldUpdateRaw<Instant>,
    val identifiers: FieldUpdateRaw<List<LegalEntityIdentifierParsed>>,
    val states: FieldUpdateRaw<List<LegalEntityStateRaw>>,
    val scriptVariants: FieldUpdateRaw<List<LegalEntityScriptVariantParsed>>
) {
    companion object {
        val NoOp = LegalEntityHeaderChangeParsed(
            legalName = FieldUpdateRaw.NoOp,
            legalShortName = FieldUpdateRaw.NoOp,
            legalForm = FieldUpdateRaw.NoOp,
            confidenceCriteria = FieldUpdateRaw.NoOp,
            isDataSpaceParticipant = FieldUpdateRaw.NoOp,
            ownershipUltimate = FieldUpdateRaw.NoOp,
            ultimateOwnerBpnl = FieldUpdateRaw.NoOp,
            currentness = FieldUpdateRaw.NoOp,
            identifiers = FieldUpdateRaw.NoOp,
            states = FieldUpdateRaw.NoOp,
            scriptVariants = FieldUpdateRaw.NoOp
        )
    }
}