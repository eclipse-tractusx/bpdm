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
 *
 * Each parser of the upsert reports through the sub-interface naming exactly what it can raise, so a reader of one
 * parser's signature learns its whole range of rejections. An error several parsers can raise subtypes each of their
 * interfaces; only the parser that joins them all reports this one.
 */
sealed interface GoldenRecordUpsertParseError

/** What planning the legal entity of a golden record upsert can be faulted for. */
sealed interface LegalEntityUpsertParseError : GoldenRecordUpsertParseError

/** What planning the site of a golden record upsert can be faulted for. */
sealed interface SiteUpsertParseError : GoldenRecordUpsertParseError

/** What planning the additional address of a golden record upsert can be faulted for. */
sealed interface AdditionalAddressUpsertParseError : GoldenRecordUpsertParseError

/** What planning the sites stated as sharing the record address can be faulted for. */
sealed interface AdditionalSitesParseError : GoldenRecordUpsertParseError

/** What the partners of one golden record upsert can contradict each other over. */
sealed interface CrossPartnerParseError : GoldenRecordUpsertParseError

/** The ways two of the addresses one upsert states can turn out to be the same address. */
sealed interface StatedAddressDistinctnessParseError : CrossPartnerParseError

/** The ways a partner one upsert states can turn out to belong to a different legal entity. */
sealed interface ParentConsistencyParseError : CrossPartnerParseError

data class LegalEntityContentInvalid(val error: LegalEntityHeaderParseError) : LegalEntityUpsertParseError

data class LegalAddressContentInvalid(val error: AddressContentParseError) : LegalEntityUpsertParseError

data class MultipleUltimateOwners(val conflictingBpnls: List<String>) : LegalEntityUpsertParseError

data class AlternativeHeadquarterCannotOwn(val bpnl: String) : LegalEntityUpsertParseError

data class SiteContentInvalid(val error: SiteHeaderParseError) : SiteUpsertParseError

data class SiteMainAddressContentInvalid(val error: AddressContentParseError) : SiteUpsertParseError

data class SiteMainAddressNotFound(val bpn: String) : SiteUpsertParseError

data class SiteDoesNotSitOnLegalAddress(val siteBpn: String, val mainAddressBpn: String) : SiteUpsertParseError

data class AdditionalAddressContentInvalid(val error: AddressContentParseError) : AdditionalAddressUpsertParseError

data class AdditionalAddressNotFound(val bpn: String) : AdditionalAddressUpsertParseError

data class AdditionalSiteContentInvalid(val index: Int, val error: SiteHeaderParseError) : AdditionalSitesParseError

data class AdditionalSiteNotFound(val index: Int, val bpn: String) : AdditionalSitesParseError

// Resolving a legal entity is part of planning all three partners: the site and the additional address each state
// which legal entity they belong under, and the legal entity states itself.
data class LegalEntityNotFound(val bpn: String) :
    LegalEntityUpsertParseError,
    SiteUpsertParseError,
    AdditionalAddressUpsertParseError

// An additional address states the site it sits on, so resolving a site is part of planning it too.
data class SiteNotFound(val bpn: String) : SiteUpsertParseError, AdditionalAddressUpsertParseError

data class SiteNotInRequestLegalEntity(val siteBpn: String, val legalEntityBpn: String?) : ParentConsistencyParseError

data class AdditionalAddressNotInRequestLegalEntity(val addressBpn: String, val legalEntityBpn: String?) : ParentConsistencyParseError

data class AdditionalSiteNotInLegalEntity(val siteBpn: String, val legalEntityBpn: String?) : ParentConsistencyParseError

data object SiteMainAddressRestatesLegalAddress : StatedAddressDistinctnessParseError

data object AdditionalAddressRestatesLegalAddress : StatedAddressDistinctnessParseError

data object AdditionalAddressRestatesSiteMainAddress : StatedAddressDistinctnessParseError

data object AdditionalSitesWithoutSite : CrossPartnerParseError

data class AdditionalSiteOmitted(val siteBpn: String) : CrossPartnerParseError

data class SiteScriptCodeNotStatedByLegalEntity(val scriptCode: String) : CrossPartnerParseError

data class ScriptVariantCoverageLost(val error: ScriptVariantCoverageParseError) : CrossPartnerParseError

// No parser raises these two: they are reported by the outbound mapper and left in place because establishing them as
// dead is tool-only work that has not been run for them.
data class LegalAddressCoverageLost(val error: ScriptVariantCoverageParseError) : GoldenRecordUpsertParseError

data class SiteMainAddressCoverageLost(val error: ScriptVariantCoverageParseError) : GoldenRecordUpsertParseError
