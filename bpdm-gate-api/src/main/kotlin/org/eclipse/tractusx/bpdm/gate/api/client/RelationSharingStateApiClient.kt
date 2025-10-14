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

package org.eclipse.tractusx.bpdm.gate.api.client

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.ApiCommons
import org.eclipse.tractusx.bpdm.gate.api.GateRelationSharingStateApi
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateType
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import java.time.Instant

@HttpExchange
interface RelationSharingStateApiClient: GateRelationSharingStateApi {

    @GetExchange(value = ApiCommons.RELATION_SHARING_STATE_PATH_V7)
    override fun get(
        @RequestParam(required = false) externalIds: Collection<String>?,
        @RequestParam(required = false) sharingStateTypes: Collection<RelationSharingStateType>?,
        @RequestParam(required = false) updatedAfter: Instant?,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<RelationSharingStateDto>

}