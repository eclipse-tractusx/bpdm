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

import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteWithMainAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.SiteResponseMapper
import org.eclipse.tractusx.bpdm.pool.model.request.SiteGetRequest
import org.eclipse.tractusx.bpdm.pool.service.operation.site.SiteGetService
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteGetParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The REST-API boundary for the V7 "get site" operation.
 */
@Service
class SiteGetApplicationV7Service(
    private val siteGetParser: SiteGetParser,
    private val siteGetService: SiteGetService,
    private val siteResponseMapper: SiteResponseMapper
) {

    /**
     * Returns the site with the given BPN and fails with a not-found error when no site carries it.
     */
    @Transactional(readOnly = true)
    fun getSite(bpns: String): SiteWithMainAddressVerboseDto {
        val criteria = siteGetParser.parse(SiteGetRequest(bpns))
        val site = siteGetService.get(criteria) ?: throw BpdmNotFoundException("Site", criteria.siteBpn)

        return siteResponseMapper.toSiteWithMainAddress(site)
    }
}
