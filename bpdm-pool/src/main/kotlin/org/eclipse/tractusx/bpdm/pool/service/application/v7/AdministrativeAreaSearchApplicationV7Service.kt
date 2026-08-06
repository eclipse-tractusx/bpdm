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
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.pool.api.model.CountrySubdivisionDto
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.AdministrativeAreaResponseMapper
import org.eclipse.tractusx.bpdm.pool.service.operation.AdministrativeAreaSearchService
import org.eclipse.tractusx.bpdm.pool.service.toDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The REST-API boundary for the V7 "search administrative areas level 1" operation.
 */
@Service
class AdministrativeAreaSearchApplicationV7Service(
    private val administrativeAreaSearchService: AdministrativeAreaSearchService,
    private val administrativeAreaResponseMapper: AdministrativeAreaResponseMapper
) {

    /**
     * Returns the requested page of all known administrative areas level 1.
     */
    @Transactional(readOnly = true)
    fun searchAdministrativeAreas(paginationRequest: PaginationRequest): PageDto<CountrySubdivisionDto> =
        administrativeAreaSearchService.search(paginationRequest.toPageRequest())
            .toDto { administrativeAreaResponseMapper.toCountrySubdivision(it) }
}
