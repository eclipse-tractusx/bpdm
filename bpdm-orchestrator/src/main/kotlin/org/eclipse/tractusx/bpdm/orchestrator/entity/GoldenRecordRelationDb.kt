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

package org.eclipse.tractusx.bpdm.orchestrator.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.eclipse.tractusx.orchestrator.api.model.AddressGoldenRecordRelationType
import org.eclipse.tractusx.orchestrator.api.model.LegalEntityGoldenRecordRelationType

@Embeddable
data class LegalEntityGoldenRecordRelationDb(
    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false)
    val relationType: LegalEntityGoldenRecordRelationType,

    @Column(name = "source_bpn", nullable = false)
    val sourceBpn: String,

    @Column(name = "target_bpn", nullable = false)
    val targetBpn: String
)

@Embeddable
data class AddressGoldenRecordRelationDb(
    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false)
    val relationType: AddressGoldenRecordRelationType,

    @Column(name = "source_bpn", nullable = false)
    val sourceBpn: String,

    @Column(name = "target_bpn", nullable = false)
    val targetBpn: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false)
    val scope: Scope
) {
    enum class Scope {
        LegalAddress,
        SiteMainAddress,
        AdditionalAddress
    }
}
