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

package org.eclipse.tractusx.bpdm.common.dto.openapidescription

object PostalAddressDescription {
    const val headerPhysical = "A physical postal address describes the physical location of an office, warehouse, gate, etc."
    const val headerAlternative = "An alternative postal address describes an alternative way of delivery for example if the goods " +
            "are to be picked up somewhere else."
    const val headerGeoCoordinates = "The exact location of the physical postal address in latitude, longitude, and altitude."

    const val country = "The 2-digit country code of the physical postal address according to ISO 3166-1."
    const val postalCode = "The alphanumeric identifier (sometimes including spaces or punctuation) of the physical postal address for " +
            "the purpose of sorting mail, synonyms:postcode, post code, PIN or ZIP code."
    const val city = "The name of the city of the physical postal address, synonyms: town, village, municipality."

    const val administrativeAreaLevel1 = "The 2-digit country subdivision code according to ISO 3166-2, such as a region within a country."
    const val administrativeAreaLevel2 = "The name of the locally regulated secondary country subdivision of the physical postal address, " +
            "such as county within a country."
    const val administrativeAreaLevel3 = "The name of the locally regulated tertiary country subdivision of the physical address, " +
            "such as townships within a country."
    const val district = "The name of the district of the physical postal address which divides the city in several smaller areas."

    const val companyPostalCode = "The company postal code of the physical postal address, which is sometimes required for large companies."
    const val industrialZone = "The industrial zone of the physical postal address, designating an area for industrial development, synonym: industrial area."
    const val building = "The alphanumeric identifier of the building addressed by the physical postal address."
    const val floor = "The number of a floor in the building addressed by the physical postal address, synonym: level."
    const val door = "The number of a door in the building on the respective floor addressed by the physical postal address, synonyms: room, suite."

    const val deliveryServiceType = "One of the alternative postal address types: P.O. box, private bag, boite postale."
    const val deliveryServiceQualifier = "The qualifier uniquely identifying the delivery service endpoint of the alternative postal address " +
            "in conjunction with the delivery service number. In some countries for example, entering a P.O. box number, " +
            "postal code and city is not sufficient to uniquely identify a P.O. box, because the same P.O. box number " +
            "is assigned multiple times in some cities."
    const val deliveryServiceNumber = "The number indicating the delivery service endpoint of the alternative postal address to which the delivery is " +
            "to be delivered, such as a P.O. box number or a private bag number."
}
