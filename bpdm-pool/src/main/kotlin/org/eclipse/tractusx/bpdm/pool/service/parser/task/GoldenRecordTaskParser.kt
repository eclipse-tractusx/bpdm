/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.service.parser.task

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.errorsOrEmpty
import org.eclipse.tractusx.bpdm.common.model.parseResultOf
import org.eclipse.tractusx.bpdm.common.model.parsedOrNull
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.GoldenRecordTaskError
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.GoldenRecordTaskParsed
import org.eclipse.tractusx.bpdm.pool.model.raw.task.GoldenRecordTaskRaw
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Decides what a golden record task writes, or finds every reason it cannot be carried out.
 *
 * One task at a time: tasks of one reservation may name the same request identifier and must then reach the same
 * business partner, which is only known once the earlier task has been written.
 */
@Service
class GoldenRecordTaskParser(
    private val referenceParser: GoldenRecordTaskReferenceParser,
    private val legalEntityUpsertParser: LegalEntityUpsertParser,
    private val siteUpsertParser: SiteUpsertParser,
    private val additionalAddressUpsertParser: AdditionalAddressUpsertParser,
    private val additionalSitesParser: AdditionalSitesParser,
    private val parentValidator: GoldenRecordTaskParentValidator,
    private val identifierDistinctnessValidator: GoldenRecordTaskIdentifierDistinctnessValidator,
    private val membershipValidator: GoldenRecordTaskMembershipValidator
) {

    /**
     * Reports what [task] writes, or every problem that stops it.
     */
    @Transactional(readOnly = true)
    fun parse(task: GoldenRecordTaskRaw): ParseResult<GoldenRecordTaskParsed, GoldenRecordTaskError> {
        val goldenRecord = task.goldenRecord
        val references = referenceParser.parse(goldenRecord)

        val parentErrors = parentValidator.validate(goldenRecord, references)

        val legalEntity = legalEntityUpsertParser.parse(goldenRecord.legalEntity, references)
        val site = goldenRecord.site?.let { siteUpsertParser.parse(it, goldenRecord.legalEntity, legalEntity.parsedOrNull(), references) }
        val additionalAddress = goldenRecord.additionalAddress?.let { additionalAddressUpsertParser.parse(it, references) }
        val distinctnessErrors = identifierDistinctnessValidator
            .validate(goldenRecord, legalEntity.parsedOrNull(), site?.parsedOrNull(), additionalAddress?.parsedOrNull())
        val additionalSites = goldenRecord.site?.let { additionalSitesParser.parse(goldenRecord.additionalSites, it, references) }

        val legalEntityPlan = legalEntity.parsedOrNull()
        val sitePlan = site?.parsedOrNull()
        val additionalAddressPlan = additionalAddress?.parsedOrNull()
        val additionalSitesPlan = additionalSites?.parsedOrNull()
        val isAdditionalAddressParsed = goldenRecord.additionalAddress == null || additionalAddressPlan != null
        val membershipErrors = if (legalEntityPlan != null && sitePlan != null && additionalSitesPlan != null && isAdditionalAddressParsed)
            membershipValidator.validate(legalEntityPlan, sitePlan, additionalAddressPlan, additionalSitesPlan)
        else
            emptyList()

        val errors = parentErrors + legalEntity.errorsOrEmpty() + site?.errorsOrEmpty().orEmpty() +
                additionalAddress?.errorsOrEmpty().orEmpty() + distinctnessErrors + additionalSites?.errorsOrEmpty().orEmpty() + membershipErrors

        val plan = legalEntityPlan?.let { GoldenRecordTaskParsed(task.sharingMemberRecordId, it, sitePlan, additionalAddressPlan, additionalSitesPlan) }
        return parseResultOf(plan, errors)
    }
}
