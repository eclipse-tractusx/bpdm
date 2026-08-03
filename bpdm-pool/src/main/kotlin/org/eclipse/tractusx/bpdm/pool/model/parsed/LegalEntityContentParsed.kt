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

package org.eclipse.tractusx.bpdm.pool.model.parsed

import org.eclipse.tractusx.bpdm.pool.entity.IdentifierTypeDb
import org.eclipse.tractusx.bpdm.pool.entity.LegalFormDb
import org.eclipse.tractusx.bpdm.pool.entity.ScriptCodeDb
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityState

data class LegalEntityContentParsed(
    val header: LegalEntityHeaderParsed,
    val legalAddress: LogisticAddressParsed
)

data class LegalEntityHeaderParsed(
    val legalName: String,
    val legalShortName: String?,
    val legalForm: LegalFormDb?,
    val identifiers: List<LegalEntityIdentifierParsed>,
    val states: List<LegalEntityState>,
    val confidenceCriteria: ConfidenceCriteriaParsed,
    val isParticipantData: Boolean,
    /** Null means the payload does not state the flag: keep the current value on update, default to false on create. */
    val ownershipUltimate: Boolean?,
    val scriptVariants: List<LegalEntityScriptVariantParsed>
) {
    fun scriptCodes(): List<String> = scriptVariants.map { it.scriptCode.technicalKey }
}

data class LegalEntityIdentifierParsed(
    val value: String,
    val type: IdentifierTypeDb,
    val issuingBody: String?
)

data class LegalEntityScriptVariantParsed(
    val scriptCode: ScriptCodeDb,
    val legalName: String,
    val shortName: String?
)
