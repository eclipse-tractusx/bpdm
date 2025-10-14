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
import com.opencsv.bean.CsvToBeanBuilder
import jakarta.validation.Validation
import jakarta.validation.Validator
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.AddressRepresentationInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.LegalEntityRepresentationInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SiteRepresentationInputDto
import org.eclipse.tractusx.bpdm.gate.exception.BpdmInvalidPartnerUploadException
import org.eclipse.tractusx.bpdm.gate.model.PartnerUploadFileHeader
import org.eclipse.tractusx.bpdm.gate.model.PartnerUploadFileRow
import org.springframework.web.multipart.MultipartFile
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object PartnerFileUtil {

    private val logger = KotlinLogging.logger { }

    fun parseCsv(file: MultipartFile): List<PartnerUploadFileRow> {
        val reader = InputStreamReader(file.inputStream)
        return CsvToBeanBuilder<PartnerUploadFileRow>(reader)
            .withType(PartnerUploadFileRow::class.java)
            .withIgnoreLeadingWhiteSpace(true)
            .build()
            .parse()
    }

    /**
     * Validates and maps a list of CSV data rows to a list of BusinessPartnerInputRequest objects.
     *
     * This function performs the following operations:
     * 1. Validates each row of CSV data using a provided validator.
     * 2. Converts each valid row into a BusinessPartnerInputRequest object.
     * 3. Collects any validation errors and throws an exception if any errors are found.
     *
     * @param csvData A list of PartnerUploadFileRow objects representing the rows of the CSV file to be processed.
     * @return A list of BusinessPartnerInputRequest objects derived from the valid CSV rows.
     * @throws BpdmInvalidPartnerUploadException if any validation errors are encountered during processing.
     */
    fun validateAndMapToBusinessPartnerInputRequests(csvData: List<PartnerUploadFileRow>, tenantBpnl: String?, legalName: String): List<BusinessPartnerInputRequest> {
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        val validator: Validator = Validation.buildDefaultValidatorFactory().validator
        val errors = mutableListOf<String>()
        val externalIdSet = mutableSetOf<String?>()

        val requests = csvData.mapIndexedNotNull { index, row ->
            try {
                validateRow(row, index, validator, errors, externalIdSet)
                BusinessPartnerInputRequest(
                    externalId = row.externalId.orEmpty(),
                    nameParts = emptyList(),
                    identifiers = listOfNotNull(
                        row.toIdentifierDto(1), row.toIdentifierDto(2), row.toIdentifierDto(3)
                    ),
                    states = emptyList(),
                    roles = emptyList(),
                    isOwnCompanyData = true,
                    legalEntity = LegalEntityRepresentationInputDto(
                        legalEntityBpn = tenantBpnl?.takeIf { it.isNotEmpty() },
                        legalName = legalName
                    ),
                    site = row.toSiteRepresentationInputDto(formatter, errors, index, row.externalId.orEmpty()),
                    address = row.toAddressRepresentationInputDto(formatter, errors, index, row.externalId.orEmpty()),
                    externalSequenceTimestamp = null
                )
            } catch (e: Exception) {
                errors.add("Row - ${index + 2}, External ID - ${row.externalId.orEmpty()} has error: ${e.message}")
                null
            }
        }

        if (errors.isNotEmpty()) {
            throw BpdmInvalidPartnerUploadException(errors)
        }

        return requests
    }

    private fun validateRow(
        row: PartnerUploadFileRow,
        index: Int,
        validator: Validator,
        errors: MutableList<String>,
        externalIdSet: MutableSet<String?>
    ) {
        val violations = validator.validate(row)
        logger.debug { "Validating Row ${index + 2}: $row" }
        if (violations.isNotEmpty()) {
            val violationMessages = violations.joinToString("; ") { it.message }
            errors.add("Row - ${index + 2}, External ID - ${row.externalId.orEmpty()} has error: $violationMessages")
        }

        if (row.externalId.isNullOrBlank()) {
            errors.add("Row - ${index + 2} has error: Column 'externalId' is missing and can not be empty or blank.")
        } else if (!externalIdSet.add(row.externalId)) {
            errors.add("Row - ${index + 2}, External ID - ${row.externalId} has error: Column 'externalId' already exists.")
        }
    }

    private fun PartnerUploadFileRow.toIdentifierDto(index: Int): BusinessPartnerIdentifierDto? {
        val type = when (index) {
            1 -> identifiersType1
            2 -> identifiersType2
            3 -> identifiersType3
            else -> null
        }
        val value = when (index) {
            1 -> identifiersValue1
            2 -> identifiersValue2
            3 -> identifiersValue3
            else -> null
        }
        val issuingBody = when (index) {
            1 -> identifiersIssuingBody1
            2 -> identifiersIssuingBody2
            3 -> identifiersIssuingBody3
            else -> null
        }?.takeIf { it.isNotEmpty() } // Convert empty string to null

        return if (type.isNullOrEmpty() || value.isNullOrEmpty()) null
        else BusinessPartnerIdentifierDto(type, value, issuingBody)
    }

    private fun PartnerUploadFileRow.toSiteRepresentationInputDto(
        formatter: DateTimeFormatter,
        errors: MutableList<String>,
        rowIndex: Int,
        externalId: String
    ): SiteRepresentationInputDto {
        return SiteRepresentationInputDto(
            siteBpn = siteBpn?.takeIf { it.isNotEmpty() },
            name = siteName?.takeIf { it.isNotEmpty() },
            states = listOfNotNull(
                if (!siteStatesValidFrom.isNullOrEmpty() && !siteStatesValidTo.isNullOrEmpty() && !siteStatesType.isNullOrEmpty())
                    BusinessPartnerStateDto(
                        validFrom = parseDate(siteStatesValidFrom, formatter, errors, rowIndex + 2, externalId, PartnerUploadFileHeader.SITE_STATES_VALID_FROM),
                        validTo = parseDate(siteStatesValidTo, formatter, errors, rowIndex + 2, externalId, PartnerUploadFileHeader.SITE_STATES_VALID_TO),
                        type = parseEnum(siteStatesType, BusinessStateType::valueOf, errors, rowIndex + 2, externalId, PartnerUploadFileHeader.SITE_STATES_TYPE)
                    ) else null
            )
        )
    }

    private fun PartnerUploadFileRow.toAddressRepresentationInputDto(
        formatter: DateTimeFormatter,
        errors: MutableList<String>,
        rowIndex: Int,
        externalId: String
    ): AddressRepresentationInputDto {
        val validAddressTypes = setOf(AddressType.SiteMainAddress, AddressType.AdditionalAddress)

        return AddressRepresentationInputDto(
            addressBpn = addressBpn?.takeIf { it.isNotEmpty() },
            name = addressName?.takeIf { it.isNotEmpty() },
            addressType = addressType?.let {
                val parsedAddressType = parseEnum(it, AddressType::valueOf, errors, rowIndex + 2, externalId, PartnerUploadFileHeader.ADDRESS_TYPE)
                if (parsedAddressType !in validAddressTypes) {
                    errors.add("Row - ${rowIndex + 2}, External ID - $externalId has error: Invalid address type. Only 'SiteMainAddress' and 'AdditionalAddress' are allowed.")
                }
                parsedAddressType
            },
            physicalPostalAddress = PhysicalPostalAddressDto(
                geographicCoordinates = GeoCoordinateDto(
                    longitude = physicalPostalAddressLongitude?.toDoubleOrNull() ?: 0.0,
                    latitude = physicalPostalAddressLatitude?.toDoubleOrNull() ?: 0.0,
                    altitude = physicalPostalAddressAltitude?.toDoubleOrNull() ?: 0.0
                ),
                country = physicalPostalAddressCountry?.let { parseEnum(it, CountryCode::valueOf, errors, rowIndex + 2, externalId, PartnerUploadFileHeader.PHYSICAL_POSTAL_ADDRESS_COUNTRY) },
                administrativeAreaLevel1 = physicalPostalAddressAdminArea1?.takeIf { it.isNotEmpty() },
                administrativeAreaLevel2 = physicalPostalAddressAdminArea2?.takeIf { it.isNotEmpty() },
                administrativeAreaLevel3 = physicalPostalAddressAdminArea3?.takeIf { it.isNotEmpty() },
                postalCode = physicalPostalAddressPostalCode?.takeIf { it.isNotEmpty() },
                city = physicalPostalAddressCity?.takeIf { it.isNotEmpty() },
                district = physicalPostalAddressDistrict?.takeIf { it.isNotEmpty() },
                street = StreetDto(
                    namePrefix = physicalPostalAddressStreetAdditionalNamePrefix?.takeIf { it.isNotEmpty() },
                    additionalNamePrefix = physicalPostalAddressStreetAdditionalNameSuffix?.takeIf { it.isNotEmpty() },
                    name = physicalPostalAddressStreetName?.takeIf { it.isNotEmpty() },
                    nameSuffix = physicalPostalAddressStreetNameSuffix?.takeIf { it.isNotEmpty() },
                    additionalNameSuffix = physicalPostalAddressStreetAdditionalNameSuffix?.takeIf { it.isNotEmpty() },
                    houseNumber = physicalPostalAddressStreetHouseNumber,
                    houseNumberSupplement = physicalPostalAddressStreetHouseNumberSupplement?.takeIf { it.isNotEmpty() },
                    milestone = physicalPostalAddressStreetMilestone?.takeIf { it.isNotEmpty() },
                    direction = physicalPostalAddressStreetDirection?.takeIf { it.isNotEmpty() }
                ),
                companyPostalCode = physicalPostalAddressCompanyPostalCode?.takeIf { it.isNotEmpty() },
                industrialZone = physicalPostalAddressIndustrialZone?.takeIf { it.isNotEmpty() },
                building = physicalPostalAddressBuilding?.takeIf { it.isNotEmpty() },
                floor = physicalPostalAddressFloor?.takeIf { it.isNotEmpty() },
                door = physicalPostalAddressDoor?.takeIf { it.isNotEmpty() }
            ),
            alternativePostalAddress = AlternativePostalAddressDto(
                geographicCoordinates = GeoCoordinateDto(
                    longitude = alternativePostalAddressLongitude?.toDoubleOrNull() ?: 0.0,
                    latitude = alternativePostalAddressLatitude?.toDoubleOrNull() ?: 0.0,
                    altitude = alternativePostalAddressAltitude?.toDoubleOrNull() ?: 0.0
                ),
                country = alternativePostalAddressCountry?.takeIf { it.isNotBlank() }?.let { parseEnum(it, CountryCode::valueOf, errors, rowIndex + 2, externalId, PartnerUploadFileHeader.ALTERNATIVE_POSTAL_ADDRESS_COUNTRY) },
                administrativeAreaLevel1 = alternativePostalAddressAdminArea1?.takeIf { it.isNotEmpty() },
                postalCode = alternativePostalAddressPostalCode?.takeIf { it.isNotEmpty() },
                city = alternativePostalAddressCity?.takeIf { it.isNotEmpty() },
                deliveryServiceType = alternativePostalAddressDeliveryServiceType?.takeIf { it.isNotBlank() }?.let {
                    parseEnum(
                        it,
                        DeliveryServiceType::valueOf,
                        errors,
                        rowIndex + 2,
                        externalId,
                        PartnerUploadFileHeader.ALTERNATIVE_POSTAL_ADDRESS_DELIVERY_SERVICE_TYPE
                    )
                },
                deliveryServiceQualifier = alternativePostalAddressDeliveryServiceQualifier?.takeIf { it.isNotEmpty() },
                deliveryServiceNumber = alternativePostalAddressDeliveryServiceNumber?.takeIf { it.isNotEmpty() }
            ),
            states = listOfNotNull(
                if (!addressStatesValidFrom.isNullOrEmpty() && !addressStatesValidTo.isNullOrEmpty() && !addressStatesType.isNullOrEmpty())
                    BusinessPartnerStateDto(
                        validFrom = parseDate(addressStatesValidFrom, formatter, errors, rowIndex + 2, externalId, PartnerUploadFileHeader.ADDRESS_STATES_VALID_FROM),
                        validTo = parseDate(addressStatesValidTo, formatter, errors, rowIndex + 2, externalId, PartnerUploadFileHeader.ADDRESS_STATES_VALID_TO),
                        type = parseEnum(addressStatesType, BusinessStateType::valueOf, errors, rowIndex + 2, externalId, PartnerUploadFileHeader.ADDRESS_STATES_TYPE)
                    ) else null
            )
        )
    }

    private fun parseDate(dateString: String?, formatter: DateTimeFormatter, errors: MutableList<String>, rowIndex: Int, externalId: String, fieldName: String): LocalDateTime? {
        return try {
            dateString?.let { LocalDateTime.parse(it, formatter) }
        } catch (e: DateTimeParseException) {
            errors.add("Row - $rowIndex, External ID - $externalId has error: Invalid date format in field '$fieldName'.")
            null
        }
    }

    private fun <T : Enum<T>> parseEnum(value: String?, enumValueOf: (String) -> T, errors: MutableList<String>, rowIndex: Int, externalId: String, fieldName: String): T? {
        return try {
            value?.let { enumValueOf(it) }
        } catch (e: IllegalArgumentException) {
            errors.add("Row - $rowIndex, External ID - $externalId has error: Invalid enum value in field '$fieldName'.")
            null
        }
    }

}