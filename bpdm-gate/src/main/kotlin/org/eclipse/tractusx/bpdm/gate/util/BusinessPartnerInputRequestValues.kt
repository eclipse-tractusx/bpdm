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

package org.eclipse.tractusx.bpdm.gate.util

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.common.util.OpenApiSchemaExample
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.AddressRepresentationInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.LegalEntityRepresentationInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SiteRepresentationInputDto
import java.time.LocalDateTime
import java.time.ZoneOffset

object BusinessPartnerInputRequestValues {
    private val now = LocalDateTime.now()
    private val state = BusinessPartnerStateDto(
        validFrom = now,
        validTo = now,
        type = BusinessStateType.ACTIVE
    )

    private val geoCoordinate = GeoCoordinateDto(
        longitude = 0.1,
        latitude = 0.1,
        altitude = 0.1
    )

    private val street = StreetDto(
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

    val businessPartnerInputRequest = BusinessPartnerInputRequest(
        externalId = "MB01",
        nameParts = listOf(""),
        identifiers = listOf(
            BusinessPartnerIdentifierDto(
                type = "EU_VAT_ID_DE",
                value = "LEGAL_ENTITY",
                issuingBody = "string"
            )
        ),
        states = listOf(state),
        roles = listOf(BusinessPartnerRole.SUPPLIER),
        isOwnCompanyData = true,
        legalEntity = LegalEntityRepresentationInputDto(
            legalEntityBpn = null,
            legalName = "MBRDI",
            shortName = "",
            legalForm = "1EQC",
            states = listOf(state)
        ),
        site = SiteRepresentationInputDto(),
        address = AddressRepresentationInputDto(
            addressBpn = null,
            name = "string",
            addressType = null,
            physicalPostalAddress = PhysicalPostalAddressDto(
                geographicCoordinates = geoCoordinate,
                country = CountryCode.UNDEFINED,
                administrativeAreaLevel1 = "string",
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
            alternativePostalAddress = AlternativePostalAddressDto(
                geographicCoordinates = geoCoordinate,
                country = CountryCode.UNDEFINED,
                administrativeAreaLevel1 = "string",
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
            BusinessPartnerScriptVariantDto(
                scriptCode = "string",
                nameParts = listOf("string"),
                legalEntity = LegalEntityScriptVariantDto(
                    legalName = "string",
                    shortName = "string"
                ),
                site = SiteScriptVariantDto(name = "string"),
                address = AddressScriptVariantDto(
                    name = "string",
                    physicalAddress = PhysicalAddressScriptVariantDto(
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
                    alternativeAddress = AlternativeAddressScriptVariantDto(
                        postalCode = "string",
                        city = "string",
                        deliveryServiceQualifier = "string",
                        deliveryServiceNumber = "string"
                    )
                )
            )
        )
    )

    val businessPartnerInputRequestExample = OpenApiSchemaExample(
        schemaName = "BusinessPartnerInputRequest",
        example = businessPartnerInputRequest
    )
}