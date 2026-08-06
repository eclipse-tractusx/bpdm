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

import org.eclipse.tractusx.bpdm.pool.api.v6.model.LegalEntityVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LegalFormDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LogisticAddressVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.SiteVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.*
import org.eclipse.tractusx.bpdm.pool.entity.*
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.toV6
import org.eclipse.tractusx.bpdm.pool.service.getAddressType
import org.eclipse.tractusx.bpdm.pool.service.mainSite
import org.eclipse.tractusx.bpdm.pool.service.toDto

/**
 * DB → `api.v6.model` DTO mappers. Aggregate shapes are named `toV6Dto`, not `toDto`, on purpose: the reused leaf value
 * mappers from `service.ResponseMappings` also declare `toDto` for the current API, so the distinct name keeps the two
 * versions unambiguous where both are in scope.
 */

fun LogisticAddressDb.toV6Dto(): LogisticAddressVerboseDtoV6 {
    return LogisticAddressVerboseDtoV6(
        bpna = bpn,
        bpnLegalEntity = legalEntity?.bpn,
        bpnSite = mainSite?.bpn,
        createdAt = createdAt,
        updatedAt = updatedAt,
        name = name,
        states = states.map { it.toDto().toV6() },
        identifiers = identifiers.map { it.toDto().toV6() },
        physicalPostalAddress = physicalPostalAddress.toDto().toV6(),
        alternativePostalAddress = alternativePostalAddress?.toDto()?.toV6(),
        confidenceCriteria = confidenceCriteria.toDto().toV6(),
        isCatenaXMemberData = legalEntity?.isDataSpaceParticipant ?: mainSite?.legalEntity?.isDataSpaceParticipant ?: false,
        addressType = getAddressType(this)
    )
}

fun SiteDb.toV6PoolDto(): SiteWithMainAddressVerboseDtoV6 {
    return SiteWithMainAddressVerboseDtoV6(
        site = toV6Dto(),
        mainAddress = mainAddress.toV6Dto()
    )
}

fun SiteDb.toV6Dto(): SiteVerboseDtoV6 {
    return SiteVerboseDtoV6(
        bpn,
        name,
        states = states.map { it.toDto().toV6() },
        bpnLegalEntity = legalEntity.bpn,
        confidenceCriteria = confidenceCriteria.toDto().toV6(),
        isCatenaXMemberData = legalEntity.isDataSpaceParticipant,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun LegalEntityDb.toV6PoolDto(): LegalEntityWithLegalAddressVerboseDtoV6 {
    return LegalEntityWithLegalAddressVerboseDtoV6(
        legalEntity = toV6Dto(),
        legalAddress = legalAddress.toV6Dto()
    )
}

fun LegalEntityDb.toV6Dto(): LegalEntityVerboseDtoV6 {
    return LegalEntityVerboseDtoV6(
        bpnl = bpn,
        legalName = legalName.value,
        legalShortName = legalName.shortName,
        legalFormVerbose = legalForm?.toV6Dto(),
        identifiers = identifiers.map { it.toDto().toV6() },
        states = states.map { it.toDto().toV6() },
        relations = startNodeRelations.plus(endNodeRelations).map { it.toV6Dto() },
        currentness = currentness,
        confidenceCriteria = confidenceCriteria.toDto().toV6(),
        isCatenaXMemberData = isDataSpaceParticipant,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun LegalFormDb.toV6Dto(): LegalFormDtoV6 {
    return LegalFormDtoV6(
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

private fun RelationDb.toV6Dto(): RelationVerboseDtoV6 {
    return RelationVerboseDtoV6(type.toV6(), startNode.bpn, endNode.bpn)
}

fun LogisticAddressDb.toCreateResponse(index: String?): AddressPartnerCreateVerboseDtoV6 {
    return AddressPartnerCreateVerboseDtoV6(
        address = toV6Dto(),
        index = index
    )
}

fun SiteDb.toUpsertDto(entryId: String?): SitePartnerCreateVerboseDtoV6 {
    return SitePartnerCreateVerboseDtoV6(
        site = toV6Dto(),
        mainAddress = mainAddress.toV6Dto(),
        index = entryId
    )
}

fun LegalEntityDb.toUpsertDto(entryId: String?): LegalEntityPartnerCreateVerboseDtoV6 {
    return LegalEntityPartnerCreateVerboseDtoV6(
        legalEntity = toV6Dto(),
        legalAddress = legalAddress.toV6Dto(),
        index = entryId
    )
}
