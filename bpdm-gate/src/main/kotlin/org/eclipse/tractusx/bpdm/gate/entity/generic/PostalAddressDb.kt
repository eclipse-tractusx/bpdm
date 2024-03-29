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

package org.eclipse.tractusx.bpdm.gate.entity.generic

import jakarta.persistence.*
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.model.BaseEntity
import org.eclipse.tractusx.bpdm.gate.entity.AlternativePostalAddressDb
import org.eclipse.tractusx.bpdm.gate.entity.PhysicalPostalAddressDb

@Entity
@Table(name = "postal_addresses")
class PostalAddressDb(

    @Column(name = "address_type")
    @Enumerated(EnumType.STRING)
    var addressType: AddressType? = null,

    @Embedded
    var physicalPostalAddress: PhysicalPostalAddressDb? = null,

    @Embedded
    var alternativePostalAddress: AlternativePostalAddressDb? = null

) : BaseEntity()
