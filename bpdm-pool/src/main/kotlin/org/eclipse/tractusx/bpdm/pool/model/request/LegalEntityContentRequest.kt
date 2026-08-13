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

data class LegalEntityContentRequest(
    val header: LegalEntityHeaderRequest,
    val legalAddress: LogisticAddressRequest
)

data class LegalEntityHeaderRequest(
    val legalName: String?,
    val legalShortName: String?,
    val legalForm: String?,
    val identifiers: List<LegalEntityIdentifier>,
    val states: List<LegalEntityState>,
    val confidenceCriteria: ConfidenceCriteriaRequest,
    val isDataSpaceParticipant: Boolean,
    /** Null means the request does not state the flag, so an update leaves it untouched (v6 cannot express it at all). */
    val ownershipUltimate: Boolean?,
    val scriptVariants: List<LegalEntityScriptVariant>
)

data class LegalEntityIdentifier(
    val value: String?,
    val type: String?,
    val issuingBody: String?
)

data class LegalEntityScriptVariant(
    val scriptCode: String,
    val legalName: String?,
    val shortName: String?
)
