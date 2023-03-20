/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.controller

import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.api.PoolBusinessPartnerApi
import org.eclipse.tractusx.bpdm.pool.api.model.request.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.ChangelogEntryResponse
import org.eclipse.tractusx.bpdm.pool.config.ControllerConfigProperties
import org.eclipse.tractusx.bpdm.pool.exception.BpdmRequestSizeException
import org.eclipse.tractusx.bpdm.pool.service.PartnerChangelogService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/catena/business-partners")
class BusinessPartnerController(
    private val partnerChangelogService: PartnerChangelogService,
    private val controllerConfigProperties: ControllerConfigProperties
): PoolBusinessPartnerApi {
    override fun getChangelogEntries(
       bpn: Array<String>?,
       modifiedAfter: Instant?,
       @ParameterObject paginationRequest: PaginationRequest
    ): PageResponse<ChangelogEntryResponse> {
        if (bpn != null && bpn.size > controllerConfigProperties.searchRequestLimit) {
            throw BpdmRequestSizeException(bpn.size, controllerConfigProperties.searchRequestLimit)
        }
        return  partnerChangelogService.getChangelogEntriesByBpn(bpn, modifiedAfter, paginationRequest.page, paginationRequest.size)
    }
}