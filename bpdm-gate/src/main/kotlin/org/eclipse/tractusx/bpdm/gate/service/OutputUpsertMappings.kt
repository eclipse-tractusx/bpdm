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

import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.eclipse.tractusx.bpdm.gate.entity.generic.*
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.*
import org.springframework.stereotype.Service

@Service
class OutputUpsertMappings {

    fun toEntity(upsertData: OutputUpsertData, sharingState: SharingStateDb): BusinessPartnerDb {
        return with(upsertData) {
            BusinessPartnerDb(
                sharingState = sharingState,
                stage = StageType.Output,
                nameParts = nameParts.toMutableList(),
                roles = roles.toSortedSet(),
                identifiers = identifiers.map { it.toEntity() }.toSortedSet(),
                states = states.map { it.toEntity() }.toSortedSet(),
                shortName = shortName,
                legalName = legalName,
                siteName = siteName,
                addressName = addressName,
                legalForm = legalForm,
                isOwnCompanyData = isOwnCompanyData,
                bpnL = legalEntityBpn,
                bpnS = siteBpn,
                bpnA = addressBpn,
                postalAddress = PostalAddressDb(addressType, physicalPostalAddress.toEntity(), alternativePostalAddress?.toEntity()),
                legalEntityConfidence = legalEntityConfidence.toEntity(),
                siteConfidence = siteConfidence?.toEntity(),
                addressConfidence = addressConfidence.toEntity()
            )
        }
    }

    private fun Identifier.toEntity() =
        IdentifierDb(type = type, value = value, issuingBody = issuingBody, businessPartnerType = businessPartnerType)

    private fun State.toEntity() =
        StateDb(type = type, validFrom = validFrom, validTo = validTo, businessPartnerTyp = businessPartnerType)

    private fun ConfidenceCriteria.toEntity() =
        ConfidenceCriteriaDb(sharedByOwner, checkedByExternalDataSource, numberOfSharingMembers, lastConfidenceCheckAt, nextConfidenceCheckAt, confidenceLevel)


    private fun PhysicalPostalAddress.toEntity() =
        PhysicalPostalAddressDb(
            geographicCoordinates = geographicCoordinates?.toEntity(),
            country = country,
            administrativeAreaLevel1 = administrativeAreaLevel1,
            administrativeAreaLevel2 = administrativeAreaLevel2,
            administrativeAreaLevel3 = administrativeAreaLevel3,
            postalCode = postalCode,
            city = city,
            district = district,
            street = street?.toEntity(),
            companyPostalCode = companyPostalCode,
            industrialZone = industrialZone,
            building = building,
            floor = floor,
            door = door,
            taxJurisdictionCode = taxJurisdictionCode
        )

    private fun AlternativeAddress.toEntity() =
        AlternativePostalAddressDb(
            geographicCoordinates = geographicCoordinates?.toEntity(),
            country = country,
            administrativeAreaLevel1 = administrativeAreaLevel1,
            postalCode = postalCode,
            city = city,
            deliveryServiceType = deliveryServiceType,
            deliveryServiceQualifier = deliveryServiceQualifier,
            deliveryServiceNumber = deliveryServiceNumber
        )

    private fun Street.toEntity() =
        StreetDb(
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

    private fun GeoCoordinate.toEntity() = GeographicCoordinateDb(latitude = latitude, longitude = longitude, altitude = altitude)

}