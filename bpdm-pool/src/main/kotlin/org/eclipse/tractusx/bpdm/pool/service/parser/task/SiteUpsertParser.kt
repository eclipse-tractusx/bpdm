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


package org.eclipse.tractusx.bpdm.pool.service.parser.task

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.model.BpnReferenceAllocation
import org.eclipse.tractusx.bpdm.pool.model.error.*
import org.eclipse.tractusx.bpdm.pool.model.parsed.BpnReferenceParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteUpsertPlan
import org.eclipse.tractusx.bpdm.pool.model.request.*
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteCreateParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteCreateWithLegalAddressAsMainParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteCreateWithReferencedAddressAsMainParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteHeaderUpdateParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteUpdateParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Decides what a request asks to happen to its site: resolve the reference, then hand the content to the parser that
 * owns that operation, or to none where nothing needs writing.
 *
 * It takes the legal entity's request as well, because a site whose main address is the legal address writes that
 * one address and the legal entity's payload for it is the one applied.
 */
@Service
class SiteUpsertParser(
    private val siteReferenceParser: SiteReferenceParser,
    private val addressReferenceParser: AddressReferenceParser,
    private val referenceResolutionParser: BpnReferenceResolutionParser,
    private val siteCreateParser: SiteCreateParser,
    private val siteCreateWithLegalAddressAsMainParser: SiteCreateWithLegalAddressAsMainParser,
    private val siteCreateWithReferencedAddressAsMainParser: SiteCreateWithReferencedAddressAsMainParser,
    private val siteUpdateParser: SiteUpdateParser,
    private val siteHeaderUpdateParser: SiteHeaderUpdateParser
) {

    /**
     * Reports the plan for this site, or every reason it cannot be carried out.
     */
    @Transactional(readOnly = true)
    fun parse(
        request: SiteUpsertRequest,
        legalEntityRequest: LegalEntityUpsertRequest,
        bpnReferences: BpnReferenceAllocation
    ): ParseResult<SiteUpsertPlan, GoldenRecordUpsertParseError> {
        val errors = mutableListOf<GoldenRecordUpsertParseError>()
        return parsePlan(request, legalEntityRequest, bpnReferences, errors).orFailure(errors)
    }

    private fun parsePlan(
        request: SiteUpsertRequest,
        legalEntityRequest: LegalEntityUpsertRequest,
        bpnReferences: BpnReferenceAllocation,
        errors: MutableList<GoldenRecordUpsertParseError>
    ): SiteUpsertPlan? {
        val resolved = when (val result = siteReferenceParser.parse(request.reference, bpnReferences)) {
            is ParseResult.Failure -> { errors += result.errors; return null }
            is ParseResult.Success -> result.parsed
        }
        val reference = resolved.reference
        val target = resolved.target

        if (target != null && request.intent == UpsertIntent.WriteOnlyIfAbsent)
            return SiteUpsertPlan.Unchanged(reference, target)

        return if (target == null) parseCreate(request, reference, bpnReferences, errors)
        else parseUpdate(request, reference, target, legalEntityRequest, bpnReferences, errors)
    }

    private fun parseCreate(
        request: SiteUpsertRequest,
        reference: BpnReferenceParsed,
        bpnReferences: BpnReferenceAllocation,
        errors: MutableList<GoldenRecordUpsertParseError>
    ): SiteUpsertPlan? =
        when (request) {
            is SiteUpsertRequest.WithLegalAddressAsMain ->
                siteCreateWithLegalAddressAsMainParser.parseContent(listOf(request.header))
                    .singleOrRecord(errors, ::toCreateError)
                    ?.let { SiteUpsertPlan.CreateOnLegalAddress(reference, it) }
            is SiteUpsertRequest.WithOwnMainAddress ->
                parseCreateWithOwnMainAddress(request, reference, bpnReferences, errors)
        }

    private fun parseCreateWithOwnMainAddress(
        request: SiteUpsertRequest.WithOwnMainAddress,
        reference: BpnReferenceParsed,
        bpnReferences: BpnReferenceAllocation,
        errors: MutableList<GoldenRecordUpsertParseError>
    ): SiteUpsertPlan? {
        val resolvedMainAddress = when (
            val result = addressReferenceParser.parse(request.mainAddress.reference, bpnReferences, ::SiteMainAddressNotFound)
        ) {
            is ParseResult.Failure -> { errors += result.errors; return null }
            is ParseResult.Success -> result.parsed
        }

        // A new site whose main address already exists adopts that address instead of duplicating it, so several
        // sites can share one main address - a different creation, with a different parser.
        val mainAddressTarget = resolvedMainAddress.target
        if (mainAddressTarget != null) {
            val onAddress = siteCreateWithReferencedAddressAsMainParser
                .parse(listOf(SiteCreateWithReferencedAddressAsMainRequest(mainAddressTarget.bpn, request.header, request.mainAddress.content)))
                .singleOrRecord(errors, ::toCreateError) ?: return null
            return SiteUpsertPlan.CreateOnExistingAddress(reference, resolvedMainAddress.reference, onAddress)
        }

        val content = siteCreateParser
            .parseContent(listOf(SiteContentRequest(request.header, request.mainAddress.content)))
            .singleOrRecord(errors, ::toCreateError) ?: return null

        return SiteUpsertPlan.CreateWithOwnMainAddress(reference, resolvedMainAddress.reference, content)
    }

    private fun parseUpdate(
        request: SiteUpsertRequest,
        reference: BpnReferenceParsed,
        target: SiteDb,
        legalEntityRequest: LegalEntityUpsertRequest,
        bpnReferences: BpnReferenceAllocation,
        errors: MutableList<GoldenRecordUpsertParseError>
    ): SiteUpsertPlan? {
        return when (request) {
            // The site does not own the legal address, so it states only itself: writing that address is the legal
            // entity's to do.
            is SiteUpsertRequest.WithLegalAddressAsMain ->
                siteHeaderUpdateParser
                    .parse(listOf(SiteHeaderUpdateRequest(target.bpn, request.header)))
                    .singleOrRecord(errors, ::toUpdateError)
                    ?.let { SiteUpsertPlan.UpdateOnLegalAddress(reference, it) }

            is SiteUpsertRequest.WithOwnMainAddress ->
                siteUpdateParser
                    .parse(
                        listOf(SiteUpdateRequest(target.bpn, SiteContentRequest(request.header, request.mainAddress.content)))
                    )
                    .singleOrRecord(errors, ::toUpdateError)
                    ?.let {
                        SiteUpsertPlan.UpdateWithOwnMainAddress(
                            reference,
                            referenceResolutionParser.parse(request.mainAddress.reference, bpnReferences),
                            target,
                            it.content
                        )
                    }
        }
    }

    private fun toCreateError(error: SiteCreateParseError): GoldenRecordUpsertParseError =
        when (error) {
            is SiteContentParseError -> SiteContentInvalid(error)
            is AddressContentParseError -> SiteMainAddressContentInvalid(error)
            is ScriptVariantCoverageParseError -> SiteMainAddressCoverageLost(error)
            is UnresolvableLegalEntity -> LegalEntityNotFound(error.bpn)
            is UnresolvableAddress -> SiteMainAddressNotFound(error.bpn)
            // No parser produces this: it is declared on SiteCreateParseError but never raised.
            is LegalAddressAlreadyMainAddress -> error("Unexpected legal-address-already-main error for site ${error.bpnSite}")
        }

    private fun toUpdateError(error: SiteUpdateParseError): GoldenRecordUpsertParseError =
        when (error) {
            is SiteContentParseError -> SiteContentInvalid(error)
            is AddressContentParseError -> SiteMainAddressContentInvalid(error)
            is ScriptVariantCoverageParseError -> SiteMainAddressCoverageLost(error)
            // The target was resolved before this parser was called.
            is UnresolvableSite -> error("Unexpected unresolvable site ${error.bpn}")
        }
}
