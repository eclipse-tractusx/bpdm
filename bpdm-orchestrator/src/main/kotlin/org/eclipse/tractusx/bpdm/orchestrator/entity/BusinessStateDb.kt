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
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.hibernate.annotations.Type

@Embeddable
data class BusinessStateDb(
    @Type(value = DbTimestampConverter::class)
    @Column(name = "valid_from")
    val validFrom: DbTimestamp?,
    @Type(value = DbTimestampConverter::class)
    @Column(name = "valid_to")
    val validTo: DbTimestamp?,
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    val type: BusinessStateType?,
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false)
    val scope: Scope
) {
    enum class Scope {
        LegalEntity,
        Site,
        Uncategorized,
        LegalAddress,
        SiteMainAddress,
        AdditionalAddress,
        UncategorizedAddress
    }
}