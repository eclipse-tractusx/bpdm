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

package org.eclipse.tractusx.bpdm.pool.service.operation

import org.eclipse.tractusx.bpdm.pool.api.model.QualityLevel
import org.eclipse.tractusx.bpdm.pool.entity.FieldQualityRuleDb
import org.eclipse.tractusx.bpdm.pool.model.FieldQualityRule
import org.eclipse.tractusx.bpdm.pool.model.parsed.FieldQualityRuleSearchParsed
import org.eclipse.tractusx.bpdm.pool.repository.FieldQualityRuleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Queries the quality rules that apply to the fields of a business partner in a given country.
 */
@Service
class FieldQualityRuleSearchService(
    private val fieldQualityRuleRepository: FieldQualityRuleRepository
) {

    /**
     * Returns the rules that apply in the given country, each country-specific rule overriding the default rule for the
     * same field, rules forbidding the field left out, ordered by schema and field path.
     */
    @Transactional(readOnly = true)
    fun search(criteria: FieldQualityRuleSearchParsed): List<FieldQualityRule> {
        val defaultRulesByField = fieldQualityRuleRepository.findByCountryCodeIsNullOrderBySchemaNameAscFieldPathAsc()
            .associateBy { it.field() }
        val countryRulesByField = fieldQualityRuleRepository.findByCountryCodeOrderBySchemaNameAscFieldPathAsc(criteria.country)
            .associateBy { it.field() }

        return (defaultRulesByField.keys + countryRulesByField.keys)
            .mapNotNull { field -> countryRulesByField[field] ?: defaultRulesByField[field] }
            .filter { it.qualityLevel != QualityLevel.FORBIDDEN }
            .map { rule ->
                FieldQualityRule(
                    fieldPath = rule.fieldPath,
                    schemaName = rule.schemaName,
                    country = rule.countryCode ?: criteria.country,
                    qualityLevel = rule.qualityLevel
                )
            }
            .sortedWith(compareBy({ it.schemaName }, { it.fieldPath }))
    }

    private fun FieldQualityRuleDb.field() = "$schemaName.$fieldPath"
}
