/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.gate.api.client

import jakarta.validation.Valid
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.GateRelationApi
import org.eclipse.tractusx.bpdm.gate.api.model.RelationDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPostRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutRequest
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.*
import java.time.Instant

@HttpExchange(GateRelationApi.RELATIONS_PATH)
interface RelationApiClient: GateRelationApi {

    @GetExchange
    override fun get(
        @RequestParam externalIds: List<String>?,
        @RequestParam relationType: RelationType?,
        @RequestParam businessPartnerSourceExternalIds: List<String>?,
        @RequestParam businessPartnerTargetExternalIds: List<String>?,
        @RequestParam updatedAtFrom: Instant?,
        @ParameterObject @Valid paginationRequest: PaginationRequest
    ): PageDto<RelationDto>

    @PostExchange
    override fun post(@RequestBody requestBody: RelationPostRequest): RelationDto

    @PutExchange
    override fun put(@RequestParam createIfNotExist: Boolean, @RequestBody  requestBody: RelationPutRequest): RelationDto
}