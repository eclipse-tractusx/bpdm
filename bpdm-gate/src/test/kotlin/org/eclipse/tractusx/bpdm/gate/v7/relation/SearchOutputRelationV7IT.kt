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

package org.eclipse.tractusx.bpdm.gate.v7.relation

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.gate.v7.UnscheduledGateTestBaseV7
import org.junit.jupiter.api.Test

class SearchOutputRelationV7IT: UnscheduledGateTestBaseV7(){


    /**
     * GIVEN relation output between two legal entities
     * WHEN output consumer searches for output with that external-ID
     * THEN output consumer sees relation output
     *
     */
    @Test
    fun `search legal entity business partner output`(){
        //GIVEN
        val sourceInput = testDataClient.upsertBusinessPartnerInput("$testName 1")
        val targetInput = testDataClient.upsertBusinessPartnerInput("$testName 2")
        val relationInput = testDataClient.upsertRelationInput(testName, sourceInput, targetInput)

        testDataClient.refineToLegalEntity(sourceInput)
        testDataClient.refineToLegalEntity(targetInput)

        val relationGoldenRecord = testDataClient.refineRelationToSuccess(relationInput)

        //WHEN
        val response = gateClient.relationOutput.postSearch()

        //THEN
        val expectedOutput = relationOutputFactory.fromGoldenRecord(relationInput.externalId, relationGoldenRecord)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedOutput))

        assertRepo.assertRelationOutput(response, expectedResponse)
    }
}