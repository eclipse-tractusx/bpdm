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

package org.eclipse.tractusx.bpdm.pool.model.error

sealed interface SiteCreateParseError

sealed interface SiteUpdateParseError

/**
 * The legal entity's legal address already backs site [bpnSite], so it can't also be a new site's main address. Only the
 * "site with legal address as main" create path can raise this; the regular site-create path builds a fresh main address.
 */
data class LegalAddressAlreadyMainAddress(val bpnSite: String) : SiteCreateParseError

/**
 * Errors produced by parsing site *header* content (everything but the main address). As a subtype of both site
 * operations from a single definition, the same content errors flow into create and update without wrapping; the main
 * address contributes its own [AddressContentParseError], which is likewise a site error directly.
 */
sealed interface SiteContentParseError : SiteCreateParseError, SiteUpdateParseError {
    data object NameMissing : SiteContentParseError
    data object ConfidenceCriteriaMissing : SiteContentParseError
    data class ScriptCodeNotFound(val index: Int, val scriptCode: String) : SiteContentParseError
}
