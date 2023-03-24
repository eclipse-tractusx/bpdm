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

package org.eclipse.tractusx.bpdm.pool.entity

import com.neovisionaries.i18n.CountryCode
import jakarta.persistence.*
import org.eclipse.tractusx.bpdm.common.model.BaseEntity

/**
 * Represents identifier type validity details specific to a country
 */
@Entity
@Table(
    name = "identifier_type_details",
    uniqueConstraints = [UniqueConstraint(
        name = "uc_identifier_type_details_country_code_identifier_type_id",
        columnNames = ["country_code", "identifier_type_id"]
    )]
)
class IdentifierTypeDetail(
    @ManyToOne
    @JoinColumn(name = "identifier_type_id", nullable = false)
    var identifierType: IdentifierType,

    // null for universal identifiers
    @Column(name = "country_code", nullable = true)
    @Enumerated(EnumType.STRING)
    var countryCode: CountryCode?,

    @Column(name = "mandatory", nullable = false)
    var mandatory: Boolean

) : BaseEntity()
