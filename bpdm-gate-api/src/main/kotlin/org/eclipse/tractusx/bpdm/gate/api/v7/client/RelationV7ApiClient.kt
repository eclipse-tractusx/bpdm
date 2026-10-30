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

package org.eclipse.tractusx.bpdm.gate.api.v7.client

import jakarta.validation.Valid
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.ApiCommons
import org.eclipse.tractusx.bpdm.gate.api.v7.GateRelationV7Api
import org.eclipse.tractusx.bpdm.gate.api.v7.model.RelationV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.request.RelationPutV7Request
import org.eclipse.tractusx.bpdm.gate.api.v7.model.request.RelationSearchV7Request
import org.eclipse.tractusx.bpdm.gate.api.v7.model.response.RelationPutV7Response
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.service.annotation.PutExchange

@HttpExchange
interface RelationV7ApiClient: GateRelationV7Api {

    @PostExchange(value = "${ApiCommons.RELATIONS_INPUT_PATH_V7}/search")
    override fun postSearch(
        @RequestBody searchRequest: RelationSearchV7Request,
        @ParameterObject @Valid paginationRequest: PaginationRequest
    ): PageDto<RelationV7Dto>

    @PutExchange(value = ApiCommons.RELATIONS_INPUT_PATH_V7)
    override fun put(@RequestParam createIfNotExist: Boolean, @RequestBody requestBody: RelationPutV7Request): RelationPutV7Response
}