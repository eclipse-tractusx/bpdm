/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.gate.service

import com.neovisionaries.i18n.CountryCode
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.exception.BpdmNullMappingException
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.gate.entity.AlternativePostalAddressDb
import org.eclipse.tractusx.bpdm.gate.entity.GeographicCoordinateDb
import org.eclipse.tractusx.bpdm.gate.entity.PhysicalPostalAddressDb
import org.eclipse.tractusx.bpdm.gate.entity.StreetDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.*
import org.eclipse.tractusx.orchestrator.api.model.*
import org.eclipse.tractusx.orchestrator.api.model.BpnReferenceType
import org.springframework.stereotype.Service
import java.time.ZoneOffset
import java.util.SortedSet

@Service
class OrchestratorMappings(
    private val bpnConfigProperties: BpnConfigProperties
) {
    private val logger = KotlinLogging.logger { }

    fun toOrchestratorDto(entity: BusinessPartnerDb): BusinessPartner {
        val postalAddress = toPostalAddress(entity)

        return BusinessPartner(
            nameParts = emptyList(),
            uncategorized = UncategorizedProperties(
                nameParts = entity.nameParts,
                identifiers = entity.identifiers.filter { it.businessPartnerType == BusinessPartnerType.GENERIC }.map { Identifier(it.value, it.type, it.issuingBody) },
                states = entity.states.filter { it.businessPartnerTyp == BusinessPartnerType.GENERIC }.map { toState(it) },
                address = postalAddress.takeIf { entity.postalAddress.addressType == null }
            ),
            owningCompany = getOwnerBpnL(entity),
            legalEntity = LegalEntity(
                bpnReference = toBpnReference(entity.bpnL),
                legalName = entity.legalName,
                legalForm = entity.legalForm,
                legalShortName = entity.shortName,
                identifiers = entity.identifiers.filter { it.businessPartnerType == BusinessPartnerType.LEGAL_ENTITY }.map { Identifier(it.value, it.type, it.issuingBody) },
                states = entity.states.filter { it.businessPartnerTyp == BusinessPartnerType.LEGAL_ENTITY }.map { toState(it) },
                confidenceCriteria = toConfidenceCriteria(entity.legalEntityConfidence),
                isCatenaXMemberData = null,
                hasChanged = true,
                legalAddress = postalAddress.takeIf { entity.postalAddress.addressType == AddressType.LegalAddress || entity.postalAddress.addressType == AddressType.LegalAndSiteMainAddress } ?: PostalAddress.empty
            ),
            site = Site(
                bpnReference = toBpnReference(entity.bpnS),
                siteName = entity.siteName,
                states = entity.states.filter { it.businessPartnerTyp == BusinessPartnerType.SITE }.map { toState(it) },
                confidenceCriteria = toConfidenceCriteria(entity.siteConfidence),
                hasChanged = true,
                siteMainAddress = postalAddress.takeIf { entity.postalAddress.addressType == AddressType.SiteMainAddress }
            ).takeIf { entity.bpnS != null || entity.siteName != null },
            additionalAddress = postalAddress.takeIf { entity.postalAddress.addressType == AddressType.AdditionalAddress }
        )
    }

    private fun toPostalAddress(entity: BusinessPartnerDb): PostalAddress {
        return PostalAddress(
            bpnReference = toBpnReference(entity.bpnA),
            addressName = entity.addressName,
            identifiers = entity.identifiers.filter { it.businessPartnerType == BusinessPartnerType.ADDRESS }.map { Identifier(it.value, it.type, it.issuingBody) },
            states = entity.states.filter { it.businessPartnerTyp == BusinessPartnerType.ADDRESS }.map { toState(it) },
            confidenceCriteria = toConfidenceCriteria(entity.addressConfidence),
            physicalAddress = entity.postalAddress.physicalPostalAddress?.let {
                with(it) {
                    PhysicalAddress(
                        geographicCoordinates = toGeographicCoordinates(geographicCoordinates),
                        country = country?.alpha2,
                        administrativeAreaLevel1 = administrativeAreaLevel1,
                        administrativeAreaLevel2 = administrativeAreaLevel2,
                        administrativeAreaLevel3 = administrativeAreaLevel3,
                        postalCode = postalCode,
                        city = city,
                        district = district,
                        street = street?.let {
                            with(it) {
                                Street(
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
                            }
                        } ?: Street.empty,
                        companyPostalCode = companyPostalCode,
                        industrialZone = industrialZone,
                        building = building,
                        floor = floor,
                        door = door
                    )
                }
            } ?: PhysicalAddress.empty,
            alternativeAddress = entity.postalAddress.alternativePostalAddress?.let {
                with(it) {
                    AlternativeAddress(
                        geographicCoordinates = toGeographicCoordinates(geographicCoordinates),
                        country = country?.alpha2,
                        administrativeAreaLevel1,
                        postalCode,
                        city,
                        deliveryServiceType,
                        deliveryServiceQualifier,
                        deliveryServiceNumber
                    )
                }
            } ?: AlternativeAddress.empty,
            hasChanged = null
        )
    }


    private fun toGeographicCoordinates(geoCoordinate: GeographicCoordinateDb?): GeoCoordinate {
        return geoCoordinate?.let { with(it) {
                GeoCoordinate(longitude, latitude, altitude)
            }
        } ?: GeoCoordinate.empty
    }


    private fun toConfidenceCriteria(confidenceCriteria: ConfidenceCriteriaDb?): ConfidenceCriteria {
        return confidenceCriteria?.let {
            with(it){
                ConfidenceCriteria(
                    sharedByOwner,
                    checkedByExternalDataSource,
                    numberOfBusinessPartners,
                    lastConfidenceCheckAt.toInstant(ZoneOffset.UTC),
                    nextConfidenceCheckAt.toInstant(ZoneOffset.UTC),
                    confidenceLevel
                )
            }
        } ?: ConfidenceCriteria.empty
    }

    private fun toState(state: StateDb): BusinessState {
        return  BusinessState(state.validFrom?.toInstant(ZoneOffset.UTC), state.validTo?.toInstant(ZoneOffset.UTC), state.type)
    }

    private fun toBpnReference(value: String?) =
        value?.let {
            BpnReference(
                referenceValue = value,
                desiredBpn = null,
                referenceType = BpnReferenceType.Bpn
            )
        } ?: BpnReference.empty

    private fun getOwnerBpnL(entity: BusinessPartnerDb): String? {
        return if(entity.associatedOwnerBpnl != null){
            entity.associatedOwnerBpnl
        }else if (entity.isOwnCompanyData) {
            bpnConfigProperties.ownerBpnL
        }
        else {
            logger.warn { "Owner BPNL property is not configured" }
            null
        }
    }


    //Mapping from orchestrator model to entity
    fun toBusinessPartner(dto: BusinessPartner, externalId: String, associatedOwnerBpnl: String?, roles: SortedSet<BusinessPartnerRole>): BusinessPartnerDb{
        val addressType = when(dto.type){
            GoldenRecordType.LegalEntity -> AddressType.LegalAddress
            GoldenRecordType.Site -> if(dto.site!!.siteMainIsLegalAddress) AddressType.LegalAndSiteMainAddress else AddressType.SiteMainAddress
            GoldenRecordType.Address -> AddressType.AdditionalAddress
            null -> throw BpdmNullMappingException(BusinessPartner::class, BusinessPartnerDb::class, BusinessPartner::type)
        }

        val postalAddress = when(addressType){
            AddressType.LegalAndSiteMainAddress -> dto.legalEntity.legalAddress
            AddressType.LegalAddress -> dto.legalEntity.legalAddress
            AddressType.SiteMainAddress -> dto.site!!.siteMainAddress!!
            AddressType.AdditionalAddress -> dto.additionalAddress!!
        }

        return with(dto) {
            BusinessPartnerDb(
                externalId = externalId,
                nameParts = uncategorized.nameParts.toMutableList(),
                shortName = legalEntity.legalShortName,
                identifiers = uncategorized.identifiers.mapNotNull { toIdentifier(it, BusinessPartnerType.GENERIC) }
                    .plus(legalEntity.identifiers.mapNotNull { toIdentifier(it, BusinessPartnerType.LEGAL_ENTITY) })
                    .plus(postalAddress.identifiers.mapNotNull { toIdentifier(it, BusinessPartnerType.ADDRESS) })
                    .toSortedSet(),
                legalName = legalEntity.legalName,
                siteName = site?.siteName,
                addressName = postalAddress.addressName,
                legalForm = legalEntity.legalForm,
                states = uncategorized.states.asSequence().mapNotNull { toState(it, BusinessPartnerType.GENERIC) }
                    .plus(legalEntity.states.mapNotNull{ toState(it, BusinessPartnerType.LEGAL_ENTITY) })
                    .plus(site?.states?.mapNotNull{ toState(it, BusinessPartnerType.SITE) } ?: emptyList())
                    .plus(postalAddress.states.mapNotNull{ toState(it, BusinessPartnerType.ADDRESS)} )
                    .toSortedSet(),
                roles = roles,
                postalAddress = PostalAddressDb(
                    addressType = addressType,
                    physicalPostalAddress = toPhysicalPostalAddress(postalAddress.physicalAddress),
                    alternativePostalAddress = postalAddress.alternativeAddress?.let(::toAlternativePostalAddress)
                ),
                bpnL = legalEntity.bpnReference.referenceValue!!,
                bpnS = site?.bpnReference?.referenceValue,
                bpnA = postalAddress.bpnReference.referenceValue!!,
                stage = StageType.Output,
                legalEntityConfidence = toConfidenceCriteria(legalEntity.confidenceCriteria),
                siteConfidence = site?.let { toConfidenceCriteria(it.confidenceCriteria) },
                addressConfidence = toConfidenceCriteria(postalAddress.confidenceCriteria),
                associatedOwnerBpnl = associatedOwnerBpnl
            )
        }
    }


    private fun toIdentifier(dto: Identifier, businessPartnerType: BusinessPartnerType) =
        dto.type?.let { type ->
            dto.value?.let { value ->
                IdentifierDb(type = type, value = value, issuingBody = dto.issuingBody, businessPartnerType = businessPartnerType)
            }
        }

    private fun toState(dto: BusinessState, businessPartnerType: BusinessPartnerType) =
        dto.type?.let { StateDb(
            type = it,
            validFrom = dto.validFrom?.atZone(ZoneOffset.UTC)?.toLocalDateTime(),
            validTo = dto.validTo?.atZone(ZoneOffset.UTC)?.toLocalDateTime(),
            businessPartnerTyp =  businessPartnerType)
        }


    private fun toPhysicalPostalAddress(dto: PhysicalAddress) =
        PhysicalPostalAddressDb(
            geographicCoordinates = toGeographicCoordinate(dto.geographicCoordinates),
            country = CountryCode.getByAlpha2Code(dto.country),
            administrativeAreaLevel1 = dto.administrativeAreaLevel1,
            administrativeAreaLevel2 = dto.administrativeAreaLevel2,
            administrativeAreaLevel3 = dto.administrativeAreaLevel3,
            postalCode = dto.postalCode,
            city = dto.city,
            district = dto.district,
            street = toStreet(dto.street),
            companyPostalCode = dto.companyPostalCode,
            industrialZone = dto.industrialZone,
            building = dto.building,
            floor = dto.floor,
            door = dto.door
        )

    private fun toAlternativePostalAddress(dto: AlternativeAddress) =
        AlternativePostalAddressDb(
            geographicCoordinates = toGeographicCoordinate(dto.geographicCoordinates),
            country = CountryCode.getByAlpha2Code(dto.country),
            administrativeAreaLevel1 = dto.administrativeAreaLevel1,
            postalCode = dto.postalCode,
            city = dto.city,
            deliveryServiceType = dto.deliveryServiceType,
            deliveryServiceQualifier = dto.deliveryServiceQualifier,
            deliveryServiceNumber = dto.deliveryServiceNumber
        )

    private fun toStreet(dto: Street) =
        StreetDb(
            name = dto.name,
            houseNumber = dto.houseNumber,
            houseNumberSupplement = dto.houseNumberSupplement,
            milestone = dto.milestone,
            direction = dto.direction,
            namePrefix = dto.namePrefix,
            additionalNamePrefix = dto.additionalNamePrefix,
            nameSuffix = dto.nameSuffix,
            additionalNameSuffix = dto.additionalNameSuffix
        )

    private fun toGeographicCoordinate(dto: GeoCoordinate) =
        dto.latitude?.let { lat ->
            dto.longitude?.let {  lon ->
                GeographicCoordinateDb(latitude = lat, longitude = lon, altitude = dto.altitude)
            }
        }

    private fun toConfidenceCriteria(dto: ConfidenceCriteria) =
        ConfidenceCriteriaDb(
            sharedByOwner = dto.sharedByOwner!!,
            checkedByExternalDataSource = dto.checkedByExternalDataSource!!,
            numberOfBusinessPartners = dto.numberOfSharingMembers!!,
            lastConfidenceCheckAt = dto.lastConfidenceCheckAt!!.atZone(ZoneOffset.UTC).toLocalDateTime(),
            nextConfidenceCheckAt = dto.nextConfidenceCheckAt!!.atZone(ZoneOffset.UTC).toLocalDateTime(),
            confidenceLevel = dto.confidenceLevel!!
        )
}