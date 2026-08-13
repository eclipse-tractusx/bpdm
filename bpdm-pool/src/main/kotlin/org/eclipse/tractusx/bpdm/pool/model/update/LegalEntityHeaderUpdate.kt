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

package org.eclipse.tractusx.bpdm.pool.model.update

import org.eclipse.tractusx.bpdm.pool.entity.LegalFormDb
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityState
import org.eclipse.tractusx.bpdm.pool.model.parsed.ConfidenceCriteriaParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityIdentifierParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityScriptVariantParsed
import org.eclipse.tractusx.bpdm.pool.model.update.LegalEntityHeaderUpdate.Companion.NoOp
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
 * A partial update to a legal entity's header: each field is a [FieldUpdate] carrying domain values, applied by
 * `LegalEntityUpdateService`. No default values — a full replace must address every field. [ultimateOwnerBpnl] is derived
 * from [ownershipUltimate] plus the ownership relations, so a payload replace leaves it [FieldUpdate.NoOp] and
 * `UltimateOwnerRecalculationService` sets only it.
 *
 * Build a targeted update by copying from [NoOp], for example
 * `LegalEntityHeaderUpdate.NoOp.copy(ultimateOwnerBpnl = FieldUpdate.Set(bpnl))`.
 */
data class LegalEntityHeaderUpdate(
    val legalName: FieldUpdate<String>,
    val legalShortName: FieldUpdate<String?>,
    val legalForm: FieldUpdate<LegalFormDb?>,
    val confidenceCriteria: FieldUpdate<ConfidenceCriteriaParsed>,
    val isDataSpaceParticipant: FieldUpdate<Boolean>,
    val ownershipUltimate: FieldUpdate<Boolean>,
    val ultimateOwnerBpnl: FieldUpdate<String?>,
    val currentness: FieldUpdate<Instant>,
    val identifiers: FieldUpdate<List<LegalEntityIdentifierParsed>>,
    val states: FieldUpdate<List<LegalEntityState>>,
    val scriptVariants: FieldUpdate<List<LegalEntityScriptVariantParsed>>
) {
    companion object {
        val NoOp = LegalEntityHeaderUpdate(
            legalName = FieldUpdate.NoOp,
            legalShortName = FieldUpdate.NoOp,
            legalForm = FieldUpdate.NoOp,
            confidenceCriteria = FieldUpdate.NoOp,
            isDataSpaceParticipant = FieldUpdate.NoOp,
            ownershipUltimate = FieldUpdate.NoOp,
            ultimateOwnerBpnl = FieldUpdate.NoOp,
            currentness = FieldUpdate.NoOp,
            identifiers = FieldUpdate.NoOp,
            states = FieldUpdate.NoOp,
            scriptVariants = FieldUpdate.NoOp
        )
    }
}