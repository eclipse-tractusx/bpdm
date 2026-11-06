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

package org.eclipse.tractusx.bpdm.pool.model.parsed.error

sealed interface SiteCreateError

sealed interface SiteUpdateError

data class LegalAddressAlreadyMainAddress(val bpnSite: String) : SiteCreateError

sealed interface SiteContentError : SiteCreateError, SiteUpdateError {
    data object NameMissing : SiteContentError
    data object ConfidenceCriteriaMissing : SiteContentError
    data class ScriptCodeNotFound(val index: Int, val scriptCode: String) : SiteContentError
    data class ScriptVariantNameMissing(val index: Int) : SiteContentError
    data class ScriptVariantDuplicateScriptCode(val index: Int, val scriptCode: String) : SiteContentError
}
