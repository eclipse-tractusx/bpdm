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

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.RelationDto
import org.eclipse.tractusx.bpdm.gate.v6.GateUnscheduledInitialStartV6Test
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.web.reactive.function.client.WebClientResponseException

class RelationDeletionV6IT: GateUnscheduledInitialStartV6Test() {

    /**
     * GIVEN existing relation
     * WHEN input manager requests deletion of relation
     * THEN input manager sees relation deleted
     */
    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `delete existing relation`(relationType: RelationType){
        // GIVEN
        val givenRelation = testDataClient.createRelationWithBusinessPartners(testName, relationType)

        //WHEN
        gateClient.relations.delete(givenRelation.externalId)

        //THEN
        val searchResponse = gateClient.relations.get(externalIds = listOf(givenRelation.externalId))
        val expected = PageDto<RelationDto>(0, 0, 0, 0, emptyList())

        assertRepo.assertRelations(searchResponse, expected)
    }


    /**
     * WHEN input manager requests deletion of not existing relation
     * THEN input manager sees 400 HTTP error response
     */
    @Test
    fun `try delete not-existing relation`(){
        //WHEN
        val request: () -> Unit  = { gateClient.relations.delete("NOT EXISTING") }

        //THEN
        Assertions.assertThatThrownBy(request).isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }
}