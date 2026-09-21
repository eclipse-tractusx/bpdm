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
import org.eclipse.tractusx.bpdm.pool.model.error.*
import org.eclipse.tractusx.bpdm.pool.model.parsed.BpnReferenceParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteUpsertPlan
import org.eclipse.tractusx.bpdm.pool.model.request.*
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteCreateOnExistingAddressParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteCreateOnLegalAddressParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteContentParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteUpdateOnLegalAddressParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteUpdateWithOwnMainAddressParser
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
    private val siteContentParser: SiteContentParser,
    private val siteCreateOnLegalAddressParser: SiteCreateOnLegalAddressParser,
    private val siteCreateOnExistingAddressParser: SiteCreateOnExistingAddressParser,
    private val siteUpdateWithOwnMainAddressParser: SiteUpdateWithOwnMainAddressParser,
    private val siteUpdateOnLegalAddressParser: SiteUpdateOnLegalAddressParser
) {

    /**
     * Reports the plan for this site, or every reason it cannot be carried out.
     */
    @Transactional(readOnly = true)
    fun parse(
        request: SiteUpsertRequest,
    ): ParseResult<SiteUpsertPlan, SiteUpsertParseError> {
        val errors = mutableListOf<SiteUpsertParseError>()
        return parsePlan(request, errors).orFailure(errors)
    }

    private fun parsePlan(
        request: SiteUpsertRequest,
        errors: MutableList<SiteUpsertParseError>
    ): SiteUpsertPlan? {
        val resolvedSite = when (val result = siteReferenceParser.parse(request.reference)) {
            is ParseResult.Failure -> { errors += result.errors; return null }
            is ParseResult.Success -> result.parsed
        }
        val siteReference = resolvedSite.reference
        val existingSite = resolvedSite.existingRecord

        if (existingSite != null && request.intent == UpsertIntent.WriteOnlyIfAbsent)
            return SiteUpsertPlan.Unchanged(siteReference, existingSite)

        return if (existingSite == null) parseCreate(request, siteReference, errors)
        else parseUpdate(request, siteReference, existingSite, errors)
    }

    private fun parseCreate(
        request: SiteUpsertRequest,
        siteReference: BpnReferenceParsed,
        errors: MutableList<SiteUpsertParseError>
    ): SiteUpsertPlan? =
        when (request) {
            is SiteUpsertRequest.WithLegalAddressAsMain ->
                siteCreateOnLegalAddressParser.parseContent(listOf(request.header))
                    .singleOrRecord(errors, ::toCreateError)
                    ?.let { SiteUpsertPlan.CreateOnLegalAddress(siteReference, it) }
            is SiteUpsertRequest.WithOwnMainAddress ->
                parseCreateWithOwnMainAddress(request, siteReference, errors)
        }

    private fun parseCreateWithOwnMainAddress(
        request: SiteUpsertRequest.WithOwnMainAddress,
        siteReference: BpnReferenceParsed,
        errors: MutableList<SiteUpsertParseError>
    ): SiteUpsertPlan? {
        val resolvedMainAddress = when (
            val result = addressReferenceParser.parse(request.mainAddress.reference, ::SiteMainAddressNotFound)
        ) {
            is ParseResult.Failure -> { errors += result.errors; return null }
            is ParseResult.Success -> result.parsed
        }

        // A new site whose main address already exists adopts that address instead of duplicating it, so several
        // sites can share one main address - a different creation, with a different parser.
        val existingMainAddress = resolvedMainAddress.existingRecord
        if (existingMainAddress != null) {
            val createdOnExistingAddress = siteCreateOnExistingAddressParser
                .parse(listOf(SiteCreateWithReferencedAddressAsMainRequest(existingMainAddress.bpn, request.header, request.mainAddress.content)))
                .singleOrRecord(errors, ::toCreateError) ?: return null
            return SiteUpsertPlan.CreateOnExistingAddress(siteReference, resolvedMainAddress.reference, createdOnExistingAddress)
        }

        val ownMainAddressContent = siteContentParser
            .parse(listOf(SiteContentRequest(request.header, request.mainAddress.content)), listOf(null))
            .singleOrRecord(errors, ::toCreateError) ?: return null

        return SiteUpsertPlan.CreateWithOwnMainAddress(siteReference, resolvedMainAddress.reference, ownMainAddressContent)
    }

    private fun parseUpdate(
        request: SiteUpsertRequest,
        siteReference: BpnReferenceParsed,
        existingSite: SiteDb,
        errors: MutableList<SiteUpsertParseError>
    ): SiteUpsertPlan? {
        return when (request) {
            is SiteUpsertRequest.WithLegalAddressAsMain ->
                siteUpdateOnLegalAddressParser
                    .parse(listOf(SiteUpdateOnLegalAddressRequest(existingSite.bpn, request.header)))
                    .singleOrRecord(errors, ::toUpdateError)
                    ?.let { SiteUpsertPlan.UpdateOnLegalAddress(siteReference, it) }

            is SiteUpsertRequest.WithOwnMainAddress ->
                siteUpdateWithOwnMainAddressParser
                    .parseWithoutScriptVariantCoverage(
                        listOf(SiteUpdateRequest(existingSite.bpn, SiteContentRequest(request.header, request.mainAddress.content)))
                    )
                    .singleOrRecord(errors, ::toUpdateError)
                    ?.let {
                        SiteUpsertPlan.UpdateWithOwnMainAddress(
                            siteReference,
                            referenceResolutionParser.parse(request.mainAddress.reference),
                            existingSite,
                            it.content
                        )
                    }
        }
    }

    private fun toCreateError(error: SiteCreateEntryParseError): SiteUpsertParseError =
        when (error) {
            is SiteHeaderParseError -> SiteContentInvalid(error)
            is AddressContentParseError -> SiteMainAddressContentInvalid(error)
            is UnresolvableLegalEntity -> LegalEntityNotFound(error.bpn)
            is UnresolvableAddress -> SiteMainAddressNotFound(error.bpn)
            // No parser produces this: it is declared on SiteCreateParseError but never raised.
            is LegalAddressAlreadyMainAddress -> error("Unexpected legal-address-already-main error for site ${error.bpnSite}")
        }

    private fun toUpdateError(error: SiteUpdateEntryParseError): SiteUpsertParseError =
        when (error) {
            is SiteHeaderParseError -> SiteContentInvalid(error)
            is AddressContentParseError -> SiteMainAddressContentInvalid(error)
            is SiteMainAddressNotLegalAddress -> SiteDoesNotSitOnLegalAddress(error.bpnSite, error.bpnMainAddress)
            // The existing site was resolved before this parser was called.
            is UnresolvableSite -> error("Unexpected unresolvable site ${error.bpn}")
        }
}
