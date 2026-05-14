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

package org.eclipse.tractusx.bpdm.gate.v7.sharingstate

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateErrorCode
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.v7.UnscheduledGateTestBaseV7
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsErrorType
import org.junit.jupiter.api.Test
import java.time.Instant

class FindRelationSharingStatesV7IT : UnscheduledGateTestBaseV7() {

    /**
     * GIVEN three upserted relations
     * WHEN input consumer searches for relation sharing state by external ID
     * THEN only the sharing state for that relation is returned
     */
    @Test
    fun `find relation sharing state by external id`() {
        //GIVEN
        testDataClient.upsertRelationInputWithBusinessPartners("IGNORED 1 $testName", RelationType.IsManagedBy)
        testDataClient.upsertRelationInputWithBusinessPartners("IGNORED 2 $testName", RelationType.IsOwnedBy)
        val relation = testDataClient.upsertRelationInputWithBusinessPartners(testName, RelationType.IsManagedBy)

        //WHEN
        val actualResponse = gateClient.relationSharingState.get(externalIds = listOf(relation.externalId))

        //THEN
        val expected = RelationSharingStateDto(
            externalId = relation.externalId,
            sharingStateType = RelationSharingStateType.Ready,
            taskId = null,
            updatedAt = Instant.MIN
        )
        val expectedResponse = PageDto(1L, 1, 0, 1, listOf(expected))
        assertRepo.assertRelationSharingStates(actualResponse, expectedResponse)
    }

    /**
     * GIVEN three upserted relations
     * WHEN input consumer searches for sharing states by all three external IDs
     * THEN all three relation sharing states are returned
     */
    @Test
    fun `find relation sharing states by multiple external ids`() {
        //GIVEN
        val rel1 = testDataClient.upsertRelationInputWithBusinessPartners("1 $testName", RelationType.IsManagedBy)
        val rel2 = testDataClient.upsertRelationInputWithBusinessPartners("2 $testName", RelationType.IsOwnedBy)
        val rel3 = testDataClient.upsertRelationInputWithBusinessPartners("3 $testName", RelationType.IsAlternativeHeadquarterFor)

        //WHEN
        val actualResponse = gateClient.relationSharingState.get(
            externalIds = listOf(rel1.externalId, rel2.externalId, rel3.externalId)
        )

        //THEN
        val expectedStates = listOf(rel1, rel2, rel3).map { rel ->
            RelationSharingStateDto(
                externalId = rel.externalId,
                sharingStateType = RelationSharingStateType.Ready,
                taskId = null,
                updatedAt = Instant.MIN
            )
        }
        val expectedResponse = PageDto(3L, 1, 0, 3, expectedStates)
        assertRepo.assertRelationSharingStates(actualResponse, expectedResponse)
    }

    /**
     * GIVEN a relation in Ready state
     * WHEN input consumer searches relation sharing states
     * THEN the relation's sharing state shows Ready
     */
    @Test
    fun `find ready relation sharing state`() {
        //GIVEN
        val relation = testDataClient.upsertRelationInputWithBusinessPartners(testName, RelationType.IsManagedBy)

        //WHEN
        val actualResponse = gateClient.relationSharingState.get()

        //THEN
        val expected = RelationSharingStateDto(
            externalId = relation.externalId,
            sharingStateType = RelationSharingStateType.Ready,
            taskId = null,
            updatedAt = Instant.MIN
        )
        val expectedResponse = PageDto(1L, 1, 0, 1, listOf(expected))
        assertRepo.assertRelationSharingStates(actualResponse, expectedResponse)
    }

    /**
     * GIVEN a relation moved to Pending state (with source/target BP outputs present)
     * WHEN input consumer searches relation sharing states
     * THEN the relation's sharing state shows Pending with the orchestrator task ID
     */
    @Test
    fun `find pending relation sharing state`() {
        //GIVEN
        val relation = testDataClient.createRelationInputWithRefinedLegalEntityBPs(testName)
        val createdTask = testDataClient.setRelationStateToPending(relation.externalId)

        //WHEN
        val actualResponse = gateClient.relationSharingState.get()

        //THEN
        val expected = RelationSharingStateDto(
            externalId = relation.externalId,
            sharingStateType = RelationSharingStateType.Pending,
            taskId = createdTask.taskId,
            updatedAt = Instant.MIN
        )
        val expectedResponse = PageDto(1L, 1, 0, 1, listOf(expected))
        assertRepo.assertRelationSharingStates(actualResponse, expectedResponse)
    }

    /**
     * GIVEN a relation refined to Success state (with source/target BP outputs present)
     * WHEN input consumer searches relation sharing states
     * THEN the relation's sharing state shows Success with the orchestrator task ID
     */
    @Test
    fun `find success relation sharing state`() {
        //GIVEN
        val relation = testDataClient.createRelationInputWithRefinedLegalEntityBPs(testName)
        val refinedTask = testDataClient.setRelationStateToSuccess(relation.externalId)

        //WHEN
        val actualResponse = gateClient.relationSharingState.get()

        //THEN
        val expected = RelationSharingStateDto(
            externalId = relation.externalId,
            sharingStateType = RelationSharingStateType.Success,
            taskId = refinedTask.taskId,
            updatedAt = Instant.MIN
        )
        val expectedResponse = PageDto(1L, 1, 0, 1, listOf(expected))
        assertRepo.assertRelationSharingStates(actualResponse, expectedResponse)
    }

