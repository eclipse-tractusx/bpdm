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

package org.eclipse.tractusx.bpdm.gate.service

import com.neovisionaries.i18n.CountryCode
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.exception.BpdmNullMappingException
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.ConfidenceCriteriaDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.StateDb
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.OutputUpsertData
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.PhysicalPostalAddress
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.State
import org.eclipse.tractusx.orchestrator.api.model.*
import org.eclipse.tractusx.orchestrator.api.model.GoldenRecordType
import org.springframework.stereotype.Service
import java.time.ZoneOffset

@Service
class OrchestratorMappings(
    private val bpnConfigProperties: BpnConfigProperties
) {
    private val logger = KotlinLogging.logger { }

    fun toCreateRequest(entity: BusinessPartnerDb): TaskCreateRequestEntry{
        return TaskCreateRequestEntry(
            recordId = entity.sharingState.orchestratorRecordId?.toString(),
            businessPartner = toOrchestratorDto(entity)
        )
    }

    fun toOrchestratorDto(entity: BusinessPartnerDb): BusinessPartner {
        val postalAddress = toPostalAddress(entity)
        val postalAddressScriptVariants = entity.scriptVariants.map { toPostalAddressScriptVariantWithScriptCode(it) }

        val isLegalAddress = entity.postalAddress.addressType == AddressType.LegalAddress || entity.postalAddress.addressType == AddressType.LegalAndSiteMainAddress
        val isMainAddress = entity.postalAddress.addressType == AddressType.SiteMainAddress

        return BusinessPartner(
            nameParts = emptyList(),
            uncategorized = UncategorizedProperties(
                nameParts = entity.nameParts,
                identifiers = entity.identifiers.filter { it.businessPartnerType == BusinessPartnerType.GENERIC }.map { Identifier(it.value, it.type, it.issuingBody) },
                states = entity.states.filter { it.businessPartnerTyp == BusinessPartnerType.GENERIC }.map { toState(it) },
                address = postalAddress.takeIf { entity.postalAddress.addressType == null }?.let { PostalAddressWithScriptVariants(it, postalAddressScriptVariants)  }
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
                isParticipantData = null,
                hasChanged = true,
                legalAddress = postalAddress.takeIf { isLegalAddress} ?: PostalAddress.empty,
                scriptVariants = entity.scriptVariants.takeIf { isLegalAddress }?.map { toLegalEntityScriptVariant(it) } ?:emptyList()
            ),
            site = Site(
                bpnReference = toBpnReference(entity.bpnS),
                siteName = entity.siteName,
                states = entity.states.filter { it.businessPartnerTyp == BusinessPartnerType.SITE }.map { toState(it) },
                confidenceCriteria = toConfidenceCriteria(entity.siteConfidence),
                hasChanged = true,
                siteMainAddress = postalAddress.takeIf { isMainAddress },
                scriptVariants = entity.scriptVariants.takeIf { isMainAddress }?.map { toSiteScriptVariant(it) } ?:emptyList()
            ).takeIf { entity.bpnS != null || entity.siteName != null },
            additionalAddress = postalAddress.takeIf { entity.postalAddress.addressType == AddressType.AdditionalAddress }?.let { PostalAddressWithScriptVariants(it, postalAddressScriptVariants) }
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
                        street = toStreet(street),
                        companyPostalCode = companyPostalCode,
                        industrialZone = industrialZone,
                        building = building,
                        floor = floor,
                        door = door,
                        taxJurisdictionCode = taxJurisdictionCode
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
            },
            hasChanged = null
        )
    }

    private fun toStreet(street: StreetDb?) =
        street?.let {
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
        } ?: Street.empty

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
        //only determine owner BPNL if it is own company data
        if(!entity.isOwnCompanyData) return null

        return entity.sharingState.tenantBpnl
                ?: bpnConfigProperties.ownerBpnL.takeIf { it.isNotBlank() }
                ?: run {
                    logger.warn { "Owner BPNL can't be determined for owned company data" }
                    null
                }
    }


    fun toOutputUpsertData(dto: BusinessPartner, roles: List<BusinessPartnerRole>, tenantBpnl: String?): OutputUpsertData {
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
            OutputUpsertData(
                nameParts = uncategorized.nameParts.toMutableList(),
                shortName = legalEntity.legalShortName,
                identifiers = uncategorized.identifiers.mapNotNull { toIdentifier(it, BusinessPartnerType.GENERIC) }
                    .plus(legalEntity.identifiers.mapNotNull { toIdentifier(it, BusinessPartnerType.LEGAL_ENTITY) })
                    .plus(postalAddress.identifiers.mapNotNull { toIdentifier(it, BusinessPartnerType.ADDRESS) }),
                legalName = legalEntity.legalName,
                siteName = site?.siteName,
                addressName = postalAddress.addressName,
                legalForm = legalEntity.legalForm,
                states = uncategorized.states.mapNotNull { toState(it, BusinessPartnerType.GENERIC) }
                    .plus(legalEntity.states.mapNotNull{ toState(it, BusinessPartnerType.LEGAL_ENTITY) })
                    .plus(site?.states?.mapNotNull{ toState(it, BusinessPartnerType.SITE) } ?: emptyList())
                    .plus(postalAddress.states.mapNotNull { toState(it, BusinessPartnerType.ADDRESS) }),
                roles = roles.toSortedSet(),
                addressType = addressType,
                physicalPostalAddress = toPhysicalPostalAddress(postalAddress.physicalAddress),
                alternativePostalAddress = postalAddress.alternativeAddress?.let(::toAlternativePostalAddress),
                legalEntityBpn = legalEntity.bpnReference.referenceValue!!,
                siteBpn = site?.bpnReference?.referenceValue,
                addressBpn = postalAddress.bpnReference.referenceValue!!,
                legalEntityConfidence = toConfidenceCriteria(legalEntity.confidenceCriteria),
                siteConfidence = site?.let { toConfidenceCriteria(it.confidenceCriteria) },
                addressConfidence = toConfidenceCriteria(postalAddress.confidenceCriteria),
                isOwnCompanyData = if (tenantBpnl != null && owningCompany != null) tenantBpnl == owningCompany else false,
                scriptVariants = toScriptVariants(addressType, dto)
            )
        }
    }

    private fun toScriptVariants(addressType: AddressType, dto: BusinessPartner): List<BusinessPartnerScriptVariantDto>{
        val addressScriptVariants = when(addressType){
            AddressType.LegalAndSiteMainAddress, AddressType.LegalAddress  -> dto.legalEntity.scriptVariants.map { PostalAddressScriptVariantWithScriptCode(it.scriptCode, it.legalAddress) }
            AddressType.SiteMainAddress -> dto.site!!.scriptVariants.map { PostalAddressScriptVariantWithScriptCode(it.scriptCode, it.mainAddress) }
            AddressType.AdditionalAddress -> dto.additionalAddress!!.scriptVariants.map { it }
        }

        val legalEntityVariantsByCode = dto.legalEntity.scriptVariants.associateBy { it.scriptCode }
        val siteScriptVariantsByCode = dto.site?.scriptVariants?.associateBy { it.scriptCode } ?: emptyMap()

        return addressScriptVariants.map { addressVariant ->
            val legalEntityVariant = toLegalEntityScriptVariant(legalEntityVariantsByCode[addressVariant.scriptCode]) ?: LegalEntityScriptVariantDto()
            val siteScriptVariant = toSiteScriptVariant(siteScriptVariantsByCode[addressVariant.scriptCode]) ?: SiteScriptVariantDto()
            val addressScriptVariant = toAddressScriptVariant(addressVariant.postalProperties)

            BusinessPartnerScriptVariantDto(addressVariant.scriptCode, legalEntity = legalEntityVariant, site = siteScriptVariant, address = addressScriptVariant)
        }
    }

    private fun toLegalEntityScriptVariant(dto: LegalEntityScriptVariant?) =
        dto?.let { LegalEntityScriptVariantDto(it.legalName, it.legalShortName) }

    private fun toSiteScriptVariant(dto: SiteScriptVariant?) =
        dto?.let { SiteScriptVariantDto(it.siteName) }

    private fun toAddressScriptVariant(dto: PostalAddressScriptVariant) =
        AddressScriptVariantDto(
            name = dto.addressName,
            physicalAddress = toPhysicalAddressScriptVariant(dto.physicalAddress),
            alternativeAddress = toAlternativeAddressScriptVariant(dto.alternativeAddress)
        )

    private fun toPhysicalAddressScriptVariant(dto: PhysicalAddressScriptVariant) =
        with(dto){
            PhysicalAddressScriptVariantDto(
                postalCode = postalCode,
                city = city,
                district = district,
                street = toScriptVariantStreet(street),
                companyPostalCode = companyPostalCode,
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door,
                taxJurisdictionCode = taxJurisdictionCode
            )
        }

    private fun toAlternativeAddressScriptVariant(dto: AlternativeAddressScriptVariant?) =
        dto?.let { with(it){
            AlternativeAddressScriptVariantDto(
                postalCode = postalCode,
                city = city,
                deliveryServiceQualifier = deliveryServiceQualifier,
                deliveryServiceNumber = deliveryServiceNumber
            )
        }
        }


    private fun toIdentifier(dto: Identifier, businessPartnerType: BusinessPartnerType) =
        dto.type?.let { type ->
            dto.value?.let { value ->
                org.eclipse.tractusx.bpdm.gate.model.upsert.output.Identifier(
                    type = type,
                    value = value,
                    issuingBody = dto.issuingBody,
                    businessPartnerType = businessPartnerType
                )
            }
        }

    private fun toState(dto: BusinessState, businessPartnerType: BusinessPartnerType) =
        dto.type?.let {
            State(
            type = it,
            validFrom = dto.validFrom?.atZone(ZoneOffset.UTC)?.toLocalDateTime(),
            validTo = dto.validTo?.atZone(ZoneOffset.UTC)?.toLocalDateTime(),
                businessPartnerType = businessPartnerType
            )
        }


    private fun toPhysicalPostalAddress(dto: PhysicalAddress) =
        PhysicalPostalAddress(
            geographicCoordinates = toGeographicCoordinate(dto.geographicCoordinates),
            country = CountryCode.getByAlpha2Code(dto.country),
            administrativeAreaLevel1 = dto.administrativeAreaLevel1,
            administrativeAreaLevel2 = dto.administrativeAreaLevel2,
            administrativeAreaLevel3 = dto.administrativeAreaLevel3,
            postalCode = dto.postalCode,
            city = dto.city ?: throw BpdmNullMappingException(BusinessPartner::class, OutputUpsertData::class, PhysicalAddress::city),
            district = dto.district,
            street = toStreet(dto.street),
            companyPostalCode = dto.companyPostalCode,
            industrialZone = dto.industrialZone,
            building = dto.building,
            floor = dto.floor,
            door = dto.door,
            taxJurisdictionCode = dto.taxJurisdictionCode
        )

    private fun toAlternativePostalAddress(dto: AlternativeAddress) =
        org.eclipse.tractusx.bpdm.gate.model.upsert.output.AlternativeAddress(
            geographicCoordinates = toGeographicCoordinate(dto.geographicCoordinates),
            country = CountryCode.getByAlpha2Code(dto.country),
            administrativeAreaLevel1 = dto.administrativeAreaLevel1,
            postalCode = dto.postalCode,
            city = dto.city ?: throw BpdmNullMappingException(BusinessPartner::class, OutputUpsertData::class, AlternativeAddress::city),
            deliveryServiceType = dto.deliveryServiceType ?: throw BpdmNullMappingException(
                BusinessPartner::class,
                OutputUpsertData::class,
                AlternativeAddress::deliveryServiceType
            ),
            deliveryServiceQualifier = dto.deliveryServiceQualifier,
            deliveryServiceNumber = dto.deliveryServiceNumber ?: throw BpdmNullMappingException(
                BusinessPartner::class,
                OutputUpsertData::class,
                AlternativeAddress::deliveryServiceNumber
            )
        )

    private fun toStreet(dto: Street) =
        org.eclipse.tractusx.bpdm.gate.model.upsert.output.Street(
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

    private fun toScriptVariantStreet(dto: Street) =
        StreetDto(
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
                org.eclipse.tractusx.bpdm.gate.model.upsert.output.GeoCoordinate(latitude = lat, longitude = lon, altitude = dto.altitude)
            }
        }

    private fun toConfidenceCriteria(dto: ConfidenceCriteria) =
        org.eclipse.tractusx.bpdm.gate.model.upsert.output.ConfidenceCriteria(
            sharedByOwner = dto.sharedByOwner!!,
            checkedByExternalDataSource = dto.checkedByExternalDataSource!!,
            numberOfSharingMembers = dto.numberOfSharingMembers!!,
            lastConfidenceCheckAt = dto.lastConfidenceCheckAt!!.atZone(ZoneOffset.UTC).toLocalDateTime(),
            nextConfidenceCheckAt = dto.nextConfidenceCheckAt!!.atZone(ZoneOffset.UTC).toLocalDateTime(),
            confidenceLevel = dto.confidenceLevel!!
        )

    private fun toPostalAddressScriptVariantWithScriptCode(scriptVariant: BusinessPartnerScriptVariantDb) =
        PostalAddressScriptVariantWithScriptCode(scriptVariant.scriptCode, toPostalAddressScriptVariant(scriptVariant))

    private fun toPostalAddressScriptVariant(scriptVariant: BusinessPartnerScriptVariantDb) =
        PostalAddressScriptVariant(
            addressName = scriptVariant.addressName,
            physicalAddress = toPhysicalAddressScriptVariant(scriptVariant.physicalAddress),
            alternativeAddress = toAlternativeAddressScriptVariant(scriptVariant.alternativeAddress)
        )

    private fun toPhysicalAddressScriptVariant(scriptVariant: PhysicalPostalAddressScriptVariantDb?) =
        scriptVariant?.let {
            with(it){
                PhysicalAddressScriptVariant(
                    postalCode = postalCode,
                    city = city,
                    district = district,
                    street = toStreet(street),
                    companyPostalCode = companyPostalCode,
                    industrialZone = industrialZone,
                    building = building,
                    floor = floor,
                    door = door,
                    taxJurisdictionCode = taxJurisdictionCode)
            }
        } ?: PhysicalAddressScriptVariant.empty


    private fun toAlternativeAddressScriptVariant(scriptVariant: AlternativePostalAddressScriptVariantDb?) =
        scriptVariant?.let {
            with(it){
                AlternativeAddressScriptVariant(
                    postalCode = postalCode,
                    city = city,
                    deliveryServiceQualifier = deliveryServiceQualifier,
                    deliveryServiceNumber = deliveryServiceNumber
                )
            }
        }

    private fun toLegalEntityScriptVariant(scriptVariantDb: BusinessPartnerScriptVariantDb) =
        LegalEntityScriptVariant(scriptVariantDb.scriptCode, scriptVariantDb.legalName, scriptVariantDb.shortName, toPostalAddressScriptVariant(scriptVariantDb))

    private fun toSiteScriptVariant(scriptVariantDb: BusinessPartnerScriptVariantDb) =
        SiteScriptVariant(scriptVariantDb.scriptCode, scriptVariantDb.siteName ?: "", toPostalAddressScriptVariant(scriptVariantDb))
}