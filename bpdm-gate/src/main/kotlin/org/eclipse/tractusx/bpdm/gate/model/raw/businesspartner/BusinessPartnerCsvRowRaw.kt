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

package org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner

import com.opencsv.bean.CsvBindByName

data class BusinessPartnerCsvRowRaw(

    @CsvBindByName(column = Columns.EXTERNAL_ID)
    val externalId: String? = null,

    @CsvBindByName(column = Columns.IDENTIFIERS_TYPE_1)
    val identifiersType1: String? = null,

    @CsvBindByName(column = Columns.IDENTIFIERS_VALUE_1)
    val identifiersValue1: String? = null,

    @CsvBindByName(column = Columns.IDENTIFIERS_ISSUING_BODY_1)
    val identifiersIssuingBody1: String? = null,

    @CsvBindByName(column = Columns.IDENTIFIERS_TYPE_2)
    val identifiersType2: String? = null,

    @CsvBindByName(column = Columns.IDENTIFIERS_VALUE_2)
    val identifiersValue2: String? = null,

    @CsvBindByName(column = Columns.IDENTIFIERS_ISSUING_BODY_2)
    val identifiersIssuingBody2: String? = null,

    @CsvBindByName(column = Columns.IDENTIFIERS_TYPE_3)
    val identifiersType3: String? = null,

    @CsvBindByName(column = Columns.IDENTIFIERS_VALUE_3)
    val identifiersValue3: String? = null,

    @CsvBindByName(column = Columns.IDENTIFIERS_ISSUING_BODY_3)
    val identifiersIssuingBody3: String? = null,

    @CsvBindByName(column = Columns.SITE_BPN)
    val siteBpn: String? = null,

    @CsvBindByName(column = Columns.SITE_NAME)
    val siteName: String? = null,

    @CsvBindByName(column = Columns.SITE_STATES_VALID_FROM)
    val siteStatesValidFrom: String? = null,

    @CsvBindByName(column = Columns.SITE_STATES_VALID_TO)
    val siteStatesValidTo: String? = null,

    @CsvBindByName(column = Columns.SITE_STATES_TYPE)
    val siteStatesType: String? = null,

    @CsvBindByName(column = Columns.ADDRESS_BPN)
    val addressBpn: String? = null,

    @CsvBindByName(column = Columns.ADDRESS_NAME)
    val addressName: String? = null,

    @CsvBindByName(column = Columns.ADDRESS_TYPE)
    val addressType: String? = null,

    @CsvBindByName(column = Columns.PHYSICAL_POSTAL_ADDRESS_LONGITUDE)
    val physicalPostalAddressLongitude: String? = null,

    @CsvBindByName(column = Columns.PHYSICAL_POSTAL_ADDRESS_LATITUDE)
    val physicalPostalAddressLatitude: String? = null,

    @CsvBindByName(column = Columns.PHYSICAL_POSTAL_ADDRESS_ALTITUDE)
    val physicalPostalAddressAltitude: String? = null,

    @CsvBindByName(column = Columns.PHYSICAL_POSTAL_ADDRESS_COUNTRY)
    val physicalPostalAddressCountry: String? = null,

    @CsvBindByName(column = Columns.PHYSICAL_POSTAL_ADDRESS_ADMIN_AREA_1)
    val physicalPostalAddressAdminArea1: String? = null,

    @CsvBindByName(column = Columns.PHYSICAL_POSTAL_ADDRESS_ADMIN_AREA_2)
    val physicalPostalAddressAdminArea2: String? = null,

    @CsvBindByName(column = Columns.PHYSICAL_POSTAL_ADDRESS_ADMIN_AREA_3)
    val physicalPostalAddressAdminArea3: String? = null,

    @CsvBindByName(column = Columns.PHYSICAL_POSTAL_ADDRESS_POSTAL_CODE)
    val physicalPostalAddressPostalCode: String? = null,

    @CsvBindByName(column = Columns.PHYSICAL_POSTAL_ADDRESS_CITY)
    val physicalPostalAddressCity: String? = null,

    @CsvBindByName(column = Columns.PHYSICAL_POSTAL_ADDRESS_DISTRICT)
    val physicalPostalAddressDistrict: String? = null,

    @CsvBindByName(column = Columns.PHYSICAL_POSTAL_ADDRESS_STREET_NAME_PREFIX)
    val physicalPostalAddressStreetNamePrefix: String? = null,

    @CsvBindByName(column = Columns.PHYSICAL_POSTAL_ADDRESS_STREET_ADDITIONAL_NAME_PREFIX)
    val physicalPostalAddressStreetAdditionalNamePrefix: String? = null,

    @CsvBindByName(column = Columns.PHYSICAL_POSTAL_ADDRESS_STREET_NAME)
    val physicalPostalAddressStreetName: String? = null,

    @CsvBindByName(column = Columns.PHYSICAL_POSTAL_ADDRESS_STREET_NAME_SUFFIX)
    val physicalPostalAddressStreetNameSuffix: String? = null,

    @CsvBindByName(column = Columns.PHYSICAL_POSTAL_ADDRESS_STREET_ADDITIONAL_NAME_SUFFIX)
    val physicalPostalAddressStreetAdditionalNameSuffix: String? = null,

    @CsvBindByName(column = Columns.PHYSICAL_POSTAL_ADDRESS_STREET_HOUSE_NUMBER)
    val physicalPostalAddressStreetHouseNumber: String? = null,

    @CsvBindByName(column = Columns.PHYSICAL_POSTAL_ADDRESS_STREET_HOUSE_NUMBER_SUPPLEMENT)
    val physicalPostalAddressStreetHouseNumberSupplement: String? = null,

    @CsvBindByName(column = Columns.PHYSICAL_POSTAL_ADDRESS_STREET_MILESTONE)
    val physicalPostalAddressStreetMilestone: String? = null,

    @CsvBindByName(column = Columns.PHYSICAL_POSTAL_ADDRESS_STREET_DIRECTION)
    val physicalPostalAddressStreetDirection: String? = null,

    @CsvBindByName(column = Columns.PHYSICAL_POSTAL_ADDRESS_COMPANY_POSTAL_CODE)
    val physicalPostalAddressCompanyPostalCode: String? = null,

    @CsvBindByName(column = Columns.PHYSICAL_POSTAL_ADDRESS_INDUSTRIAL_ZONE)
    val physicalPostalAddressIndustrialZone: String? = null,

    @CsvBindByName(column = Columns.PHYSICAL_POSTAL_ADDRESS_BUILDING)
    val physicalPostalAddressBuilding: String? = null,

    @CsvBindByName(column = Columns.PHYSICAL_POSTAL_ADDRESS_FLOOR)
    val physicalPostalAddressFloor: String? = null,

    @CsvBindByName(column = Columns.PHYSICAL_POSTAL_ADDRESS_DOOR)
    val physicalPostalAddressDoor: String? = null,

    @CsvBindByName(column = Columns.ALTERNATIVE_POSTAL_ADDRESS_LONGITUDE)
    val alternativePostalAddressLongitude: String? = null,

    @CsvBindByName(column = Columns.ALTERNATIVE_POSTAL_ADDRESS_LATITUDE)
    val alternativePostalAddressLatitude: String? = null,

    @CsvBindByName(column = Columns.ALTERNATIVE_POSTAL_ADDRESS_ALTITUDE)
    val alternativePostalAddressAltitude: String? = null,

    @CsvBindByName(column = Columns.ALTERNATIVE_POSTAL_ADDRESS_COUNTRY)
    val alternativePostalAddressCountry: String? = null,

    @CsvBindByName(column = Columns.ALTERNATIVE_POSTAL_ADDRESS_ADMIN_AREA_1)
    val alternativePostalAddressAdminArea1: String? = null,

    @CsvBindByName(column = Columns.ALTERNATIVE_POSTAL_ADDRESS_POSTAL_CODE)
    val alternativePostalAddressPostalCode: String? = null,

    @CsvBindByName(column = Columns.ALTERNATIVE_POSTAL_ADDRESS_CITY)
    val alternativePostalAddressCity: String? = null,

    @CsvBindByName(column = Columns.ALTERNATIVE_POSTAL_ADDRESS_DELIVERY_SERVICE_TYPE)
    val alternativePostalAddressDeliveryServiceType: String? = null,

    @CsvBindByName(column = Columns.ALTERNATIVE_POSTAL_ADDRESS_DELIVERY_SERVICE_QUALIFIER)
    val alternativePostalAddressDeliveryServiceQualifier: String? = null,

    @CsvBindByName(column = Columns.ALTERNATIVE_POSTAL_ADDRESS_DELIVERY_SERVICE_NUMBER)
    val alternativePostalAddressDeliveryServiceNumber: String? = null,

    @CsvBindByName(column = Columns.ADDRESS_STATES_VALID_FROM)
    val addressStatesValidFrom: String? = null,

    @CsvBindByName(column = Columns.ADDRESS_STATES_VALID_TO)
    val addressStatesValidTo: String? = null,

    @CsvBindByName(column = Columns.ADDRESS_STATES_TYPE)
    val addressStatesType: String? = null
) {

    object Columns {
        const val EXTERNAL_ID = "externalId"
        const val IDENTIFIERS_TYPE_1 = "identifiers1.type"
        const val IDENTIFIERS_VALUE_1 = "identifiers1.value"
        const val IDENTIFIERS_ISSUING_BODY_1 = "identifiers1.issuingBody"
        const val IDENTIFIERS_TYPE_2 = "identifiers2.type"
        const val IDENTIFIERS_VALUE_2 = "identifiers2.value"
        const val IDENTIFIERS_ISSUING_BODY_2 = "identifiers2.issuingBody"
        const val IDENTIFIERS_TYPE_3 = "identifiers3.type"
        const val IDENTIFIERS_VALUE_3 = "identifiers3.value"
        const val IDENTIFIERS_ISSUING_BODY_3 = "identifiers3.issuingBody"
        const val SITE_BPN = "site.siteBpn"
        const val SITE_NAME = "site.name"
        const val SITE_STATES_VALID_FROM = "site.states.validFrom"
        const val SITE_STATES_VALID_TO = "site.states.validTo"
        const val SITE_STATES_TYPE = "site.states.type"
        const val ADDRESS_BPN = "address.addressBpn"
        const val ADDRESS_NAME = "address.name"
        const val ADDRESS_TYPE = "address.addressType"
        const val PHYSICAL_POSTAL_ADDRESS_LONGITUDE = "address.physicalPostalAddress.geographicCoordinates.longitude"
        const val PHYSICAL_POSTAL_ADDRESS_LATITUDE = "address.physicalPostalAddress.geographicCoordinates.latitude"
        const val PHYSICAL_POSTAL_ADDRESS_ALTITUDE = "address.physicalPostalAddress.geographicCoordinates.altitude"
        const val PHYSICAL_POSTAL_ADDRESS_COUNTRY = "address.physicalPostalAddress.country"
        const val PHYSICAL_POSTAL_ADDRESS_ADMIN_AREA_1 = "address.physicalPostalAddress.administrativeAreaLevel1"
        const val PHYSICAL_POSTAL_ADDRESS_ADMIN_AREA_2 = "address.physicalPostalAddress.administrativeAreaLevel2"
        const val PHYSICAL_POSTAL_ADDRESS_ADMIN_AREA_3 = "address.physicalPostalAddress.administrativeAreaLevel3"
        const val PHYSICAL_POSTAL_ADDRESS_POSTAL_CODE = "address.physicalPostalAddress.postalCode"
        const val PHYSICAL_POSTAL_ADDRESS_CITY = "address.physicalPostalAddress.city"
        const val PHYSICAL_POSTAL_ADDRESS_DISTRICT = "address.physicalPostalAddress.district"
        const val PHYSICAL_POSTAL_ADDRESS_STREET_NAME_PREFIX = "address.physicalPostalAddress.street.namePrefix"
        const val PHYSICAL_POSTAL_ADDRESS_STREET_ADDITIONAL_NAME_PREFIX = "address.physicalPostalAddress.street.additionalNamePrefix"
        const val PHYSICAL_POSTAL_ADDRESS_STREET_NAME = "address.physicalPostalAddress.street.name"
        const val PHYSICAL_POSTAL_ADDRESS_STREET_NAME_SUFFIX = "address.physicalPostalAddress.street.nameSuffix"
        const val PHYSICAL_POSTAL_ADDRESS_STREET_ADDITIONAL_NAME_SUFFIX = "address.physicalPostalAddress.street.additionalNameSuffix"
        const val PHYSICAL_POSTAL_ADDRESS_STREET_HOUSE_NUMBER = "address.physicalPostalAddress.street.houseNumber"
        const val PHYSICAL_POSTAL_ADDRESS_STREET_HOUSE_NUMBER_SUPPLEMENT = "address.physicalPostalAddress.street.houseNumberSupplement"
        const val PHYSICAL_POSTAL_ADDRESS_STREET_MILESTONE = "address.physicalPostalAddress.street.milestone"
        const val PHYSICAL_POSTAL_ADDRESS_STREET_DIRECTION = "address.physicalPostalAddress.street.direction"
        const val PHYSICAL_POSTAL_ADDRESS_COMPANY_POSTAL_CODE = "address.physicalPostalAddress.companyPostalCode"
        const val PHYSICAL_POSTAL_ADDRESS_INDUSTRIAL_ZONE = "address.physicalPostalAddress.industrialZone"
        const val PHYSICAL_POSTAL_ADDRESS_BUILDING = "address.physicalPostalAddress.building"
        const val PHYSICAL_POSTAL_ADDRESS_FLOOR = "address.physicalPostalAddress.floor"
        const val PHYSICAL_POSTAL_ADDRESS_DOOR = "address.physicalPostalAddress.door"
        const val ALTERNATIVE_POSTAL_ADDRESS_LONGITUDE = "address.alternativePostalAddress.geographicCoordinates.longitude"
        const val ALTERNATIVE_POSTAL_ADDRESS_LATITUDE = "address.alternativePostalAddress.geographicCoordinates.latitude"
        const val ALTERNATIVE_POSTAL_ADDRESS_ALTITUDE = "address.alternativePostalAddress.geographicCoordinates.altitude"
        const val ALTERNATIVE_POSTAL_ADDRESS_COUNTRY = "address.alternativePostalAddress.country"
        const val ALTERNATIVE_POSTAL_ADDRESS_ADMIN_AREA_1 = "address.alternativePostalAddress.administrativeAreaLevel1"
        const val ALTERNATIVE_POSTAL_ADDRESS_POSTAL_CODE = "address.alternativePostalAddress.postalCode"
        const val ALTERNATIVE_POSTAL_ADDRESS_CITY = "address.alternativePostalAddress.city"
        const val ALTERNATIVE_POSTAL_ADDRESS_DELIVERY_SERVICE_TYPE = "address.alternativePostalAddress.deliveryServiceType"
        const val ALTERNATIVE_POSTAL_ADDRESS_DELIVERY_SERVICE_QUALIFIER = "address.alternativePostalAddress.deliveryServiceQualifier"
        const val ALTERNATIVE_POSTAL_ADDRESS_DELIVERY_SERVICE_NUMBER = "address.alternativePostalAddress.deliveryServiceNumber"
        const val ADDRESS_STATES_VALID_FROM = "address.states.validForm"
        const val ADDRESS_STATES_VALID_TO = "address.states.validTo"
        const val ADDRESS_STATES_TYPE = "address.states.type"

        val COLUMNS = listOf(
            EXTERNAL_ID,
            IDENTIFIERS_TYPE_1, IDENTIFIERS_VALUE_1, IDENTIFIERS_ISSUING_BODY_1,
            IDENTIFIERS_TYPE_2, IDENTIFIERS_VALUE_2, IDENTIFIERS_ISSUING_BODY_2,
            IDENTIFIERS_TYPE_3, IDENTIFIERS_VALUE_3, IDENTIFIERS_ISSUING_BODY_3,
            SITE_BPN, SITE_NAME, SITE_STATES_VALID_FROM, SITE_STATES_VALID_TO, SITE_STATES_TYPE,
            ADDRESS_BPN, ADDRESS_NAME, ADDRESS_TYPE,
            PHYSICAL_POSTAL_ADDRESS_LONGITUDE, PHYSICAL_POSTAL_ADDRESS_LATITUDE, PHYSICAL_POSTAL_ADDRESS_ALTITUDE,
            PHYSICAL_POSTAL_ADDRESS_COUNTRY,
            PHYSICAL_POSTAL_ADDRESS_ADMIN_AREA_1, PHYSICAL_POSTAL_ADDRESS_ADMIN_AREA_2, PHYSICAL_POSTAL_ADDRESS_ADMIN_AREA_3,
            PHYSICAL_POSTAL_ADDRESS_POSTAL_CODE, PHYSICAL_POSTAL_ADDRESS_CITY, PHYSICAL_POSTAL_ADDRESS_DISTRICT,
            PHYSICAL_POSTAL_ADDRESS_STREET_NAME_PREFIX, PHYSICAL_POSTAL_ADDRESS_STREET_ADDITIONAL_NAME_PREFIX,
            PHYSICAL_POSTAL_ADDRESS_STREET_NAME,
            PHYSICAL_POSTAL_ADDRESS_STREET_NAME_SUFFIX, PHYSICAL_POSTAL_ADDRESS_STREET_ADDITIONAL_NAME_SUFFIX,
            PHYSICAL_POSTAL_ADDRESS_STREET_HOUSE_NUMBER, PHYSICAL_POSTAL_ADDRESS_STREET_HOUSE_NUMBER_SUPPLEMENT,
            PHYSICAL_POSTAL_ADDRESS_STREET_MILESTONE, PHYSICAL_POSTAL_ADDRESS_STREET_DIRECTION,
            PHYSICAL_POSTAL_ADDRESS_COMPANY_POSTAL_CODE, PHYSICAL_POSTAL_ADDRESS_INDUSTRIAL_ZONE,
            PHYSICAL_POSTAL_ADDRESS_BUILDING, PHYSICAL_POSTAL_ADDRESS_FLOOR, PHYSICAL_POSTAL_ADDRESS_DOOR,
            ALTERNATIVE_POSTAL_ADDRESS_LONGITUDE, ALTERNATIVE_POSTAL_ADDRESS_LATITUDE, ALTERNATIVE_POSTAL_ADDRESS_ALTITUDE,
            ALTERNATIVE_POSTAL_ADDRESS_COUNTRY, ALTERNATIVE_POSTAL_ADDRESS_ADMIN_AREA_1,
            ALTERNATIVE_POSTAL_ADDRESS_POSTAL_CODE, ALTERNATIVE_POSTAL_ADDRESS_CITY,
            ALTERNATIVE_POSTAL_ADDRESS_DELIVERY_SERVICE_TYPE, ALTERNATIVE_POSTAL_ADDRESS_DELIVERY_SERVICE_QUALIFIER,
            ALTERNATIVE_POSTAL_ADDRESS_DELIVERY_SERVICE_NUMBER,
            ADDRESS_STATES_VALID_FROM, ADDRESS_STATES_VALID_TO, ADDRESS_STATES_TYPE
        )
    }
}
