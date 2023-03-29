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

import jakarta.persistence.*
import org.eclipse.tractusx.bpdm.common.model.BaseEntity
import org.eclipse.tractusx.bpdm.common.model.ThoroughfareType

@Entity
@Table(
    name = "thoroughfares",
    indexes = [
        Index(columnList = "address_id")
    ]
)
class ThoroughfareGate (
    @Column(name = "`value`", nullable = false)
    val value: String,
    @Column(name = "name")
    val name: String?,
    @Column(name = "short_name")
    val shortName: String?,
    @Column(name = "number")
    val number: String?,
    @Column(name = "direction")
    val direction: String?,
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    val type: ThoroughfareType,
    @ManyToOne
    @JoinColumn(name = "address_id", nullable = false)
    var address: AddressGate
) : BaseEntity()
