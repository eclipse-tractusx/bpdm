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

package org.eclipse.tractusx.bpdm.pool.api.v6.client

import io.swagger.v3.oas.annotations.Parameter
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.ApiCommons
import org.eclipse.tractusx.bpdm.pool.api.v6.PoolSiteV6Api
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteCreateWithLegalAddressAsMainV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SitePartnerCreateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SitePartnerCreateV6ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SitePartnerUpdateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SitePartnerUpdateV6ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteSearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteWithMainAddressVerboseV6Dto
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.service.annotation.PutExchange

@HttpExchange
interface SiteV6ApiClient: PoolSiteV6Api {

    @PostExchange(value = ApiCommons.SITE_BASE_PATH_V6)
    override fun createSite(
        @RequestBody requests: Collection<SitePartnerCreateV6Request>
    ): SitePartnerCreateV6ResponseWrapper

    @PostExchange(value = "${ApiCommons.SITE_BASE_PATH_V6}/legal-main-sites")
    override fun createSiteWithLegalReference(
        @RequestBody request: Collection<SiteCreateWithLegalAddressAsMainV6Request>
    ): SitePartnerCreateV6ResponseWrapper

    @PutExchange(value = ApiCommons.SITE_BASE_PATH_V6)
    override fun updateSite(
        @RequestBody requests: Collection<SitePartnerUpdateV6Request>
    ): SitePartnerUpdateV6ResponseWrapper

    @PostExchange(value = "${ApiCommons.SITE_BASE_PATH_V6}/search")
    override fun postSiteSearch(
        @RequestBody searchRequest: SiteSearchV6Request,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<SiteWithMainAddressVerboseV6Dto>

    @GetExchange(value = ApiCommons.SITE_BASE_PATH_V6)
    override fun getSites(
        @ParameterObject searchRequest: SiteSearchV6Request,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<SiteWithMainAddressVerboseV6Dto>

    @GetExchange(value = "${ApiCommons.SITE_BASE_PATH_V6}/{bpns}")
    override fun getSite(
        @Parameter(description = "BPNS value") @PathVariable bpns: String
    ): SiteWithMainAddressVerboseV6Dto
}