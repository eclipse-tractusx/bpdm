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

package org.eclipse.tractusx.bpdm.pool.service

import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.stereotype.Service
import java.time.ZoneOffset

@Service
class TaskResolutionMapper {

    fun toTaskResult(legalEntity: LegalEntityWithLegalAddressVerboseDto, hasChanged: Boolean?): LegalEntity{
        return toTaskResult(legalEntity.header, legalEntity.legalAddress, hasChanged, legalEntity.scriptVariants)
    }

    fun toTaskResult(legalEntity: LegalEntityHeaderVerboseDto, legalAddress: LogisticAddressInvariantVerboseDto, hasChanged: Boolean?, scriptVariants: List<LegalEntityScriptVariantDto>): LegalEntity{
        return with(legalEntity){
            LegalEntity(
                bpnReference = BpnReference(bpnl, null, BpnReferenceType.Bpn),
                legalName = legalName,
                legalShortName = legalShortName,
                legalForm = legalForm,
                identifiers = identifiers.map { Identifier(it.value, it.type, it.issuingBody) },
                states = states.map { BusinessState(it.validFrom?.toInstant(ZoneOffset.UTC), it.validTo?.toInstant(ZoneOffset.UTC), it.type) },
                confidenceCriteria = toTaskResult(confidenceCriteria),
                isParticipantData = legalEntity.isParticipantData,
                hasChanged = hasChanged,
                legalAddress = toTaskResult(legalAddress, hasChanged),
                scriptVariants = scriptVariants.map { toTaskResult(it) }
            )
        }
    }

    fun toTaskResult(site: SiteVerboseDto, siteMainAddress: LogisticAddressInvariantVerboseDto, hasChanged: Boolean?): Site{
        return with(site){
            Site(
                bpnReference = BpnReference(bpns, null, BpnReferenceType.Bpn),
                siteName = name,
                states = states.map { BusinessState(it.validFrom?.toInstant(ZoneOffset.UTC), it.validTo?.toInstant(ZoneOffset.UTC), it.type) },
                confidenceCriteria = toTaskResult(confidenceCriteria),
                hasChanged = hasChanged,
                //Normally this should be null if the site main address is also the legal address
                //However, due to synchronization issues we will pass the address here
                // and perform that last step to set this to null later on after we use this site main address to override the legal entities legal address
                siteMainAddress = toTaskResult(siteMainAddress, hasChanged),
                scriptVariants = scriptVariants.map { toTaskResult(it) }
            )
        }
    }

    fun toTaskResult(confidenceCriteria: ConfidenceCriteriaDto): ConfidenceCriteria{
        return with(confidenceCriteria){
            ConfidenceCriteria(
                sharedByOwner = sharedByOwner,
                checkedByExternalDataSource = checkedByExternalDataSource,
                numberOfSharingMembers = numberOfSharingMembers,
                lastConfidenceCheckAt = lastConfidenceCheckAt.toInstant(ZoneOffset.UTC),
                nextConfidenceCheckAt = nextConfidenceCheckAt.toInstant(ZoneOffset.UTC),
                confidenceLevel = confidenceLevel
            )
        }
    }

    fun toTaskResult(postalAddress: LogisticAddressInvariantVerboseDto, scriptVariants: List<LogisticAddressScriptVariantDto>, hasChanged: Boolean?): PostalAddressWithScriptVariants{
        return PostalAddressWithScriptVariants(toTaskResult(postalAddress, hasChanged), scriptVariants.map { toTaskResult(it) })
    }

    fun toTaskResult(postalAddress: LogisticAddressInvariantVerboseDto, hasChanged: Boolean?): PostalAddress{
        return with(postalAddress){
            PostalAddress(
                bpnReference = BpnReference(bpna, null, BpnReferenceType.Bpn),
                addressName = name,
                identifiers = identifiers.map { Identifier(it.value, it.type, null) },
                states =  states.map { BusinessState(it.validFrom?.toInstant(ZoneOffset.UTC), it.validTo?.toInstant(ZoneOffset.UTC), it.type) },
                confidenceCriteria = toTaskResult(confidenceCriteria),
                physicalAddress = toTaskResult(physicalPostalAddress),
                alternativeAddress =  alternativePostalAddress?.let { toTaskResult(it) },
                hasChanged = hasChanged
            )
        }
    }

