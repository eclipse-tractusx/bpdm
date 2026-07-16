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

package org.eclipse.tractusx.bpdm.pool.model.request

import org.eclipse.tractusx.bpdm.pool.model.LegalEntityState

/**
 * Loose (unvalidated) inbound legal-entity content: the legal-entity [header] plus its [legalAddress]. The two are parsed
 * independently — the header by [org.eclipse.tractusx.bpdm.pool.service.parser.LegalEntityHeaderParser], the legal address by the
 * shared address content parser — then recombined into the bounded [org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityContentParsed].
 */
data class LegalEntityContentRequest(
    val header: LegalEntityHeaderRequest,
    val legalAddress: AddressContentRequest
)

/**
 * Loose legal-entity header (everything but the legal address). `legalName`/`confidenceCriteria` are relaxed so the header
 * parse validates them (yielding [org.eclipse.tractusx.bpdm.pool.model.error.LegalEntityContentParseError.NameMissing] /
 * [org.eclipse.tractusx.bpdm.pool.model.error.LegalEntityContentParseError.ConfidenceCriteriaMissing]); `legalForm` and identifier/script-code references are loose
 * strings the parser resolves to metadata entities.
 */
data class LegalEntityHeaderRequest(
    val legalName: String?,
    val legalShortName: String?,
    val legalForm: String?,
    val identifiers: List<LegalEntityIdentifier>,
    val states: List<LegalEntityState>,
    val confidenceCriteria: ConfidenceCriteriaRequest,
    val isParticipantData: Boolean,
    val scriptVariants: List<LegalEntityScriptVariant>
)

/** Loose legal-entity identifier: `value`/`type` are validated by the parser, `type` additionally resolved to its entity; `issuingBody` is free text (legal-entity-only — addresses have no issuing body). */
data class LegalEntityIdentifier(
    val value: String?,
    val type: String?,
    val issuingBody: String?
)

/** Loose legal-entity-header script variant: only `scriptCode` needs resolving to its entity; the localized legal address travels with [AddressContentRequest]. */
data class LegalEntityScriptVariant(
    val scriptCode: String,
    val legalName: String?,
    val shortName: String?
)
