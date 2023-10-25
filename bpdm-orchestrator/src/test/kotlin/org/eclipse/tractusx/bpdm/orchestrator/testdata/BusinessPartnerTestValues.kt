/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.orchestrator.testdata

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.*
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.ClassificationType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.orchestrator.api.model.*
import org.eclipse.tractusx.orchestrator.api.model.AddressIdentifierDto
import org.eclipse.tractusx.orchestrator.api.model.AddressStateDto
import org.eclipse.tractusx.orchestrator.api.model.AlternativePostalAddressDto
import org.eclipse.tractusx.orchestrator.api.model.LegalEntityDto
import org.eclipse.tractusx.orchestrator.api.model.LegalEntityIdentifierDto
import org.eclipse.tractusx.orchestrator.api.model.LogisticAddressDto
import org.eclipse.tractusx.orchestrator.api.model.PhysicalPostalAddressDto
import org.eclipse.tractusx.orchestrator.api.model.SiteDto
import org.eclipse.tractusx.orchestrator.api.model.SiteStateDto
import org.eclipse.tractusx.orchestrator.api.model.StreetDto
import java.time.LocalDateTime

/**
 * Contains complex test values of business partners that can be used as templates by the test classes
 * Test values here should have as many unique values as possible to reduce the probability of finding matching errors
 */
object BusinessPartnerTestValues {

