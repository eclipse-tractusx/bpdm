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

import org.eclipse.tractusx.bpdm.pool.entity.ScriptCodeDb
import org.eclipse.tractusx.bpdm.pool.model.SiteState

/**
 * Bounded counterpart of [SiteContentRequest]: the validated [header] plus the main address validated to the shared
 * [AddressContentParsed]. The entity mapper consumes it to build/mutate a site.
 */
data class SiteContentParsed(
    val header: SiteHeaderParsed,
    val mainAddress: AddressContentParsed
)

/**
 * Bounded counterpart of [SiteHeaderRequest]: name/confidence validated to non-null and script-variant codes resolved
 * to entities.
 */
data class SiteHeaderParsed(
    val name: String,
    val states: List<SiteState>,
    val confidenceCriteria: ConfidenceCriteriaParsed,
    val scriptVariants: List<SiteScriptVariantParsed>
)

/** Parsed counterpart of [SiteScriptVariant] with the script code resolved to its entity. */
data class SiteScriptVariantParsed(
    val scriptCode: ScriptCodeDb,
    val name: String
)
