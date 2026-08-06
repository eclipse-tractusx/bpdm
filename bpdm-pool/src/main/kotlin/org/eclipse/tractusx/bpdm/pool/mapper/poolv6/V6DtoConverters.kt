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

package org.eclipse.tractusx.bpdm.pool.mapper.poolv6

/*
 * Translation between the frozen v6 API DTOs and the (evolving) v7 API DTOs.
 *
 * The v6 model was split off from v7 into its own package so the two can evolve independently. The internal Pool
 * services and the shared v7 request/response mappers still speak the v7 DTOs, so the v6-specific controllers,
 * application services and mappers bridge across with these converters. Where a v7 type carries something v6 does not
 * - script variants, for instance - its converter here is the single place that reconciles the difference.
 */

import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.api.model.request.*
import org.eclipse.tractusx.bpdm.pool.api.model.response.*
import org.eclipse.tractusx.bpdm.pool.api.v6.model.AddressIdentifierDtoV6 as V6AddressIdentifierDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.AddressIdentifierVerboseDtoV6 as V6AddressIdentifierVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.AddressStateDtoV6 as V6AddressStateDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.AddressStateVerboseDtoV6 as V6AddressStateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.AlternativePostalAddressDtoV6 as V6AlternativePostalAddressDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.AlternativePostalAddressVerboseDtoV6 as V6AlternativePostalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.ChangelogTypeV6 as V6ChangelogType
import org.eclipse.tractusx.bpdm.pool.api.v6.model.ConfidenceCriteriaDtoV6 as V6ConfidenceCriteriaDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.CountrySubdivisionDtoV6 as V6CountrySubdivisionDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.CxMembershipDtoV6 as V6CxMembershipDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.IdentifierBusinessPartnerTypeV6 as V6IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.v6.model.IdentifierTypeDetailDtoV6 as V6IdentifierTypeDetailDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.IdentifierTypeDtoV6 as V6IdentifierTypeDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LegalEntityIdentifierVerboseDtoV6 as V6LegalEntityIdentifierVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LegalEntityRelationTypeV6 as V6LegalEntityRelationType
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LegalEntityStateVerboseDtoV6 as V6LegalEntityStateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LogisticAddressDtoV6 as V6LogisticAddressDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.PhysicalPostalAddressDtoV6 as V6PhysicalPostalAddressDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.PhysicalPostalAddressVerboseDtoV6 as V6PhysicalPostalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.RegionDtoV6 as V6RegionDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.SiteDtoV6 as V6SiteDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.SiteStateDtoV6 as V6SiteStateDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.SiteStateVerboseDtoV6 as V6SiteStateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.StreetDtoV6 as V6StreetDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.AddressSearchRequestV6 as V6AddressSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.BpnRequestIdentifierSearchRequestV6 as V6BpnRequestIdentifierSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.ChangelogSearchRequestV6 as V6ChangelogSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.CxMembershipSearchRequestV6 as V6CxMembershipSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.CxMembershipUpdateRequestV6 as V6CxMembershipUpdateRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.IdentifiersSearchRequestV6 as V6IdentifiersSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalEntitySearchRequestV6 as V6LegalEntitySearchRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalFormRequestV6 as V6LegalFormRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.SiteCreateRequestWithLegalAddressAsMainV6 as V6SiteCreateRequestWithLegalAddressAsMain
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.SitePartnerCreateRequestV6 as V6SitePartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.SitePartnerUpdateRequestV6 as V6SitePartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.SiteSearchRequestV6 as V6SiteSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.BpnIdentifierMappingDtoV6 as V6BpnIdentifierMappingDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.BpnRequestIdentifierMappingDtoV6 as V6BpnRequestIdentifierMappingDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.ChangelogEntryVerboseDtoV6 as V6ChangelogEntryVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.ErrorInfoV6 as V6ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.FieldQualityRuleDtoV6 as V6FieldQualityRuleDto

/* ----------------------------- Enums ----------------------------- */