    //Business Partner with two entries in every collection
    val businessPartner1 = BusinessPartnerGenericDto(
        nameParts = listOf("NamePart1", "NamePart2"),
        shortName = "shortname",
        identifiers = listOf(
            BusinessPartnerIdentifierDto(
                type = "identifier-type-1",
                value = "identifier-value-1",
                issuingBody = "issuingBody-1"
            ),
            BusinessPartnerIdentifierDto(
                type = "identifier-type-2",
                value = "identifier-value-2",
                issuingBody = "issuingBody-2"
            ),
        ),
        legalForm = "legal-form",
        states = listOf(
            BusinessPartnerStateDto(
                validFrom = LocalDateTime.of(2020, 9, 22, 15, 50),
                validTo = LocalDateTime.of(2023, 10, 23, 16, 40),
                type = BusinessStateType.INACTIVE,
                description = "business-state-description-1"
            ),
            BusinessPartnerStateDto(
                validFrom = LocalDateTime.of(2000, 8, 21, 14, 30),
                validTo = LocalDateTime.of(2020, 9, 22, 15, 50),
                type = BusinessStateType.ACTIVE,
                description = "business-state-description-2"
            )
        ),
        classifications = listOf(
            ClassificationDto(
                type = ClassificationType.NACE,
                code = "code-1",
                value = "value-1"
            ),
            ClassificationDto(
                type = ClassificationType.NAF,
                code = "code-2",
                value = "value-2"
            ),
        ),
        roles = listOf(
            BusinessPartnerRole.CUSTOMER,
            BusinessPartnerRole.SUPPLIER
        ),
        postalAddress = PostalAddressDto(
            addressType = AddressType.AdditionalAddress,
            physicalPostalAddress = PhysicalPostalAddressDto(
                geographicCoordinates = GeoCoordinateDto(0.5f, 0.5f, 0.5f),
                country = CountryCode.DE,
                administrativeAreaLevel1 = "DE-BW",
                administrativeAreaLevel2 = "bw-admin-level-2",
                administrativeAreaLevel3 = "bw-admin-level-3",
                postalCode = "phys-postal-code",
                city = "city",
                district = "district",
                street = StreetDto(
                    name = "name",
                    houseNumber = "house-number",
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
            alternativePostalAddress = AlternativePostalAddressDto(
                geographicCoordinates = GeoCoordinateDto(0.6f, 0.6f, 0.6f),
                country = CountryCode.DE,
                administrativeAreaLevel1 = "DE-BY",
                postalCode = "alt-post-code",
                city = "alt-city",
                deliveryServiceNumber = "delivery-service-number",
                deliveryServiceQualifier = "delivery-service-qualifier",
                deliveryServiceType = DeliveryServiceType.PO_BOX
            )
        ),
        ownerBpnL = "BPNL_OWNER_TEST_1",
        bpnL = "BPNLTEST",
        bpnS = "BPNSTEST",
        bpnA = "BPNATEST"
    )

    //Business Partner with single entry in every collection
    val businessPartner2 = BusinessPartnerGenericDto(
        nameParts = listOf("name-part-2"),
        shortName = "shortname-2",
        identifiers = listOf(
            BusinessPartnerIdentifierDto(
                type = "identifier-type-2",
                value = "identifier-value-2",
                issuingBody = "issuingBody-2"
            )
        ),
        legalForm = "legal-form-2",
        states = listOf(
            BusinessPartnerStateDto(
                validFrom = LocalDateTime.of(1988, 10, 4, 22, 30),
                validTo = LocalDateTime.of(2023, 1, 1, 10, 10),
                type = BusinessStateType.ACTIVE,
                description = "business-state-description-2"
            )
        ),
        classifications = listOf(
            ClassificationDto(
                type = ClassificationType.SIC,
                code = "code-2",
                value = "value-2"
            )
        ),
        roles = listOf(
            BusinessPartnerRole.CUSTOMER
        ),
        postalAddress = PostalAddressDto(
            addressType = AddressType.LegalAddress,
            physicalPostalAddress = PhysicalPostalAddressDto(
                geographicCoordinates = GeoCoordinateDto(0.4f, 0.4f, 0.4f),
                country = CountryCode.FR,
                administrativeAreaLevel1 = "FR-ARA",
                administrativeAreaLevel2 = "fr-admin-level-2",
                administrativeAreaLevel3 = "fr-admin-level-3",
                postalCode = "phys-postal-code-2",
                city = "city-2",
                district = "district-2",
                street = StreetDto(
                    name = "name-2",
                    houseNumber = "house-number-2",
                    milestone = "milestone-2",
                    direction = "direction-2",
                    namePrefix = "name-prefix-2",
                    additionalNamePrefix = "add-name-prefix-2",
                    nameSuffix = "name-suffix-2",
                    additionalNameSuffix = "add-name-suffix-2"

                ),
                companyPostalCode = "comp-postal-code-2",
                industrialZone = "industrial-zone-2",
                building = "building-2",
                floor = "floor-2",
                door = "door-2"
            ),
            alternativePostalAddress = AlternativePostalAddressDto(
                geographicCoordinates = GeoCoordinateDto(0.2f, 0.2f, 0.2f),
                country = CountryCode.FR,
                administrativeAreaLevel1 = "FR-BFC",
                postalCode = "alt-post-code-2",
                city = "alt-city-2",
                deliveryServiceNumber = "delivery-service-number-2",
                deliveryServiceQualifier = "delivery-service-qualifier-2",
                deliveryServiceType = DeliveryServiceType.BOITE_POSTALE
            )
        ),
        ownerBpnL = "BPNL_OWNER_TEST_2",
        bpnL = "BPNLTEST-2",
        bpnS = "BPNSTEST-2",
        bpnA = "BPNATEST-2"
    )

    val logisticAddress1 = LogisticAddressDto(
        name = "Address Name 1",
        states = listOf(
            AddressStateDto(
                description = "Address State 1",
                validFrom = LocalDateTime.of(1970, 4, 4, 4, 4),
                validTo = LocalDateTime.of(1975, 5, 5, 5, 5),
                type = BusinessStateType.ACTIVE
            ),
            AddressStateDto(
                description = "Address State 2",
                validFrom = LocalDateTime.of(1975, 5, 5, 5, 5),
                validTo = null,
                type = BusinessStateType.INACTIVE
            ),
        ),
        identifiers = listOf(
            AddressIdentifierDto(
                value = "Address Identifier Value 1",
                type = "Address Identifier Type 1"
            ),
            AddressIdentifierDto(
                value = "Address Identifier Value 2",
                type = "Address Identifier Type 2"
            )
        ),
        physicalPostalAddress = PhysicalPostalAddressDto(
            geographicCoordinates = GeoCoordinateDto(0.12f, 0.12f, 0.12f),
            country = CountryCode.AD,
            administrativeAreaLevel1 = "AD-07",
            administrativeAreaLevel2 = "Admin-Level 2-1",
            administrativeAreaLevel3 = "Admin-Level 3-1",
            postalCode = "Postal Code 1",
            city = "City 1",
            district = "District 1",
            street = StreetDto(
                name = "Street Name 1",
                houseNumber = "House Number 1",
                milestone = "Milestone 1",
                direction = "Direction 1",
                namePrefix = "Name Prefix 1",
                additionalNameSuffix = "Additional Name Suffix 1",
                additionalNamePrefix = "Additional Name Prefix 1",
                nameSuffix = "Name Suffix 1"
            ),
            companyPostalCode = "Company Postal Code 1",
            industrialZone = "Industrial Zone 1",
            building = "Building 1",
            floor = "Floor 1",
            door = "Door 1"
        ),
        alternativePostalAddress = AlternativePostalAddressDto(
            geographicCoordinates = GeoCoordinateDto(0.23f, 0.23f, 0.23f),
            country = CountryCode.AD,
            administrativeAreaLevel1 = "AD-08",
            postalCode = "Postal Code Alt 1",
            city = "City Alt 1",
            deliveryServiceType = DeliveryServiceType.PRIVATE_BAG,
            deliveryServiceQualifier = "Delivery Service Qualifier 1",
            deliveryServiceNumber = "Delivery Service Number 1"
        ),
        bpnAReference = BpnReferenceDto(
            referenceType = BpnReferenceType.Bpn,
            referenceValue = "BPNATEST-1"
        ),
        hasChanged = true
    )

    val logisticAddress2 = LogisticAddressDto(
        name = "Address Name 2",
        states = listOf(
            AddressStateDto(
                description = "Address State 2",
                validFrom = LocalDateTime.of(1971, 4, 4, 4, 4),
                validTo = null,
                type = BusinessStateType.ACTIVE
            )
        ),
        identifiers = listOf(
            AddressIdentifierDto(
                value = "Address Identifier Value 2-1",
                type = "Address Identifier Type 2-1"
            )
        ),
        physicalPostalAddress = PhysicalPostalAddressDto(
            geographicCoordinates = GeoCoordinateDto(0.45f, 0.46f, 0.47f),
            country = CountryCode.AD,
            administrativeAreaLevel1 = "AD-07",
            administrativeAreaLevel2 = "Admin-Level 2-2",
            administrativeAreaLevel3 = "Admin-Level 3-2",
            postalCode = "Postal Code 2",
            city = "City 2",
            district = "District 2",
            street = StreetDto(
                name = "Street Name 2",
                houseNumber = "House Number 2",
                milestone = "Milestone 2",
                direction = "Direction 2",
                namePrefix = "Name Prefix 2",
                additionalNameSuffix = "Additional Name Suffix 2",
                additionalNamePrefix = "Additional Name Prefix 2",
                nameSuffix = "Name Suffix 2"
            ),
            companyPostalCode = "Company Postal Code 2",
            industrialZone = "Industrial Zone 2",
            building = "Building 2",
            floor = "Floor 2",
            door = "Door 2"
        ),
        alternativePostalAddress = AlternativePostalAddressDto(
            geographicCoordinates = GeoCoordinateDto(0.01f, 0.02f, 0.03f),
            country = CountryCode.AD,
            administrativeAreaLevel1 = "AD-08",
            postalCode = "Postal Code Alt 2",
            city = "City Alt 2",
            deliveryServiceType = DeliveryServiceType.PO_BOX,
            deliveryServiceQualifier = "Delivery Service Qualifier 2",
            deliveryServiceNumber = "Delivery Service Number 2"
        ),
        bpnAReference = BpnReferenceDto(
            referenceType = BpnReferenceType.BpnRequestIdentifier,
            referenceValue = "BPN_REQUEST_ID_TEST"
        ),
        hasChanged = true
    )

    val legalEntity1 = LegalEntityDto(
        legalName = "Legal Entity Name 1",
        legalShortName = "Legal Short Name 1",
        identifiers = listOf(
            LegalEntityIdentifierDto(
                value = "Legal Identifier Value 1",
                type = "Legal Identifier Type 1",
                issuingBody = "Legal Issuing Body 1"
            ),
            LegalEntityIdentifierDto(
                value = "Legal Identifier Value 2",
                type = "Legal Identifier Type 2",
                issuingBody = "Legal Issuing Body 2"
            ),
        ),
        legalForm = "Legal Form 1",
        states = listOf(
            LegalEntityState(
                description = "Legal State Description 1",
                validFrom = LocalDateTime.of(1995, 2, 2, 3, 3),
                validTo = LocalDateTime.of(2000, 3, 3, 4, 4),
                type = BusinessStateType.ACTIVE
            ),
            LegalEntityState(
                description = "Legal State Description 2",
                validFrom = LocalDateTime.of(2000, 3, 3, 4, 4),
                validTo = null,
                type = BusinessStateType.INACTIVE
            ),
        ),
        classifications = listOf(
            ClassificationDto(
                type = ClassificationType.SIC,
                code = "Classification Code 1",
                value = "Classification Value 1"
            ),
            ClassificationDto(
                type = ClassificationType.NACE,
                code = "Classification Code 2",
                value = "Classification Value 2"
            )
        ),
        legalAddress = logisticAddress1,
        bpnLReference = BpnReferenceDto(
            referenceValue = "BPNL1-TEST",
            referenceType = BpnReferenceType.Bpn
        ),
        hasChanged = false
    )

    val legalEntity2 = LegalEntityDto(
        legalName = "Legal Entity Name 2",
        legalShortName = "Legal Short Name 2",
        identifiers = listOf(
            LegalEntityIdentifierDto(
                value = "Legal Identifier Value 2",
                type = "Legal Identifier Type 2",
                issuingBody = "Legal Issuing Body 2"
            )
        ),
        legalForm = "Legal Form 2",
        states = listOf(
            LegalEntityState(
                description = "Legal State Description 2",
                validFrom = LocalDateTime.of(1900, 5, 5, 5, 5),
                validTo = null,
                type = BusinessStateType.ACTIVE
            )
        ),
        classifications = listOf(
            ClassificationDto(
                type = ClassificationType.SIC,
                code = "Classification Code 2",
                value = "Classification Value 2"
            )
        ),
        legalAddress = logisticAddress2,
        bpnLReference = BpnReferenceDto(
            referenceValue = "BPNL-REQUEST_ID_TEST",
            referenceType = BpnReferenceType.BpnRequestIdentifier
        ),
        hasChanged = false
    )

    val site1 = SiteDto(
        name = "Site Name 1",
        states = listOf(
            SiteStateDto(
                description = "Site State Description 1",
                validFrom = LocalDateTime.of(1991, 10, 10, 10, 10),
                validTo = LocalDateTime.of(2001, 11, 11, 11, 11),
                type = BusinessStateType.ACTIVE
            ),
            SiteStateDto(
                description = "Site State Description 2",
                validFrom = LocalDateTime.of(2001, 11, 11, 11, 11),
                validTo = null,
                type = BusinessStateType.INACTIVE
            )
        ),
        mainAddress = logisticAddress1,
        bpnSReference = BpnReferenceDto(
            referenceValue = "BPNS_TEST",
            referenceType = BpnReferenceType.Bpn
        ),
        hasChanged = false
    )

    val site2 = SiteDto(
        name = "Site Name 2",
        states = listOf(
            SiteStateDto(
                description = "Site State Description 2",
                validFrom = LocalDateTime.of(1997, 12, 12, 12, 12),
                validTo = null,
                type = BusinessStateType.ACTIVE
            )
        ),
        mainAddress = logisticAddress2,
        bpnSReference = BpnReferenceDto(
            referenceValue = "BPNS_REQUEST_ID_TEST",
            referenceType = BpnReferenceType.BpnRequestIdentifier
        ),
        hasChanged = true
    )

    val businessPartner1Full = BusinessPartnerFullDto(
        generic = businessPartner1,
        legalEntity = legalEntity1,
        site = site1,
        address = logisticAddress1
    )

    val businessPartner2Full = BusinessPartnerFullDto(
        generic = businessPartner2,
        legalEntity = legalEntity2,
        site = site2,
        address = logisticAddress2
    )

}