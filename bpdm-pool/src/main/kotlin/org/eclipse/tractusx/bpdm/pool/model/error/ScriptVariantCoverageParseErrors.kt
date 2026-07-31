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

/**
 * The two ways a write can leave a business partner named in a script its address is not written in.
 *
 * Both subtype every operation that writes an address or a partner name, so one validator serves them all; where an
 * operation cannot reach one of them, its mapper turns it into an internal error.
 */
sealed interface ScriptVariantCoverageParseError :
    LegalEntityCreateParseError,
    LegalEntityUpdateParseError,
    SiteCreateParseError,
    SiteUpdateParseError,
    AddressUpdateParseError

data class ScriptVariantNotCoveredByAddress(val scriptCode: String) : ScriptVariantCoverageParseError

data class ScriptVariantCoverageStillNeeded(val scriptCode: String, val requiredByBpn: String) : ScriptVariantCoverageParseError