fun IdentifierBusinessPartnerType.toV6() = V6IdentifierBusinessPartnerType.valueOf(name)
fun V6IdentifierBusinessPartnerType.toV7() = IdentifierBusinessPartnerType.valueOf(name)
fun LegalEntityRelationType.toV6() = V6LegalEntityRelationType.valueOf(name)

/* --------------------- Outbound: v7 verbose -> v6 --------------------- */

fun ConfidenceCriteriaDto.toV6() = V6ConfidenceCriteriaDto(
    sharedByOwner = sharedByOwner,
    checkedByExternalDataSource = checkedByExternalDataSource,
    numberOfSharingMembers = numberOfSharingMembers,
    lastConfidenceCheckAt = lastConfidenceCheckAt,
    nextConfidenceCheckAt = nextConfidenceCheckAt,
    confidenceLevel = confidenceLevel
)

fun RegionDto.toV6() = V6RegionDto(countryCode = countryCode, regionCode = regionCode, regionName = regionName)

fun StreetDto.toV6() = V6StreetDto(
    name = name,
    houseNumber = houseNumber,
    houseNumberSupplement = houseNumberSupplement,
    milestone = milestone,
    direction = direction,
    namePrefix = namePrefix,
    additionalNamePrefix = additionalNamePrefix,
    nameSuffix = nameSuffix,
    additionalNameSuffix = additionalNameSuffix
)

fun AddressStateVerboseDto.toV6() = V6AddressStateVerboseDto(validFrom = validFrom, validTo = validTo, typeVerbose = typeVerbose)
fun AddressIdentifierVerboseDto.toV6() = V6AddressIdentifierVerboseDto(value = value, typeVerbose = typeVerbose)
fun SiteStateVerboseDto.toV6() = V6SiteStateVerboseDto(validFrom = validFrom, validTo = validTo, typeVerbose = typeVerbose)
fun LegalEntityStateVerboseDto.toV6() = V6LegalEntityStateVerboseDto(validFrom = validFrom, validTo = validTo, typeVerbose = typeVerbose)
fun LegalEntityIdentifierVerboseDto.toV6() = V6LegalEntityIdentifierVerboseDto(value = value, typeVerbose = typeVerbose, issuingBody = issuingBody)

fun PhysicalPostalAddressVerboseDto.toV6() = V6PhysicalPostalAddressVerboseDto(
    geographicCoordinates = geographicCoordinates,
    countryVerbose = countryVerbose,
    administrativeAreaLevel1Verbose = administrativeAreaLevel1Verbose?.toV6(),
    administrativeAreaLevel2 = administrativeAreaLevel2,
    administrativeAreaLevel3 = administrativeAreaLevel3,
    postalCode = postalCode,
    city = city,
    district = district,
    street = street?.toV6(),
    companyPostalCode = companyPostalCode,
    industrialZone = industrialZone,
    building = building,
    floor = floor,
    door = door,
    taxJurisdictionCode = taxJurisdictionCode
)

fun AlternativePostalAddressVerboseDto.toV6() = V6AlternativePostalAddressVerboseDto(
    geographicCoordinates = geographicCoordinates,
    countryVerbose = countryVerbose,
    administrativeAreaLevel1Verbose = administrativeAreaLevel1Verbose?.toV6(),
    postalCode = postalCode,
    city = city,
    deliveryServiceType = deliveryServiceType,
    deliveryServiceQualifier = deliveryServiceQualifier,
    deliveryServiceNumber = deliveryServiceNumber
)

fun ChangelogEntryVerboseDto.toV6() = V6ChangelogEntryVerboseDto(
    bpn = bpn,
    businessPartnerType = businessPartnerType,
    timestamp = timestamp,
    changelogType = V6ChangelogType.valueOf(changelogType.name)
)

fun CountrySubdivisionDto.toV6() = V6CountrySubdivisionDto(countryCode = countryCode, code = code, name = name)

fun FieldQualityRuleDto.toV6() = V6FieldQualityRuleDto(
    fieldPath = fieldPath,
    schemaName = schemaName,
    country = country,
    qualityLevel = qualityLevel
)

fun BpnIdentifierMappingDto.toV6() = V6BpnIdentifierMappingDto(idValue = idValue, bpn = bpn)

