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

package org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound

import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.FieldQualityRuleV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.QualityLevelV6
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.QualityLevelV7
import org.eclipse.tractusx.bpdm.pool.model.FieldQualityRule
import org.springframework.stereotype.Component

/**
 * Maps the results of the field quality rule search to the v6 API field quality rule DTOs.
 */
@Component
class FieldQualityRuleResponseMapperV6 {

    /**
     * Returns the given field quality rule as the v6 API reports it.
     */
    fun toFieldQualityRule(rule: FieldQualityRule): FieldQualityRuleV6Dto =
        FieldQualityRuleV6Dto(
            fieldPath = rule.fieldPath,
            schemaName = rule.schemaName,
            country = rule.country,
            qualityLevel = toQualityLevel(rule.qualityLevel)
        )

    private fun toQualityLevel(qualityLevel: QualityLevelV7): QualityLevelV6 =
        when (qualityLevel) {
            QualityLevelV7.MANDATORY -> QualityLevelV6.MANDATORY
            QualityLevelV7.OPTIONAL -> QualityLevelV6.OPTIONAL
            QualityLevelV7.FORBIDDEN -> QualityLevelV6.FORBIDDEN
        }
}
