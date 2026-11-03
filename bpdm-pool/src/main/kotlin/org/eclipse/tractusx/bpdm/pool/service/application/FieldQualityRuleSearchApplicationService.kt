/*******************************************************************************
 * Copyright (c) 2021,2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.service.application

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.mapper.DbToOutputMapper
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.FieldQualityRuleSearchResult
import org.eclipse.tractusx.bpdm.pool.model.raw.metadata.FieldQualityRuleSearchRaw
import org.eclipse.tractusx.bpdm.pool.service.operation.metadata.FieldQualityRuleSearchService
import org.eclipse.tractusx.bpdm.pool.service.parser.metadata.FieldQualityRuleSearchParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Searches the quality rules that apply to the fields of a business partner in a country, for every API version.
 */
@Service
class FieldQualityRuleSearchApplicationService(
    private val fieldQualityRuleSearchParser: FieldQualityRuleSearchParser,
    private val fieldQualityRuleSearchService: FieldQualityRuleSearchService,
    private val dbToOutputMapper: DbToOutputMapper
) {

    /**
     * Returns the field quality rules that apply in the country of [request], or every reason the criteria are rejected.
     */
    @Transactional(readOnly = true)
    fun search(request: FieldQualityRuleSearchRaw): FieldQualityRuleSearchResult =
        when (val parseResult = fieldQualityRuleSearchParser.parse(request)) {
            is ParseResult.Failure -> FieldQualityRuleSearchResult.Rejected(parseResult.errors)
            is ParseResult.Success -> FieldQualityRuleSearchResult.Found(
                fieldQualityRuleSearchService.search(parseResult.parsed).map { dbToOutputMapper.toRecord(it, parseResult.parsed.country) }
            )
        }
}
