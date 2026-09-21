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

/**
 * The problems one site creation can be faulted for on its own. Script variant coverage is judged over every address a
 * caller writes, so it is not among them and subtypes [SiteCreateParseError] directly.
 */
sealed interface SiteCreateEntryParseError : SiteCreateParseError

sealed interface SiteUpdateParseError

/**
 * The problems one site update can be faulted for on its own. Script variant coverage is judged over every address a
 * caller writes, so it is not among them and subtypes [SiteUpdateParseError] directly.
 */
sealed interface SiteUpdateEntryParseError : SiteUpdateParseError

data class LegalAddressAlreadyMainAddress(val bpnSite: String) : SiteCreateEntryParseError

data class SiteMainAddressNotLegalAddress(val bpnSite: String, val bpnMainAddress: String) : SiteUpdateEntryParseError

/**
 * The problems the content of one site can be faulted for: its header and its main address.
 */
sealed interface SiteContentParseError : SiteCreateEntryParseError, SiteUpdateEntryParseError

sealed interface SiteHeaderParseError : SiteContentParseError {
    data object NameMissing : SiteHeaderParseError
    data object ConfidenceCriteriaMissing : SiteHeaderParseError
    data class ScriptCodeNotFound(val index: Int, val scriptCode: String) : SiteHeaderParseError
    data class ScriptVariantNameMissing(val index: Int) : SiteHeaderParseError
    data class ScriptVariantDuplicateScriptCode(val index: Int, val scriptCode: String) : SiteHeaderParseError
}
