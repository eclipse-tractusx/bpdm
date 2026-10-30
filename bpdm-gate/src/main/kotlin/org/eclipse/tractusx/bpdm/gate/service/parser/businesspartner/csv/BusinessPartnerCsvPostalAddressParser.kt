/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.bpdm.gate.service.parser.businesspartner.csv

import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.combine
import org.eclipse.tractusx.bpdm.common.model.zipParseResults
import org.eclipse.tractusx.bpdm.gate.model.raw.value.BusinessPartnerValuesRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.value.StreetRaw
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerCsvHeader
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerCsvRow
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerCsvRowError
import org.springframework.stereotype.Service

/**
 * Turns the postal address cells of a business partner CSV row into its physical and alternative postal address.
 */
@Service
class BusinessPartnerCsvPostalAddressParser(
    private val coordinatesParser: CsvCoordinatesParser,
    private val enumParser: CsvEnumParser
) {

    /**
     * Returns the physical postal address [row] describes, or fails with every cell that holds no valid value.
     */
    fun parsePhysical(row: BusinessPartnerCsvRow): ParseResult<BusinessPartnerValuesRaw.PhysicalPostalAddress, BusinessPartnerCsvRowError> =
        with(row) {
            coordinatesParser.parse(
                CsvCell(BusinessPartnerCsvHeader.PHYSICAL_POSTAL_ADDRESS_LONGITUDE, physicalPostalAddressLongitude),
                CsvCell(BusinessPartnerCsvHeader.PHYSICAL_POSTAL_ADDRESS_LATITUDE, physicalPostalAddressLatitude),
                CsvCell(BusinessPartnerCsvHeader.PHYSICAL_POSTAL_ADDRESS_ALTITUDE, physicalPostalAddressAltitude)
            ).combine(emptyList()) { coordinates ->
                BusinessPartnerValuesRaw.PhysicalPostalAddress(
                    geographicCoordinates = coordinates,
                    country = cellText(physicalPostalAddressCountry),
                    administrativeAreaLevel1 = cellText(physicalPostalAddressAdminArea1),
                    administrativeAreaLevel2 = cellText(physicalPostalAddressAdminArea2),
                    administrativeAreaLevel3 = cellText(physicalPostalAddressAdminArea3),
                    postalCode = cellText(physicalPostalAddressPostalCode),
                    city = cellText(physicalPostalAddressCity),
                    district = cellText(physicalPostalAddressDistrict),
                    street = StreetRaw(
                        namePrefix = cellText(physicalPostalAddressStreetNamePrefix),
                        additionalNamePrefix = cellText(physicalPostalAddressStreetAdditionalNamePrefix),
                        name = cellText(physicalPostalAddressStreetName),
                        nameSuffix = cellText(physicalPostalAddressStreetNameSuffix),
                        additionalNameSuffix = cellText(physicalPostalAddressStreetAdditionalNameSuffix),
                        houseNumber = cellText(physicalPostalAddressStreetHouseNumber),
                        houseNumberSupplement = cellText(physicalPostalAddressStreetHouseNumberSupplement),
                        milestone = cellText(physicalPostalAddressStreetMilestone),
                        direction = cellText(physicalPostalAddressStreetDirection)
                    ),
                    companyPostalCode = cellText(physicalPostalAddressCompanyPostalCode),
                    industrialZone = cellText(physicalPostalAddressIndustrialZone),
                    building = cellText(physicalPostalAddressBuilding),
                    floor = cellText(physicalPostalAddressFloor),
                    door = cellText(physicalPostalAddressDoor),
                    taxJurisdictionCode = null
                )
            }
        }

    /**
     * Returns the alternative postal address [row] describes, or fails with every cell that holds no valid value.
     */
    fun parseAlternative(row: BusinessPartnerCsvRow): ParseResult<BusinessPartnerValuesRaw.AlternativePostalAddress, BusinessPartnerCsvRowError> =
        with(row) {
            zipParseResults(
                coordinatesParser.parse(
                    CsvCell(BusinessPartnerCsvHeader.ALTERNATIVE_POSTAL_ADDRESS_LONGITUDE, alternativePostalAddressLongitude),
                    CsvCell(BusinessPartnerCsvHeader.ALTERNATIVE_POSTAL_ADDRESS_LATITUDE, alternativePostalAddressLatitude),
                    CsvCell(BusinessPartnerCsvHeader.ALTERNATIVE_POSTAL_ADDRESS_ALTITUDE, alternativePostalAddressAltitude)
                ),
                enumParser.parse(
                    CsvCell(BusinessPartnerCsvHeader.ALTERNATIVE_POSTAL_ADDRESS_DELIVERY_SERVICE_TYPE, alternativePostalAddressDeliveryServiceType),
                    DeliveryServiceType::valueOf
                )
            ) { coordinates, deliveryServiceType ->
                BusinessPartnerValuesRaw.AlternativePostalAddress(
                    geographicCoordinates = coordinates,
                    country = cellText(alternativePostalAddressCountry),
                    administrativeAreaLevel1 = cellText(alternativePostalAddressAdminArea1),
                    postalCode = cellText(alternativePostalAddressPostalCode),
                    city = cellText(alternativePostalAddressCity),
                    deliveryServiceType = deliveryServiceType,
                    deliveryServiceQualifier = cellText(alternativePostalAddressDeliveryServiceQualifier),
                    deliveryServiceNumber = cellText(alternativePostalAddressDeliveryServiceNumber)
                )
            }
        }
}
