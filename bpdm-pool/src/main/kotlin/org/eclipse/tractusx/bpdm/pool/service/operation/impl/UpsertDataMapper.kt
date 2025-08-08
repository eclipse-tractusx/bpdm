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

package org.eclipse.tractusx.bpdm.pool.service.operation.impl

import org.eclipse.tractusx.bpdm.pool.dto.valid.*
import org.eclipse.tractusx.bpdm.pool.entity.*
import org.eclipse.tractusx.bpdm.pool.util.toDbTime
import org.springframework.stereotype.Service

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
@Service
class UpsertDataMapper {

    fun toConfidence(confidence: ConfidenceValid): ConfidenceCriteriaDb {
        return ConfidenceCriteriaDb(
            sharedByOwner = confidence.sharedByOwner,
            checkedByExternalDataSource = confidence.checkedByExternalDataSource,
            numberOfBusinessPartners = confidence.numberOfSharingMembers,
            lastConfidenceCheckAt = confidence.lastConfidenceCheckAt.toDbTime(),
            nextConfidenceCheckAt = confidence.nextConfidenceCheckAt.toDbTime(),
            confidenceLevel = confidence.confidenceLevel
        )
    }

    fun toPhysicalAddress(physicalPostalAddress: PhysicalAddressValid): PhysicalPostalAddressDb {
        return with(physicalPostalAddress){
            PhysicalPostalAddressDb(
                postalAddress.geographicCoordinates?.let { toGeoData(it) },
                postalAddress.country,
                postalAddress.administrativeAreaLevel1,
                administrativeAreaLevel2,
                administrativeAreaLevel3,
                null,
                postalAddress.postCode,
                postalAddress.city,
                district,
                null,
                toStreet(street),
                companyPostCode,
                industrialZone,
                building,
                floor,
                door,
                taxJurisdictionCode
            )
        }
    }

    fun toAlternativeAddress(alternativePostalAddress: AlternativeAddressValid): AlternativePostalAddressDb {
        return with(alternativePostalAddress){
            AlternativePostalAddressDb(
                postalAddress.geographicCoordinates?.let { toGeoData(it) },
                postalAddress.country,
                postalAddress.administrativeAreaLevel1,
                postalAddress.postCode,
                postalAddress.city,
                deliveryServiceType,
                deliveryServiceNumber,
                deliveryServiceQualifier
            )
        }
    }

    fun toStreet(street: StreetValid): StreetDb {
       return with(street){
           StreetDb(
               name,
               houseNumber,
               houseNumberSupplement,
               milestone,
               direction,
               namePrefix,
               additionalNamePrefix,
               nameSuffix,
               additionalNameSuffix
           )
       }
    }

    fun toAddressIdentifier(
        address: LogisticAddressDb,
        identifier: IdentifierValid
    ): AddressIdentifierDb {
        return AddressIdentifierDb(identifier.identifierValue, identifier.identifierType, address)
    }

    fun toAddressState(
        address: LogisticAddressDb,
        businessState: BusinessStateValid
    ): AddressStateDb {
       return AddressStateDb(businessState.validFrom?.toDbTime(), businessState.validTo?.toDbTime(), businessState.type, address)
    }

    private fun toGeoData(
        geoData: GeoDataValid
    ): GeographicCoordinateDb {
        return GeographicCoordinateDb(geoData.latitude, geoData.longitude, geoData.altitude)
    }
}