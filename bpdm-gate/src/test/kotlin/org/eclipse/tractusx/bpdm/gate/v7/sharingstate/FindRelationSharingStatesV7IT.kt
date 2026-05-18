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
        testDataClient.relation.upsertRelationInputWithBusinessPartners("IGNORED 1 $testName", RelationType.IsManagedBy)
        testDataClient.relation.upsertRelationInputWithBusinessPartners("IGNORED 2 $testName", RelationType.IsOwnedBy)
        val relation = testDataClient.relation.upsertRelationInputWithBusinessPartners(testName, RelationType.IsManagedBy)

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
        val rel1 = testDataClient.relation.upsertRelationInputWithBusinessPartners("1 $testName", RelationType.IsManagedBy)
        val rel2 = testDataClient.relation.upsertRelationInputWithBusinessPartners("2 $testName", RelationType.IsOwnedBy)
        val rel3 = testDataClient.relation.upsertRelationInputWithBusinessPartners("3 $testName", RelationType.IsAlternativeHeadquarterFor)

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
        val relation = testDataClient.relation.upsertRelationInputWithBusinessPartners(testName, RelationType.IsManagedBy)

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
        val relation = testDataClient.relation.createRelationInputWithRefinedLegalEntityBPs(testName)
        val createdTask = testDataClient.relation.setRelationStateToPending(relation.externalId)

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
        val relation = testDataClient.relation.createRelationInputWithRefinedLegalEntityBPs(testName)
        val refinedTask = testDataClient.relation.setRelationStateToSuccess(relation.externalId)

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
        val relation = testDataClient.relation.createRelationInputWithRefinedLegalEntityBPs(testName)
        val errorTask = testDataClient.relation.setRelationStateToError(relation.externalId, errorType = TaskRelationsErrorType.Unspecified)

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
        val relPending = testDataClient.relation.createRelationInputWithRefinedLegalEntityBPs("pending $testName")
        val createdTask = testDataClient.relation.setRelationStateToPending(relPending.externalId)

        val relReady = testDataClient.relation.upsertRelationInputWithBusinessPartners("ready $testName", RelationType.IsOwnedBy)

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
        val rel1 = testDataClient.relation.upsertRelationInputWithBusinessPartners("1 $testName", RelationType.IsManagedBy)
        val rel2 = testDataClient.relation.upsertRelationInputWithBusinessPartners("2 $testName", RelationType.IsOwnedBy)
        val rel3 = testDataClient.relation.upsertRelationInputWithBusinessPartners("3 $testName", RelationType.IsAlternativeHeadquarterFor)

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
        testDataClient.relation.upsertRelationInputWithBusinessPartners("1 $testName", RelationType.IsManagedBy)
        testDataClient.relation.upsertRelationInputWithBusinessPartners("2 $testName", RelationType.IsOwnedBy)

        //WHEN
        val response = gateClient.relationSharingState.get(paginationRequest = PaginationRequest(page = 1, size = 5))

        //THEN
        assertRepo.assertRelationSharingStatePageMetadata(response, totalElements = 2L, totalPages = 1, page = 1, contentSize = 0)
    }

    /**
     * GIVEN an IsManagedBy relation where the source has a legal entity output and the target has a site output,
     *   and the golden record task creation has been triggered
     * WHEN input consumer searches for the relation sharing state
     * THEN the sharing state shows Error because IsManagedBy requires both sides to be legal entities
     */
    @Test
    fun `IsManagedBy between legal entity source and site target leads to sharing error`() {
        //GIVEN
        val relation = testDataClient.relation.createRelationWithOutputs(testName, RelationType.IsManagedBy,
            { testDataClient.businessPartner.refineToLegalEntity(it) },
            { testDataClient.businessPartner.refineToSite(it) }
        )
        relationTaskCreationService.sendTasks()

        //WHEN
        val actual = gateClient.relationSharingState.get(externalIds = listOf(relation.externalId))

        //THEN
        val expected = expectedErrorState(relation.externalId)
        assertRepo.assertRelationSharingStates(actual, PageDto(1L, 1, 0, 1, listOf(expected)))
    }

    /**
     * GIVEN an IsManagedBy relation where the source has a site output and the target has a legal entity output,
     *   and the golden record task creation has been triggered
     * WHEN input consumer searches for the relation sharing state
     * THEN the sharing state shows Error because IsManagedBy requires both sides to be legal entities
     */
    @Test
    fun `IsManagedBy between site source and legal entity target leads to sharing error`() {
        //GIVEN
        val relation = testDataClient.relation.createRelationWithOutputs(testName, RelationType.IsManagedBy,
            { testDataClient.businessPartner.refineToSite(it) },
            { testDataClient.businessPartner.refineToLegalEntity(it) }
        )
        relationTaskCreationService.sendTasks()

        //WHEN
        val actual = gateClient.relationSharingState.get(externalIds = listOf(relation.externalId))

        //THEN
        val expected = expectedErrorState(relation.externalId)
        assertRepo.assertRelationSharingStates(actual, PageDto(1L, 1, 0, 1, listOf(expected)))
    }

    /**
     * GIVEN an IsManagedBy relation where the source has a legal entity output and the target has an additional address output,
     *   and the golden record task creation has been triggered
     * WHEN input consumer searches for the relation sharing state
     * THEN the sharing state shows Error because IsManagedBy requires both sides to be legal entities
     */
    @Test
    fun `IsManagedBy between legal entity source and additional address target leads to sharing error`() {
        //GIVEN
        val relation = testDataClient.relation.createRelationWithOutputs(testName, RelationType.IsManagedBy,
            { testDataClient.businessPartner.refineToLegalEntity(it) },
            { testDataClient.businessPartner.refineToAdditionalAddressOfSite(it) }
        )
        relationTaskCreationService.sendTasks()

        //WHEN
        val actual = gateClient.relationSharingState.get(externalIds = listOf(relation.externalId))

        //THEN
        val expected = expectedErrorState(relation.externalId)
        assertRepo.assertRelationSharingStates(actual, PageDto(1L, 1, 0, 1, listOf(expected)))
    }

    /**
     * GIVEN an IsManagedBy relation where the source has an additional address output and the target has a legal entity output,
     *   and the golden record task creation has been triggered
     * WHEN input consumer searches for the relation sharing state
     * THEN the sharing state shows Error because IsManagedBy requires both sides to be legal entities
     */
    @Test
    fun `IsManagedBy between additional address source and legal entity target leads to sharing error`() {
        //GIVEN
        val relation = testDataClient.relation.createRelationWithOutputs(testName, RelationType.IsManagedBy,
            { testDataClient.businessPartner.refineToAdditionalAddressOfSite(it) },
            { testDataClient.businessPartner.refineToLegalEntity(it) }
        )
        relationTaskCreationService.sendTasks()

        //WHEN
        val actual = gateClient.relationSharingState.get(externalIds = listOf(relation.externalId))

        //THEN
        val expected = expectedErrorState(relation.externalId)
        assertRepo.assertRelationSharingStates(actual, PageDto(1L, 1, 0, 1, listOf(expected)))
    }

    /**
     * GIVEN an IsOwnedBy relation where both source and target have site outputs,
     *   and the golden record task creation has been triggered
     * WHEN input consumer searches for the relation sharing state
     * THEN the sharing state shows Error because IsOwnedBy requires both sides to be legal entities
     */
    @Test
    fun `IsOwnedBy between two sites leads to sharing error`() {
        //GIVEN
        val relation = testDataClient.relation.createRelationWithOutputs(testName, RelationType.IsOwnedBy,
            { testDataClient.businessPartner.refineToSite(it) },
            { testDataClient.businessPartner.refineToSite(it) }
        )
        relationTaskCreationService.sendTasks()

        //WHEN
        val actual = gateClient.relationSharingState.get(externalIds = listOf(relation.externalId))

        //THEN
        val expected = expectedErrorState(relation.externalId)
        assertRepo.assertRelationSharingStates(actual, PageDto(1L, 1, 0, 1, listOf(expected)))
    }

    /**
     * GIVEN an IsAlternativeHeadquarterFor relation where both source and target have additional address outputs,
     *   and the golden record task creation has been triggered
     * WHEN input consumer searches for the relation sharing state
     * THEN the sharing state shows Error because IsAlternativeHeadquarterFor requires both sides to be legal entities
     */
    @Test
    fun `IsAlternativeHeadquarterFor between two additional addresses leads to sharing error`() {
        //GIVEN
        val relation = testDataClient.relation.createRelationWithOutputs(testName, RelationType.IsAlternativeHeadquarterFor,
            { testDataClient.businessPartner.refineToAdditionalAddressOfSite(it) },
            { testDataClient.businessPartner.refineToAdditionalAddressOfSite(it) }
        )
        relationTaskCreationService.sendTasks()

        //WHEN
        val actual = gateClient.relationSharingState.get(externalIds = listOf(relation.externalId))

        //THEN
        val expected = expectedErrorState(relation.externalId)
        assertRepo.assertRelationSharingStates(actual, PageDto(1L, 1, 0, 1, listOf(expected)))
    }

    /**
     * GIVEN an IsReplacedBy relation where both source and target have legal entity outputs,
     *   and the golden record task creation has been triggered
     * WHEN input consumer searches for the relation sharing state
     * THEN the sharing state shows Error because IsReplacedBy requires one side to be a legal entity and the other an additional address
     */
    @Test
    fun `IsReplacedBy between two legal entities leads to sharing error`() {
        //GIVEN
        val relation = testDataClient.relation.createRelationWithOutputs(testName, RelationType.IsReplacedBy,
            { testDataClient.businessPartner.refineToLegalEntity(it) },
            { testDataClient.businessPartner.refineToLegalEntity(it) }
        )
        relationTaskCreationService.sendTasks()

        //WHEN
        val actual = gateClient.relationSharingState.get(externalIds = listOf(relation.externalId))

        //THEN
        val expected = expectedErrorState(relation.externalId)
        assertRepo.assertRelationSharingStates(actual, PageDto(1L, 1, 0, 1, listOf(expected)))
    }

    /**
     * GIVEN an IsReplacedBy relation where the source has a site output and the target has an additional address output,
     *   and the golden record task creation has been triggered
     * WHEN input consumer searches for the relation sharing state
     * THEN the sharing state shows Error because IsReplacedBy requires the non-additional side to be a legal entity, not a site
     */
    @Test
    fun `IsReplacedBy between site source and additional address target leads to sharing error`() {
        //GIVEN
        val relation = testDataClient.relation.createRelationWithOutputs(testName, RelationType.IsReplacedBy,
            { testDataClient.businessPartner.refineToSite(it) },
            { testDataClient.businessPartner.refineToAdditionalAddressOfSite(it) }
        )
        relationTaskCreationService.sendTasks()

        //WHEN
        val actual = gateClient.relationSharingState.get(externalIds = listOf(relation.externalId))

        //THEN
        val expected = expectedErrorState(relation.externalId)
        assertRepo.assertRelationSharingStates(actual, PageDto(1L, 1, 0, 1, listOf(expected)))
    }

    /**
     * GIVEN an IsReplacedBy relation where the source has a legal entity output and the target has a site output,
     *   and the golden record task creation has been triggered
     * WHEN input consumer searches for the relation sharing state
     * THEN the sharing state shows Error because IsReplacedBy requires the non-legal-entity side to be an additional address, not a site
     */
    @Test
    fun `IsReplacedBy between legal entity source and site target leads to sharing error`() {
        //GIVEN
        val relation = testDataClient.relation.createRelationWithOutputs(testName, RelationType.IsReplacedBy,
            { testDataClient.businessPartner.refineToLegalEntity(it) },
            { testDataClient.businessPartner.refineToSite(it) }
        )
        relationTaskCreationService.sendTasks()

        //WHEN
        val actual = gateClient.relationSharingState.get(externalIds = listOf(relation.externalId))

        //THEN
        val expected = expectedErrorState(relation.externalId)
        assertRepo.assertRelationSharingStates(actual, PageDto(1L, 1, 0, 1, listOf(expected)))
    }

    /**
     * GIVEN an IsReplacedBy relation where both source and target have additional address outputs,
     *   and the golden record task creation has been triggered
     * WHEN input consumer searches for the relation sharing state
     * THEN the sharing state shows Error because IsReplacedBy requires one side to be a legal entity
     */
    @Test
    fun `IsReplacedBy between two additional addresses leads to sharing error`() {
        //GIVEN
        val relation = testDataClient.relation.createRelationWithOutputs(testName, RelationType.IsReplacedBy,
            { testDataClient.businessPartner.refineToAdditionalAddressOfSite(it) },
            { testDataClient.businessPartner.refineToAdditionalAddressOfSite(it) }
        )
        relationTaskCreationService.sendTasks()

        //WHEN
        val actual = gateClient.relationSharingState.get(externalIds = listOf(relation.externalId))

        //THEN
        val expected = expectedErrorState(relation.externalId)
        assertRepo.assertRelationSharingStates(actual, PageDto(1L, 1, 0, 1, listOf(expected)))
    }

    private fun expectedErrorState(externalId: String) = RelationSharingStateDto(
        externalId = externalId,
        sharingStateType = RelationSharingStateType.Error,
        sharingErrorCode = RelationSharingStateErrorCode.SharingProcessError,
        taskId = null,
        updatedAt = Instant.MIN
    )
}
