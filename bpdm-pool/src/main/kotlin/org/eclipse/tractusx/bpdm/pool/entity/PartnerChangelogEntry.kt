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

import jakarta.persistence.*
import org.eclipse.tractusx.bpdm.pool.api.dto.ChangelogType

@Entity
@Table(name = "partner_changelog_entries")
class PartnerChangelogEntry(
    @Enumerated(EnumType.STRING)
    @Column(name = "changelog_type", nullable = false, updatable = false)
    val changelogType: ChangelogType,
    @Column(name = "bpn", nullable = false, updatable = false)
    val bpn: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "changelog_subject", nullable = false, updatable = false)
    val changelogSubject: ChangelogSubject
) : BaseEntity()



enum class ChangelogSubject {
    LEGAL_ENTITY,
    ADDRESS,
    SITE
}