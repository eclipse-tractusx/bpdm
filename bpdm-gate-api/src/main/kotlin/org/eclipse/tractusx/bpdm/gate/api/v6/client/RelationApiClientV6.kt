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

package org.eclipse.tractusx.bpdm.gate.api.v6.client

import io.swagger.v3.oas.annotations.Operation
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.ApiCommons
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.api.v6.GateRelationApi
import org.eclipse.tractusx.bpdm.gate.api.v6.model.request.RelationPostRequest
import org.eclipse.tractusx.bpdm.gate.api.v6.model.request.RelationPutEntryV6
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.RelationDto
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.DeleteExchange
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.service.annotation.PutExchange
import java.time.Instant

interface RelationApiClientV6: GateRelationApi {


    @GetExchange(value = ApiCommons.RELATIONS_INPUT_PATH_V6)
    override fun get(
        @RequestParam externalIds: List<String>?,
        @RequestParam relationType: RelationType?,
        @RequestParam businessPartnerSourceExternalIds: List<String>?,
        @RequestParam businessPartnerTargetExternalIds: List<String>?,
        @RequestParam updatedAtFrom: Instant?,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<RelationDto>

    @Operation(
        summary = "Create a new business partner input relation",
        description = "Create a new relation between two business partner entries on the input stage. " +
                "The external identifier is optional and a new one will be automatically created if not given. " +
                "A given external identifier has to be unique."
    )

    @PostExchange(value = ApiCommons.RELATIONS_INPUT_PATH_V6)
    override fun post(@RequestBody requestBody: RelationPostRequest): RelationDto

    @PutExchange(value = ApiCommons.RELATIONS_INPUT_PATH_V6)
    override fun put(
        @RequestParam createIfNotExist: Boolean,
        @RequestBody requestBody: RelationPutEntryV6
    ): RelationDto

    @DeleteExchange(value = ApiCommons.RELATIONS_INPUT_PATH_V6)
    override fun delete(@RequestParam externalId: String)
}