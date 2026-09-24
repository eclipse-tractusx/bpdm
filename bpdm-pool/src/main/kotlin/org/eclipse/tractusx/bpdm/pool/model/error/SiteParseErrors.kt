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

import java.time.LocalDate

sealed interface SiteCreateParseError


sealed interface SiteUpdateParseError


data class LegalAddressAlreadyMainAddress(val bpnSite: String) : SiteCreateParseError

data class SiteMainAddressNotLegalAddress(val bpnSite: String, val bpnMainAddress: String) : SiteUpdateParseError

/**
 * The problems the content of one site can be faulted for: its header and its main address.
 */
sealed interface SiteContentParseError : SiteCreateParseError, SiteUpdateParseError

sealed interface SiteHeaderParseError : SiteContentParseError {
    data object NameMissing : SiteHeaderParseError
    data object ConfidenceCriteriaMissing : SiteHeaderParseError
    data class ScriptCodeNotFound(val index: Int, val scriptCode: String) : SiteHeaderParseError
    data class ScriptVariantNameMissing(val index: Int) : SiteHeaderParseError
    data class ScriptVariantDuplicateScriptCode(val index: Int, val scriptCode: String) : SiteHeaderParseError
}

/**
 * The ways the states a site write states can contradict a succession the site already takes part in.
 */
sealed interface SiteStateRelationParseError : SiteHeaderParseError {
    data class ReplacedSiteRecordedActive(val bpn: String, val successorBpn: String, val validFrom: LocalDate) : SiteStateRelationParseError
    data class ReplacingSiteRecordedInactive(val bpn: String, val predecessorBpn: String, val validFrom: LocalDate) : SiteStateRelationParseError
}
