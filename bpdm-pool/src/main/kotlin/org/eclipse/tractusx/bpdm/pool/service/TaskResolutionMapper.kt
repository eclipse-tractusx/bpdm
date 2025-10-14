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
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.stereotype.Service
import java.time.ZoneOffset

@Service
class TaskResolutionMapper {

    fun toTaskResult(legalEntity: LegalEntityVerboseDto, legalAddress: LogisticAddressVerboseDto, hasChanged: Boolean?): LegalEntity{
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
                legalAddress = toTaskResult(legalAddress, hasChanged)
            )
        }
    }

    fun toTaskResult(site: SiteVerboseDto, siteMainAddress: LogisticAddressVerboseDto, hasChanged: Boolean?): Site{
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
                siteMainAddress = toTaskResult(siteMainAddress, hasChanged)
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

    fun toTaskResult(postalAddress: LogisticAddressVerboseDto, hasChanged: Boolean?): PostalAddress{
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


}