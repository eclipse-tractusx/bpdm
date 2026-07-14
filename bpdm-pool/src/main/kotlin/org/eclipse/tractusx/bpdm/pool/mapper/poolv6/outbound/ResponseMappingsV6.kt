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

package org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound

import org.eclipse.tractusx.bpdm.pool.api.v6.model.LegalEntityVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LegalFormDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.SiteVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.AddressPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.RelationVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SitePartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.LegalFormDb
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.service.getAddressType
import org.eclipse.tractusx.bpdm.pool.service.mainSite
import org.eclipse.tractusx.bpdm.pool.service.toDto

/**
 * Shared v6 response mappers: translate persistence entities into the versioned `api.v6.model` DTOs. These previously
 * lived as duplicated member extensions inside the three `controller/v6` legacy service mappers; consolidating them here
 * removes that duplication and lets both the v6 read paths (legacy mappers) and the v6 write paths
 * (`service/application/v6` application services) share one copy. Mirrors the v7 side's `service/ResponseMappings.kt`.
 *
 * The aggregate DB→v6-DTO shapes are named `toV6Dto` rather than `toDto` on purpose: the leaf value mappers
 * (`getAddressType`, state/identifier/postal/confidence `toDto`) reused here are the shared, version-agnostic ones from
 * `service.ResponseMappings`, which also declares its own `LogisticAddressDb.toDto`/`SiteDb.toDto`/`LegalEntityDb.toDto`
 * for the current API — the distinct name keeps the two versions unambiguous wherever both are in scope.
 */

fun LogisticAddressDb.toV6Dto(): LogisticAddressVerboseDto {
    return LogisticAddressVerboseDto(
        bpna = bpn,
        bpnLegalEntity = legalEntity?.bpn,
        bpnSite = mainSite?.bpn,
        createdAt = createdAt,
        updatedAt = updatedAt,
        name = name,
        states = states.map { it.toDto() },
        identifiers = identifiers.map { it.toDto() },
        physicalPostalAddress = physicalPostalAddress.toDto(),
        alternativePostalAddress = alternativePostalAddress?.toDto(),
        confidenceCriteria = confidenceCriteria.toDto(),
        isCatenaXMemberData = legalEntity?.isCatenaXMemberData ?: mainSite?.legalEntity?.isCatenaXMemberData ?: false,
        addressType = getAddressType(this)
    )
}

fun SiteDb.toV6Dto(): SiteVerboseDto {
    return SiteVerboseDto(
        bpn,
        name,
        states = states.map { it.toDto() },
        bpnLegalEntity = legalEntity.bpn,
        confidenceCriteria = confidenceCriteria.toDto(),
        isCatenaXMemberData = legalEntity.isCatenaXMemberData,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun LegalEntityDb.toV6Dto(): LegalEntityVerboseDto {
    return LegalEntityVerboseDto(
        bpnl = bpn,
        legalName = legalName.value,
        legalShortName = legalName.shortName,
        legalFormVerbose = legalForm?.toV6Dto(),
        identifiers = identifiers.map { it.toDto() },
        states = states.map { it.toDto() },
        relations = startNodeRelations.plus(endNodeRelations).map { it.toV6Dto() },
        currentness = currentness,
        confidenceCriteria = confidenceCriteria.toDto(),
        isCatenaXMemberData = isCatenaXMemberData,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun LegalFormDb.toV6Dto(): LegalFormDto {
    return LegalFormDto(
        technicalKey = technicalKey,
        name = name,
        transliteratedName = transliteratedName,
        abbreviation = abbreviation,
        transliteratedAbbreviations = transliteratedAbbreviations,
        country = countryCode,
        language = languageCode,
        administrativeAreaLevel1 = administrativeArea?.regionCode,
        isActive = isActive
    )
}

private fun RelationDb.toV6Dto(): RelationVerboseDto {
    return RelationVerboseDto(type, startNode.bpn, endNode.bpn)
}

fun LogisticAddressDb.toCreateResponse(index: String?): AddressPartnerCreateVerboseDto {
    return AddressPartnerCreateVerboseDto(
        address = toV6Dto(),
        index = index
    )
}

fun SiteDb.toUpsertDto(entryId: String?): SitePartnerCreateVerboseDto {
    return SitePartnerCreateVerboseDto(
        site = toV6Dto(),
        mainAddress = mainAddress.toV6Dto(),
        index = entryId
    )
}

fun LegalEntityDb.toUpsertDto(entryId: String?): LegalEntityPartnerCreateVerboseDto {
    return LegalEntityPartnerCreateVerboseDto(
        legalEntity = toV6Dto(),
        legalAddress = legalAddress.toV6Dto(),
        index = entryId
    )
}
