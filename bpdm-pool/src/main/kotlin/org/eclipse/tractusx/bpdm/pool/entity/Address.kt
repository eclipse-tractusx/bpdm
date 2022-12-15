/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.entity

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.model.AddressType
import jakarta.persistence.*

@Entity
@Table(
    name = "addresses"
)
class Address(
    @Column(name = "care_of")
    var careOf: String?,
    @ElementCollection(targetClass = String::class)
    @JoinTable(
        name = "address_contexts",
        joinColumns = [JoinColumn(name = "address_id")],
        indexes = [Index(columnList = "address_id")]
    )
    @Column(name = "context", nullable = false)
    val contexts: MutableSet<String> = mutableSetOf(),
    @Column(name = "country", nullable = false)
    @Enumerated(EnumType.STRING)
    var country: CountryCode,
    @ElementCollection(targetClass = AddressType::class)
    @Enumerated(EnumType.STRING)
    @JoinTable(
        name = "address_types",
        joinColumns = [JoinColumn(name = "address_id")],
        indexes = [Index(columnList = "address_id")]
    )
    @Column(name = "type", nullable = false)
    val types: MutableSet<AddressType> = mutableSetOf(),
    @Embedded
    var version: AddressVersion,
    @Embedded
    var geoCoordinates: GeographicCoordinate?
) : BaseEntity() {
    @OneToMany(mappedBy = "address", cascade = [CascadeType.ALL], orphanRemoval = true)
    val administrativeAreas: MutableSet<AdministrativeArea> = mutableSetOf()

    @OneToMany(mappedBy = "address", cascade = [CascadeType.ALL], orphanRemoval = true)
    val postCodes: MutableSet<PostCode> = mutableSetOf()

    @OneToMany(mappedBy = "address", cascade = [CascadeType.ALL], orphanRemoval = true)
    val thoroughfares: MutableSet<Thoroughfare> = mutableSetOf()

    @OneToMany(mappedBy = "address", cascade = [CascadeType.ALL], orphanRemoval = true)
    val premises: MutableSet<Premise> = mutableSetOf()

    @OneToMany(mappedBy = "address", cascade = [CascadeType.ALL], orphanRemoval = true)
    val postalDeliveryPoints: MutableSet<PostalDeliveryPoint> = mutableSetOf()

    @OneToMany(mappedBy = "address", cascade = [CascadeType.ALL], orphanRemoval = true)
    val localities: MutableSet<Locality> = mutableSetOf()
}



