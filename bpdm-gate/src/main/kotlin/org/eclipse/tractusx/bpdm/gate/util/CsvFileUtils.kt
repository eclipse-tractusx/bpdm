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

package org.eclipse.tractusx.bpdm.gate.util

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.AddressRepresentationInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.LegalEntityRepresentationInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SiteRepresentationInputDto
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object CsvFileUtils {

    fun parseCsv(file: MultipartFile): List<Map<String, String>> {
        val csvData: MutableList<Map<String, String>> = mutableListOf()
        val reader = BufferedReader(InputStreamReader(file.inputStream))

        // Read the CSV file line by line
        var line: String?
        var isFirstLine = true
        var headers: List<String> = emptyList()
        while (reader.readLine().also { line = it } != null) {
            if (isFirstLine) {
                headers = line!!.split(",").map { it.trim() }
                isFirstLine = false
            } else {
                val rowValues = line!!.split(",").map { it.trim() }
                val rowData: MutableMap<String, String> = mutableMapOf()
                for (i in headers.indices) {
                    rowData[headers[i]] = rowValues.getOrElse(i) { "" }
                }
                csvData.add(rowData)
            }
        }
        reader.close()
        return csvData
    }

    fun mapToBusinessPartnerRequests(csvData: List<Map<String, String>>): List<BusinessPartnerInputRequest> {

        val formatter = DateTimeFormatter.ISO_DATE_TIME

        return csvData.map { row ->
            BusinessPartnerInputRequest(
                externalId = row["externalId"] ?: "",
                nameParts = listOfNotNull(row["nameParts1"], row["nameParts2"], row["nameParts3"], row["nameParts4"]),
                identifiers = listOf(
                    BusinessPartnerIdentifierDto(
                    type = row["identifiers.type"] ?: "",
                    value = row["identifiers.value"] ?: "",
                    issuingBody = row["identifiers.issuingBody"] ?: ""
                )
                ),
                states = listOf(BusinessPartnerStateDto(
                    validFrom = row["states.validFrom"]?.let { LocalDateTime.parse(it, formatter) },
                    validTo = row["states.validTo"]?.let { LocalDateTime.parse(it, formatter) },
                    type = row["states.type"]?.let { BusinessStateType.valueOf(it) }
                )),
                roles = listOfNotNull(row["roles"]?.let { BusinessPartnerRole.valueOf(it) }),
                isOwnCompanyData = row["isOwnCompanyData"]?.toBoolean() ?: false,
                legalEntity = LegalEntityRepresentationInputDto(
                    legalEntityBpn = row["legalEntity.legalEntityBpn"] ?: "",
                    legalName = row["legalEntity.legalName"] ?: "",
                    shortName = row["legalEntity.shortName"] ?: "",
                    legalForm = row["legalEntity.legalForm"] ?: "",
                    states = listOf(BusinessPartnerStateDto(
                        validFrom = row["states.validFrom"]?.let { LocalDateTime.parse(it, formatter) },
                        validTo = row["states.validTo"]?.let { LocalDateTime.parse(it, formatter) },
                        type = row["states.type"]?.let { BusinessStateType.valueOf(it) }
                    ))
                ),
                site = SiteRepresentationInputDto(
                    siteBpn = row["site.siteBpn"] ?: "",
                    name = row["site.name"] ?: "",
                    states = listOf(BusinessPartnerStateDto(
                        validFrom = row["states.validFrom"]?.let { LocalDateTime.parse(it, formatter) },
                        validTo = row["states.validTo"]?.let { LocalDateTime.parse(it, formatter) },
                        type = row["states.type"]?.let { BusinessStateType.valueOf(it) }
                    ))
                ),
                address = AddressRepresentationInputDto(
                    addressBpn = row["address.addressBpn"] ?: "",
                    name = row["address.name"] ?: "",
                    addressType = AddressType.valueOf(row["address.addressType"] ?: ""),
                    physicalPostalAddress = PhysicalPostalAddressDto(
                        geographicCoordinates = GeoCoordinateDto(
                            longitude = row["address.physicalPostalAddress.geographicCoordinates.longitude"]?.toFloatOrNull() ?: 0f,
                            latitude = row["address.physicalPostalAddress.geographicCoordinates.latitude"]?.toFloatOrNull() ?: 0f,
                            altitude = row["address.physicalPostalAddress.geographicCoordinates.altitude"]?.toFloatOrNull() ?: 0f
                        ),
                        country = row["address.physicalPostalAddress.country"]?.let { CountryCode.valueOf(it) },
                        administrativeAreaLevel1 = row["address.physicalPostalAddress.administrativeAreaLevel1"] ?: "",
                        administrativeAreaLevel2 = row["address.physicalPostalAddress.administrativeAreaLevel2"] ?: "",
                        administrativeAreaLevel3 = row["address.physicalPostalAddress.administrativeAreaLevel3"] ?: "",
                        postalCode = row["address.physicalPostalAddress.postalCode"] ?: "",
                        city = row["address.physicalPostalAddress.city"] ?: "",
                        district = row["address.physicalPostalAddress.district"] ?: "",
                        street = StreetDto(
                            namePrefix = row["address.physicalPostalAddress.street.namePrefix"] ?: "",
                            additionalNamePrefix = row["address.physicalPostalAddress.street.additionalNamePrefix"] ?: "",
                            name = row["address.physicalPostalAddress.street.name"] ?: "",
                            nameSuffix = row["address.physicalPostalAddress.street.nameSuffix"] ?: "",
                            additionalNameSuffix = row["address.physicalPostalAddress.street.additionalNameSuffix"] ?: "",
                            houseNumber = row["address.physicalPostalAddress.street.houseNumber"] ?: "",
                            houseNumberSupplement = row["address.physicalPostalAddress.street.houseNumberSupplement"] ?: "",
                            milestone = row["address.physicalPostalAddress.street.milestone"] ?: "",
                            direction = row["address.physicalPostalAddress.street.direction"] ?: ""
                        ),
                        companyPostalCode = row["address.physicalPostalAddress.companyPostalCode"] ?: "",
                        industrialZone = row["address.physicalPostalAddress.industrialZone"] ?: "",
                        building = row["address.physicalPostalAddress.building"] ?: "",
                        floor = row["address.physicalPostalAddress.floor"] ?: "",
                        door = row["address.physicalPostalAddress.door"] ?: ""
                    ),
                    alternativePostalAddress = AlternativePostalAddressDto(
                        geographicCoordinates = GeoCoordinateDto(
                            longitude = row["address.physicalPostalAddress.geographicCoordinates.longitude"]?.toFloatOrNull() ?: 0f,
                            latitude = row["address.physicalPostalAddress.geographicCoordinates.latitude"]?.toFloatOrNull() ?: 0f,
                            altitude = row["address.physicalPostalAddress.geographicCoordinates.altitude"]?.toFloatOrNull() ?: 0f
                        ),
                        country = row["address.physicalPostalAddress.country"]?.let { CountryCode.valueOf(it) },
                        administrativeAreaLevel1 = row["address.alternativePostalAddress.administrativeAreaLevel1"] ?: "",
                        postalCode = row["address.alternativePostalAddress.postalCode"] ?: "",
                        city = row["address.alternativePostalAddress.city"] ?: "",
                        deliveryServiceType = row["address.alternativePostalAddress.deliveryServiceType"]?.let { DeliveryServiceType.valueOf(it) },
                        deliveryServiceQualifier = row["address.alternativePostalAddress.deliveryServiceQualifier"] ?: "",
                        deliveryServiceNumber = row["address.alternativePostalAddress.deliveryServiceNumber"] ?: ""
                    ),
                    states = listOf(BusinessPartnerStateDto(
                        validFrom = row["states.validFrom"]?.let { LocalDateTime.parse(it, formatter) },
                        validTo = row["states.validTo"]?.let { LocalDateTime.parse(it, formatter) },
                        type = row["states.type"]?.let { BusinessStateType.valueOf(it) }
                    ))
                )
            )
        }
    }
}