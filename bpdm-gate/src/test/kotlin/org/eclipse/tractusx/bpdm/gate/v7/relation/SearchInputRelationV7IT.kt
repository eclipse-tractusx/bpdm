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
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.model.RelationDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationSearchRequest
import org.eclipse.tractusx.bpdm.gate.v7.UnscheduledGateTestBaseV7
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.other
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.Instant

class SearchInputRelationV7IT : UnscheduledGateTestBaseV7() {

    /**
     * GIVEN three upserted relations
     * WHEN input consumer searches without filters
     * THEN all three relations are returned
     */
    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `find all upserted relations`(relationType: RelationType) {
        //GIVEN
        val rel1 = testDataClient.upsertRelationInputWithBusinessPartners("$testName 1", relationType)
        val rel2 = testDataClient.upsertRelationInputWithBusinessPartners("$testName 2", relationType)
        val rel3 = testDataClient.upsertRelationInputWithBusinessPartners("$testName 3", relationType)

        //WHEN
        val response = gateClient.relation.postSearch()

        //THEN
        val expected = PageDto(3L, 1, 0, 3, listOf(rel1, rel2, rel3))
        assertRepo.assertRelations(response, expected)
    }

    /**
     * GIVEN no upserted relations
     * WHEN input consumer searches without filters
     * THEN an empty page is returned
     */
    @Test
    fun `find no relations when none exist`() {
        //WHEN
        val response = gateClient.relation.postSearch()

        //THEN
        val expected = PageDto<RelationDto>(0L, 0, 0, 0, emptyList())
        assertRepo.assertRelations(response, expected)
    }

    /**
     * GIVEN three upserted relations
     * WHEN input consumer searches by a single external ID
     * THEN only the relation with that external ID is returned
     */
    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `find relation by external ID`(relationType: RelationType) {
        //GIVEN
        val inquired = testDataClient.upsertRelationInputWithBusinessPartners("$testName 1", relationType)
        testDataClient.upsertRelationInputWithBusinessPartners("$testName 2", relationType)
        testDataClient.upsertRelationInputWithBusinessPartners("$testName 3", relationType)

        //WHEN
        val response = gateClient.relation.postSearch(RelationSearchRequest(externalIds = listOf(inquired.externalId)))

        //THEN
        val expected = PageDto(1L, 1, 0, 1, listOf(inquired))
        assertRepo.assertRelations(response, expected)
    }

    /**
     * GIVEN three upserted relations
     * WHEN input consumer searches by two external IDs
     * THEN only the two matching relations are returned
     */
    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `find relations by multiple external IDs`(relationType: RelationType) {
        //GIVEN
        val rel1 = testDataClient.upsertRelationInputWithBusinessPartners("$testName 1", relationType)
        val rel2 = testDataClient.upsertRelationInputWithBusinessPartners("$testName 2", relationType)
        testDataClient.upsertRelationInputWithBusinessPartners("$testName 3", relationType)

        //WHEN
        val response = gateClient.relation.postSearch(
            RelationSearchRequest(externalIds = listOf(rel1.externalId, rel2.externalId))
        )

        //THEN
        val expected = PageDto(2L, 1, 0, 2, listOf(rel1, rel2))
        assertRepo.assertRelations(response, expected)
    }

    /**
     * GIVEN relations of different types
     * WHEN input consumer filters by a specific relation type
     * THEN only relations of that type are returned
     */
    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `find relations by relation type`(relationType: RelationType) {
        //GIVEN
        val inquired = testDataClient.upsertRelationInputWithBusinessPartners("$testName 1", relationType)
        testDataClient.upsertRelationInputWithBusinessPartners("$testName 2", relationType.other())
        testDataClient.upsertRelationInputWithBusinessPartners("$testName 3", relationType.other())

        //WHEN
        val response = gateClient.relation.postSearch(RelationSearchRequest(relationType = relationType))

        //THEN
        val expected = PageDto(1L, 1, 0, 1, listOf(inquired))
        assertRepo.assertRelations(response, expected)
    }

