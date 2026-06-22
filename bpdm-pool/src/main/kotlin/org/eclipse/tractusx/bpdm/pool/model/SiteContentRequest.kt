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

package org.eclipse.tractusx.bpdm.pool.model

import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import java.time.Instant

/**
 * Loose (unvalidated) inbound site content: the site header plus its main address. `name`/`confidenceCriteria` are
 * relaxed so a single `parse` validates them (yielding [SiteContentParseError.NameMissing] /
 * [SiteContentParseError.ConfidenceCriteriaMissing]). The main address reuses the shared [AddressContentRequest], so its
 * content is validated by the same address content parser. `parse` turns this into the bounded [SiteContentParsed].
 */
data class SiteContentRequest(
    val name: String?,
    val states: List<SiteState>,
    val confidenceCriteria: ConfidenceCriteriaRequest,
    val scriptVariants: List<SiteScriptVariant>,
    val mainAddress: AddressContentRequest
)

/** Loose site-header script variant: only `scriptCode` needs resolving to its entity; the localized main address travels with [AddressContentRequest]. */
data class SiteScriptVariant(
    val scriptCode: String,
    val name: String
)

/** Reused as-is by both request and parsed stages: a site state has no metadata to resolve and its type is always present. */
data class SiteState(
    val validFrom: Instant?,
    val validTo: Instant?,
    val type: BusinessStateType
)
