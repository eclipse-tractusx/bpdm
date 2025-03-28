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

package org.eclipse.tractusx.bpdm.gate.controller

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.GateRelationSharingStateApi
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateType
import org.eclipse.tractusx.bpdm.gate.service.RelationSharingStateService
import org.eclipse.tractusx.bpdm.gate.util.PrincipalUtil
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class RelationSharingStateController(
    private val relationSharingStateService: RelationSharingStateService,
    private val principalUtil: PrincipalUtil
): GateRelationSharingStateApi {

    override fun get(
        externalIds: Collection<String>?,
        sharingStateTypes: Collection<RelationSharingStateType>?,
        updatedAfter: Instant?,
        paginationRequest: PaginationRequest
    ): PageDto<RelationSharingStateDto> {
        return relationSharingStateService.findSharingStates(
            principalUtil.resolveTenantBpnl(),
            externalIds ?: emptyList(),
            sharingStateTypes ?: emptyList(),
            updatedAfter,
            paginationRequest
        )
    }


}