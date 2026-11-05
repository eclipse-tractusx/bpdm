/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.service.parser.task

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.orRecord
import org.eclipse.tractusx.bpdm.common.model.parseResultOf
import org.eclipse.tractusx.bpdm.common.model.parsedOrNull
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.model.PartnerScriptCodes
import org.eclipse.tractusx.bpdm.pool.model.SiteState
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.BpnReferenceParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.GoldenRecordTaskError
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.GoldenRecordTaskReferencesParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.LegalEntityUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.SiteUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.raw.site.SiteUpdateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.LegalEntityUpsertRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.SiteHeaderRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.SiteUpsertRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UpsertIntent
import org.eclipse.tractusx.bpdm.pool.model.request.LogisticAddressRequest
import org.eclipse.tractusx.bpdm.pool.model.request.SiteContentRequest
import org.eclipse.tractusx.bpdm.pool.model.request.SiteCreateWithReferencedAddressAsMainRequest
import org.eclipse.tractusx.bpdm.pool.model.request.SiteHeaderRequest
import org.eclipse.tractusx.bpdm.pool.service.parser.ScriptVariantCoverageValidator
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressBpnParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteBpnParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteCreateParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteCreateWithReferencedAddressAsMainParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteHeaderParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteUpdateParser
import org.springframework.stereotype.Service

/**
 * Decides whether a golden record task creates its site, writes over it, or leaves it as it stands, and where the site's
 * main address comes from.
 */
