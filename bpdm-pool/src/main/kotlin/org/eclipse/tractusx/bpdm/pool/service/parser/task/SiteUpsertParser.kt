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
import org.eclipse.tractusx.bpdm.pool.model.raw.site.SiteStateRaw
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.BpnReferenceParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.GoldenRecordTaskError
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.GoldenRecordTaskReferencesParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.LegalEntityUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.SiteUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.raw.site.SiteUpdateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.LegalEntityUpsertRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.SiteUpsertHeaderRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.SiteUpsertRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UpsertIntent
import org.eclipse.tractusx.bpdm.pool.model.raw.address.LogisticAddressRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.site.SiteContentRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.site.SiteCreateWithReferencedAddressAsMainRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.site.SiteHeaderRaw
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressBpnParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteBpnParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteCreateParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteCreateWithReferencedAddressAsMainParser
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
    private val siteCreateParser: SiteCreateParser,
    private val siteCreateWithReferencedAddressAsMainParser: SiteCreateWithReferencedAddressAsMainParser,
    private val siteUpdateParser: SiteUpdateParser
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
            is SiteUpsertRaw.WithLegalAddressAsMain -> {
                val content = SiteContentRaw(header, sharedLegalAddress(site, legalEntity))
                if (existingBpn != null)
                    parseUpdate(existingBpn, reference, BpnReferenceParsed.Unstated, content, errors)
                else
                    parseCreateOnLegalAddress(reference, content, legalEntityPlan, errors)
            }

            is SiteUpsertRaw.WithOwnMainAddress -> {
                val mainAddressReference = references.resolve(site.mainAddress.reference)
                if (namesLegalAddress(mainAddressReference, references.resolve(legalEntity.legalAddress.reference), legalEntityPlan)) {
                    errors += GoldenRecordTaskError.SiteMainAddressIsLegalAddress
                    return null
                }
                val content = SiteContentRaw(header, site.mainAddress.content)
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
        content: SiteContentRaw,
        errors: MutableList<GoldenRecordTaskError>
    ): SiteUpsertParsed? =
        siteUpdateParser.parse(listOf(SiteUpdateRaw(siteBpn, content))).single()
            .orRecord(errors) { GoldenRecordTaskError.SiteUpdateInvalid(it) }
            ?.let { SiteUpsertParsed.Update(reference, mainAddressReference, it) }

    private fun parseCreateOnExistingAddress(
        reference: BpnReferenceParsed,
        mainAddressReference: BpnReferenceParsed,
        mainAddress: LogisticAddressDb,
        content: SiteContentRaw,
        errors: MutableList<GoldenRecordTaskError>
    ): SiteUpsertParsed? =
        siteCreateWithReferencedAddressAsMainParser
            .parse(listOf(SiteCreateWithReferencedAddressAsMainRaw(mainAddress.bpn, content.header, content.mainAddress))).single()
            .orRecord(errors) { GoldenRecordTaskError.SiteCreateInvalid(it) }
            ?.let { SiteUpsertParsed.CreateOnExistingAddress(reference, mainAddressReference, it) }

    private fun parseCreateOnLegalAddress(
        reference: BpnReferenceParsed,
        content: SiteContentRaw,
        legalEntityPlan: LegalEntityUpsertParsed?,
        errors: MutableList<GoldenRecordTaskError>
    ): SiteUpsertParsed? {
        val legalAddressBpn = legalEntityPlan?.existingLegalEntity?.legalAddress?.bpn

        return siteCreateParser.parseContent(listOf(content), listOf(legalAddressBpn)).single()
            .orRecord(errors) { GoldenRecordTaskError.SiteCreateInvalid(it) }
            ?.let { SiteUpsertParsed.CreateOnLegalAddress(reference, it) }
    }

    // The site writes the legal address as its main address, so the legal entity's variant wins for a script both state;
    // a script only the legal entity is named in stays on the address because the legal entity still needs it.
    private fun sharedLegalAddress(site: SiteUpsertRaw.WithLegalAddressAsMain, legalEntity: LegalEntityUpsertRaw): LogisticAddressRaw {
        val legalAddress = legalEntity.legalAddress.content
        val legalAddressVariantsByCode = legalAddress.scriptVariants.associateBy { it.scriptCode }

        return legalAddress.copy(scriptVariants = site.mainAddressScriptVariants.map { legalAddressVariantsByCode[it.scriptCode] ?: it })
    }

    private fun toHeaderRequest(header: SiteUpsertHeaderRaw, errors: MutableList<GoldenRecordTaskError>): SiteHeaderRaw? {
        if (header.states.any { it.type == null }) {
            errors += GoldenRecordTaskError.StateTypeMissing
            return null
        }

        return SiteHeaderRaw(
            name = header.name,
            states = header.states.map { SiteStateRaw(it.validFrom, it.validTo, it.type!!) },
            confidenceCriteria = header.confidenceCriteria,
            scriptVariants = header.scriptVariants
        )
    }
}
