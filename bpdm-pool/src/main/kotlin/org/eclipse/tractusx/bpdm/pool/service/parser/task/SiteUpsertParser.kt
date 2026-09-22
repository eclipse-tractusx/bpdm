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
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.model.error.*
import org.eclipse.tractusx.bpdm.pool.model.parsed.BpnReferenceParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.request.*
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressContentParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteContentParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteHeaderParser
import org.eclipse.tractusx.bpdm.pool.util.orFailure
import org.eclipse.tractusx.bpdm.pool.util.parsedOrRecord
import org.eclipse.tractusx.bpdm.pool.util.singleOrRecord
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Decides what a request asks to happen to its site: create it, write over it, or leave it as it stands.
 *
 * Which of those the site's main address takes part in depends on whether the site brings one of its own, so the
 * variant of the request decides what is written as much as the site's own existence does.
 */
@Service
class SiteUpsertParser(
    private val siteReferenceParser: SiteReferenceParser,
    private val addressReferenceParser: AddressReferenceParser,
    private val referenceResolutionParser: BpnReferenceResolutionParser,
    private val siteHeaderParser: SiteHeaderParser,
    private val siteContentParser: SiteContentParser,
    private val addressContentParser: AddressContentParser
) {

    /**
     * Reports what is to be written for this site, or every reason it cannot be carried out.
     */
    @Transactional(readOnly = true)
    fun parse(
        request: SiteUpsertRequest
    ): ParseResult<SiteUpsertParsed, SiteUpsertParseError> {
        val errors = mutableListOf<SiteUpsertParseError>()
        return parseUpsert(request, errors).orFailure(errors)
    }

    private fun parseUpsert(
        request: SiteUpsertRequest,
        errors: MutableList<SiteUpsertParseError>
    ): SiteUpsertParsed? {
        val siteReference = referenceResolutionParser.parse(request.reference)
        val existingSite = siteReferenceParser.parse(request.reference).parsedOrRecord(errors)?.existingRecord

        if (existingSite != null && request.intent == UpsertIntent.WriteOnlyIfAbsent)
            return SiteUpsertParsed.Unchanged(siteReference, existingSite)

        return if (existingSite == null) parseCreate(request, siteReference, errors)
        else parseUpdate(request, siteReference, existingSite, errors)
    }

    private fun parseCreate(
        request: SiteUpsertRequest,
        siteReference: BpnReferenceParsed,
        errors: MutableList<SiteUpsertParseError>
    ): SiteUpsertParsed? =
        when (request) {
            is SiteUpsertRequest.WithLegalAddressAsMain ->
                parseHeader(request.header, errors)
                    ?.let { SiteUpsertParsed.CreateOnLegalAddress(siteReference, it) }
            is SiteUpsertRequest.WithOwnMainAddress ->
                parseCreateWithOwnMainAddress(request, siteReference, errors)
        }

    private fun parseCreateWithOwnMainAddress(
        request: SiteUpsertRequest.WithOwnMainAddress,
        siteReference: BpnReferenceParsed,
        errors: MutableList<SiteUpsertParseError>
    ): SiteUpsertParsed? {
        val mainAddressReference = referenceResolutionParser.parse(request.mainAddress.reference)
        val existingMainAddress = addressReferenceParser
            .parse(request.mainAddress.reference, ::SiteMainAddressNotFound)
            .parsedOrRecord(errors)?.existingRecord

        // A new site whose main address already exists adopts that address instead of duplicating it, so several
        // sites can share one main address - a different creation, with a different variant.
        if (existingMainAddress != null)
            return parseCreateOnExistingAddress(request, siteReference, mainAddressReference, existingMainAddress, errors)

        val content = siteContentParser
            .parse(listOf(SiteContentRequest(request.header, request.mainAddress.content)), listOf(null))
            .singleOrRecord(errors, ::toContentError) ?: return null

        return SiteUpsertParsed.CreateWithOwnMainAddress(siteReference, mainAddressReference, content)
    }

    private fun parseCreateOnExistingAddress(
        request: SiteUpsertRequest.WithOwnMainAddress,
        siteReference: BpnReferenceParsed,
        mainAddressReference: BpnReferenceParsed,
        existingMainAddress: LogisticAddressDb,
        errors: MutableList<SiteUpsertParseError>
    ): SiteUpsertParsed? {
        val header = parseHeader(request.header, errors)
        val mainAddressContent = addressContentParser
            .parse(listOf(request.mainAddress.content), listOf(existingMainAddress.bpn))
            .singleOrRecord(errors, ::SiteMainAddressContentInvalid)

        if (header == null || mainAddressContent == null) return null

        return SiteUpsertParsed.CreateOnExistingAddress(
            siteReference, mainAddressReference, existingMainAddress, header, mainAddressContent
        )
    }

    private fun parseUpdate(
        request: SiteUpsertRequest,
        siteReference: BpnReferenceParsed,
        existingSite: SiteDb,
        errors: MutableList<SiteUpsertParseError>
    ): SiteUpsertParsed? =
        when (request) {
            is SiteUpsertRequest.WithLegalAddressAsMain -> {
                if (!existingSite.sitsOnLegalAddress())
                    errors += SiteDoesNotSitOnLegalAddress(existingSite.bpn, existingSite.mainAddress.bpn)
                parseHeader(request.header, errors)
                    ?.let { SiteUpsertParsed.UpdateOnLegalAddress(siteReference, existingSite, it) }
            }

            is SiteUpsertRequest.WithOwnMainAddress ->
                siteContentParser
                    .parse(
                        listOf(SiteContentRequest(request.header, request.mainAddress.content)),
                        listOf(existingSite.mainAddress.bpn)
                    )
                    .singleOrRecord(errors, ::toContentError)
                    ?.let {
                        SiteUpsertParsed.UpdateWithOwnMainAddress(
                            siteReference,
                            referenceResolutionParser.parse(request.mainAddress.reference),
                            existingSite,
                            it
                        )
                    }
        }

    private fun parseHeader(header: SiteHeaderRequest, errors: MutableList<SiteUpsertParseError>) =
        siteHeaderParser.parse(listOf(header)).singleOrRecord(errors, ::SiteContentInvalid)

    private fun toContentError(error: SiteContentParseError): SiteUpsertParseError =
        when (error) {
            is SiteHeaderParseError -> SiteContentInvalid(error)
            is AddressContentParseError -> SiteMainAddressContentInvalid(error)
        }
}