@Service
class SiteUpsertParser(
    private val siteBpnParser: SiteBpnParser,
    private val addressBpnParser: AddressBpnParser,
    private val siteHeaderParser: SiteHeaderParser,
    private val siteCreateParser: SiteCreateParser,
    private val siteCreateWithReferencedAddressAsMainParser: SiteCreateWithReferencedAddressAsMainParser,
    private val siteUpdateParser: SiteUpdateParser,
    private val scriptVariantCoverageValidator: ScriptVariantCoverageValidator
) {

    /**
     * Reports what the task writes for [site], or every problem that stops it; [legalEntityPlan] is null when the task's
     * legal entity could not be parsed.
     */
    fun parse(
        site: SiteUpsertRaw,
        legalEntity: LegalEntityUpsertRaw,
        legalEntityPlan: LegalEntityUpsertParsed?,
        references: GoldenRecordTaskReferencesParsed
    ): ParseResult<SiteUpsertParsed, GoldenRecordTaskError> {
        val errors = mutableListOf<GoldenRecordTaskError>()
        return parseResultOf(parseUpsert(site, legalEntity, legalEntityPlan, references, errors), errors)
    }

    private fun parseUpsert(
        site: SiteUpsertRaw,
        legalEntity: LegalEntityUpsertRaw,
        legalEntityPlan: LegalEntityUpsertParsed?,
        references: GoldenRecordTaskReferencesParsed,
        errors: MutableList<GoldenRecordTaskError>
    ): SiteUpsertParsed? {
        val reference = references.resolve(site.reference)
        val existingBpn = (reference as? BpnReferenceParsed.Existing)?.bpn

        if (existingBpn != null && site.intent == UpsertIntent.WriteOnlyIfAbsent) {
            val existingSite = siteBpnParser.parse(listOf(existingBpn)).single().parsedOrNull()
            if (existingSite == null) errors += GoldenRecordTaskError.UnchangedSiteNotFound(existingBpn)
            return existingSite?.let { SiteUpsertParsed.Unchanged(reference, it) }
        }

        val header = toHeaderRequest(site.header, errors) ?: return null

        return when (site) {
            is SiteUpsertRaw.WithLegalAddressAsMain ->
                if (existingBpn != null)
                    parseUpdate(existingBpn, reference, BpnReferenceParsed.Unstated, SiteContentRequest(header, sharedLegalAddress(site, legalEntity)), errors)
                else
                    parseCreateOnLegalAddress(reference, header, legalEntityPlan, errors)

            is SiteUpsertRaw.WithOwnMainAddress -> {
                val mainAddressReference = references.resolve(site.mainAddress.reference)
                if (namesLegalAddress(mainAddressReference, references.resolve(legalEntity.legalAddress.reference), legalEntityPlan)) {
                    errors += GoldenRecordTaskError.SiteMainAddressIsLegalAddress
                    return null
                }
                val content = SiteContentRequest(header, site.mainAddress.content)
                val existingMainAddress = (mainAddressReference as? BpnReferenceParsed.Existing)
                    ?.let { addressBpnParser.parse(listOf(it.bpn)).single().parsedOrNull() }

                when {
                    existingBpn != null -> parseUpdate(existingBpn, reference, mainAddressReference, content, errors)
                    // A new site whose main address already exists adopts that address instead of creating a duplicate,
                    // so several sites can share one main address.
                    existingMainAddress != null -> parseCreateOnExistingAddress(reference, mainAddressReference, existingMainAddress, content, errors)
                    else -> siteCreateParser.parseContent(listOf(content)).single()
                        .orRecord(errors) { GoldenRecordTaskError.SiteCreateInvalid(it) }
                        ?.let { SiteUpsertParsed.CreateWithOwnMainAddress(reference, mainAddressReference, it) }
                }
            }
        }
    }

    // A site whose main address is the legal address states no main address of its own, so naming the legal address here
    // describes one address twice.
    private fun namesLegalAddress(
        mainAddressReference: BpnReferenceParsed,
        legalAddressReference: BpnReferenceParsed,
        legalEntityPlan: LegalEntityUpsertParsed?
    ): Boolean =
        when (mainAddressReference) {
            is BpnReferenceParsed.Pending -> mainAddressReference == legalAddressReference
            is BpnReferenceParsed.Existing -> mainAddressReference == legalAddressReference ||
                    legalEntityPlan?.existingLegalEntity?.legalAddress?.bpn == mainAddressReference.bpn
            BpnReferenceParsed.Unstated -> false
        }

    private fun parseUpdate(
        siteBpn: String,
        reference: BpnReferenceParsed,
        mainAddressReference: BpnReferenceParsed,
        content: SiteContentRequest,
        errors: MutableList<GoldenRecordTaskError>
    ): SiteUpsertParsed? =
        siteUpdateParser.parseWithoutCoverageCheck(listOf(SiteUpdateRaw(siteBpn, content))).single()
            .orRecord(errors) { GoldenRecordTaskError.SiteUpdateInvalid(it) }
            ?.let { SiteUpsertParsed.Update(reference, mainAddressReference, it) }

    private fun parseCreateOnExistingAddress(
        reference: BpnReferenceParsed,
        mainAddressReference: BpnReferenceParsed,
        mainAddress: LogisticAddressDb,
        content: SiteContentRequest,
        errors: MutableList<GoldenRecordTaskError>
    ): SiteUpsertParsed? =
        siteCreateWithReferencedAddressAsMainParser
            .parse(listOf(SiteCreateWithReferencedAddressAsMainRequest(mainAddress.bpn, content.header, content.mainAddress))).single()
            .orRecord(errors) { GoldenRecordTaskError.SiteCreateInvalid(it) }
            ?.let { SiteUpsertParsed.CreateOnExistingAddress(reference, mainAddressReference, it) }

    // Coverage is judged against the legal address as the task leaves it, and only once the legal entity parsed: otherwise
    // the task is rejected for the legal entity anyway.
    private fun parseCreateOnLegalAddress(
        reference: BpnReferenceParsed,
        header: SiteHeaderRequest,
        legalEntityPlan: LegalEntityUpsertParsed?,
        errors: MutableList<GoldenRecordTaskError>
    ): SiteUpsertParsed? {
        val parsedHeader = siteHeaderParser.parse(listOf(header)).single().orRecord(errors) { GoldenRecordTaskError.SiteCreateInvalid(it) } ?: return null
        val legalAddressScriptCodes = legalEntityPlan?.let { legalAddressScriptCodesAfter(it) }
            ?: return SiteUpsertParsed.CreateOnLegalAddress(reference, parsedHeader)

        val coverageErrors = scriptVariantCoverageValidator.check(legalAddressScriptCodes, listOf(PartnerScriptCodes(bpn = null, parsedHeader.scriptCodes())))
        errors += coverageErrors.map { GoldenRecordTaskError.SiteCreateInvalid(it) }

        return SiteUpsertParsed.CreateOnLegalAddress(reference, parsedHeader)
    }

    private fun legalAddressScriptCodesAfter(legalEntity: LegalEntityUpsertParsed): List<String> =
        when (legalEntity) {
            is LegalEntityUpsertParsed.Unchanged -> legalEntity.legalEntity.legalAddress.scriptCodes()
            is LegalEntityUpsertParsed.Create -> legalEntity.create.content.legalAddress.scriptCodes()
            is LegalEntityUpsertParsed.Update -> legalEntity.update.content.legalAddress.scriptCodes()
        }

    // The site writes the legal address as its main address, so that address keeps the legal entity's script variants:
    // the legal entity's variant wins for a script both state, and a script only the legal entity states stays covered.
    private fun sharedLegalAddress(site: SiteUpsertRaw.WithLegalAddressAsMain, legalEntity: LegalEntityUpsertRaw): LogisticAddressRequest {
        val legalAddress = legalEntity.legalAddress.content
        val legalAddressVariantsByCode = legalAddress.scriptVariants.associateBy { it.scriptCode }
        val siteVariants = site.mainAddressScriptVariants.map { legalAddressVariantsByCode[it.scriptCode] ?: it }
        val siteScriptCodes = siteVariants.map { it.scriptCode }.toSet()

        return legalAddress.copy(scriptVariants = siteVariants + legalAddress.scriptVariants.filterNot { it.scriptCode in siteScriptCodes })
    }

    private fun toHeaderRequest(header: SiteHeaderRaw, errors: MutableList<GoldenRecordTaskError>): SiteHeaderRequest? {
        if (header.states.any { it.type == null }) {
            errors += GoldenRecordTaskError.StateTypeMissing
            return null
        }

        return SiteHeaderRequest(
            name = header.name,
            states = header.states.map { SiteState(it.validFrom, it.validTo, it.type!!) },
            confidenceCriteria = header.confidenceCriteria,
            scriptVariants = header.scriptVariants
        )
    }
}
