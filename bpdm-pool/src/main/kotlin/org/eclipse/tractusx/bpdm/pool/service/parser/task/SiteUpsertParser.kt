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
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteCreateWithOwnMainAddressParser
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
    private val siteCreateWithOwnMainAddressParser: SiteCreateWithOwnMainAddressParser,
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
        legalEntityRequest: LegalEntityUpsertRequest
    ): ParseResult<SiteUpsertPlan, SiteUpsertParseError> {
        val errors = mutableListOf<SiteUpsertParseError>()
        return parsePlan(request, legalEntityRequest, errors).orFailure(errors)
    }

    private fun parsePlan(
        request: SiteUpsertRequest,
        legalEntityRequest: LegalEntityUpsertRequest,
        errors: MutableList<SiteUpsertParseError>
    ): SiteUpsertPlan? {
        val resolved = when (val result = siteReferenceParser.parse(request.reference)) {
            is ParseResult.Failure -> { errors += result.errors; return null }
            is ParseResult.Success -> result.parsed
        }
        val siteReference = resolved.reference
        val target = resolved.target

        if (target != null && request.intent == UpsertIntent.WriteOnlyIfAbsent)
            return SiteUpsertPlan.Unchanged(siteReference, target)

        return if (target == null) parseCreate(request, siteReference, errors)
        else parseUpdate(request, siteReference, target, errors)
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
        val mainAddressTarget = resolvedMainAddress.target
        if (mainAddressTarget != null) {
            val createdOnExistingAddress = siteCreateOnExistingAddressParser
                .parse(listOf(SiteCreateWithReferencedAddressAsMainRequest(mainAddressTarget.bpn, request.header, request.mainAddress.content)))
                .singleOrRecord(errors, ::toCreateError) ?: return null
            return SiteUpsertPlan.CreateOnExistingAddress(siteReference, resolvedMainAddress.reference, createdOnExistingAddress)
        }

        val ownMainAddressContent = siteCreateWithOwnMainAddressParser
            .parseContent(listOf(SiteContentRequest(request.header, request.mainAddress.content)))
            .singleOrRecord(errors, ::toCreateError) ?: return null

        return SiteUpsertPlan.CreateWithOwnMainAddress(siteReference, resolvedMainAddress.reference, ownMainAddressContent)
    }

    private fun parseUpdate(
        request: SiteUpsertRequest,
        siteReference: BpnReferenceParsed,
        target: SiteDb,
        errors: MutableList<SiteUpsertParseError>
    ): SiteUpsertPlan? {
        return when (request) {
            is SiteUpsertRequest.WithLegalAddressAsMain ->
                siteUpdateOnLegalAddressParser
                    .parse(listOf(SiteUpdateOnLegalAddressRequest(target.bpn, request.header)))
                    .singleOrRecord(errors, ::toUpdateError)
                    ?.let { SiteUpsertPlan.UpdateOnLegalAddress(siteReference, it) }

            is SiteUpsertRequest.WithOwnMainAddress ->
                siteUpdateWithOwnMainAddressParser
                    .parseWithoutScriptVariantCoverage(
                        listOf(SiteUpdateRequest(target.bpn, SiteContentRequest(request.header, request.mainAddress.content)))
                    )
                    .singleOrRecord(errors, ::toUpdateError)
                    ?.let {
                        SiteUpsertPlan.UpdateWithOwnMainAddress(
                            siteReference,
                            referenceResolutionParser.parse(request.mainAddress.reference),
                            target,
                            it.content
                        )
                    }
        }
    }

    private fun toCreateError(error: SiteCreateEntryParseError): SiteUpsertParseError =
        when (error) {
            is SiteContentParseError -> SiteContentInvalid(error)
            is AddressContentParseError -> SiteMainAddressContentInvalid(error)
            is UnresolvableLegalEntity -> LegalEntityNotFound(error.bpn)
            is UnresolvableAddress -> SiteMainAddressNotFound(error.bpn)
            // No parser produces this: it is declared on SiteCreateParseError but never raised.
            is LegalAddressAlreadyMainAddress -> error("Unexpected legal-address-already-main error for site ${error.bpnSite}")
        }

    private fun toUpdateError(error: SiteUpdateEntryParseError): SiteUpsertParseError =
        when (error) {
            is SiteContentParseError -> SiteContentInvalid(error)
            is AddressContentParseError -> SiteMainAddressContentInvalid(error)
            is SiteMainAddressNotLegalAddress -> SiteDoesNotSitOnLegalAddress(error.bpnSite, error.bpnMainAddress)
            // The target was resolved before this parser was called.
            is UnresolvableSite -> error("Unexpected unresolvable site ${error.bpn}")
        }
}
