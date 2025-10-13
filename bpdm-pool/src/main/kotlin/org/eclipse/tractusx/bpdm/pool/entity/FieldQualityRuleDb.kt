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

package org.eclipse.tractusx.bpdm.pool.entity

import com.neovisionaries.i18n.CountryCode
import jakarta.persistence.*
import org.eclipse.tractusx.bpdm.common.model.BaseEntity
import org.eclipse.tractusx.bpdm.pool.api.model.QualityLevel

@Entity
@Table(
    name = "field_quality_rule",
    uniqueConstraints = [UniqueConstraint(
        name = "uc_field_quality_rule_country_schema_field_id",
        columnNames = ["country_code", "schema_name", "field_path"]
    )]
)
class FieldQualityRuleDb(

    @Column(name = "country_code", nullable = false)
    @Enumerated(EnumType.STRING)
    var countryCode: CountryCode?,

    @Column(name = "schema_name", nullable = false)
    var schemaName: String,

    @Column(name = "field_path", nullable = false)
    var fieldPath: String,

    @Column(name = "quality_level", nullable = false)
    @Enumerated(EnumType.STRING)
    var qualityLevel: QualityLevel,

    ) : BaseEntity()
