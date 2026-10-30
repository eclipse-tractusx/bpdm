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

package org.eclipse.tractusx.bpdm.gate.config

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.common.util.OpenApiSchemaExample
import org.eclipse.tractusx.bpdm.gate.api.v7.model.*
import org.eclipse.tractusx.bpdm.gate.api.v7.model.request.BusinessPartnerInputV7Request
import org.eclipse.tractusx.bpdm.gate.api.v7.model.response.AddressRepresentationInputV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.response.LegalEntityRepresentationInputV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.response.SiteRepresentationInputV7Dto
import java.time.LocalDateTime
import java.time.ZoneOffset

object BusinessPartnerInputRequestValues {
    private val now = LocalDateTime.now()
    private val state = BusinessPartnerStateV7Dto(
        validFrom = now,
        validTo = now,
        type = BusinessStateType.ACTIVE
    )

    private val geoCoordinate = GeoCoordinateDto(
        longitude = 0.1,
        latitude = 0.1,
        altitude = 0.1
    )

    private val street = StreetV7Dto(
        namePrefix = "string",
        additionalNamePrefix = "string",
        name = "string",
        nameSuffix = "string",
        additionalNameSuffix = "string",
        houseNumber = "string",
        houseNumberSupplement = "string",
        milestone = "string",
        direction = "string"
    )

    private val scriptVariantStreet = StreetScriptVariantV7Dto(
        namePrefix = "string",
        additionalNamePrefix = "string",
        name = "string",
        nameSuffix = "string",
        additionalNameSuffix = "string",
        direction = "string"
    )

    val businessPartnerInputRequest = BusinessPartnerInputV7Request(
        externalId = "MB01",
        nameParts = listOf(""),
        identifiers = listOf(
            BusinessPartnerIdentifierV7Dto(
                type = "EU_VAT_ID_DE",
                value = "LEGAL_ENTITY",
                issuingBody = "string"
            )
        ),
        states = listOf(state),
        roles = listOf(BusinessPartnerRole.SUPPLIER),
        isOwnCompanyData = true,
        legalEntity = LegalEntityRepresentationInputV7Dto(
            legalEntityBpn = null,
            legalName = "MBRDI",
            shortName = "",
            legalForm = "1EQC",
            states = listOf(state)
        ),
        site = SiteRepresentationInputV7Dto(),
        address = AddressRepresentationInputV7Dto(
            addressBpn = null,
            name = "string",
            addressType = null,
            physicalPostalAddress = PhysicalPostalAddressV7Dto(
                geographicCoordinates = geoCoordinate,
                country = CountryCode.UNDEFINED.alpha2,
                administrativeAreaLevel1 = "AD-02",
                administrativeAreaLevel2 = "string",
                administrativeAreaLevel3 = "string",
                postalCode = "string",
                city = "string",
                district = "string",
                street = street,
                companyPostalCode = "string",
                industrialZone = "string",
                building = "string",
                floor = "string",
                door = "string",
                taxJurisdictionCode = "string"
            ),
            alternativePostalAddress = AlternativePostalAddressV7Dto(
                geographicCoordinates = geoCoordinate,
                country = CountryCode.UNDEFINED.alpha2,
                administrativeAreaLevel1 = "AD-02",
                postalCode = "string",
                city = "string",
                deliveryServiceType = DeliveryServiceType.PO_BOX,
                deliveryServiceQualifier = "string",
                deliveryServiceNumber = "string"
            ),
            states = listOf(state)
        ),
        externalSequenceTimestamp = now.toInstant(ZoneOffset.UTC),
        scriptVariants = listOf(
            BusinessPartnerScriptVariantV7Dto(
                scriptCode = "string",
                nameParts = listOf("string"),
                legalEntity = LegalEntityScriptVariantV7Dto(
                    legalName = "string",
                    shortName = "string"
                ),
                site = SiteScriptVariantV7Dto(name = "string"),
                address = AddressScriptVariantV7Dto(
                    name = "string",
                    physicalAddress = PhysicalAddressScriptVariantV7Dto(
                        city = "string",
                        district = "string",
                        street = scriptVariantStreet,
                        industrialZone = "string",
                        building = "string",
                        floor = "string",
                        door = "string"
                    ),
                    alternativeAddress = AlternativeAddressScriptVariantV7Dto(
                        city = "string"
                    )
                )
            )
        )
    )

    val businessPartnerInputRequestExample = OpenApiSchemaExample(
        schemaName = "BusinessPartnerInputV7Request",
        example = businessPartnerInputRequest
    )
}