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

package org.eclipse.tractusx.bpdm.gate.v6.auth

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutEntry
import org.eclipse.tractusx.bpdm.gate.api.v6.client.RelationApiClientV6
import org.eclipse.tractusx.bpdm.gate.api.v6.model.request.RelationPostRequest
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.RelationDto
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType
import org.junit.jupiter.api.Test
import java.time.Instant

class RelationAuthV6IT: GateAuthV6Test(), RelationApiClientV6 {

    @Test
    fun testGet(){
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Authorized,
            outputConsumer = AuthExpectationType.Forbidden
        ){
            get()
        }
    }

    @Test
    fun testPost(){
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Forbidden,
            outputConsumer = AuthExpectationType.Forbidden
        ){
            post(RelationPostRequest(null, RelationType.IsManagedBy, "", ""))
        }
    }

    @Test
    fun testPut(){
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Forbidden,
            outputConsumer = AuthExpectationType.Forbidden
        ){
            put(true, RelationPutEntry("", RelationType.IsManagedBy, "", ""))
        }
    }

    @Test
    fun testDelete(){
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Forbidden,
            outputConsumer = AuthExpectationType.Forbidden
        ){
            delete("")
        }
    }

    override fun get(
        externalIds: List<String>?,
        relationType: RelationType?,
        businessPartnerSourceExternalIds: List<String>?,
        businessPartnerTargetExternalIds: List<String>?,
        updatedAtFrom: Instant?,
        paginationRequest: PaginationRequest
    ): PageDto<RelationDto> {
        return gateClient.relations.get(externalIds, relationType, businessPartnerSourceExternalIds, businessPartnerTargetExternalIds, updatedAtFrom, paginationRequest)
    }

    override fun post(requestBody: RelationPostRequest): RelationDto {
        return gateClient.relations.post(requestBody)
    }

    override fun put(
        createIfNotExist: Boolean,
        requestBody: RelationPutEntry
    ): RelationDto {
        return gateClient.relations.put(createIfNotExist, requestBody)
    }

    override fun delete(externalId: String) {
        return gateClient.relations.delete(externalId)
    }
}