fun BpnRequestIdentifierMappingDto.toV6() = V6BpnRequestIdentifierMappingDto(requestedIdentifier = requestedIdentifier, bpn = bpn)

fun DataSpaceParticipantDto.toV6() = V6CxMembershipDto(bpnL = bpnL, isCatenaXMember = isDataSpaceParticipant)

fun IdentifierTypeDto.toV6() = V6IdentifierTypeDto(
    technicalKey = technicalKey,
    businessPartnerType = businessPartnerType.toV6(),
    name = name,
    abbreviation = abbreviation,
    transliteratedName = transliteratedName,
    transliteratedAbbreviation = transliteratedAbbreviation,
    details = details.map { V6IdentifierTypeDetailDto(country = it.country, mandatory = it.mandatory) }
)

/* --------------------- Inbound: v6 -> v7 --------------------- */

fun V6ConfidenceCriteriaDto.toV7() = ConfidenceCriteriaDto(
    sharedByOwner = sharedByOwner,
    checkedByExternalDataSource = checkedByExternalDataSource,
    numberOfSharingMembers = numberOfSharingMembers,
    lastConfidenceCheckAt = lastConfidenceCheckAt,
    nextConfidenceCheckAt = nextConfidenceCheckAt,
    confidenceLevel = confidenceLevel
)

fun V6StreetDto.toV7() = StreetDto(
    name = name,
    houseNumber = houseNumber,
    houseNumberSupplement = houseNumberSupplement,
    milestone = milestone,
    direction = direction,
    namePrefix = namePrefix,
    additionalNamePrefix = additionalNamePrefix,
    nameSuffix = nameSuffix,
    additionalNameSuffix = additionalNameSuffix
)

fun V6AddressStateDto.toV7() = AddressStateDto(validFrom = validFrom, validTo = validTo, type = type)
fun V6AddressIdentifierDto.toV7() = org.eclipse.tractusx.bpdm.pool.api.model.AddressIdentifierDto(value = value, type = type)
fun V6SiteStateDto.toV7() = SiteStateDto(validFrom = validFrom, validTo = validTo, type = type)

fun V6PhysicalPostalAddressDto.toV7() = org.eclipse.tractusx.bpdm.pool.api.model.PhysicalPostalAddressDto(
    geographicCoordinates = geographicCoordinates,
    country = country,
    administrativeAreaLevel1 = administrativeAreaLevel1,
    administrativeAreaLevel2 = administrativeAreaLevel2,
    administrativeAreaLevel3 = administrativeAreaLevel3,
    postalCode = postalCode,
    city = city,
    district = district,
    street = street?.toV7(),
    companyPostalCode = companyPostalCode,
    industrialZone = industrialZone,
    building = building,
    floor = floor,
    door = door,
    taxJurisdictionCode = taxJurisdictionCode
)

fun V6AlternativePostalAddressDto.toV7() = org.eclipse.tractusx.bpdm.pool.api.model.AlternativePostalAddressDto(
    geographicCoordinates = geographicCoordinates,
    country = country,
    administrativeAreaLevel1 = administrativeAreaLevel1,
    postalCode = postalCode,
    city = city,
    deliveryServiceType = deliveryServiceType,
    deliveryServiceQualifier = deliveryServiceQualifier,
    deliveryServiceNumber = deliveryServiceNumber
)

fun V6LogisticAddressDto.toV7() = LogisticAddressDto(
    name = name,
    states = states.map { it.toV7() },
    identifiers = identifiers.map { it.toV7() },
    physicalPostalAddress = physicalPostalAddress.toV7(),
    alternativePostalAddress = alternativePostalAddress?.toV7(),
    confidenceCriteria = confidenceCriteria.toV7(),
    updatedAt = updatedAt
)

// v6 has no script variants, so every v6 write sends none. Because a v7 write replaces the full script variant list,
// this means a v6 write drops the script variants a business partner may have gained through v7 or the task path.
fun V6SiteDto.toV7() = SiteDto(
    name = name,
    states = states.map { it.toV7() },
    mainAddress = mainAddress.toV7(),
    confidenceCriteria = confidenceCriteria.toV7(),
    scriptVariants = emptyList(),
    updatedAt = updatedAt
)

