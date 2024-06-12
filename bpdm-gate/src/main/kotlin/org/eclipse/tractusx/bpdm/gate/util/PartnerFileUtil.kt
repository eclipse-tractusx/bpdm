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
import com.opencsv.bean.CsvToBeanBuilder
import jakarta.validation.Validation
import jakarta.validation.Validator
import mu.KotlinLogging
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
    fun validateAndMapToBusinessPartnerInputRequests(csvData: List<PartnerUploadFileRow>): List<BusinessPartnerInputRequest> {
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        val validator: Validator = Validation.buildDefaultValidatorFactory().validator
        val errors = mutableListOf<String>()
        val externalIdSet = mutableSetOf<String?>()

        val requests = csvData.mapIndexedNotNull { index, row ->
            try {
                validateRow(row, index, validator, errors, externalIdSet)
                BusinessPartnerInputRequest(
                    externalId = row.externalId.orEmpty(),
                    nameParts = listOfNotNull(row.nameParts1, row.nameParts2, row.nameParts3, row.nameParts4).filter { it.isNotEmpty() },
                    identifiers = listOfNotNull(
                        row.toIdentifierDto(1), row.toIdentifierDto(2), row.toIdentifierDto(3)
                    ),
                    states = listOfNotNull(
                        row.toStateDto(1, formatter, errors, index),
                        row.toStateDto(2, formatter, errors, index)
                    ),
                    roles = row.roles.toEnumList(BusinessPartnerRole::valueOf, errors, index + 1, PartnerUploadFileHeader.ROLES),
                    isOwnCompanyData = row.isOwnCompanyData?.toBoolean() ?: false,
                    legalEntity = row.toLegalEntityRepresentationInputDto(formatter, errors, index),
                    site = row.toSiteRepresentationInputDto(formatter, errors, index),
                    address = row.toAddressRepresentationInputDto(formatter, errors, index)
                )
            } catch (e: Exception) {
                errors.add("Row ${index + 1} has error: ${e.message}")
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
        logger.debug { "Validating row ${index + 1}: $row" }
        if (violations.isNotEmpty()) {
            val violationMessages = violations.joinToString("; ") { it.message }
            errors.add("Row ${index + 1} has error: $violationMessages")
        }

        if (row.externalId.isNullOrBlank()) {
            errors.add("Row ${index + 1} has error: Column 'externalId' is null or blank.")
        } else if (!externalIdSet.add(row.externalId)) {
            errors.add("Row ${index + 1} has error: Column 'externalId' already exists.")
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

    private fun PartnerUploadFileRow.toStateDto(
        index: Int,
        formatter: DateTimeFormatter,
        errors: MutableList<String>,
        rowIndex: Int
    ): BusinessPartnerStateDto? {
        val validFrom = when (index) {
            1 -> statesValidFrom1
            2 -> statesValidFrom2
            else -> null
        }
        val validTo = when (index) {
            1 -> statesValidTo1
            2 -> statesValidTo2
            else -> null
        }
        val type = when (index) {
            1 -> statesType1
            2 -> statesType2
            else -> null
        }
        if (validFrom.isNullOrEmpty() && validTo.isNullOrEmpty() && type.isNullOrEmpty()) return null
        return BusinessPartnerStateDto(
            validFrom = validFrom?.let { parseDate(it, formatter, errors, rowIndex + 1, "states$index.validFrom") },
            validTo = validTo?.let { parseDate(it, formatter, errors, rowIndex + 1, "states$index.validTo") },
            type = type?.let { parseEnum(it, BusinessStateType::valueOf, errors, rowIndex + 1, "states$index.type") }
        )
    }

    private fun PartnerUploadFileRow.toLegalEntityRepresentationInputDto(
        formatter: DateTimeFormatter,
        errors: MutableList<String>,
        rowIndex: Int
    ): LegalEntityRepresentationInputDto {
        return LegalEntityRepresentationInputDto(
            legalEntityBpn = legalEntityBpn?.takeIf { it.isNotEmpty() },
            legalName = legalEntityName?.takeIf { it.isNotEmpty() },
            shortName = legalEntityShortName?.takeIf { it.isNotEmpty() },
            legalForm = legalEntityLegalForm?.takeIf { it.isNotEmpty() },
            states = listOfNotNull(
                if (!legalEntityStatesValidFrom.isNullOrEmpty() && !legalEntityStatesValidTo.isNullOrEmpty() && !legalEntityStatesType.isNullOrEmpty())
                    BusinessPartnerStateDto(
                        validFrom = parseDate(legalEntityStatesValidFrom, formatter, errors, rowIndex + 1, PartnerUploadFileHeader.LEGAL_ENTITY_STATES_VALID_FROM),
                        validTo = parseDate(legalEntityStatesValidTo, formatter, errors, rowIndex + 1, PartnerUploadFileHeader.LEGAL_ENTITY_STATES_VALID_TO),
                        type = parseEnum(legalEntityStatesType, BusinessStateType::valueOf, errors, rowIndex + 1, PartnerUploadFileHeader.LEGAL_ENTITY_STATES_TYPE)
                    ) else null
            )
        )
    }

    private fun PartnerUploadFileRow.toSiteRepresentationInputDto(
        formatter: DateTimeFormatter,
        errors: MutableList<String>,
        rowIndex: Int
    ): SiteRepresentationInputDto {
        return SiteRepresentationInputDto(
            siteBpn = siteBpn?.takeIf { it.isNotEmpty() },
            name = siteName?.takeIf { it.isNotEmpty() },
            states = listOfNotNull(
                if (!siteStatesValidFrom.isNullOrEmpty() && !siteStatesValidTo.isNullOrEmpty() && !siteStatesType.isNullOrEmpty())
                    BusinessPartnerStateDto(
                        validFrom = parseDate(siteStatesValidFrom, formatter, errors, rowIndex + 1, PartnerUploadFileHeader.SITE_STATES_VALID_FROM),
                        validTo = parseDate(siteStatesValidTo, formatter, errors, rowIndex + 1, PartnerUploadFileHeader.SITE_STATES_VALID_TO),
                        type = parseEnum(siteStatesType, BusinessStateType::valueOf, errors, rowIndex + 1, PartnerUploadFileHeader.SITE_STATES_TYPE)
                    ) else null
            )
        )
    }

    private fun PartnerUploadFileRow.toAddressRepresentationInputDto(
        formatter: DateTimeFormatter,
        errors: MutableList<String>,
        rowIndex: Int
    ): AddressRepresentationInputDto {
        return AddressRepresentationInputDto(
            addressBpn = addressBpn?.takeIf { it.isNotEmpty() },
            name = addressName?.takeIf { it.isNotEmpty() },
            addressType = addressType?.let { parseEnum(it, AddressType::valueOf, errors, rowIndex + 1, PartnerUploadFileHeader.ADDRESS_TYPE) },
            physicalPostalAddress = PhysicalPostalAddressDto(
                geographicCoordinates = GeoCoordinateDto(
                    longitude = physicalPostalAddressLongitude?.toFloatOrNull() ?: 0f,
                    latitude = physicalPostalAddressLatitude?.toFloatOrNull() ?: 0f,
                    altitude = physicalPostalAddressAltitude?.toFloatOrNull() ?: 0f
                ),
                country = physicalPostalAddressCountry?.let { parseEnum(it, CountryCode::valueOf, errors, rowIndex + 1, PartnerUploadFileHeader.PHYSICAL_POSTAL_ADDRESS_COUNTRY) },
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
                    longitude = alternativePostalAddressLongitude?.toFloatOrNull() ?: 0f,
                    latitude = alternativePostalAddressLatitude?.toFloatOrNull() ?: 0f,
                    altitude = alternativePostalAddressAltitude?.toFloatOrNull() ?: 0f
                ),
                country = alternativePostalAddressCountry?.let { parseEnum(it, CountryCode::valueOf, errors, rowIndex + 1, PartnerUploadFileHeader.ALTERNATIVE_POSTAL_ADDRESS_COUNTRY) },
                administrativeAreaLevel1 = alternativePostalAddressAdminArea1?.takeIf { it.isNotEmpty() },
                postalCode = alternativePostalAddressPostalCode?.takeIf { it.isNotEmpty() },
                city = alternativePostalAddressCity?.takeIf { it.isNotEmpty() },
                deliveryServiceType = alternativePostalAddressDeliveryServiceType?.let {
                    parseEnum(
                        it,
                        DeliveryServiceType::valueOf,
                        errors,
                        rowIndex + 1,
                        PartnerUploadFileHeader.ALTERNATIVE_POSTAL_ADDRESS_DELIVERY_SERVICE_TYPE
                    )
                },
                deliveryServiceQualifier = alternativePostalAddressDeliveryServiceQualifier?.takeIf { it.isNotEmpty() },
                deliveryServiceNumber = alternativePostalAddressDeliveryServiceNumber?.takeIf { it.isNotEmpty() }
            ),
            states = listOfNotNull(
                if (!addressStatesValidFrom.isNullOrEmpty() && !addressStatesValidTo.isNullOrEmpty() && !addressStatesType.isNullOrEmpty())
                    BusinessPartnerStateDto(
                        validFrom = parseDate(addressStatesValidFrom, formatter, errors, rowIndex + 1, PartnerUploadFileHeader.ADDRESS_STATES_VALID_FROM),
                        validTo = parseDate(addressStatesValidTo, formatter, errors, rowIndex + 1, PartnerUploadFileHeader.ADDRESS_STATES_VALID_TO),
                        type = parseEnum(addressStatesType, BusinessStateType::valueOf, errors, rowIndex + 1, PartnerUploadFileHeader.ADDRESS_STATES_TYPE)
                    ) else null
            )
        )
    }

    private fun parseDate(dateString: String?, formatter: DateTimeFormatter, errors: MutableList<String>, rowIndex: Int, fieldName: String): LocalDateTime? {
        return try {
            dateString?.let { LocalDateTime.parse(it, formatter) }
        } catch (e: DateTimeParseException) {
            errors.add("Row $rowIndex has error: Invalid date format in field '$fieldName'.")
            null
        }
    }

    private fun <T : Enum<T>> parseEnum(value: String?, enumValueOf: (String) -> T, errors: MutableList<String>, rowIndex: Int, fieldName: String): T? {
        return try {
            value?.let { enumValueOf(it) }
        } catch (e: IllegalArgumentException) {
            errors.add("Row $rowIndex has error: Invalid enum value in field '$fieldName'.")
            null
        }
    }

    private fun <T : Enum<T>> String?.toEnumList(enumValueOf: (String) -> T, errors: MutableList<String>, rowIndex: Int, fieldName: String): List<T> {
        return try {
            this?.split(",")?.map { enumValueOf(it.trim()) } ?: emptyList()
        } catch (e: IllegalArgumentException) {
            errors.add("Row $rowIndex has error: Invalid enum value in field '$fieldName'.")
            emptyList()
        }
    }

}