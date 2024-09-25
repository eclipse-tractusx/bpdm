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

object PartnerUploadFileHeader {
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
}