/* --------------------- Inbound: v6 request -> v7 request --------------------- */

fun V6AddressSearchRequest.toV7() = AddressSearchRequest(
    addressBpns = addressBpns,
    legalEntityBpns = legalEntityBpns,
    siteBpns = siteBpns,
    name = name
)

fun V6ChangelogSearchRequest.toV7() = ChangelogSearchRequest(
    timestampAfter = timestampAfter,
    bpns = bpns,
    businessPartnerTypes = businessPartnerTypes
)

fun V6CxMembershipSearchRequest.toV7() = DataSpaceParticipantSearchRequest(
    bpnLs = bpnLs,
    isDataSpaceParticipant = isCatenaXMember
)

fun V6CxMembershipUpdateRequest.toV7() = DataSpaceParticipantUpdateRequest(
    participants = memberships.map { DataSpaceParticipantDto(bpnL = it.bpnL, isDataSpaceParticipant = it.isCatenaXMember) }
)

fun V6LegalFormRequest.toV7() = LegalFormRequest(
    technicalKey = technicalKey,
    name = name,
    transliteratedName = transliteratedName,
    abbreviations = abbreviation,
    transliteratedAbbreviations = transliteratedAbbreviations,
    country = country,
    language = language,
    administrativeAreaLevel1 = administrativeAreaLevel1,
    isActive = isActive
)

fun V6IdentifierTypeDto.toV7() = IdentifierTypeDto(
    technicalKey = technicalKey,
    businessPartnerType = businessPartnerType.toV7(),
    name = name,
    abbreviation = abbreviation,
    transliteratedName = transliteratedName,
    transliteratedAbbreviation = transliteratedAbbreviation,
    format = null,
    categories = sortedSetOf(),
    details = details.map { IdentifierTypeDetailDto(country = it.country, mandatory = it.mandatory) }
)

fun V6IdentifiersSearchRequest.toV7() = IdentifiersSearchRequest(
    businessPartnerType = businessPartnerType.toV7(),
    idType = idType,
    idValues = idValues
)

fun V6BpnRequestIdentifierSearchRequest.toV7() = BpnRequestIdentifierSearchRequest(requestedIdentifiers = requestedIdentifiers)

fun V6LegalEntitySearchRequest.toV7() = LegalEntitySearchRequest(
    bpnLs = bpnLs,
    legalName = legalName
)

fun V6SiteSearchRequest.toV7() = SiteSearchRequest(
    siteBpns = siteBpns,
    legalEntityBpns = legalEntityBpns,
    name = name
)

fun V6SitePartnerCreateRequest.toV7() = SitePartnerCreateRequest(site = site.toV7(), bpnlParent = bpnlParent, index = index)
fun V6SitePartnerUpdateRequest.toV7() = SitePartnerUpdateRequest(bpns = bpns, site = site.toV7())
fun V6SiteCreateRequestWithLegalAddressAsMain.toV7() = SiteCreateRequestWithLegalAddressAsMain(
    name = name,
    states = states.map { it.toV7() },
    confidenceCriteria = confidenceCriteria.toV7(),
    bpnLParent = bpnLParent,
    scriptVariants = emptyList()
)

/* --------------------- Outbound: v7 error info -> v6 --------------------- */

/**
 * Converts a v7 [ErrorInfo] to its v6 counterpart. The target v6 error enum must be given explicitly (it cannot be
 * inferred); the matching constant is resolved by name, which is safe because the v6 and v7 error enums share constants.
 */
inline fun <SOURCE, reified TARGET> ErrorInfo<SOURCE>.toV6(): V6ErrorInfo<TARGET>
        where SOURCE : Enum<SOURCE>, SOURCE : ErrorCode, TARGET : Enum<TARGET>, TARGET : org.eclipse.tractusx.bpdm.pool.api.v6.model.response.ErrorCodeV6 =
    V6ErrorInfo(enumValueOf<TARGET>(errorCode.name), message, entityKey)
