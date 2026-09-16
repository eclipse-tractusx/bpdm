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
 * Why a golden record upsert cannot be carried out.
 *
 * A request states up to three addresses, so a content error is reported against the partner it was found on rather
 * than on its own: the same missing city means something different on a legal address than on an additional one.
 */
sealed interface GoldenRecordUpsertParseError

data class LegalEntityContentInvalid(val error: LegalEntityContentParseError) : GoldenRecordUpsertParseError

data class LegalAddressContentInvalid(val error: AddressContentParseError) : GoldenRecordUpsertParseError

data class SiteContentInvalid(val error: SiteContentParseError) : GoldenRecordUpsertParseError

data class SiteMainAddressContentInvalid(val error: AddressContentParseError) : GoldenRecordUpsertParseError

data class AdditionalAddressContentInvalid(val error: AddressContentParseError) : GoldenRecordUpsertParseError

data class MembershipSiteContentInvalid(val index: Int, val error: SiteContentParseError) : GoldenRecordUpsertParseError

data class LegalAddressCoverageLost(val error: ScriptVariantCoverageParseError) : GoldenRecordUpsertParseError

data class SiteMainAddressCoverageLost(val error: ScriptVariantCoverageParseError) : GoldenRecordUpsertParseError

data class LegalEntityNotFound(val bpn: String) : GoldenRecordUpsertParseError

data class SiteNotFound(val bpn: String) : GoldenRecordUpsertParseError

data class SiteMainAddressNotFound(val bpn: String) : GoldenRecordUpsertParseError

data class AdditionalAddressNotFound(val bpn: String) : GoldenRecordUpsertParseError

data class MembershipSiteNotFound(val index: Int, val bpn: String) : GoldenRecordUpsertParseError

data class SiteNotInRequestLegalEntity(val siteBpn: String, val legalEntityBpn: String?) : GoldenRecordUpsertParseError

data class AdditionalAddressNotInRequestLegalEntity(val addressBpn: String, val legalEntityBpn: String?) : GoldenRecordUpsertParseError

data class MultipleUltimateOwners(val conflictingBpnls: List<String>) : GoldenRecordUpsertParseError

data class AlternativeHeadquarterCannotOwn(val bpnl: String) : GoldenRecordUpsertParseError

data class ScriptVariantCoverageLost(val error: ScriptVariantCoverageParseError) : GoldenRecordUpsertParseError

data class MembershipSiteNotInLegalEntity(val siteBpn: String, val legalEntityBpn: String?) : GoldenRecordUpsertParseError

data object SiteMainAddressRestatesLegalAddress : GoldenRecordUpsertParseError

data class SiteDoesNotSitOnLegalAddress(val siteBpn: String, val mainAddressBpn: String) : GoldenRecordUpsertParseError

data object AdditionalAddressRestatesLegalAddress : GoldenRecordUpsertParseError

data object AdditionalAddressRestatesSiteMainAddress : GoldenRecordUpsertParseError

data class SiteScriptCodeNotStatedByLegalEntity(val scriptCode: String) : GoldenRecordUpsertParseError
