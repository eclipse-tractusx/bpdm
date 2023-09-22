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

package org.eclipse.tractusx.bpdm.orchestrator.util

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.*
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.ClassificationType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.orchestrator.api.model.*
import org.eclipse.tractusx.orchestrator.api.model.AlternativePostalAddressDto
import org.eclipse.tractusx.orchestrator.api.model.PhysicalPostalAddressDto
import org.eclipse.tractusx.orchestrator.api.model.StreetDto
import java.time.LocalDateTime

/**
 * Contains complex test values that can be used as templates by the test classes
 * Test values here should have as many unique values as possible to reduce the probability of finding matching errors
 */
object TestValues {

    //Business Partner with two entries in every collection
    val businessPartner1 = BusinessPartnerDto(
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
        ownerBpnl = null,
        bpnL = "BPNLTEST",
        bpnS = "BPNSTEST",
        bpnA = "BPNATEST"
    )

    //Business Partner with single entry in every collection
    val businessPartner2 = BusinessPartnerDto(
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
        ownerBpnl = "BPNLTEST-2",
        bpnL = "BPNLTEST-2",
        bpnS = "BPNSTEST-2",
        bpnA = "BPNATEST-2"
    )

}