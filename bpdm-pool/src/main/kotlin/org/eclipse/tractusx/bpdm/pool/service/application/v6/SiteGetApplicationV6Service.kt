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

package org.eclipse.tractusx.bpdm.pool.service.application.v6

import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SiteWithMainAddressVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.SiteResponseMapperV6
import org.eclipse.tractusx.bpdm.pool.model.request.SiteGetRequest
import org.eclipse.tractusx.bpdm.pool.service.operation.site.SiteGetService
import org.eclipse.tractusx.bpdm.pool.service.parser.SiteGetParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The REST-API boundary for the legacy v6 "get site" operation, using the v6 response shape.
 */
@Service
class SiteGetApplicationV6Service(
    private val siteGetParser: SiteGetParser,
    private val siteGetService: SiteGetService,
    private val siteResponseMapperV6: SiteResponseMapperV6
) {

    /**
     * Returns the site with the given BPN and fails with a not-found error when no site carries it.
     */
    @Transactional(readOnly = true)
    fun getSite(bpns: String): SiteWithMainAddressVerboseDtoV6 {
        val criteria = siteGetParser.parse(SiteGetRequest(bpns))
        val site = siteGetService.get(criteria) ?: throw BpdmNotFoundException("Site", criteria.siteBpn)

        return siteResponseMapperV6.toSiteWithMainAddress(site)
    }
}
