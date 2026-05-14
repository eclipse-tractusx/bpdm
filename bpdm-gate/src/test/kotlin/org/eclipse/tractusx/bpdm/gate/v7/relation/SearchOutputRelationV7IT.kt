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
import org.eclipse.tractusx.bpdm.gate.api.model.RelationOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.api.model.SharableRelationType
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationOutputSearchRequest
import org.eclipse.tractusx.bpdm.gate.v7.UnscheduledGateTestBaseV7
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.GateTestTypes
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.Instant

class SearchOutputRelationV7IT: UnscheduledGateTestBaseV7(){

    /**
     * GIVEN relation output between two legal entities
     * WHEN output consumer searches for output with that external-ID
     * THEN output consumer sees relation output
     *
     */
    @ParameterizedTest
    @EnumSource(GateTestTypes.LegalEntityRelationType::class)
    fun `search relation output between legal entities`(legalEntityRelationType: GateTestTypes.LegalEntityRelationType) {
        //GIVEN
        val (relationInput, relationGoldenRecord) = testDataClient.createLegalEntityRelationOutput(testName, legalEntityRelationType.gateRelationType)

        //WHEN
        val response = gateClient.relationOutput.postSearch()

        //THEN
        val expectedOutput = relationOutputFactory.fromGoldenRecord(relationInput.externalId, relationGoldenRecord)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedOutput))

        assertRepo.assertRelationOutput(response, expectedResponse)
    }

    /**
     * GIVEN relation output between two legal entities
     * WHEN output consumer searches for output with that external-ID
     * THEN output consumer sees relation output
     *
     */
    @ParameterizedTest
    @EnumSource(GateTestTypes.LegalEntityRelationType::class)
    fun `search updated relation output between legal entities`(legalEntityRelationType: GateTestTypes.LegalEntityRelationType) {
        //GIVEN
        val (relationInput, _) = testDataClient.createLegalEntityRelationOutput(testName, legalEntityRelationType.gateRelationType)
        val otherType = RelationType.entries.find { it != legalEntityRelationType.gateRelationType }!!
        val relationGoldenRecord = testDataClient.updateLegalEntityRelationOutput(relationInput, "Updated $testName", otherType)

        //WHEN
        val response = gateClient.relationOutput.postSearch()

        //THEN
        val expectedOutput = relationOutputFactory.fromGoldenRecord(relationInput.externalId, relationGoldenRecord)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedOutput))

        assertRepo.assertRelationOutput(response, expectedResponse)
    }

    /**
     * GIVEN relation output between a legal address and additional address
     * WHEN output consumer searches for output with that external-ID
     * THEN output consumer sees relation output
     *
     */
    @ParameterizedTest
    @EnumSource(GateTestTypes.AddressRelationType::class)
    fun `search relation output between legal address and additional address`(addressRelationType: GateTestTypes.AddressRelationType) {
        //GIVEN
        val (relationInput, relationGoldenRecord) = testDataClient.createAddressRelationOutput(testName, addressRelationType.gateRelationType)

        //WHEN
        val response = gateClient.relationOutput.postSearch()

        //THEN
        val expectedOutput = relationOutputFactory.fromGoldenRecord(relationInput.externalId, relationGoldenRecord)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedOutput))

        assertRepo.assertRelationOutput(response, expectedResponse)
    }


    /**
     * GIVEN no relation outputs
     * WHEN output consumer searches without filters
     * THEN an empty page is returned
     */
    @Test
    fun `find no outputs when none exist`() {
        //WHEN
        val response = gateClient.relationOutput.postSearch()

        //THEN
        val emptyOutputs: List<RelationOutputDto> = emptyList()
        assertRepo.assertRelationOutput(response, PageDto(0, 0, 0, 0, emptyOutputs))
    }

    /**
     * GIVEN three relation outputs
     * WHEN output consumer searches by a single external ID
     * THEN only the output with that external ID is returned
     */
    @Test
    fun `find output by external ID`() {
        //GIVEN
        val (inquired, goldenRecord) = testDataClient.createLegalEntityRelationOutput("$testName 1")
        testDataClient.createLegalEntityRelationOutput("$testName 2")
        testDataClient.createLegalEntityRelationOutput("$testName 3")

        //WHEN
        val response = gateClient.relationOutput.postSearch(RelationOutputSearchRequest(externalIds = listOf(inquired.externalId)))

        //THEN
        val expectedOutput = relationOutputFactory.fromGoldenRecord(inquired.externalId, goldenRecord)
        assertRepo.assertRelationOutput(response, PageDto(1, 1, 0, 1, listOf(expectedOutput)))
    }

    /**
     * GIVEN three relation outputs
     * WHEN output consumer searches by two external IDs
     * THEN only the two matching outputs are returned
     */
    @Test
    fun `find outputs by multiple external IDs`() {
        //GIVEN
        val (rel1, golden1) = testDataClient.createLegalEntityRelationOutput("$testName 1")
        val (rel2, golden2) = testDataClient.createLegalEntityRelationOutput("$testName 2")
        testDataClient.createLegalEntityRelationOutput("$testName 3")

        //WHEN
        val response = gateClient.relationOutput.postSearch(
            RelationOutputSearchRequest(externalIds = listOf(rel1.externalId, rel2.externalId))
        )

        //THEN
        val expectedOutputs = listOf(
            relationOutputFactory.fromGoldenRecord(rel1.externalId, golden1),
            relationOutputFactory.fromGoldenRecord(rel2.externalId, golden2)
        )
        assertRepo.assertRelationOutput(response, PageDto(2, 1, 0, 2, expectedOutputs))
    }

    /**
     * GIVEN three relation outputs of potentially differing types
     * WHEN output consumer filters by a specific relation type
     * THEN only outputs of that type are returned
     */
    @Test
    fun `find outputs by relation type`() {
        //GIVEN
        val (rel1, golden1) = testDataClient.createLegalEntityRelationOutput("$testName 1")
        val (rel2, golden2) = testDataClient.createLegalEntityRelationOutput("$testName 2")
        val (rel3, golden3) = testDataClient.createLegalEntityRelationOutput("$testName 3")

        val filterType = SharableRelationType.valueOf(golden1.relationType.name)

        //WHEN
        val response = gateClient.relationOutput.postSearch(RelationOutputSearchRequest(relationType = filterType))

        //THEN
        val allOutputs = listOf(
            relationOutputFactory.fromGoldenRecord(rel1.externalId, golden1),
            relationOutputFactory.fromGoldenRecord(rel2.externalId, golden2),
            relationOutputFactory.fromGoldenRecord(rel3.externalId, golden3)
        )
        val expectedOutputs = allOutputs.filter { it.relationType == filterType }
        assertRepo.assertRelationOutput(response, PageDto(expectedOutputs.size.toLong(), 1, 0, expectedOutputs.size, expectedOutputs))
    }

    /**
     * GIVEN three relation outputs with distinct source BPNs
     * WHEN output consumer filters by a single source BPN
     * THEN only the output with that source BPN is returned
     */
    @Test
    fun `find output by source BPN`() {
        //GIVEN
        val (inquired, goldenRecord) = testDataClient.createLegalEntityRelationOutput("$testName 1")
        testDataClient.createLegalEntityRelationOutput("$testName 2")
        testDataClient.createLegalEntityRelationOutput("$testName 3")

        //WHEN
        val response = gateClient.relationOutput.postSearch(
            RelationOutputSearchRequest(sourceBpns = listOf(goldenRecord.businessPartnerSourceBpn))
        )

        //THEN
        val expectedOutput = relationOutputFactory.fromGoldenRecord(inquired.externalId, goldenRecord)
        assertRepo.assertRelationOutput(response, PageDto(1, 1, 0, 1, listOf(expectedOutput)))
    }

    /**
     * GIVEN three relation outputs with distinct source BPNs
     * WHEN output consumer filters by two source BPNs
     * THEN only the two matching outputs are returned
     */
    @Test
    fun `find outputs by multiple source BPNs`() {
        //GIVEN
        val (rel1, golden1) = testDataClient.createLegalEntityRelationOutput("$testName 1")
        val (rel2, golden2) = testDataClient.createLegalEntityRelationOutput("$testName 2")
        testDataClient.createLegalEntityRelationOutput("$testName 3")

        //WHEN
        val response = gateClient.relationOutput.postSearch(
            RelationOutputSearchRequest(sourceBpns = listOf(golden1.businessPartnerSourceBpn, golden2.businessPartnerSourceBpn))
        )

        //THEN
        val expectedOutputs = listOf(
            relationOutputFactory.fromGoldenRecord(rel1.externalId, golden1),
            relationOutputFactory.fromGoldenRecord(rel2.externalId, golden2)
        )
        assertRepo.assertRelationOutput(response, PageDto(2, 1, 0, 2, expectedOutputs))
    }

    /**
     * GIVEN three relation outputs with distinct target BPNs
     * WHEN output consumer filters by a single target BPN
     * THEN only the output with that target BPN is returned
     */
    @Test
    fun `find output by target BPN`() {
        //GIVEN
        val (inquired, goldenRecord) = testDataClient.createLegalEntityRelationOutput("$testName 1")
        testDataClient.createLegalEntityRelationOutput("$testName 2")
        testDataClient.createLegalEntityRelationOutput("$testName 3")

        //WHEN
        val response = gateClient.relationOutput.postSearch(
            RelationOutputSearchRequest(targetBpns = listOf(goldenRecord.businessPartnerTargetBpn))
        )

        //THEN
        val expectedOutput = relationOutputFactory.fromGoldenRecord(inquired.externalId, goldenRecord)
        assertRepo.assertRelationOutput(response, PageDto(1, 1, 0, 1, listOf(expectedOutput)))
    }

    /**
     * GIVEN three relation outputs with distinct target BPNs
     * WHEN output consumer filters by two target BPNs
     * THEN only the two matching outputs are returned
     */
    @Test
    fun `find outputs by multiple target BPNs`() {
        //GIVEN
        val (rel1, golden1) = testDataClient.createLegalEntityRelationOutput("$testName 1")
        val (rel2, golden2) = testDataClient.createLegalEntityRelationOutput("$testName 2")
        testDataClient.createLegalEntityRelationOutput("$testName 3")

        //WHEN
        val response = gateClient.relationOutput.postSearch(
            RelationOutputSearchRequest(targetBpns = listOf(golden1.businessPartnerTargetBpn, golden2.businessPartnerTargetBpn))
        )

        //THEN
        val expectedOutputs = listOf(
            relationOutputFactory.fromGoldenRecord(rel1.externalId, golden1),
            relationOutputFactory.fromGoldenRecord(rel2.externalId, golden2)
        )
        assertRepo.assertRelationOutput(response, PageDto(2, 1, 0, 2, expectedOutputs))
    }

    /**
     * GIVEN outputs created before and after a timestamp
     * WHEN output consumer filters by updatedAtFrom set to that timestamp
     * THEN only outputs created after the timestamp are returned
     */
    @Test
    fun `find outputs updated after timestamp`() {
        //GIVEN
        testDataClient.createLegalEntityRelationOutput("$testName old 1")
        testDataClient.createLegalEntityRelationOutput("$testName old 2")

        val updatedAtFrom = Instant.now()

        val (new1, golden1) = testDataClient.createLegalEntityRelationOutput("$testName new 1")
        val (new2, golden2) = testDataClient.createLegalEntityRelationOutput("$testName new 2")

        //WHEN
        val response = gateClient.relationOutput.postSearch(RelationOutputSearchRequest(updatedAtFrom = updatedAtFrom))

        //THEN
        val expectedOutputs = listOf(
            relationOutputFactory.fromGoldenRecord(new1.externalId, golden1),
            relationOutputFactory.fromGoldenRecord(new2.externalId, golden2)
        )
        assertRepo.assertRelationOutput(response, PageDto(2, 1, 0, 2, expectedOutputs))
    }

    /**
     * GIVEN several relation outputs with different types, sources and targets
     * WHEN output consumer applies multiple filters simultaneously
     * THEN only the single output matching all criteria is returned
     */
    @Test
    fun `find output using combined filters`() {
        //GIVEN
        val (rel1, golden1) = testDataClient.createLegalEntityRelationOutput("$testName 1")
        val (rel2, _) = testDataClient.createLegalEntityRelationOutput("$testName 2")
        testDataClient.createLegalEntityRelationOutput("$testName 3")

        val filterType = SharableRelationType.valueOf(golden1.relationType.name)

        //WHEN
        val response = gateClient.relationOutput.postSearch(
            RelationOutputSearchRequest(
                externalIds = listOf(rel1.externalId, rel2.externalId),
                relationType = filterType,
                sourceBpns = listOf(golden1.businessPartnerSourceBpn)
            )
        )

        //THEN
        val expectedOutput = relationOutputFactory.fromGoldenRecord(rel1.externalId, golden1)
        assertRepo.assertRelationOutput(response, PageDto(1, 1, 0, 1, listOf(expectedOutput)))
    }

    /**
     * GIVEN a relation output that gets updated with new source and target business partners
     * WHEN output consumer searches by the new source and target BPNs
     * THEN the updated output is returned; searching by the superseded BPNs returns nothing
     */
    @Test
    fun `search updated output is findable by new source and target BPNs`() {
        //GIVEN
        val (relationInput, initialGoldenRecord) = testDataClient.createLegalEntityRelationOutput(testName, RelationType.IsManagedBy)
        val updatedGoldenRecord = testDataClient.updateLegalEntityRelationOutput(relationInput, "$testName updated", RelationType.IsOwnedBy)

        //WHEN - search by new source and target BPNs
        val responseByNewBpns = gateClient.relationOutput.postSearch(
            RelationOutputSearchRequest(
                sourceBpns = listOf(updatedGoldenRecord.businessPartnerSourceBpn),
                targetBpns = listOf(updatedGoldenRecord.businessPartnerTargetBpn)
            )
        )

        //WHEN - search by the superseded source BPN
        val responseByOldSource = gateClient.relationOutput.postSearch(
            RelationOutputSearchRequest(sourceBpns = listOf(initialGoldenRecord.businessPartnerSourceBpn))
        )

        //THEN - new BPNs find the updated output
        val expectedOutput = relationOutputFactory.fromGoldenRecord(relationInput.externalId, updatedGoldenRecord)
        assertRepo.assertRelationOutput(responseByNewBpns, PageDto(1, 1, 0, 1, listOf(expectedOutput)))

        //THEN - superseded source BPN returns nothing
        assertRepo.assertRelationOutput(responseByOldSource, PageDto(0, 0, 0, 0, emptyList()))
    }
}