    /**
     * GIVEN three upserted relations with distinct sources
     * WHEN input consumer filters by a single source external ID
     * THEN only the relation with that source is returned
     */
    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `find relation by source external ID`(relationType: RelationType) {
        //GIVEN
        val inquired = testDataClient.upsertRelationInputWithBusinessPartners("$testName 1", relationType)
        testDataClient.upsertRelationInputWithBusinessPartners("$testName 2", relationType)
        testDataClient.upsertRelationInputWithBusinessPartners("$testName 3", relationType)

        //WHEN
        val response = gateClient.relation.postSearch(
            RelationSearchRequest(businessPartnerSourceExternalIds = listOf(inquired.businessPartnerSourceExternalId))
        )

        //THEN
        val expected = PageDto(1L, 1, 0, 1, listOf(inquired))
        assertRepo.assertRelations(response, expected)
    }

    /**
     * GIVEN three upserted relations with distinct sources
     * WHEN input consumer filters by two source external IDs
     * THEN only the two matching relations are returned
     */
    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `find relations by multiple source external IDs`(relationType: RelationType) {
        //GIVEN
        val rel1 = testDataClient.upsertRelationInputWithBusinessPartners("$testName 1", relationType)
        val rel2 = testDataClient.upsertRelationInputWithBusinessPartners("$testName 2", relationType)
        testDataClient.upsertRelationInputWithBusinessPartners("$testName 3", relationType)

        //WHEN
        val response = gateClient.relation.postSearch(
            RelationSearchRequest(
                businessPartnerSourceExternalIds = listOf(
                    rel1.businessPartnerSourceExternalId,
                    rel2.businessPartnerSourceExternalId
                )
            )
        )

        //THEN
        val expected = PageDto(2L, 1, 0, 2, listOf(rel1, rel2))
        assertRepo.assertRelations(response, expected)
    }

    /**
     * GIVEN three upserted relations with distinct targets
     * WHEN input consumer filters by a single target external ID
     * THEN only the relation with that target is returned
     */
    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `find relation by target external ID`(relationType: RelationType) {
        //GIVEN
        val inquired = testDataClient.upsertRelationInputWithBusinessPartners("$testName 1", relationType)
        testDataClient.upsertRelationInputWithBusinessPartners("$testName 2", relationType)
        testDataClient.upsertRelationInputWithBusinessPartners("$testName 3", relationType)

        //WHEN
        val response = gateClient.relation.postSearch(
            RelationSearchRequest(businessPartnerTargetExternalIds = listOf(inquired.businessPartnerTargetExternalId))
        )

        //THEN
        val expected = PageDto(1L, 1, 0, 1, listOf(inquired))
        assertRepo.assertRelations(response, expected)
    }

    /**
     * GIVEN three upserted relations with distinct targets
     * WHEN input consumer filters by two target external IDs
     * THEN only the two matching relations are returned
     */
    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `find relations by multiple target external IDs`(relationType: RelationType) {
        //GIVEN
        val rel1 = testDataClient.upsertRelationInputWithBusinessPartners("$testName 1", relationType)
        val rel2 = testDataClient.upsertRelationInputWithBusinessPartners("$testName 2", relationType)
        testDataClient.upsertRelationInputWithBusinessPartners("$testName 3", relationType)

        //WHEN
        val response = gateClient.relation.postSearch(
            RelationSearchRequest(
                businessPartnerTargetExternalIds = listOf(
                    rel1.businessPartnerTargetExternalId,
                    rel2.businessPartnerTargetExternalId
                )
            )
        )

        //THEN
        val expected = PageDto(2L, 1, 0, 2, listOf(rel1, rel2))
        assertRepo.assertRelations(response, expected)
    }

