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

package org.eclipse.tractusx.bpdm.test.system.utils

import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.api.model.RelationValidityPeriodDto
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutEntry
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.TestDataFactoryGateV7
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withExternalId
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withRelationType
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withSource
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withTarget
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withValidityPeriods
import java.time.LocalDate


/**
 * Generates test data for business partner relation system tests.
 *
 * Follows the same two-seed strategy as [ShareOwnCompanyDataTestDataGenerator]:
 * - scenarioId: seeds structural values, stable across runs for the same scenario.
 * - runId: replaces identifiers so each run produces collision-free values.
 */
class BusinessPartnerRelationTestDataGenerator(
    private val testDataFactoryGate: TestDataFactoryGateV7
) {

    data class RelationInputResult(
        val relationInputEntry: RelationPutEntry
    )

    fun buildRelationInputDataWithFutureValidity(
        id: String,
        relationType: String,
        fromRecordId: String,
        toRecordId: String
    ): RelationInputResult {
        val futureValidity = listOf(RelationValidityPeriodDto(
            validFrom = LocalDate.now().plusYears(1),
            validTo = null
        ))
        val result = buildRelationInputData(id, relationType, fromRecordId, toRecordId)
        return result.copy(relationInputEntry = result.relationInputEntry.withValidityPeriods(futureValidity))
    }

    fun buildRelationInputData(
        id: String,
        relationType: String,
        fromRecordId: String,
        toRecordId: String
    ): RelationInputResult {
        val context = ScenarioContext.current()!!
        val entry = testDataFactoryGate.relation.input.request.fromSeed(scenarioId(id))
            .withExternalId(context.runId(id))
            .withRelationType(RelationType.valueOf(relationType))
            .withSource(context.runId(fromRecordId))
            .withTarget(context.runId(toRecordId))
        return RelationInputResult(entry)
    }

    private fun scenarioId(id: String) = "$id${ScenarioContext.current()!!.scenarioSuffix}"
}
