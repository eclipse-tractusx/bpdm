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

package org.eclipse.tractusx.bpdm.gate.v6.relation

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.v6.GateUnscheduledInitialStartV6Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.Instant

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
class RelationSearchV6IT: GateUnscheduledInitialStartV6Test() {

    /**
     * GIVEN relations
     * WHEN input consumer searches for relations
     * THEN input consumer sees the relations
     */
    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `find relations`(relationType: RelationType) {
        //GIVEN
        val relation1 = testDataClient.createRelationWithBusinessPartners("$testName 1", relationType)
        val relation2 = testDataClient.createRelationWithBusinessPartners("$testName 2", relationType)
        val relation3 = testDataClient.createRelationWithBusinessPartners("$testName 3", relationType)

        //WHEN
        val response = gateClient.relations.get()

        //THEN
        val expected = PageDto(3, 1, 0, 3, listOf(relation1, relation2, relation3))

        assertRepo.assertRelations(response, expected)
    }

    /**
     * GIVEN relations
     * WHEN input consumer searches for relation by external-ID
     * THEN input consumer sees only relation with that external-ID
     */
    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `find created relation by external-ID`(relationType: RelationType) {
        //GIVEN
        val inquiredRelation = testDataClient.createRelationWithBusinessPartners("$testName 1", relationType)
        testDataClient.createRelationWithBusinessPartners("$testName 2", relationType)
        testDataClient.createRelationWithBusinessPartners("$testName 3", relationType)

        //WHEN
        val response = gateClient.relations.get(externalIds = listOf(inquiredRelation.externalId))

        //THEN
        val expected = PageDto(1, 1, 0, 1, listOf(inquiredRelation))

        assertRepo.assertRelations(response, expected)
    }

    /**
     * GIVEN relations
     * WHEN input consumer searches for relation by relation type
     * THEN input consumer sees only relation with that relation type
     */
    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `find created relation by relation type`(relationType: RelationType) {
        //GIVEN
        val inquiredRelation = testDataClient.createRelationWithBusinessPartners("$testName 1", relationType)
        testDataClient.createRelationWithBusinessPartners("$testName 2", relationType.toNewType())
        testDataClient.createRelationWithBusinessPartners("$testName 3", relationType.toNewType())

        //WHEN
        val response = gateClient.relations.get(relationType = inquiredRelation.relationType)

        //THEN
        val expected = PageDto(1, 1, 0, 1, listOf(inquiredRelation))

        assertRepo.assertRelations(response, expected)
    }

    /**
     * GIVEN relations
     * WHEN input consumer searches for relation by source
     * THEN input consumer sees only relation with that source
     */
    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `find created relation by source`(relationType: RelationType) {
        //GIVEN
        val inquiredRelation = testDataClient.createRelationWithBusinessPartners("$testName 1", relationType)
        testDataClient.createRelationWithBusinessPartners("$testName 2", relationType.toNewType())
        testDataClient.createRelationWithBusinessPartners("$testName 3", relationType.toNewType())

        //WHEN
        val response = gateClient.relations.get(businessPartnerSourceExternalIds = listOf(inquiredRelation.businessPartnerSourceExternalId))

        //THEN
        val expected = PageDto(1, 1, 0, 1, listOf(inquiredRelation))

        assertRepo.assertRelations(response, expected)
    }

    /**
     * GIVEN relations
     * WHEN input consumer searches for relation by target
     * THEN input consumer sees only relation with that target
     */
    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `find created relation by target`(relationType: RelationType) {
        //GIVEN
        val inquiredRelation = testDataClient.createRelationWithBusinessPartners("$testName 1", relationType)
        testDataClient.createRelationWithBusinessPartners("$testName 2", relationType.toNewType())
        testDataClient.createRelationWithBusinessPartners("$testName 3", relationType.toNewType())

        //WHEN
        val response = gateClient.relations.get(businessPartnerTargetExternalIds = listOf(inquiredRelation.businessPartnerTargetExternalId))

        //THEN
        val expected = PageDto(1, 1, 0, 1, listOf(inquiredRelation))

        assertRepo.assertRelations(response, expected)
    }

    /**
     * GIVEN relations
     * WHEN input consumer searches for relation updated after time X
     * THEN input consumer sees only relation updated after that time
     */
    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `find created relation by updatedAt time`(relationType: RelationType) {
        //GIVEN
        testDataClient.createRelationWithBusinessPartners("$testName 2", relationType.toNewType())
        testDataClient.createRelationWithBusinessPartners("$testName 3", relationType.toNewType())

        val timeX = Instant.now()
        val inquiredRelation = testDataClient.createRelationWithBusinessPartners("$testName 1", relationType)

        //WHEN
        val response = gateClient.relations.get(updatedAtFrom = timeX)
        //THEN
        val expected = PageDto(1, 1, 0, 1, listOf(inquiredRelation))

        assertRepo.assertRelations(response, expected)
    }

    private fun RelationType.toNewType() = RelationType.entries.find { it != this }!!
}