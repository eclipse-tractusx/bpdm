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

package org.eclipse.tractusx.bpdm.pool.mapper.apiv6

import org.eclipse.tractusx.bpdm.pool.api.v6.model.bpn.BpnRequestIdentifierSearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.bpn.IdentifiersSearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.changelog.ChangelogSearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.IdentifierBusinessPartnerTypeV6
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.IdentifierBusinessPartnerTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.IdentifierTypeV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.LegalFormV6Request
import org.eclipse.tractusx.bpdm.pool.model.raw.bpn.BpnIdentifierSearchRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.bpn.BpnRequestIdentifierSearchRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.changelog.ChangelogSearchRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.metadata.FieldQualityRuleSearchRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.metadata.IdentifierTypeCreateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.metadata.IdentifierTypeDetailRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.metadata.IdentifierTypeSearchRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.metadata.LegalFormCreateRaw
import org.springframework.stereotype.Component

/**
 * Maps API v6 requests to their raw model.
 */
@Component
class ApiV6InboundMapper {

    /**
     * Returns the raw identifier type search criteria a client sent.
     */
    fun toIdentifierTypeSearchRaw(businessPartnerType: IdentifierBusinessPartnerTypeV6, country: String?): IdentifierTypeSearchRaw =
        IdentifierTypeSearchRaw(
            businessPartnerType = IdentifierBusinessPartnerTypeV7.valueOf(businessPartnerType.name),
            country = country
        )

    /**
     * Returns the raw field quality rule search for [country].
     */
    fun toFieldQualityRuleSearchRaw(country: String): FieldQualityRuleSearchRaw =
        FieldQualityRuleSearchRaw(country = country)

    /**
     * Returns the raw identifier type create holding [identifierType].
     */
    fun toIdentifierTypeCreateRaw(identifierType: IdentifierTypeV6Dto): IdentifierTypeCreateRaw =
        IdentifierTypeCreateRaw(
            technicalKey = identifierType.technicalKey,
            businessPartnerType = IdentifierBusinessPartnerTypeV7.valueOf(identifierType.businessPartnerType.name),
            name = identifierType.name,
            abbreviation = identifierType.abbreviation,
            transliteratedName = identifierType.transliteratedName,
            transliteratedAbbreviation = identifierType.transliteratedAbbreviation,
            format = null,
            categories = emptySet(),
            details = identifierType.details.map { IdentifierTypeDetailRaw(it.country, it.mandatory) }
        )

    /**
     * Returns the raw legal form create holding [legalForm].
     */
    fun toLegalFormCreateRaw(legalForm: LegalFormV6Request): LegalFormCreateRaw =
        LegalFormCreateRaw(
            technicalKey = legalForm.technicalKey,
            name = legalForm.name,
            transliteratedName = legalForm.transliteratedName,
            abbreviations = legalForm.abbreviation,
            transliteratedAbbreviations = legalForm.transliteratedAbbreviations,
            country = legalForm.country,
            language = legalForm.language,
            administrativeAreaLevel1 = legalForm.administrativeAreaLevel1,
            isActive = legalForm.isActive
        )

    /**
     * Returns the raw BPN-by-identifier search holding [request].
     */
    fun toBpnIdentifierSearchRaw(request: IdentifiersSearchV6Request): BpnIdentifierSearchRaw =
        BpnIdentifierSearchRaw(
            businessPartnerType = IdentifierBusinessPartnerTypeV7.valueOf(request.businessPartnerType.name),
            identifierTypeKey = request.idType,
            identifierValues = request.idValues
        )

    /**
     * Returns the raw BPN-by-request-identifier search holding [request].
     */
    fun toBpnRequestIdentifierSearchRaw(request: BpnRequestIdentifierSearchV6Request): BpnRequestIdentifierSearchRaw =
        BpnRequestIdentifierSearchRaw(requestedIdentifiers = request.requestedIdentifiers)

    /**
     * Returns the raw changelog search holding [request].
     */
    fun toChangelogSearchRaw(request: ChangelogSearchV6Request): ChangelogSearchRaw =
        ChangelogSearchRaw(
            bpns = request.bpns,
            businessPartnerTypes = request.businessPartnerTypes,
            timestampAfter = request.timestampAfter
        )
}
