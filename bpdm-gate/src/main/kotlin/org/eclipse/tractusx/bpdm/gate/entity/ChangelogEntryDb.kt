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

package org.eclipse.tractusx.bpdm.gate.entity

import jakarta.persistence.*
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.model.BaseEntity
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.model.ChangelogType

@Entity
@Table(name = "changelog_entries")
class ChangelogEntryDb(

    @Column(name = "external_id", nullable = false, updatable = false)
    val externalId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "business_partner_type", nullable = false, updatable = false)
    val businessPartnerType: BusinessPartnerType,

    @Column(name = "associated_owner_bpnl", nullable = true)
    var associatedOwnerBpnl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "changelog_type", nullable = false, updatable = false)
    val changelogType: ChangelogType,

    @Column(name = "data_type")
    @Enumerated(EnumType.STRING)
    var stage: StageType

) : BaseEntity()
