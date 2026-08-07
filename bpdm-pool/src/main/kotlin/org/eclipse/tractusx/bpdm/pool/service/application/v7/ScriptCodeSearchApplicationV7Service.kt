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

package org.eclipse.tractusx.bpdm.pool.service.application.v7

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.pool.api.model.ScriptCodeDto
import org.eclipse.tractusx.bpdm.pool.service.operation.ScriptCodeSearchService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The REST-API boundary for the V7 "search script codes" operation.
 */
@Service
class ScriptCodeSearchApplicationV7Service(
    private val scriptCodeSearchService: ScriptCodeSearchService
) {

    /**
     * Returns the requested page of all known script codes.
     */
    @Transactional(readOnly = true)
    fun searchScriptCodes(paginationRequest: PaginationRequest): PageDto<ScriptCodeDto> =
        scriptCodeSearchService.search(paginationRequest.toPageRequest())
            .toPageDto { ScriptCodeDto(it.technicalKey, it.description) }
}
