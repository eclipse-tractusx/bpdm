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

package org.eclipse.tractusx.bpdm.gate.model

import com.opencsv.bean.CsvBindByName
import jakarta.validation.constraints.NotEmpty

data class PartnerUploadFileRow(

    @CsvBindByName(column = PartnerUploadFileHeader.EXTERNAL_ID)
    val externalId: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.IDENTIFIERS_TYPE_1)
    val identifiersType1: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.IDENTIFIERS_VALUE_1)
    val identifiersValue1: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.IDENTIFIERS_ISSUING_BODY_1)
    val identifiersIssuingBody1: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.IDENTIFIERS_TYPE_2)
    val identifiersType2: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.IDENTIFIERS_VALUE_2)
    val identifiersValue2: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.IDENTIFIERS_ISSUING_BODY_2)
    val identifiersIssuingBody2: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.IDENTIFIERS_TYPE_3)
    val identifiersType3: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.IDENTIFIERS_VALUE_3)
    val identifiersValue3: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.IDENTIFIERS_ISSUING_BODY_3)
    val identifiersIssuingBody3: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.SITE_BPN)
    val siteBpn: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.SITE_NAME)
    val siteName: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.SITE_STATES_VALID_FROM)
    val siteStatesValidFrom: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.SITE_STATES_VALID_TO)
    val siteStatesValidTo: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.SITE_STATES_TYPE)
    val siteStatesType: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.ADDRESS_BPN)
    val addressBpn: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.ADDRESS_NAME)
    val addressName: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.ADDRESS_TYPE)
    val addressType: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.PHYSICAL_POSTAL_ADDRESS_LONGITUDE)
    val physicalPostalAddressLongitude: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.PHYSICAL_POSTAL_ADDRESS_LATITUDE)
    val physicalPostalAddressLatitude: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.PHYSICAL_POSTAL_ADDRESS_ALTITUDE)
    val physicalPostalAddressAltitude: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.PHYSICAL_POSTAL_ADDRESS_COUNTRY)
    val physicalPostalAddressCountry: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.PHYSICAL_POSTAL_ADDRESS_ADMIN_AREA_1)
    val physicalPostalAddressAdminArea1: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.PHYSICAL_POSTAL_ADDRESS_ADMIN_AREA_2)
    val physicalPostalAddressAdminArea2: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.PHYSICAL_POSTAL_ADDRESS_ADMIN_AREA_3)
    val physicalPostalAddressAdminArea3: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.PHYSICAL_POSTAL_ADDRESS_POSTAL_CODE)
    val physicalPostalAddressPostalCode: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.PHYSICAL_POSTAL_ADDRESS_CITY)
    val physicalPostalAddressCity: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.PHYSICAL_POSTAL_ADDRESS_DISTRICT)
    val physicalPostalAddressDistrict: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.PHYSICAL_POSTAL_ADDRESS_STREET_NAME_PREFIX)
    val physicalPostalAddressStreetNamePrefix: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.PHYSICAL_POSTAL_ADDRESS_STREET_ADDITIONAL_NAME_PREFIX)
    val physicalPostalAddressStreetAdditionalNamePrefix: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.PHYSICAL_POSTAL_ADDRESS_STREET_NAME)
    val physicalPostalAddressStreetName: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.PHYSICAL_POSTAL_ADDRESS_STREET_NAME_SUFFIX)
    val physicalPostalAddressStreetNameSuffix: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.PHYSICAL_POSTAL_ADDRESS_STREET_ADDITIONAL_NAME_SUFFIX)
    val physicalPostalAddressStreetAdditionalNameSuffix: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.PHYSICAL_POSTAL_ADDRESS_STREET_HOUSE_NUMBER)
    val physicalPostalAddressStreetHouseNumber: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.PHYSICAL_POSTAL_ADDRESS_STREET_HOUSE_NUMBER_SUPPLEMENT)
    val physicalPostalAddressStreetHouseNumberSupplement: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.PHYSICAL_POSTAL_ADDRESS_STREET_MILESTONE)
    val physicalPostalAddressStreetMilestone: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.PHYSICAL_POSTAL_ADDRESS_STREET_DIRECTION)
    val physicalPostalAddressStreetDirection: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.PHYSICAL_POSTAL_ADDRESS_COMPANY_POSTAL_CODE)
    val physicalPostalAddressCompanyPostalCode: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.PHYSICAL_POSTAL_ADDRESS_INDUSTRIAL_ZONE)
    val physicalPostalAddressIndustrialZone: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.PHYSICAL_POSTAL_ADDRESS_BUILDING)
    val physicalPostalAddressBuilding: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.PHYSICAL_POSTAL_ADDRESS_FLOOR)
    val physicalPostalAddressFloor: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.PHYSICAL_POSTAL_ADDRESS_DOOR)
    val physicalPostalAddressDoor: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.ALTERNATIVE_POSTAL_ADDRESS_LONGITUDE)
    val alternativePostalAddressLongitude: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.ALTERNATIVE_POSTAL_ADDRESS_LATITUDE)
    val alternativePostalAddressLatitude: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.ALTERNATIVE_POSTAL_ADDRESS_ALTITUDE)
    val alternativePostalAddressAltitude: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.ALTERNATIVE_POSTAL_ADDRESS_COUNTRY)
    val alternativePostalAddressCountry: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.ALTERNATIVE_POSTAL_ADDRESS_ADMIN_AREA_1)
    val alternativePostalAddressAdminArea1: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.ALTERNATIVE_POSTAL_ADDRESS_POSTAL_CODE)
    val alternativePostalAddressPostalCode: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.ALTERNATIVE_POSTAL_ADDRESS_CITY)
    val alternativePostalAddressCity: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.ALTERNATIVE_POSTAL_ADDRESS_DELIVERY_SERVICE_TYPE)
    val alternativePostalAddressDeliveryServiceType: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.ALTERNATIVE_POSTAL_ADDRESS_DELIVERY_SERVICE_QUALIFIER)
    val alternativePostalAddressDeliveryServiceQualifier: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.ALTERNATIVE_POSTAL_ADDRESS_DELIVERY_SERVICE_NUMBER)
    val alternativePostalAddressDeliveryServiceNumber: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.ADDRESS_STATES_VALID_FROM)
    val addressStatesValidFrom: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.ADDRESS_STATES_VALID_TO)
    val addressStatesValidTo: String? = null,

    @CsvBindByName(column = PartnerUploadFileHeader.ADDRESS_STATES_TYPE)
    val addressStatesType: String? = null
)