    fun toTaskResult(physicalAddress: PhysicalPostalAddressVerboseDto): PhysicalAddress{
        return with(physicalAddress){
            PhysicalAddress(
                geographicCoordinates = geographicCoordinates?.let { with(it){ GeoCoordinate(longitude, latitude, altitude) } } ?: GeoCoordinate.empty,
                country = physicalAddress.country.alpha2,
                administrativeAreaLevel1 = physicalAddress.administrativeAreaLevel1,
                administrativeAreaLevel2 = physicalAddress.administrativeAreaLevel2,
                administrativeAreaLevel3 = physicalAddress.administrativeAreaLevel3,
                postalCode = postalCode,
                city = city,
                district = district,
                street = street?.let { toTaskResult(it) } ?: Street.empty,
                companyPostalCode = companyPostalCode,
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door,
                taxJurisdictionCode = taxJurisdictionCode

            )
        }
    }

    fun toTaskResult(alternativeAddress: AlternativePostalAddressVerboseDto): AlternativeAddress{
        return with(alternativeAddress){
            AlternativeAddress(
                geographicCoordinates = geographicCoordinates?.let { with(it){ GeoCoordinate(longitude, latitude, altitude) } } ?: GeoCoordinate.empty,
                country = country.alpha2,
                administrativeAreaLevel1 = administrativeAreaLevel1,
                postalCode = postalCode,
                city = city,
                deliveryServiceType = deliveryServiceType,
                deliveryServiceQualifier = deliveryServiceQualifier,
                deliveryServiceNumber = deliveryServiceNumber
            )
        }
    }


    fun toTaskResult(street: StreetDto): Street{
        return with(street){
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
    }

    fun toTaskResult(legalEntityScriptVariant: LegalEntityScriptVariantDto): LegalEntityScriptVariant{
        return with(legalEntityScriptVariant){
            LegalEntityScriptVariant(
                scriptCode = scriptCode,
                legalName = legalName,
                legalShortName = shortName,
                legalAddress = toTaskResult(legalAddress)
            )
        }
    }

    fun toTaskResult(siteScriptVariant: SiteScriptVariantDto): SiteScriptVariant{
        return with(siteScriptVariant){
            SiteScriptVariant(
                scriptCode = scriptCode,
                siteName = name,
                mainAddress = toTaskResult(mainAddress)
            )
        }
    }

    fun toTaskResult(addressScriptVariant: LogisticAddressScriptVariantDto): PostalAddressScriptVariantWithScriptCode{
        return with(addressScriptVariant){
            PostalAddressScriptVariantWithScriptCode(addressScriptVariant.scriptCode, toTaskResult(address))
        }
    }

    fun toTaskResult(addressScriptVariant: PostalAddressScriptVariantDto): org.eclipse.tractusx.orchestrator.api.model.PostalAddressScriptVariant{
        return with(addressScriptVariant){
            PostalAddressScriptVariant(
                addressName = addressName,
                physicalAddress = toTaskResult(physicalAddress),
                alternativeAddress = alternativeAddress?.let { toTaskResult(it) })
        }
    }

    fun toTaskResult(physicalAddress: PhysicalAddressScriptVariantDto): PhysicalAddressScriptVariant{
        return with(physicalAddress){
            PhysicalAddressScriptVariant(
                postalCode = postalCode,
                city = city,
                district = district,
                street = street?.let { toTaskResult(it) } ?: Street.empty,
                companyPostalCode = companyPostalCode,
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door,
                taxJurisdictionCode = taxJurisdictionCode)
        }
    }

    fun toTaskResult(alternativeAddressScriptVariant: AlternativeAddressScriptVariantDto): AlternativeAddressScriptVariant{
        return with(alternativeAddressScriptVariant){
            AlternativeAddressScriptVariant(
                postalCode = postalCode,
                city = city,
                deliveryServiceQualifier = deliveryServiceQualifier,
                deliveryServiceNumber = deliveryServiceNumber
            )
        }
    }


}