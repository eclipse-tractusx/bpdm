/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.gate.entity

import com.neovisionaries.i18n.CountryCode
import jakarta.persistence.*
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType

@Embeddable
data class AlternativePostalAddress(

    @Embedded
    @AttributeOverride(name = "latitude", column = Column(name = "alt_latitude"))
    @AttributeOverride(name = "longitude", column = Column(name = "alt_longitude"))
    @AttributeOverride(name = "altitude", column = Column(name = "alt_altitude"))
    val geographicCoordinates: GeographicCoordinate?,

    @Column(name = "alt_country")
    @Enumerated(EnumType.STRING)
    val country: CountryCode?,

    /**
     * Region within the country
     */
    @Column(name = "alt_admin_area_l1_region")
    val administrativeAreaLevel1: String?,

    /**
     * A postal code, also known as postcode, PIN or ZIP Code
     */
    @Column(name = "alt_postcode")
    val postalCode: String?,

    /**
     * The city of the address (Synonym: Town, village, municipality)
     */
    @Column(name = "alt_city")
    val city: String?,

    /**
     * The type of this specified delivery
     */
    @Column(name = "alt_delivery_service_type")
    @Enumerated(EnumType.STRING)
    val deliveryServiceType: DeliveryServiceType?,

    /**
     * The qualifier uniquely identifies the delivery service endpoint in conjunction with the delivery service number
     */
    @Column(name = "alt_delivery_service_qualifier")
    val deliveryServiceQualifier: String?,

    /**
     * Describes the PO Box or private Bag number the delivery should be placed at
     */
    @Column(name = "alt_delivery_service_number")
    val deliveryServiceNumber: String?
)
