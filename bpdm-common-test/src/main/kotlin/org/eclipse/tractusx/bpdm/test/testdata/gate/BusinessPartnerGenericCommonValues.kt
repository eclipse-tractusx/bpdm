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

package org.eclipse.tractusx.bpdm.test.testdata.gate

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.orchestrator.api.model.*
import java.time.LocalDateTime
import java.time.ZoneOffset

object BusinessPartnerGenericCommonValues {

    val address = PostalAddress(
        bpnReference = BpnReference("000000123CCC333", null, BpnReferenceType.BpnRequestIdentifier),
        hasChanged = true,
        addressName = "Address Name",
        physicalAddress = PhysicalAddress(
            geographicCoordinates = GeoCoordinate(0.5f, 0.5f, 0.5f),
            country = CountryCode.PT.alpha2,
            administrativeAreaLevel1 = "PT-PT",
            administrativeAreaLevel2 = "pt-admin-level-2-cleaned",
            administrativeAreaLevel3 = "pt-admin-level-3-cleaned",
            postalCode = "phys-postal-code-cleaned",
            city = "city",
            district = "district",
            street = Street(
                name = "name",
                houseNumber = "house-number",
                houseNumberSupplement = "house-number-supplement",
                milestone = "milestone",
                direction = "direction",
                namePrefix = "name-prefix",
                additionalNamePrefix = "add-name-prefix",
                nameSuffix = "name-suffix",
                additionalNameSuffix = "add-name-suffix"

            ),
            companyPostalCode = "comp-postal-code",
            industrialZone = "industrial-zone",
            building = "building",
            floor = "floor",
            door = "door"
        ),
        alternativeAddress = AlternativeAddress(
            geographicCoordinates = GeoCoordinate(0.6f, 0.6f, 0.6f),
            country = CountryCode.PT.alpha2,
            administrativeAreaLevel1 = "PT-PT",
            postalCode = "postal-code-cleaned",
            city = "alt-city-cleaned",
            deliveryServiceNumber = "delivery-service-number-cleaned",
            deliveryServiceQualifier = "delivery-service-qualifier-cleaned",
            deliveryServiceType = DeliveryServiceType.PO_BOX
        ),
        confidenceCriteria = ConfidenceCriteria(
            sharedByOwner = false,
            checkedByExternalDataSource = true,
            numberOfSharingMembers = 4,
            lastConfidenceCheckAt = LocalDateTime.of(2020, 4, 3, 2, 1).toInstant(ZoneOffset.UTC),
            nextConfidenceCheckAt = LocalDateTime.of(2028, 4, 3, 2, 1).toInstant(ZoneOffset.UTC),
            confidenceLevel = 5
        ),
        identifiers = emptyList(),
        states = emptyList()
    )

    //Business Partner with two entries in every collection
    val businessPartner1 = BusinessPartner(
        nameParts = emptyList(),
        uncategorized = UncategorizedProperties(
            nameParts = listOf("part-cleaned-1", "name-cleaned-2"),
            identifiers = listOf(
                Identifier(
                    type = "identifier-type-1-cleaned",
                    value = "identifier-value-1-cleaned",
                    issuingBody = "issuingBody-1-cleaned"
                ),
                Identifier(
                    type = "identifier-type-2-cleaned",
                    value = "identifier-value-2-cleaned",
                    issuingBody = "issuingBody-2-cleaned"
                )
            ),
            states = listOf(
                BusinessState(
                    validFrom = LocalDateTime.of(2020, 9, 22, 15, 50).toInstant(ZoneOffset.UTC),
                    validTo = LocalDateTime.of(2023, 10, 23, 16, 40).toInstant(ZoneOffset.UTC),
                    type = BusinessStateType.INACTIVE
                ),
                BusinessState(
                    validFrom = LocalDateTime.of(2000, 8, 21, 14, 30).toInstant(ZoneOffset.UTC),
                    validTo = LocalDateTime.of(2020, 9, 22, 15, 50).toInstant(ZoneOffset.UTC),
                    type = BusinessStateType.ACTIVE
                )
            ),
            address = address
        ),
        owningCompany = "BPNL_CLEANED_VALUES",
        legalEntity = LegalEntity(
            bpnReference = BpnReference("000000123AAA123", null, BpnReferenceType.BpnRequestIdentifier),
            legalName = "legal-name-cleaned",
            legalShortName = "shot-name-cleaned",
            legalForm = "legal-form-cleaned",
            confidenceCriteria = ConfidenceCriteria(
                sharedByOwner = true,
                checkedByExternalDataSource = true,
                numberOfSharingMembers = 7,
                lastConfidenceCheckAt = LocalDateTime.of(2022, 4, 3, 2, 1).toInstant(ZoneOffset.UTC),
                nextConfidenceCheckAt = LocalDateTime.of(2026, 4, 3, 2, 1).toInstant(ZoneOffset.UTC),
                confidenceLevel = 1
            ),
            identifiers = emptyList(),
            states = emptyList(),
            isCatenaXMemberData = false,
            hasChanged = true,
            legalAddress = address
        ),
        site = Site(
            bpnReference = BpnReference("000000123BBB222", null, BpnReferenceType.BpnRequestIdentifier),
            siteName = "Site Name",
            confidenceCriteria = ConfidenceCriteria(
                sharedByOwner = false,
                checkedByExternalDataSource = false,
                numberOfSharingMembers = 8,
                lastConfidenceCheckAt = LocalDateTime.of(2023, 4, 3, 2, 1).toInstant(ZoneOffset.UTC),
                nextConfidenceCheckAt = LocalDateTime.of(2024, 4, 3, 2, 1).toInstant(ZoneOffset.UTC),
                confidenceLevel = 2
            ),
            states = emptyList(),
            hasChanged = true,
            siteMainAddress = address
        ),
        additionalAddress = address
    )
}