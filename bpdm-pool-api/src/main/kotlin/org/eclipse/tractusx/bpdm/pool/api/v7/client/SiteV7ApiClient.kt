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

package org.eclipse.tractusx.bpdm.pool.api.v7.client

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.ApiCommons
import org.eclipse.tractusx.bpdm.pool.api.v7.PoolSiteV7Api
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteCreateWithLegalAddressAsMainV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SitePartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SitePartnerCreateV7ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SitePartnerUpdateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SitePartnerUpdateV7ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteSearchV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteWithMainAddressVerboseV7Dto
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.service.annotation.PutExchange

@HttpExchange
interface SiteV7ApiClient : PoolSiteV7Api {
    @PostExchange(value = ApiCommons.SITE_BASE_PATH_V7)
    override fun createSite(
        @RequestBody requests: Collection<SitePartnerCreateV7Request>
    ): SitePartnerCreateV7ResponseWrapper

    @GetExchange(value = "${ApiCommons.SITE_BASE_PATH_V7}/{bpns}")
    override fun getSite(
        @PathVariable bpns: String
    ): SiteWithMainAddressVerboseV7Dto

    @GetExchange(value = ApiCommons.SITE_BASE_PATH_V7)
    override fun getSites(
        @ParameterObject searchRequest: SiteSearchV7Request,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<SiteWithMainAddressVerboseV7Dto>

    @PostExchange(value = "${ApiCommons.SITE_BASE_PATH_V7}/search")
    override fun postSiteSearch(
        @RequestBody searchRequest: SiteSearchV7Request,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<SiteWithMainAddressVerboseV7Dto>

    @PutExchange(value = ApiCommons.SITE_BASE_PATH_V7)
    override fun updateSite(
        @RequestBody requests: Collection<SitePartnerUpdateV7Request>
    ): SitePartnerUpdateV7ResponseWrapper

    @PostExchange(value = "${ApiCommons.SITE_BASE_PATH_V7}/legal-main-sites")
    override fun createSiteWithLegalReference(
        @RequestBody request: Collection<SiteCreateWithLegalAddressAsMainV7Request>
    ): SitePartnerCreateV7ResponseWrapper

}