    /**
     * GIVEN a relation moved to Error state (with source/target BP outputs present)
     * WHEN input consumer searches relation sharing states
     * THEN the relation's sharing state shows Error with the error code and message
     */
    @Test
    fun `find error relation sharing state`() {
        //GIVEN
        val relation = testDataClient.createRelationInputWithRefinedLegalEntityBPs(testName)
        val errorTask = testDataClient.setRelationStateToError(relation.externalId, errorType = TaskRelationsErrorType.Unspecified)

        //WHEN
        val actualResponse = gateClient.relationSharingState.get()

        //THEN
        val expected = RelationSharingStateDto(
            externalId = relation.externalId,
            sharingStateType = RelationSharingStateType.Error,
            sharingErrorCode = RelationSharingStateErrorCode.SharingProcessError,
            sharingErrorMessage = errorTask.processingState.errors.single().description,
            taskId = errorTask.taskId,
            updatedAt = Instant.MIN
        )
        val expectedResponse = PageDto(1L, 1, 0, 1, listOf(expected))
        assertRepo.assertRelationSharingStates(actualResponse, expectedResponse)
    }

    /**
     * GIVEN relations in Pending and Ready states
     * WHEN input consumer filters by a specific sharing state type
     * THEN only relations in that state are returned
     */
    @Test
    fun `filter relation sharing states by sharing state type`() {
        //GIVEN - create relPending with refined BP outputs, move it to Pending, then create relReady (stays Ready)
        val relPending = testDataClient.createRelationInputWithRefinedLegalEntityBPs("pending $testName")
        val createdTask = testDataClient.setRelationStateToPending(relPending.externalId)

        val relReady = testDataClient.upsertRelationInputWithBusinessPartners("ready $testName", RelationType.IsOwnedBy)

        //WHEN filter by Pending
        val pendingResponse = gateClient.relationSharingState.get(sharingStateTypes = listOf(RelationSharingStateType.Pending))

        //THEN only the Pending relation is returned
        val expectedPending = RelationSharingStateDto(
            externalId = relPending.externalId,
            sharingStateType = RelationSharingStateType.Pending,
            taskId = createdTask.taskId,
            updatedAt = Instant.MIN
        )
        assertRepo.assertRelationSharingStates(pendingResponse, PageDto(1L, 1, 0, 1, listOf(expectedPending)))

        //WHEN filter by Ready
        val readyResponse = gateClient.relationSharingState.get(sharingStateTypes = listOf(RelationSharingStateType.Ready))

        //THEN only the Ready relation is returned
        val expectedReady = RelationSharingStateDto(
            externalId = relReady.externalId,
            sharingStateType = RelationSharingStateType.Ready,
            taskId = null,
            updatedAt = Instant.MIN
        )
        assertRepo.assertRelationSharingStates(readyResponse, PageDto(1L, 1, 0, 1, listOf(expectedReady)))
    }

    /**
     * GIVEN three upserted relations
     * WHEN input consumer requests sharing states with page size 2
     * THEN pages are returned with correct metadata and all three relations across both pages
     */
    @Test
    fun `paginate relation sharing states across multiple pages`() {
        //GIVEN
        val rel1 = testDataClient.upsertRelationInputWithBusinessPartners("1 $testName", RelationType.IsManagedBy)
        val rel2 = testDataClient.upsertRelationInputWithBusinessPartners("2 $testName", RelationType.IsOwnedBy)
        val rel3 = testDataClient.upsertRelationInputWithBusinessPartners("3 $testName", RelationType.IsAlternativeHeadquarterFor)

        //WHEN
        val page0 = gateClient.relationSharingState.get(paginationRequest = PaginationRequest(page = 0, size = 2))
        val page1 = gateClient.relationSharingState.get(paginationRequest = PaginationRequest(page = 1, size = 2))

        //THEN
        assertRepo.assertRelationSharingStatePageMetadata(page0, totalElements = 3L, totalPages = 2, page = 0, contentSize = 2)
        assertRepo.assertRelationSharingStatePageMetadata(page1, totalElements = 3L, totalPages = 2, page = 1, contentSize = 1)

        val allReturned = page0.content + page1.content
        val expectedStates = listOf(rel1, rel2, rel3).map { rel ->
            RelationSharingStateDto(
                externalId = rel.externalId,
                sharingStateType = RelationSharingStateType.Ready,
                taskId = null,
                updatedAt = Instant.MIN
            )
        }
        assertRepo.assertRelationSharingStates(allReturned, expectedStates)
    }

    /**
     * GIVEN two upserted relations
     * WHEN input consumer requests a page beyond the available data
     * THEN an empty page with correct total metadata is returned
     */
    @Test
    fun `get page beyond available relation sharing states returns empty`() {
        //GIVEN
        testDataClient.upsertRelationInputWithBusinessPartners("1 $testName", RelationType.IsManagedBy)
        testDataClient.upsertRelationInputWithBusinessPartners("2 $testName", RelationType.IsOwnedBy)

        //WHEN
        val response = gateClient.relationSharingState.get(paginationRequest = PaginationRequest(page = 1, size = 5))

        //THEN
        assertRepo.assertRelationSharingStatePageMetadata(response, totalElements = 2L, totalPages = 1, page = 1, contentSize = 0)
    }
}
