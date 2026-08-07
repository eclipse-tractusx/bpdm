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

import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.api.model.response.*
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LegalEntityVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LegalFormDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LogisticAddressVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.SiteVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.*
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.toV6

/*
 * Aggregate-level translation of the v7 API response DTOs into their v6 counterparts, on top of the leaf converters in
 * V6DtoConverters. The v6 controllers reach the stored data through the v7 response mappers and convert here, so v6
 * response shaping stays a translation of the current API rather than a second mapping of the entities.
 */

fun LogisticAddressInvariantVerboseDto.toV6Dto(): LogisticAddressVerboseDtoV6 =
    LogisticAddressVerboseDtoV6(
        bpna = bpna,
        bpnLegalEntity = bpnLegalEntity,
        bpnSite = bpnSite,
        createdAt = createdAt,
        updatedAt = updatedAt,
        name = name,
        states = states.map { it.toV6() },
        identifiers = identifiers.map { it.toV6() },
        physicalPostalAddress = physicalPostalAddress.toV6(),
        alternativePostalAddress = alternativePostalAddress?.toV6(),
        confidenceCriteria = confidenceCriteria.toV6(),
        isCatenaXMemberData = isParticipantData,
        addressType = addressType
    )

fun SiteWithMainAddressVerboseDto.toV6PoolDto(): SiteWithMainAddressVerboseDtoV6 =
    SiteWithMainAddressVerboseDtoV6(
        site = site.toV6Dto(),
        mainAddress = mainAddress.toV6Dto()
    )

fun SiteVerboseDto.toV6Dto(): SiteVerboseDtoV6 =
    SiteVerboseDtoV6(
        bpns,
        name,
        states = states.map { it.toV6() },
        bpnLegalEntity = bpnLegalEntity,
        confidenceCriteria = confidenceCriteria.toV6(),
        isCatenaXMemberData = isParticipantData,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun LegalEntityWithLegalAddressVerboseDto.toV6PoolDto(): LegalEntityWithLegalAddressVerboseDtoV6 =
    LegalEntityWithLegalAddressVerboseDtoV6(
        legalEntity = header.toV6Dto(),
        legalAddress = legalAddress.toV6Dto()
    )

fun LegalEntityHeaderVerboseDto.toV6Dto(): LegalEntityVerboseDtoV6 =
    LegalEntityVerboseDtoV6(
        bpnl = bpnl,
        legalName = legalName,
        legalShortName = legalShortName,
        legalFormVerbose = legalFormVerbose?.toV6Dto(),
        identifiers = identifiers.map { it.toV6() },
        states = states.map { it.toV6() },
        relations = relations.map { it.toV6Dto() },
        currentness = currentness,
        confidenceCriteria = confidenceCriteria.toV6(),
        isCatenaXMemberData = isParticipantData,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun LegalFormDto.toV6Dto(): LegalFormDtoV6 =
    LegalFormDtoV6(
        technicalKey = technicalKey,
        name = name,
        transliteratedName = transliteratedName,
        abbreviation = abbreviations,
        transliteratedAbbreviations = transliteratedAbbreviations,
        country = country,
        language = language,
        administrativeAreaLevel1 = administrativeAreaLevel1,
        isActive = isActive
    )

private fun RelationVerboseDto.toV6Dto(): RelationVerboseDtoV6 =
    RelationVerboseDtoV6(type.toV6(), businessPartnerSourceBpnl, businessPartnerTargetBpnl)

fun AddressPartnerCreateVerboseDto.toV6CreateResponse(): AddressPartnerCreateVerboseDtoV6 =
    AddressPartnerCreateVerboseDtoV6(
        address = address.toV6Dto(),
        index = index
    )

fun SitePartnerCreateVerboseDto.toV6UpsertDto(): SitePartnerCreateVerboseDtoV6 =
    SitePartnerCreateVerboseDtoV6(
        site = site.toV6Dto(),
        mainAddress = mainAddress.toV6Dto(),
        index = index
    )

fun LegalEntityPartnerCreateVerboseDto.toV6UpsertDto(): LegalEntityPartnerCreateVerboseDtoV6 =
    LegalEntityPartnerCreateVerboseDtoV6(
        legalEntity = legalEntity.header.toV6Dto(),
        legalAddress = legalEntity.legalAddress.toV6Dto(),
        index = index
    )