    /**
     * GIVEN relations created before and after a timestamp
     * WHEN input consumer filters by updatedAtFrom set to that timestamp
     * THEN only relations created after the timestamp are returned
     */
    @Test
    fun `find relations updated after timestamp`() {
        //GIVEN
        testDataClient.upsertRelationInputWithBusinessPartners("$testName old 1", RelationType.IsManagedBy)
        testDataClient.upsertRelationInputWithBusinessPartners("$testName old 2", RelationType.IsOwnedBy)

        val updatedAtFrom = Instant.now()

        val new1 = testDataClient.upsertRelationInputWithBusinessPartners("$testName new 1", RelationType.IsManagedBy)
        val new2 = testDataClient.upsertRelationInputWithBusinessPartners("$testName new 2", RelationType.IsOwnedBy)

        //WHEN
        val response = gateClient.relation.postSearch(RelationSearchRequest(updatedAtFrom = updatedAtFrom))

        //THEN
        val expected = PageDto(2L, 1, 0, 2, listOf(new1, new2))
        assertRepo.assertRelations(response, expected)
    }

    /**
     * GIVEN several relations with varying types, sources and targets
     * WHEN input consumer applies multiple filters simultaneously
     * THEN only the single relation matching all criteria is returned
     */
    @Test
    fun `find relation using combined filters`() {
        //GIVEN
        val rel1 = testDataClient.upsertRelationInputWithBusinessPartners("$testName 1", RelationType.IsManagedBy)
        val rel2 = testDataClient.upsertRelationInputWithBusinessPartners("$testName 2", RelationType.IsOwnedBy)
        val rel3 = testDataClient.upsertRelationInputWithBusinessPartners("$testName 3", RelationType.IsManagedBy)

        //WHEN
        val response = gateClient.relation.postSearch(
            RelationSearchRequest(
                externalIds = listOf(rel1.externalId, rel3.externalId),
                relationType = RelationType.IsManagedBy,
                businessPartnerSourceExternalIds = listOf(rel1.businessPartnerSourceExternalId)
            )
        )

        //THEN
        val expected = PageDto(1L, 1, 0, 1, listOf(rel1))
        assertRepo.assertRelations(response, expected)
    }

    /**
     * GIVEN three upserted relations
     * WHEN input consumer requests the first page with page size 2 and the second page
     * THEN each page returns correct metadata and together they contain all three relations
     */
    @Test
    fun `paginate relations across multiple pages`() {
        //GIVEN
        val rel1 = testDataClient.upsertRelationInputWithBusinessPartners("$testName 1", RelationType.IsManagedBy)
        val rel2 = testDataClient.upsertRelationInputWithBusinessPartners("$testName 2", RelationType.IsOwnedBy)
        val rel3 = testDataClient.upsertRelationInputWithBusinessPartners("$testName 3", RelationType.IsAlternativeHeadquarterFor)

        //WHEN
        val page0 = gateClient.relation.postSearch(paginationRequest = PaginationRequest(page = 0, size = 2))
        val page1 = gateClient.relation.postSearch(paginationRequest = PaginationRequest(page = 1, size = 2))

        //THEN
        assertRepo.assertRelationPageMetadata(page0, totalElements = 3L, totalPages = 2, page = 0, contentSize = 2)
        assertRepo.assertRelationPageMetadata(page1, totalElements = 3L, totalPages = 2, page = 1, contentSize = 1)
        assertRepo.assertRelations(page0.content + page1.content, listOf(rel1, rel2, rel3))
    }

    /**
     * GIVEN two upserted relations
     * WHEN input consumer requests a page beyond the available data
     * THEN an empty page with correct total metadata is returned
     */
    @Test
    fun `get page beyond available relations returns empty`() {
        //GIVEN
        testDataClient.upsertRelationInputWithBusinessPartners("$testName 1", RelationType.IsManagedBy)
        testDataClient.upsertRelationInputWithBusinessPartners("$testName 2", RelationType.IsOwnedBy)

        //WHEN
        val response = gateClient.relation.postSearch(paginationRequest = PaginationRequest(page = 1, size = 5))

        //THEN
        assertRepo.assertRelationPageMetadata(response, totalElements = 2L, totalPages = 1, page = 1, contentSize = 0)
    }
}
